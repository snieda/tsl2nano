/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 20.10.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.h5.navigation;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.ElementArray;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.incubation.vnet.workflow.Act;
import de.tsl2.nano.incubation.vnet.workflow.ComparableMap;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.util.operation.ConditionOperator;
import de.tsl2.nano.util.operation.Operator;

/**
 * Uses EJB-QL as expression and {@link ConditionOperator} to check for activation. On activation, the EJB-QL will be
 * executed to get a result of Collection<Entity-Beans> - wrapped into a {@link BeanCollector}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@Default(value = DefaultType.FIELD, required = false)
public class BeanAct extends Act<BeanDefinition<?>> {
    private static final Log LOG = LogFactory.getLog(BeanAct.class);
    
    /** serialVersionUID */
    private static final long serialVersionUID = 5821178690873604621L;

    /** parNames parameter names for EBJ-QL. */
    @ElementArray(name="query-parameter", entry="parameter")
    String[] parNames;

    
    /**
     * constructor
     */
    public BeanAct() {
        super();
    }

    /**
     * constructor
     * 
     * @param name activity name
     * @param condition pre condition to be checked if activation can be done.
     * @param expression ejb-ql expression to be executed on activation
     * @param stateValues state properties.
     * @param parNames parameter names for EBJ-QL.
     */
    public BeanAct(String name,
            String condition,
            String expression,
            ComparableMap<CharSequence, Object> stateValues,
            String... parNames) {
        super(name, condition, expression, stateValues);
        this.parNames = parNames;
    }

    @Override
    public BeanDefinition<?> action() throws Exception {
        LOG.info("executing activity '" + getShortDescription() + "'");
        Collection<Object> entities = BeanContainer.instance().getBeansByQuery(expression, false, getArguments());
        if (entities == null || entities.isEmpty())
            return null;
        else if (entities.size() > 1) {
            return BeanCollector.getBeanCollector(null, entities, BeanCollector.MODE_ALL, null);
        } else {
            return Bean.getBean((Serializable) entities.iterator().next());
        }
    }

    /**
     * evaluates the arguments, given by {@link #parNames} from {@link Operator#getValues()}.
     * 
     * @return query arguments
     */
    protected Object[] getArguments() {
        Map<CharSequence, Object> values = op.getValues();
        Object args[] = new Object[parNames.length];
        for (int i = 0; i < parNames.length; i++) {
            args[i] = values.get(parNames[i]);
        }
        return args;
    }
}
