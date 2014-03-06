package de.tsl2.nano.service.util.finder;

import static de.tsl2.nano.service.util.ServiceUtil.addMemberExpression;

import java.util.Arrays;
import java.util.Collection;

/**
 * member finder
 * 
 * @param <T> result bean type
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Member<T, H> extends AbstractFinder<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 7667346649256697587L;

    public Member(H holder, Class<T> beanType, String attributeName, Class<Object>... relationsToLoad) {
        super(beanType, relationsToLoad);
        par = Arrays.asList(holder, beanType, attributeName);
    }

    @Override
    StringBuffer createQuery(StringBuffer currentQuery,
            Collection<Object> parameter,
            Collection<Class<Object>> lazyRelations) {
        return addMemberExpression(currentQuery,
            getSubSelectSubst(),
            index,
            par.get(0),
            (Class<Object>) par.get(1),
            (String) par.get(2));
    }
}