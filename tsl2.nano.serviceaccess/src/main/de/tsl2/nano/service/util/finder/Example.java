package de.tsl2.nano.service.util.finder;

import static de.tsl2.nano.service.util.ServiceUtil.OP_LIKE;
import static de.tsl2.nano.service.util.ServiceUtil.addAndConditions;

import java.util.Collection;

/**
 * example finder
 * 
 * @param <T> result bean type
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Example<T> extends AbstractFinder<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 3047663581726904442L;

    T example;
    public Example(T example, Class<Object>... relationsToLoad) {
        super((Class<T>) example.getClass(), relationsToLoad);
        this.example = example;
    }

    @Override
    StringBuffer createQuery(StringBuffer currentQuery,
            Collection<Object> parameter,
            Collection<Class<Object>> lazyRelations) {
        return addAndConditions(currentQuery, getSubSelectSubst() + ".", getAndClause(), example, OP_LIKE, parameter, false);
    }
}