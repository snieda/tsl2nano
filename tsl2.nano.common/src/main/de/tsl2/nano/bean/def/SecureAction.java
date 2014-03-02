/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Sep 22, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.bean.def;

import org.apache.commons.logging.Log;
import de.tsl2.nano.log.LogFactory;

import de.tsl2.nano.Messages;
import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;

/**
 * action with role-depended permission. action mode respects dialog OK and CANCEL results.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public abstract class SecureAction<RETURNTYPE> extends CommonAction<RETURNTYPE> {
    /** serialVersionUID */
    private static final long serialVersionUID = 2212543928201596619L;

    private static final Log LOG = LogFactory.getLog(SecureAction.class);

    int actionMode;
    boolean allPermission = false;

    /**
     * constructor
     */
    protected SecureAction() {
        super();
    }
    
    public SecureAction(Class<?> prefix, String name, int actionMode, boolean isdefault, String imagePath) {
        super();
        this.id = BeanContainer.getActionId(prefix, true, name);
        this.shortDescription = BeanContainer.getActionText(id, false);
        this.longDescription = BeanContainer.getActionText(id, true);
        this.isDefault = isdefault;
        this.actionMode = actionMode;
        this.imagePath = imagePath;
    }
    /**
     * constructor
     * 
     * @param id action id, will be used as label, too (through resource bundle).
     */
    public SecureAction(String id) {
        this(id, id, id, 0);
    }

    /**
     * constructor
     * 
     * @param id action id
     * @param label action label
     */
    public SecureAction(String id, String label) {
        this(id, label, label, 0);
    }

    /**
     * constructor
     * 
     * @param id action id, will be used as label, too (through resource bundle).
     * @param actionMode mode {@link IAction#MODE_DLG_CANCEL} or {@link IAction#MODE_DLG_OK}.
     */
    public SecureAction(String id, int actionMode) {
        this(id, id, id, actionMode);
    }

    /**
     * constructor
     * 
     * @param id
     * @param shortDescription
     * @param longDescription
     * @param actionMode mode {@link IAction#MODE_DLG_CANCEL} or {@link IAction#MODE_DLG_OK}.
     * @param showFinishDialog whether to show a finish info dialog.
     */
    public SecureAction(String id,
            String shortDescription,
            String longDescription,
            int actionMode) {
        super(id, Messages.getStringOpt(shortDescription, true), Messages.getStringOpt(longDescription));
        this.actionMode = actionMode;
        LOG.info("creating dialog action with action-id and permission-id: '" + id + "' and label '" + shortDescription);
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && (isAllPermission() || BeanContainer.instance().hasPermission(id, null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getActionMode() {
        return actionMode;
    }

    /**
     * @return Returns the allPermission.
     */
    public boolean isAllPermission() {
        return allPermission;
    }

    /**
     * isClosingMode
     * @return true, if mode is {@link IAction#MODE_DLG_CANCEL} or {@link IAction#MODE_DLG_OK}
     */
    public boolean isClosingMode() {
        return actionMode == MODE_DLG_CANCEL || actionMode == MODE_DLG_CANCEL;
    }
    
    /**
     * if true, no permission request will be done to enable the action.
     * 
     * @param allPermission The allPermission to set.
     */
    public void setAllPermission(boolean allPermission) {
        this.allPermission = allPermission;
    }
}
