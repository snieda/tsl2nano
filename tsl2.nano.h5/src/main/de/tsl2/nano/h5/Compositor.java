/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 02.04.2016
 * 
 * Copyright: (c) Thomas Schneider 2016, all rights reserved
 */
package de.tsl2.nano.h5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.Composition;
import de.tsl2.nano.bean.def.IBeanFinder;
import de.tsl2.nano.bean.def.SecureAction;

/**
 * The Compositor is a fast- or one-click collector using a base-type to create composition instances from. It's a kind
 * of charging items of the base-type.
 * <p/>
 * It provides actions to refresh, save and storno the list of charges. Each charge (=add or create) will be done by
 * activing the representable action for an item. Each item will represented by such an action.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "serial", "rawtypes" })
public class Compositor<COLLECTIONTYPE extends Collection<T>, T> extends BeanCollector<COLLECTIONTYPE, T> {
    /** base type of items to create composition instances from */
    Class<?> baseType;
    /** composition instance attribute to assign the base instance to */
    private String baseAttribute;
    /** base instance attribute name to evaluate an image path for the compositor action */
    private String iconAttribute;

    /**
     * constructor
     */
    public Compositor() {
    }

    /**
     * constructor
     * 
     * @param beanType
     * @param workingMode
     */
    public Compositor(Class<T> beanType, Class<?> baseType,
            final String baseAttribute,
            final String iconAttribute) {
        super(beanType, MODE_SEARCHABLE);
        this.baseType = baseType;
        this.baseAttribute = baseAttribute;
        this.iconAttribute = iconAttribute;
        //build the actions
        getActions();
    }

    @Override
    public Collection<IAction> getActions() {
        Collection<IAction> actions = super.getActions();
        List<IAction> compositorActions =
            createCompositorActions(baseType, getSearchPanelBeans().iterator().next(), baseAttribute, iconAttribute);
        compositorActions.addAll(actions);
        return compositorActions;
    }

    /**
     * creates an compositor-action for each item of type baseType, found in the database.
     * 
     * @param baseType see {@link #baseType}
     * @param preparedComposition pre-filled instance to be used to create new composition instances
     * @param baseAttribute see {@link #baseAttribute}
     * @param iconAttribute (optional) see {@link #iconAttribute}
     * @return list of compositor actions
     */
    public List<IAction> createCompositorActions(Class<?> baseType,
            final T preparedComposition,
            final String baseAttribute,
            final String iconAttribute) {
        Collection<?> bases = BeanContainer.instance().getBeans(baseType, 0, -1);
        ArrayList<IAction> actions = new ArrayList<>();
        for (final Object base : bases) {
            actions.add(new SecureAction<T>(Bean.getBean(base).getId().toString(),
                Bean.getBean(base).toString()) {
                T item;

                @Override
                public T action() throws Exception {
                    item = createItem(preparedComposition);
                    Bean.getBean(item).setValue(baseAttribute, base);
                    return item;
                }

                @Override
                public String getImagePath() {
                    return (String) (iconAttribute != null ? Bean.getBean(base).getValue(iconAttribute)
                        : Bean.getBean(base).getPresentable().getIcon());
                }
            });
        }
        return actions;
    }
}
