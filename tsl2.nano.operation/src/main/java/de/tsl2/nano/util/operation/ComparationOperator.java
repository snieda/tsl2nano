/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 30.12.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.util.operation;

import java.util.HashMap;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;

/**
 * UNDER CONSTRUCTION
 * <p/>
 * compares two operands and returns a boolean. both operands have to be evaluated before and have to exist in the value
 * map.
 * 
 * @author Tom
 * @version $Revision$
 */
public class ComparationOperator extends SOperator<Boolean> {

    @Override
    @SuppressWarnings({ "rawtypes", "serial", "unchecked" })
    protected void createOperations() {
        syntax.put(KEY_OPERATION, "[!&|?:]");
        operationDefs = new HashMap<CharSequence, IAction<Boolean>>();
        addOperation("=", new CommonAction<Boolean>() {
            @Override
            public Boolean action() throws Exception {
                return
                (parameters().getValue(0) instanceof Comparable
                    ? ((Comparable) parameters().getValue(0)).compareTo(parameters().getValue(1)) == 0 : parameters().getValue(1) == null
                        ? true : false);
            }
        });
        addOperation(">", new CommonAction<Boolean>() {
            @Override
            public Boolean action() throws Exception {
                return
                (parameters().getValue(0) instanceof Comparable
                    ? ((Comparable) parameters().getValue(0)).compareTo(parameters().getValue(1)) > 0 : parameters().getValue(1) == null
                        ? false : false);
            }
        });
        addOperation("<", new CommonAction<Boolean>() {
            @Override
            public Boolean action() throws Exception {
                return
                (parameters().getValue(0) instanceof Comparable
                    ? ((Comparable) parameters().getValue(0)).compareTo(parameters().getValue(1)) > 0 : parameters().getValue(1) == null
                        ? false : true);
            }
        });
    }

}
