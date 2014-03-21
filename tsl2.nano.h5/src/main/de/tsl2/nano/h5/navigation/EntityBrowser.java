/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 02.11.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.h5.navigation;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Stack;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IBeanCollector;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.h5.Html5Presentation;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class EntityBrowser implements IBeanNavigator {
    transient protected Stack<BeanDefinition<?>> navigation;
    transient protected BeanDefinition<?> current;

    /**
     * constructor
     * 
     * @param navigation
     */
    public EntityBrowser(Stack<BeanDefinition<?>> navigation) {
        super();
        this.navigation = navigation;
    }

    /**
     * the next model may be a new bean model, if the response object is not null, not in the current navigation stack
     * and not a cancel action.
     * 
     * @param userResponseObject result of {@link #processInput(String, Properties, Number)}
     * @return next bean model or null
     */
    @Override
    public BeanDefinition<?> next(Object userResponseObject) {
        boolean isOnWork = false;
        boolean goBack = userResponseObject == null || userResponseObject == IAction.CANCELED;
        if (!goBack) {
            BeanDefinition<?> userResponseBean =
                (BeanDefinition<?>) (userResponseObject instanceof BeanDefinition<?> ? userResponseObject
                    : (Util.isContainer(userResponseObject)
                        ? BeanCollector.getBeanCollector((Collection) userResponseObject, 0)
                        : Bean.getBean((Serializable) userResponseObject)));
            isOnWork = navigation.contains(userResponseBean);
            if (!isOnWork) //--> go forward
                return (current = navigation.push(userResponseBean));
            else {
                if (current != userResponseBean) {
                    while (!userResponseBean.equals(navigation.peek()))
                        navigation.pop();
                    return current = navigation.peek();
                }
            }

        }
        //go back
        if (!isOnWork && current != null)//checking to be not the first page
            navigation.pop();

        current = navigation.size() > 0 ? navigation.peek() : null;
        //workaround for a canceled new action
        if (userResponseObject == IAction.CANCELED && current instanceof IBeanCollector) {
            removeUnpersistedNewEntities((BeanCollector) current);
        }
        return current;
    }

    @Override
    public BeanDefinition<?> current() {
        return current;
    }

    /**
     * workaround for 'new' action on a beancollector followed by a cancel action - means the new instance is added to
     * the beancollector, but the cancel action has to remove the instance.
     * 
     * @param collector collector holding a canceled/transient instance.
     */
    @SuppressWarnings("rawtypes")
    private void removeUnpersistedNewEntities(BeanCollector collector) {
        if (!BeanContainer.instance().isPersistable(collector.getBeanFinder().getType()))
            return;
        Collection currentData = collector.getCurrentData();
        for (Iterator iterator = currentData.iterator(); iterator.hasNext();) {
            Object item = iterator.next();
            if (BeanContainer.isTransient(item))
                iterator.remove();
        }
    }

    /**
     * navigation stack
     * 
     * @return current navigation queue wrapped into an object array
     */
    @Override
    public BeanDefinition<?>[] toArray() {
        BeanDefinition<?>[] beans = new BeanDefinition[navigation.size()];
        for (int i = 0; i < navigation.size(); i++) {
            beans[i] = navigation.get(i);
        }
        return beans;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BeanDefinition<?> fromUrl(String uri) {
        BeanDefinition<?> linkBean = null;
        String link = StringUtil.substring(uri, Html5Presentation.PREFIX_BEANLINK, null, true);
        for (BeanDefinition<?> bean : navigation) {
            if (bean.getName().equals(link)) {
                linkBean = bean;
                break;
            }
        }
        return linkBean;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(BeanDefinition<?> bean) {
        navigation.add(bean);
    }

    @Override
    public boolean isEmpty() {
        return navigation.empty();
    }

}
