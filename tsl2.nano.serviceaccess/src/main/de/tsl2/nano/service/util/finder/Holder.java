package de.tsl2.nano.service.util.finder;

import java.util.Arrays;
import java.util.Collection;
import static de.tsl2.nano.service.util.ServiceUtil.*;

/**
 * holder finder
 * 
 * @param <T> result bean type
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Holder<T, H> extends AbstractFinder<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 7667346649256697587L;

    public Holder(T member, Class<H> holderType, String attributeName, Class<Object>... relationsToLoad) {
        super((Class<T>) member.getClass(), relationsToLoad);
        par = Arrays.asList( member, holderType, attributeName);
    }

    @Override
    StringBuffer createQuery(StringBuffer currentQuery,
            Collection<Object> parameter,
            Collection<Class<Object>> lazyRelations) {
        return addHolderExpression(currentQuery, getSubSelectSubst(), index, par.get(0), (Class<Object>)par.get(1), (String)par.get(2));
    }
}