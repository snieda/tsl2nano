/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 05.01.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.bean.annotation.Action;
import de.tsl2.nano.bean.annotation.ConstraintValueSet;
import de.tsl2.nano.bean.annotation.Presentable;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.format.RegExpFormat;

/**
 * uses the {@link IAction} to call a {@link Method}. The method can be enriched through annotations {@link Action} and
 * {@link Constraint}. The first method argument value (see setParameters()) may be the instance on invoking the method. 
 * So be careful setting the parameters and method arguments.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class MethodAction<T> extends CommonAction<T> implements IsPresentable {
    public static final String DEFAULT_ICON = "icons/go.png";

	/** serialVersionUID */
    private static final long serialVersionUID = -8818848476107346589L;

    @Element
    Class declaringClass;
    @Attribute
    String methodName;
    @Element(required=false)
    Class[] parameterTypes;
    transient Method method;
    transient Constraint[] constraints;

	private IPresentable presentable;

    static final String ACTION_PREFIX = "action";

    public MethodAction() {
    }

    /**
     * constructor
     * 
     * @param method
     */
    public MethodAction(Method method) {
        super(createId(method), createName(method),
            Messages.getStringOpt(method.toGenericString()));
        setMethod(method);
        String imagePath = getId() + ".icon";
        if (Messages.hasKey(imagePath)) {
            setImagePath(Messages.getString(imagePath));
        } else {
            setImagePath(DEFAULT_ICON);
        }
    }

    void setMethod(Method method) {
        this.method = method;
        this.declaringClass = method.getDeclaringClass();
        this.methodName = method.getName();
        this.parameterTypes = method.getParameterTypes();
    }

    /**
     * creates a readable unique method string - cuts method name prefix {@link #ACTION_PREFIX}.
     * 
     * @param m method
     * @return method id
     */
    static final String createId(Method m) {
        final String cls = m.getDeclaringClass().getSimpleName().toLowerCase();
        final String name = m.getName().substring(ACTION_PREFIX.length());
        return cls + "." + name.toLowerCase();
    }

    private static String createName(Method method) {
        final String name = method.getName().substring(ACTION_PREFIX.length());
        return ENV.translate(name, true);
    }

    protected Method method() {
    	if (method == null)
    		initDeserializing();
    	return method;
    }
    @Override
    public T action() throws Exception {
    	if (!hasInstanceAsFirstParameter())
    		throw ManagedException.implementationError("First parameter of method " + this + " must be invoking instance!", getInstance(), parameters());
        Object[] args = getArgumentValues();
        return (T) method().invoke(getInstance(), args);
    }

	private Object getInstance() {
		return getParameter()[0];
	}

	@Override
	public void setParameter(Object... parameter) {
		super.setParameter(parameter);
		if (parameters().getTypes() == null && getConstraints() != null)
			parameters().setConstraints(CollectionUtil.concat(new Constraint[] {new Constraint<>(Object.class)}, getConstraints()));
	}
	
	private Object[] getArgumentValues() {
		return Arrays.copyOfRange(getParameter(), 1, getParameter().length);
	}

    /**
     * reads {@link Method} annotations {@link Action} and {@link Constraint} to create method argument constraints
     * 
     * @return method argument constraints
     */
    public Constraint[] getConstraints() {
        if (constraints == null && method().isAnnotationPresent(Action.class)) {
            Action annAction = method().getAnnotation(Action.class);
            if (!annAction.name().isEmpty())
                shortDescription = ENV.translate(annAction.name(), true);
            Annotation[][] cana;
            if ((cana = method.getParameterAnnotations()) != null) {
                constraints = new Constraint[method.getParameterTypes().length];
                //on each parameter, the constraint annotation has to be the first one
                de.tsl2.nano.bean.annotation.Constraint c;
                for (int i = 0; i < cana.length; i++) {
                    if (cana[i].length > 0) {
                        c = (de.tsl2.nano.bean.annotation.Constraint) cana[i][0];
                        //IMPROvE: allowed are only strings...
                        String[] allowed = ConstraintValueSet.preDefined(c.allowed());
                        constraints[i] =
                            new Constraint(c.type() != Object.class ? c.type() : getArgumentTypes()[i],
                                allowed.length > 0 ? allowed : null);
                        constraints[i].defaultValue = !c.defaultValue().isEmpty() ? c.defaultValue() : null;
                        constraints[i].format =
                            !c.pattern().isEmpty() ? c.length() > -1 ? new RegExpFormat(c.pattern(), c.length())
                                : new RegExpFormat(c.pattern(), null) : null;
                        constraints[i].length = c.length();
                        constraints[i].min = !c.min().isEmpty() ? c.min() : null;
                        constraints[i].max = !c.max().isEmpty() ? c.max() : null;
                    } else {//if no constraint annotation was defined for a parameter, add the default
                    	if (i == method.getParameterCount() - 1 && method.getParameterTypes()[i].isArray())
                    		break; //last optional parameter
                    	constraints[i] = new Constraint<>(method.getParameterTypes()[i]);
                    	if (constraints[i].getType().isPrimitive())
                    		constraints[i].setDefault(PrimitiveUtil.getDefaultValue(constraints[i].getType()));
                    }
                }
            }
        }
        return constraints;
    }

    @Override
    public Class[] getArgumentTypes() {
        return method().getParameterTypes();
    }

    /**
     * evaluates unique method argument names either through its parameter types or {@link Action#argNames()}.
     * 
     * @return unique argument names
     */
    public String[] getArgumentNames() {
        Action ana;
        if (method().isAnnotationPresent(Action.class)
            && (ana = method.getAnnotation(Action.class)).argNames().length > 0) {
        	if (ana.argNames().length != getArgumentTypes().length)
        		throw ManagedException.implementationError("The annotation " + ana + " must define exactly the argument-names for the method " + method(), ana.argNames(), getArgumentTypes());
            return ana.argNames();
        } else {
            return getValidMethodParameterNames();
        }
    }

	private String[] getValidMethodParameterNames() {
		if (getArgumentTypes().length == 0)
			return new String[0];
		else if (parameters() != null && parameters().size() == getArgumentTypes().length)
			return parameters().getNames();
		else if (hasInstanceAsFirstParameter()) {
			String[] names = parameters().getNames();
			return Arrays.copyOfRange(names, 1, names.length);
		} else {
			return getIdNames(getArgumentTypes());
		}
	}

	private boolean hasInstanceAsFirstParameter() {
		return parameters() != null && parameters().size() == getArgumentTypes().length + 1;
	}

    /**
     * convenience for any parametrized action to evaluate its argument names
     * 
     * @param action any action
     * @return default argument names or - if annotations found - defined argument names
     */
    public static final String[] getArgumentNames(IAction action) {
        return action instanceof MethodAction ? ((MethodAction) action).getArgumentNames()
            : action instanceof CommonAction ? ((CommonAction)action).parameters().getNames() : getIdNames(action.getArgumentTypes());
    }

    /**
     * uses names of given argument classes. if not unique, adds a counter to be unique.
     * 
     * @param argumentTypes
     * @return unique argument names
     */
    public static final String[] getIdNames(Class[] argumentTypes) {
        ArrayList<String> argNames = new ArrayList<String>(argumentTypes.length);
        String name;
        int i = 0;
        for (Class cls : argumentTypes) {
            name = cls.getSimpleName();
            name = argNames.contains(name) ? name + ++i : name;
            argNames.add(name);
        }
        return argNames.toArray(new String[0]);
    }

    /**
     * materializes a {@link Method} through the given arguments.
     * 
     * @param declaringClass methods declaring class
     * @param name method name
     * @param pars method parameters
     * @return instance of {@link Method}
     */
    static Method getMethod(Class declaringClass, String name, Class... pars) {
        try {
            return declaringClass.getMethod(name, pars);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public BeanDefinition toBean() {
    	return toBean(Bean.UNDEFINED);
    }
    
    /**
     * creates a new bean through informations of argument names and constraints
     * 
     * @return
     */
    public BeanDefinition toBean(Object instance) {
        Bean b = new Bean();
        b.setInstance(instance);
        b.setName(getShortDescription());
        String[] args = getArgumentNames();
        Constraint[] c = getConstraints();
        if (c != null && args.length != c.length)
        	ManagedException.implementationError("The annotated argNames do not relate to the real methods arguments", StringUtil.toString(args, -1));
        for (int i = 0; i < args.length; i++) {
            b.addAttribute(new BeanValue<T>(new ValueHolder(null), args[i], c != null && c.length > i &&  c[i] != null ? c[i] : new Constraint(getArgumentTypes()[i])));
        }
        b.addAction(this);
        b.setDefault(false);
        return b;
    }

    @Commit
    private void initDeserializing() {
        method = getMethod(declaringClass, methodName, parameterTypes);
    }

	@Override
	public IPresentable getPresentable() {
		if (presentable == null) {
			if (method().isAnnotationPresent(Presentable.class)) {
				presentable = de.tsl2.nano.bean.def.Presentable.createPresentable(method().getAnnotation(Presentable.class));
			}
		}
		return presentable;
	}
}
