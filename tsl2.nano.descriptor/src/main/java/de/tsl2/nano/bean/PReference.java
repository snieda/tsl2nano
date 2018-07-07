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

    transient String path;
    
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

	private String path(String description) {
		String path = StringUtil.substring(description, PREFIX_PATH, null);
		//if path starts with the object type itself, we eliminate that
		if (path != null && path.toLowerCase().startsWith(type(description).getSimpleName().toLowerCase()))
			path = StringUtil.substring(path, ".", null);
		return path;
	}
	
	/**
	 * throws an unchecked exception on path error
	 */
	protected void checkDescription(String description) {
		super.checkDescription(description);
		Class<O> type = type(description);
		checkPath(type, 0, path(description).split("\\."));
	}
	
	void checkPath(Class<?> type, int attrIndex, String...spath) {
		BeanAttribute<?> beanAttr = BeanAttribute.getBeanAttribute(type, StringUtil.substring(spath[attrIndex], null, "["));
		if (attrIndex < spath.length-1) {
			Class<?> genericType = beanAttr.getGenericType();
			checkPath(genericType != null ? genericType : beanAttr.getType(), ++attrIndex, spath);
		}
	}
	
	protected static String createDescription(String type, Object id, Object path) {
		return AReference.createDescription(BeanClass.load(type), id) + PREFIX_PATH + path;
	}
	
	@SuppressWarnings("unchecked")
	public IValueAccess<V> getValueAccess() {
		return ValuePath.getValueAccess(resolve(), getPath().split("[.]"));
	}
}
