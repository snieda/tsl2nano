package de.tsl2.nano.service.util.finder;

import java.util.Collection;

/**
 * additional order by statement creator
 * 
 * @param <T> result bean type
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class OrderBy<T> extends AbstractFinder<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -6987663981295488159L;

    String[] attributeNames;
    public OrderBy(String[] attributeNames) {
        this(null, attributeNames);
    }

    /**
     * constructor
     * 
     * @param attributeNames, for ascending use prefix +. for descendings prefix -. default is ascending.
     */
    public OrderBy(Class<T> resultType, String[] attributeNames) {
        super(resultType);
        this.attributeNames = attributeNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    StringBuffer prepareQuery(int index,
            StringBuffer currentQuery,
            Collection<Object> parameter,
            Collection<Class<Object>> lazyRelations) {
        //do nothing
        return currentQuery;
    }

    @Override
    StringBuffer createQuery(StringBuffer currentQuery,
            Collection<Object> parameter,
            Collection<Class<Object>> lazyRelations) {
        return currentQuery.append(createOrderByPostfix( (String[]) attributeNames));
    }
    
    static String createOrderByPostfix(String[] attributeNames) {
        StringBuffer buf = new StringBuffer(8 + attributeNames.length * 13);
        buf.append("\n order by");
        String n;
        for (int i = 0; i < attributeNames.length; i++) {
            n = attributeNames[i].startsWith("-") ? attributeNames[i].substring(1) + " desc"
                : attributeNames[i].startsWith("+") ? attributeNames[i].substring(1) + " asc" : attributeNames[i];
            buf.append(" " + n + ",");
        }
        return buf.substring(0, buf.length() - 1);
    }
}