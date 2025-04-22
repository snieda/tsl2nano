package de.tsl2.nano.h5;

import java.util.List;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.core.ISession;

/**
 * may be implemented by any application to provide side panels and menu info/about actions
 */
@SuppressWarnings("rawtypes")
public interface IFrameProvider {
    /** null or html content to be placed on the left side of the main content */
    String getLeftPanel(ISession session, BeanDefinition bean);
    /** null or html content to be placed on the right side of the main content */
    String getRightPanel(ISession session, BeanDefinition bean);
    /** empty list  or any actions to be placed as info inside the top menu */
    List<IAction> getMenuActions(ISession session, BeanDefinition bean);
}
