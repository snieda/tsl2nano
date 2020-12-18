package de.tsl2.nano.incubation.specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.Constraint;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.execution.IPRunnable;

/**
 * runnable checking its arguments against constraints. provides checks against a specification/assertions. if member
 * operation is an url (e.g. starting with file:) the url content will be used as operation.
 * 
 * @param <T> result type
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public abstract class AbstractRunnable<T> implements IPRunnable<T, Map<String, Object>>, IPrefixed {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(AbstractRunnable.class);
    @Attribute
    protected String name;
    @ElementMap(entry = "parameter", attribute = true, inline = true, keyType = String.class, key = "name", valueType = ParType.class, value = "type", required = false)
    protected LinkedHashMap<String, ParType> parameter;
    @ElementMap(entry = "constraint", attribute = true, inline = true, keyType = String.class, key = "name", value = "definition", valueType = Constraint.class, required = false)
    protected Map<String, Constraint<?>> constraints;
    @ElementList(required = false, inline = true, type = Specification.class)
    protected Collection<Specification> specifications;
    @Element
    protected String operation;
    transient protected String operationContent;

    /**
     * add this key to your arguments, if you want to fill the parameter not through key-names but through a sequence
     */
    static final String KEY_ARGUMENTS_AS_SEQUENCE = "arguments as sequence";

    public AbstractRunnable() {
        super();
    }

    /**
     * constructor
     * 
     * @param operation
     * @param parameter
     */
    public AbstractRunnable(String name, String operation, LinkedHashMap<String, ParType> parameter) {
        this.name = name;
        this.operation = operation;
        this.parameter = parameter;
        initDeserializing();
    }

    /**
     * @return Returns the name.
     */
    @Override
    public String getName() {
        return name;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Map<String, ParType> createParameters(Class declaringClass) {
        BeanDefinition<?> def = BeanDefinition.getBeanDefinition(declaringClass);
        List<IAttribute> attrs = def.getAttributes(false);
        Map<String, ParType> par = new LinkedHashMap<>();
        for (IAttribute a : attrs) {
            par.put(a.getName(), new ParType(a.getType()));
        }
        return par;
    }

    public static LinkedHashMap<String, ParType> parameters(String... names) {
        return (LinkedHashMap<String, ParType>) createSimpleParameters(names);
    }

    protected static Map<String, ParType> createSimpleParameters(String... names) {
        Map<String, ParType> par = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            par.put(names[i], new ParType(Object.class));
        }
        return par;
    }

    /**
     * checks arguments against defined parameter and returns new map of arguments, defined by parameters. Use
     * {@link #KEY_ARGUMENTS_AS_SEQUENCE} to define a sequential iteration of arguments (without using key names)
     * 
     * @param arguments to be checked and collected
     * @param strict if true, arguments not defined as parameter will throw an {@link IllegalArgumentException}.
     */
    @Override
    public Map<String, Object> checkedArguments(Map<String, Object> arguments, boolean strict) {
        boolean asSequence = asSequence(arguments);
        Map<String, Object> args = new LinkedHashMap<String, Object>();
        Set<String> keySet = arguments.keySet();
        Set<String> defs = parameter != null ? parameter.keySet() : arguments.keySet();
        Iterator<Object> values = arguments.values().iterator();
        Object arg;
        for (String par : defs) {
            if (asSequence) {
                arg = values.next();
                if (arg == null && parameter != null)
                    arg = parameter.get(par).getDefaultValue();
            } else {
                if (!keySet.contains(par)) {
                    arg = parameter != null ? parameter.get(par).getDefaultValue() : null;
                    if (arg == null && strict)
                        throw new IllegalArgumentException(par);
                } else {
                    arg = arguments.get(par);
                }
            }
            checkConstraint(par, arg);
            args.put(par, arg);
        }
        return args;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void createConstraints() {
        if (constraints == null) {
            constraints = new HashMap<String, Constraint<?>>();
        }
        if (parameter == null)
            return;
        Set<String> pars = parameter.keySet();
        for (CharSequence p : pars) {
            Class<?> cls = parameter.get(p).getType();
            Constraint constraint = constraints.get(p);
            if (constraint == null) {
                constraint = new Constraint(cls);
            } else {
                constraint.setType(cls);
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void checkConstraint(CharSequence par, Object arg) {
        if (constraints == null)
            createConstraints();
        Constraint constraint = constraints.get(par);
        if (constraint != null) {
            constraint.check(getName(), arg);
        }
    }

    /**
     * getParameter
     * 
     * @return defined rule parameters
     */
    @Override
    public Map<String, ParType> getParameter() {
        return parameter;
    }

    /**
     * getParameterList
     * 
     * @return real parameters
     */
    public List<Class<?>> getParameterList() {
        if (parameter == null) {
            return new ArrayList<Class<?>>(0);
        }
        List<Class<?>> pars = new ArrayList<Class<?>>(parameter.size());
        for (ParType par : parameter.values()) {
            pars.add(par.getType());
        }
        return pars;
    }

    /**
     * prefix
     * 
     * @return referencing name prefix
     */
    public String prefix() {
        return "";
    }

    @Override
    public String toString() {
        return name + "{" + operation + "}";
    }

    /**
     * addConstraint
     * 
     * @param parameterName parameter name to add the constraint for
     * @param constraint new constraint
     */
    public void addConstraint(String parameterName, Constraint<?> constraint) {
        constraints.put(parameterName, constraint);
    }

    public Specification addSpecification(String name,
            String description,
            Object exptected,
            Map<String, Object> arguments) {
        return addSpecification(new Specification(name, description, exptected, arguments));
    }

    /**
     * define test scenarios to be checked while loading by a factory. if no parameter were defined yet, the given
     * argments will be used to define the parameters.
     * 
     * @param s specification
     * @return new specification instance
     */
    public Specification addSpecification(Specification s) {
        if (specifications == null)
            specifications = new LinkedList<Specification>();
        specifications.add(s);
        if (parameter == null) {
            parameter = new LinkedHashMap<String, ParType>();
            Map<String, Object> args = s.getArguments();
            for (String k : args.keySet()) {
                Object value = args.get(k);
                parameter.put(k, new ParType(value != null ? BeanClass.getDefiningClass(value.getClass())
                    : String.class));
            }
        }
        checkSpecifications();
        return s;
    }

    /**
     * tests the defined specifications. at least boundary conditions should be defined as specifications/assertions.
     * helps to test a new/changed rule against expected values. throws an {@link IllegalStateException} on unexpected
     * results.
     */
    void checkSpecifications() {
        if (specifications != null) {
            for (Specification s : specifications) {
                checkSpecification(s);
            }
            LOG.info("rule " + getName() + " loaded and checked against " + specifications.size() + " specifications");
        } else {
            LOG.warn("rule '" + getName() + "' didn't define any specification to be tested against!");
        }
    }

    /**
     * checkSpecification
     * 
     * @param s
     */
    protected void checkSpecification(Specification s) {
        LOG.debug("checking rule '" + getName() + " for specification " + s);
        T result = run(s.getArguments());
        if (result != null && (!result.equals(s.getExptected()))) {
            // script engines like nashorn and graalvm differ in handling number types, so this is a workaround for different number types
            if (!((result instanceof Number) && NumberUtil.roundAbout((Number)result, (Number)s.getExptected())))
                throw new IllegalStateException("assertion failed on rule " + getName() + ", " + s + ": expected="
                    + s.getExptected()
                    + " , but was: " + result);
        }
    }

    public String getOperation() {
        if (operationContent == null && operation != null) {
            if (NetUtil.isURL(operation))
                operationContent = new String(FileUtil.getFileBytes(operation, null));
            else
                operationContent = operation;
        }
        return operationContent;
    }

    public void setName(String name) {
    	this.name = name;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
        operationContent = null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void markArgumentsAsSequence(Map args) {
        args.put(KEY_ARGUMENTS_AS_SEQUENCE, Boolean.TRUE);
    }
    
    public static boolean asSequence(Map<String, Object> arguments) {
        return arguments.containsKey(KEY_ARGUMENTS_AS_SEQUENCE) && arguments.remove(KEY_ARGUMENTS_AS_SEQUENCE) != null;
    }

    @Commit
    protected void initDeserializing() {
        createConstraints();
        if (ENV.get("rule.check.specifications", true))
            checkSpecifications();
    }
}