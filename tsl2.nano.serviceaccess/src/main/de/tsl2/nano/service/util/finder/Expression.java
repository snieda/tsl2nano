package de.tsl2.nano.service.util.finder;

import java.util.Arrays;
import java.util.Collection;

/**
 * additional query finder. use that, if the standard finders don't match your constraints.
 * 
 * @param <T> result bean type
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Expression<T> extends AbstractFinder<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -6987663981295488159L;

    String expression;
//    boolean asSubSelect = false;

    /**
     * constructor
     * 
     * @param queryString
     * @param asSubSelect
     * @param args
     * @param relationsToLoad
     */
    public Expression(String queryString, boolean asSubSelect, Object[] args, Class<Object>... relationsToLoad) {
        this(null, queryString, asSubSelect, args, relationsToLoad);
    }

    /**
     * constructor
     * 
     * @param resultType
     * @param queryString
     * @param asSubSelect
     * @param args
     * @param relationsToLoad
     */
    public Expression(Class<T> resultType,
            String queryString,
            boolean asSubSelect,
            Object[] args,
            Class<Object>... relationsToLoad) {
        super(resultType, relationsToLoad);
        expression = queryString;
//        this.asSubSelect = asSubSelect;
        if (args != null)
            par = Arrays.asList(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    StringBuffer createQuery(StringBuffer currentQuery,
            Collection<Object> parameter,
            Collection<Class<Object>> lazyRelations) {
        currentQuery.append("\n" + getConnector(currentQuery) + " (" + expression + ")");
        return currentQuery;
    }
}