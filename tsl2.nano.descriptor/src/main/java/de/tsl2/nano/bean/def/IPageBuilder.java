package de.tsl2.nano.bean.def;

import java.util.Collection;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.core.ISession;


/**
 * a page builder should create a component representing the given bean.
 * 
 * @param <PAGE> type of element to create a page
 * @param <OUTPUT> output type of builder
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public interface IPageBuilder<PAGE, OUTPUT> {

    /**
     * creates the full page
     * @param model model to present
     * @param message optional message to show
     * @param interactive if false, no buttons and edit fields are shown
     * @param navigation (optional) navigation objects
     * @return page text
     */
    OUTPUT build(ISession session, BeanDefinition<?> model, Object message, boolean interactive, BeanDefinition<?>... navigation);

    /**
     * create a full page and decorate a message
     * @param message
     * @return new page with decorated message
     */
    OUTPUT page(String message);
    
    /**
     * decorate a message
     * @param message
     * @return decorated 
     */
    OUTPUT decorate(String title, String message);
    
    /**
     * creates extended actions like 'help', 'configure', 'exit' etc.
     */
    Collection<IAction> getApplicationActions(ISession session);
    
    /**
     * creates extended actions like 'logout' etc.
     */
    Collection<IAction> getSessionActions(ISession session);
    
    /**
     * creates extended actions like 'print', 'help', 'export', 'select-all', 'deselect-all' etc.
     */
    Collection<IAction> getPageActions(ISession session);

	void reset();
    
}