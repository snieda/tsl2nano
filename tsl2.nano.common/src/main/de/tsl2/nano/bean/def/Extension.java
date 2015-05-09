/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 01.03.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.util.PrivateAccessor;
import de.tsl2.nano.util.operation.IConverter;

/**
 * Workaround for de-/serializings through simple-xml. while you must know the type of your deserializing root element,
 * it is not possible, to load any class-extensions.
 * <p/>
 * This class can be used as member of your base-class. after deserializing you can create the desired extension
 * instance through informations of this extension-instance.
 * <p/>
 * USE:
 * 
 * <pre>
 * - mark all extending members with annotation {@link Transient} (not with javas keyword 'transient')
 * - on serialization of your extension class, call {@link #Extension(Object)} in your method annotated with {@link Commit}.
 *     {AT}Persist
 *     protected void initSerialization() {
 *         extension = new Extension(this);
 *         if (extension.isEmpty())
 *             extension = null;
 *     }
 * - on de-serialization, call {@link #to(Object)} getting the desired instance.
 * </pre>
 * 
 * @author Tom
 * @version $Revision$
 */
public class Extension<BASE, EXT> implements Serializable, IConverter<BASE, EXT> {
    /** serialVersionUID */
    private static final long serialVersionUID = -3429339914632368033L;

    /**
     * while there are problems on simple-xml serializing an own extension of hashmap, we use it only as member-variable
     */
    @ElementMap(entry = "member", attribute = true, inline = true, required = false, empty = true, keyType = String.class, key = "name")
    private LinkedHashMap<String, Object> members;

    /** only used to check type by generics */
    protected transient Class<BASE> baseClass;
    /** extension class - has to have a default constructor */
    @Attribute
    protected Class<EXT> declaringClass;

    /**
     * constructor
     */
    protected Extension() {
        super();
    }

    /**
     * constructor
     * 
     * @param baseClass
     * @param declaringClass
     */
    protected Extension(Class<BASE> baseClass, Class<EXT> declaringClass) {
        super();
        this.baseClass = baseClass;
        this.declaringClass = declaringClass;
    }

    /**
     * evaluates the given instance and stores all found members, having the Annotation {@link Transient} but is
     * implementing {@link Serializable}.
     * 
     * @param extInstance instance to prepare for serialization
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Extension(EXT extInstance) {
        declaringClass = (Class<EXT>) extInstance.getClass();
        PrivateAccessor extAcc = new PrivateAccessor(extInstance);
        List<String> members = extAcc.memberNames(Transient.class);
        Object value;
        for (String m : members) {
            value = extAcc.member(m);
            if (value != null && Serializable.class.isAssignableFrom(value.getClass())) {
                members().put(m, value);
            }
        }

    }

    private final LinkedHashMap<String, Object> members() {
        if (members == null) {
            members = new LinkedHashMap<String, Object>();
        }
        return members;
    }

    /**
     * @return Returns the declaringClass.
     */
    protected Class<EXT> getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public BASE from(EXT toValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EXT to(BASE fromValue) {
        EXT ext = BeanClass.createInstance(declaringClass);
        PrivateAccessor<EXT> extAcc = new PrivateAccessor<EXT>(ext);
        BeanUtil.copy(fromValue, ext);
        Set<String> keys = members().keySet();
        for (String member : keys) {
            extAcc.set(member, members().get(member));
        }
        //TODO: eval the right method name having annotation 'Commit'.
        extAcc.call("initDeserialization", Void.class);
        return ext;
    }

    public boolean isEmpty() {
        return members().isEmpty();
    }

}
