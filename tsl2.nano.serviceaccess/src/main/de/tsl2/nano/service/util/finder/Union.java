package de.tsl2.nano.service.util.finder;

import java.util.Collection;

import de.tsl2.nano.service.util.ServiceUtil;

/**
 * TODO: implement additional union statement creator
 * 
 * @param <T> result bean type
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Union<T> extends Expression<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -6987663981295488159L;

    /**
     * constructor
     * 
     * @param attributeNames, for ascending use prefix +. for descendings prefix -. default is ascending.
     */
    public Union(Class<T> resultType) {
        super(resultType, createUnion(resultType), false, new Object[] {});
    }

    @Override
    StringBuffer createQuery(StringBuffer currentQuery,
            Collection<Object> parameter,
            Collection<Class<Object>> lazyRelations) {
        return currentQuery.append(createUnion(resultType));
    }
    static <T>String createUnion(Class<T> resultType) {
        return "\nunion " + ServiceUtil.createStatement(resultType, ServiceUtil.SUBST_RESULTBEAN/* + "u."*/) + " where 1 = 1";
    }
}