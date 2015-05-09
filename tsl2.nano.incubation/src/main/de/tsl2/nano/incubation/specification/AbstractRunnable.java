package de.tsl2.nano.incubation.specification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.bean.def.Constraint;
import de.tsl2.nano.execution.IPRunnable;

public abstract class AbstractRunnable<T> implements IPRunnable<T, Map<String, Object>> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    @Attribute
    protected String name;
    @ElementMap(entry = "parameter", attribute = true, inline = true, keyType = String.class, key = "name", valueType = ParType.class, value = "type")
    protected LinkedHashMap<String, ParType> parameter;
    @ElementMap(entry = "constraint", attribute = true, inline = true, keyType = String.class, key = "name", value = "definition", valueType = Constraint.class, required = false)
    Map<String, Constraint<?>> constraints;
    @Element
    protected String operation;

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

    /**
     * checks arguments against defined parameter and returns new map of arguments, defined by parameters.
     * 
     * @param arguments to be checked and collected
     * @param strict if true, arguments not defined as parameter will throw an {@link IllegalArgumentException}.
     */
    @Override
    public Map<String, Object> checkedArguments(Map<String, Object> arguments, boolean strict) {
        Map<String, Object> args = new LinkedHashMap<String, Object>();
        Set<String> keySet = arguments.keySet();
        Set<String> defs = parameter.keySet();
        for (String par : defs) {
            if (strict && !keySet.contains(par)) {
                throw new IllegalArgumentException(par);
            }
            Object arg = arguments.get(par);
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
        List<Class<?>> pars = new ArrayList<Class<?>>(parameter.size());
        if (parameter == null) {
            return new ArrayList<Class<?>>(0);
        }

        for (ParType par : parameter.values()) {
            pars.add(par.getType());
        }
        return pars;
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

    @Commit
    protected void initDeserializing() {
        createConstraints();
    }
}