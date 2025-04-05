package de.tsl2.nano.action;

@FunctionalInterface
public interface IFAction<RETURNTYPE> {

    /**
     * The action to be started
     * 
     * @return result of the action. use {@link #CANCELED} to define for buttons, the action was canceled.
     * @throws Exception
     */
    RETURNTYPE action() throws Exception;

}