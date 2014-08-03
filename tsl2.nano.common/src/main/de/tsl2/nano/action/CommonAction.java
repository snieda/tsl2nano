/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Dec 30, 2009
 * 
 * Copyright: (c) Thomas Schneider 2009, all rights reserved
 */
package de.tsl2.nano.action;

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.ThreadUtil;

/**
 * @param <RETURNTYPE>
 * @author Thomas Schneider
 * @version $Revision$
 */
public abstract class CommonAction<RETURNTYPE> implements IAction<RETURNTYPE>, Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 7933702124402104693L;
    protected String id;
    protected String shortDescription;
    protected String longDescription;
    protected boolean isDefault;
    protected boolean isEnabled;
    protected boolean synchron;
    protected Object keyStroke;
    RETURNTYPE result;
    protected Collection<String> receiverIDs;
    protected IActivable enabler;
    protected Object[] parameter;
    protected String imagePath;
    private static final Log LOG = LogFactory.getLog(CommonAction.class);
    protected static final String UNNAMED = "unknown";

    /**
     * The default constructor can be used for hidden actions. This are action, not connected to a user interface like a
     * button.
     */
    public CommonAction() {
        this(String.valueOf(System.currentTimeMillis()), UNNAMED, UNNAMED);
    }

    /**
     * @param id unique id
     */
    public CommonAction(String id) {
        this(id, UNNAMED, UNNAMED);
    }

    /**
     * @param id unique id
     * @param shortDescription name of the button (representing this action).
     * @param longDescription tooltip text of the button (representing this action).
     */
    public CommonAction(String id, String shortDescription, String longDescription) {
        this(id, false, true, null, shortDescription, longDescription, true, (IActivable) null, null);
    }

    /**
     * @param id unique id
     * @param shortDescription name of the button (representing this action).
     * @param longDescription tooltip text of the button (representing this action).
     * @param instance for an action enabler
     */
    public CommonAction(String id, String shortDescription, String longDescription, IActivable actionEnabler) {
        this(id, false, true, null, shortDescription, longDescription, true, actionEnabler, null);
    }

    /**
     * @param id unique id
     * @param shortDescription name of the button (representing this action).
     * @param longDescription tooltip text of the button (representing this action).
     * @param enabler is able to to functional checks for button enabling
     * @param receiverIDs ids of visible components to receive the result of this action.
     */
    public CommonAction(String id,
            String shortDescription,
            String longDescription,
            IActivable enabler,
            Collection<String> receiverIDs) {
        this(id, false, true, null, shortDescription, longDescription, true, enabler, receiverIDs);
    }

    /**
     * Constructor
     * 
     * @param id unique id
     * @param isDefault whether the button (representing this action) is the default button.
     * @param isEnabled whether the button (representing this action) is enabled.
     * @param keyStroke button key stroke
     * @param longDescription tooltip text of the button (representing this action).
     * @param shortDescription name of the button (representing this action).
     * @param synchron whether this method is synchron or asynchron.
     */
    public CommonAction(String id,
            boolean isDefault,
            boolean isEnabled,
            Object keyStroke,
            String shortDescription,
            String longDescription,
            boolean synchron,
            IActivable actionEnabler,
            Collection<String> receiverIDs) {
        super();
        this.id = id;
        this.isDefault = isDefault;
        this.isEnabled = isEnabled;
        this.longDescription = longDescription;
        this.shortDescription = shortDescription;
        this.synchron = synchron;
        this.enabler = actionEnabler;
        this.receiverIDs = receiverIDs;
        this.keyStroke = keyStroke;
    }

    /**
     * isEnabled
     * 
     * @see de.tsl2.nano.action.IAction#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        if (enabler != null) {
            return enabler.isActive();
        } else {
            return isEnabled;
        }
    }

    /**
     * setEnabled
     * 
     * @see de.tsl2.nano.action.IAction#setEnabled(boolean)
     */
    @Override
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    
    /**
     * @return Returns the enabler.
     */
    public IActivable getEnabler() {
        return enabler;
    }

    /**
     * @param enabler The enabler to set.
     */
    public void setEnabler(IActivable enabler) {
        this.enabler = enabler;
    }

    /**
     * getId
     * 
     * @see de.tsl2.nano.action.IAction#getId()
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * getShortDescription
     * 
     * @see de.tsl2.nano.action.IAction#getShortDescription()
     */
    @Override
    public String getShortDescription() {
        return shortDescription;
    }

    /**
     * getLongDescription
     * 
     * @see de.tsl2.nano.action.IAction#getLongDescription()
     */
    @Override
    public String getLongDescription() {
        return longDescription;
    }

    /**
     * isDefault
     * 
     * @see de.tsl2.nano.action.IAction#isDefault()
     */
    @Override
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    /**
     * Getter isSynchron
     * 
     * @return Returns the synchron.
     */
    @Override
    public boolean isSynchron() {
        return synchron;
    }

    /**
     * run
     * 
     * @see de.tsl2.nano.action.IAction#run()
     */
    @Override
    public void run() {
        try {
            if (isDebugMode() || !UNNAMED.equals(getShortDescription())) {
                LOG.info("starting " + (synchron ? " " : "asyncron ")
                    + "action ==> '"
                    + (getShortDescription() != null ? getShortDescription() + "(Id:" + getId() + ")" : getId())
                    + "' parameter: "
                    + parameter);
            }

            result = action();

            if (isDebugMode() || !UNNAMED.equals(getShortDescription())) {
                LOG.info("finishing " + (synchron ? " " : "asyncron ")
                    + "action ==> '"
                    + getShortDescription()
                    + "(Id:"
                    + getId()
                    + ")"
                    + "' result: "
                    + StringUtil.toString(result, 30));
            }
        } catch (final Exception e) {
            ManagedException.forward(e);
        }
    }

    /**
     * override this method to enable debugging
     * 
     * @return true, if debugging is enabled
     */
    protected boolean isDebugMode() {
        return false;
    }

    /**
     * if the action is asynchron, the result will be mostly null.
     * 
     * @see de.tsl2.nano.action.IAction#activate()
     */
    @Override
    public RETURNTYPE activate() {
//        if (!isEnabled()) {
//            ManagedException.implementationError("Aktion ist deaktiviert!", getShortDescription());
//        }
        if (isSynchron()) {
            final Object lastCursor = setWaitCursor();
            try {
                run();
            } finally {
                resetCursor(lastCursor);
            }
        } else {// asynchron
            result = null;
            ThreadUtil.startDaemon(getId(), this);
        }
        return result;
    }

    /**
     * override this method to handle the mouse cursor
     * 
     * @return the last cursor instance
     */
    protected Object setWaitCursor() {
        return null;
    }

    /**
     * override this method to handle the mouse cursor
     * 
     * @param lastCursor cursor to be set
     */
    protected void resetCursor(Object lastCursor) {
        //do nothing
    }

    /**
     * @return Returns the receiverIDs.
     */
    @Override
    public Collection<String> getReceiverIDs() {
        return receiverIDs;
    }

    /**
     * @return Returns the parameter.
     */
    @Override
    public Object[] getParameter() {
        return parameter;
    }

    @Override
    public Object getParameter(int i) {
        return parameter != null && parameter.length > i ? parameter[i] : null;
    }
    
    /**
     * @param parameter The parameter to set.
     */
    @Override
    public void setParameter(Object... parameter) {
        this.parameter = parameter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getImagePath() {
        return imagePath;
    }

    public RETURNTYPE getLastResult() {
        return result;
    }

    /**
     * @return Returns the keyStroke.
     */
    @Override
    public Object getKeyStroke() {
        return keyStroke;
    }

    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Id: " + id
            + ", Name: " + shortDescription
            + ", Syncron: "
            + synchron
            + ", Result: "
            + result
            + ", Receivers-Ids: "
            + StringUtil.toString(receiverIDs, 30)
            + ", Enabler: "
            + enabler
            + ", Parameter: "
            + StringUtil.toString(parameter, 30);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getActionMode() {
        return MODE_UNDEFINED;
    }

}