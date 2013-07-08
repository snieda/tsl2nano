package de.tsl2.nano.service.util.finder;

import java.util.Arrays;
import java.util.Collection;
import static de.tsl2.nano.service.util.ServiceUtil.*;

/**
 * all finder
 * TODO: TEST
 * @param <T> result bean type
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class All<T> extends AbstractFinder<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 4019550910630642421L;

    public All(Class<T> type, Class<Object>... relationsToLoad) {
        super(type, relationsToLoad);
        resultType = type;
    }

    @Override
    StringBuffer createQuery(StringBuffer currentQuery,
            Collection<Object> parameter,
            Collection<Class<Object>> lazyRelations) {
        lazyRelations.addAll(Arrays.asList(relationsToLoad));
        //the super class will create the main query
        return currentQuery;
    }
}