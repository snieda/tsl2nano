package de.tsl2.nano.service.util.finder;

import java.util.Arrays;
import java.util.Collection;

/**
 * id finder
 * 
 * @param <T> result bean type
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Id<T> extends AbstractFinder<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 4019550910630642421L;

    public Id(Class<T> type, Object id, Class<Object>... relationsToLoad) {
        super(type, relationsToLoad);
        resultType = type;
        par = Arrays.asList(id);
    }

    @Override
    StringBuffer createQuery(StringBuffer currentQuery,
            Collection<Object> parameter,
            Collection<Class<Object>> lazyRelations) {
        lazyRelations.addAll(Arrays.asList(relationsToLoad));
        return null;
    }
}