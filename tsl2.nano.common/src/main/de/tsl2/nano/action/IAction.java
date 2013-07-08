package de.tsl2.nano.action;

import java.util.Collection;

/**
 * provides all informations to start an action (through Runnable) and to handle a graphical user interface of type
 * button etc.
 * 
 * @author ts 13.11.2008
 * @version $Revision: 1.0 $
 */
public interface IAction<RETURNTYPE> extends Runnable {
    public static final String CANCELED = "de.tsl2.nano.action.action_canceled";

    /** dialog return value - no dialog handling will be done */
    public static final int MODE_UNDEFINED = 0;
    /** dialog return value: dialog will be closed, and IDialogConstants.CANCEL_ID will be returned */
    public static final int MODE_DLG_CANCEL = 1;
    /** dialog return value: dialog will be closed, and IDialogConstants.OK_ID will be returned */
    public static final int MODE_DLG_OK = 2;

    /**
     * Getter isEnabled
     * 
     * @return Returns the isEnabled.
     */
    boolean isEnabled();

    /**
     * Setter isEnabled
     * 
     * @param isEnabled The isEnabled to set.
     */
    void setEnabled(boolean isEnabled);

    /**
     * Getter getKeyStroke
     * 
     * @return Returns the keyStroke.
     */
    Object getKeyStroke();

    /**
     * Getter getId
     * 
     * @return Returns the id.
     */
    String getId();

    /**
     * Getter getShortDescription
     * 
     * @return Returns the shortDescription.
     */
    String getShortDescription();

    /**
     * Getter getLongDescription
     * 
     * @return Returns the longDescription.
     */
    String getLongDescription();

    /**
     * @return image
     */
    String getImagePath();

    /**
     * Getter isDefault
     * 
     * @return Returns the isDefault.
     */
    boolean isDefault();

    /**
     * setDefault
     * 
     * @param isDefault whether to set the action as default action
     */
    public void setDefault(boolean isDefault);

    /**
     * Getter isSynchron
     * 
     * @return Returns the synchron.
     */
    public boolean isSynchron();

    /**
     * The action to be started
     * 
     * @return result of the action. use {@link #CANCELED} to define for buttons, the action was canceled.
     * @throws Exception
     */
    public RETURNTYPE action() throws Exception;

    /**
     * starts this action.
     * 
     * @return the result
     */
    public RETURNTYPE activate();

    /**
     * the receivers will receive the result of the activation.
     * 
     * @return list of ids of receiver fields (IComponentDescriptor)
     */
    Collection<String> getReceiverIDs();

    /**
     * @return optional call arguments for action.
     */
    Object[] getParameter();

    /**
     * @param parameter call arguments
     */
    void setParameter(Object[] parameter);

    /**
     * action mode. e.g. on dialogs it is usable to define the buttons result type. see {@link #MODE_DLG_CANCEL},
     * {@link #MODE_DLG_OK}.
     * 
     * @return action mode
     */
    int getActionMode();
}