package de.tsl2.nano.service.util.finder;

import static de.tsl2.nano.service.util.ServiceUtil.CLAUSE_AND;
import static de.tsl2.nano.service.util.ServiceUtil.CLAUSE_WHERE;
import static de.tsl2.nano.service.util.ServiceUtil.addBetweenConditions;

import java.util.Collection;

import de.tsl2.nano.service.util.ServiceUtil;

/**
 * between finder. see {@link ServiceUtil} and Finder.
 * 
 * @param <T> result bean type
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Between<T> extends AbstractFinder<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 6586051554455888398L;

    T minObject;
    T maxObject;
    
    public Between(T minObject, T maxObject, Class<Object>... relationsToLoad) {
        super((Class<T>) minObject.getClass(), relationsToLoad);
        this.minObject = minObject;
        this.maxObject = maxObject;
    }

    @Override
    StringBuffer createQuery(StringBuffer currentQuery,
            Collection<Object> parameter,
            Collection<Class<Object>> lazyRelations) {
        return addBetweenConditions(currentQuery, getSubSelectSubst() + ".", getAndClause(), minObject, maxObject, parameter, false);
    }
}