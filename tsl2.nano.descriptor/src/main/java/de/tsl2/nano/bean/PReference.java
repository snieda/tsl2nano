/**
 * 
 */
package de.tsl2.nano.bean;

import de.tsl2.nano.core.cls.AReference;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IValueAccess;
import de.tsl2.nano.core.cls.ValuePath;
import de.tsl2.nano.core.util.StringUtil;

/**
 * Extends {@link BReference} to provide an attribute-path, starting from given
 * materialized bean instance. Using {@link BeanClass#getValueAccess(Object, String...)} with
 * our attribute-path we get the right attribute.
 * 
 * @author Tom
 */
public class PReference<O, V> extends BReference<O> {
    protected static final String PREFIX_PATH = "#";

    protected String path;
    
	public PReference() {
		postfixID = PREFIX_PATH;
	}

	
	public PReference(O instance, String path) {
		super(instance);
		setDescription(getDescription() + PREFIX_PATH + path);
	}

	public String getPath() {
		if (path == null && getDescription() != null) {
			path = path(getDescription());
		}
		return path;
	}

	protected String path(String description) {
		if (description == null)
			return null;
		String path = StringUtil.substring(description, PREFIX_PATH, null);
		//if path starts with the object type itself, we eliminate that
		path = trimpath(type(description), path);
		return path;
	}


	protected static String trimpath(Class type, String path) {
		if (path != null && path.toLowerCase().startsWith(type.getSimpleName().toLowerCase()))
			path = StringUtil.substring(path, ".", null);
		else if (path != null && path.startsWith(type.getName()))
			path = StringUtil.substring(path, type.getName() + ".", null);
		return path;
	}
	
	/**
	 * throws an unchecked exception on path error
	 */
	protected void checkDescription(String description) {
		super.checkDescription(description);
		Class<O> type = type(description);
		if (description != null)
			checkPath(type, 0, path(description).split("\\."));
	}
	
	void checkPath(Class<?> type, int attrIndex, String...spath) {
		BeanAttribute<?> beanAttr = BeanAttribute.getBeanAttribute(type, StringUtil.substring(spath[attrIndex], null, "["));
		if (attrIndex < spath.length-1) {
			Class<?> genericType = beanAttr.getGenericType();
			checkPath(genericType != null ? genericType : beanAttr.getType(), ++attrIndex, spath);
		}
	}
	@Override
	protected void setDescription(String description) {
		super.setDescription(description);
		path = null;
	}
	protected static String createDescription(String type, Object id, Object path) {
		
		Class<?> ctype = BeanClass.load(type);
		path = trimpath(ctype, String.valueOf(path));
		return AReference.createDescription(ctype, id) + PREFIX_PATH + path;
	}
	
	@SuppressWarnings("unchecked")
	public IValueAccess<V> getValueAccess() {
		return getDescription() != null ? ValuePath.getValueAccess(resolve(), getPath().split("[.]")) : null;
	}
}
