package de.tsl2.nano.codegen;

import java.sql.Time;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.util.ObjectUtil;

/**
 * helper class for {@linkplain ClassGenerator}
 * 
 * @author ts 07.12.2008
 * @version $Revision: 1.0 $
 * 
 */
public class GeneratorBeanUtility extends GeneratorUtility {
    Properties props = new Properties(System.getProperties());

    /**
     * Constructor
     */
    protected GeneratorBeanUtility() {
        super();
    }

    /**
     * evaluates the type of the given attribute to return the name of the constant to define the component.
     * 
     * if the type is not a simple type (string, date, boolean), the type list will be returned.
     * 
     * this method should only be used in a generating context.
     * 
     * @param attr beanattribute to evaluate
     * @return name of the type-constant (e.g.: 'IFieldDescriptor.TYPE_LIST')
     */
    public String getEditorTypeString(BeanAttribute attr) {
//        AbstractValidator validator = (AbstractValidator)getValidator(attr);
//        if (validator != null && validator.isSelection())
//            return "IFieldDescriptor.TYPE_LIST";

        String type = null;
        if (Time.class.isAssignableFrom(attr.getType())) {
            type = "IFieldDescriptor.TYPE_TIME";
        } else if (Date.class.isAssignableFrom(attr.getType())) {
            type = "IFieldDescriptor.TYPE_DATE";
        } else if (Boolean.class.isAssignableFrom(attr.getType()) || boolean.class.isAssignableFrom(attr.getType())) {
            type = "IFieldDescriptor.TYPE_BOOLEAN";
        } else if (Date.class.isAssignableFrom(attr.getType())) {
            type = "IFieldDescriptor.TYPE_DATE";
        } else if (attr.getType().isArray() || Collection.class.isAssignableFrom(attr.getType())) {//complex type --> list
            type = "IFieldDescriptor.TYPE_LIST_TABLE";
        } else if (ObjectUtil.isStandardType(attr.getType())) {
            type = "IFieldDescriptor.TYPE_TEXT";
        } else {//complex type --> combo box list
            type = "IFieldDescriptor.TYPE_LIST";
        }
        return type;
    }

    /**
     * Returns the "value" of the mandatory parameter for an BeanAttribute.
     * 
     * @param attr bean attribute
     * @return "true" if mandatory, or "false" if not
     */
    public String getMandatoryString(BeanAttribute attr) {
        return Boolean.toString(attr.getType().isPrimitive());
    }

    /**
     * override this method to create the desired validators
     * 
     * @param attr bean attribute to evaluate
     * @return mandatory validator or "null"
     */
    public String getValidator(BeanAttribute attr) {
        final boolean isPrimitive = attr.getType().isPrimitive() && !boolean.class.isAssignableFrom(attr.getType());
        if (isPrimitive) {
            return "new MandatoryValidator(\"" + attr.getName() + "\", false)";
        } else {
            return "null";
        }
    }

    /**
     * override this method to create the desired formatters
     * 
     * @param attr bean attribute to evaluate
     * @return number formatter or "null"
     */
    public String getFormatter(BeanAttribute attr) {
        //create a max number formatter
        if (Number.class.isAssignableFrom(attr.getType()) || double.class.isAssignableFrom(attr.getType())
            || float.class.isAssignableFrom(attr.getType())) {
            return "RegExpFormat.createNumberRegExp(10, 4)";
        } else if (int.class.isAssignableFrom(attr.getType()) || long.class.isAssignableFrom(attr.getType())
            || short.class.isAssignableFrom(attr.getType())) {
            return "RegExpFormat.createNumberRegExp(10, 4)";
        } else {
            return "null";
        }
    }

}
