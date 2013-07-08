package de.tsl2.nano.service.util.finder;

import java.util.Collection;

/**
 * additional group by statement creator
 * 
 * @param <T> result bean type
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class GroupBy<T> extends AbstractFinder<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -6987663981295488159L;

    String[] attributeNames;
    public GroupBy(String[] attributeNames) {
        this(null, attributeNames);
    }
    
    /**
     * constructor
     * 
     * @param attributeNames, group by names
     */
    public GroupBy(Class<T> resultType, String[] attributeNames) {
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
        return currentQuery.append(createGroupByPostfix( attributeNames));
    }
    
    static String createGroupByPostfix(String[] attributeNames) {
        StringBuffer buf = new StringBuffer(8 + attributeNames.length * 13);
        buf.append("\n group by");
        for (int i = 0; i < attributeNames.length; i++) {
            buf.append(" " + attributeNames[i] + ",");
        }
        return buf.substring(0, buf.length() - 1);
    }
}