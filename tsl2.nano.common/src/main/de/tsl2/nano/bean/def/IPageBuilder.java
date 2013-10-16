package de.tsl2.nano.bean.def;


/**
 * a page builder should create a component representing the given bean.
 * 
 * @param <PAGE> type of element to create a page
 * @param <OUTPUT> output type of builder
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IPageBuilder<PAGE, OUTPUT> {

    /**
     * creates the full page
     * @param model model to present
     * @param message optional message to show
     * @param interactive if false, no buttons and edit fields are shown
     * @param navigation (optional) navigation objects
     * @return page text
     */
    OUTPUT build(BeanDefinition<?> model, String message, boolean interactive, BeanDefinition<?>... navigation);

    /**
     * decorate a message
     * @param message
     * @return decorated 
     */
    OUTPUT decorate(String message);
}