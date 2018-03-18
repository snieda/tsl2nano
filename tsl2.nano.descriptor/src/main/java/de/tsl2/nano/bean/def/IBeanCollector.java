package de.tsl2.nano.bean.def;

import java.util.Collection;

/**
 * Defines a bean, holding an instance of a collection. optional actions like open, new, delete may be provided.
 * 
 * @param <COLLECTIONTYPE> type of collection, holding bean instances
 * @param <T> bean type
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IBeanCollector<COLLECTIONTYPE extends Collection<T>, T> extends ITableDescriptor<T> {

    /** whether a selected bean can be opened to be changed */
    static final int MODE_EDITABLE = 1;
    /** whether new items can be added to the collection of bean-collector  */
    static final int MODE_CREATABLE = 2;
    /** whether items can be deleted from collection of bean-collector  */
    static final int MODE_DELETABLE = 4;

    /** whether more than one item of the collection can be selected */
    static final int MODE_MULTISELECTION = 8;
    /** whether a filter/search panel should be available */
    static final int MODE_SEARCHABLE = 16;
    /** whether the selection should be returned to the parent caller - to be assigned as result */
    static final int MODE_ASSIGNABLE = 32;
    /** show one-to-many relations in table (will have effects on {@link BeanDefinition#attributeFilter} and {@link BeanDefinition#attributeDefinitions} */
    static final int MODE_SHOW_MULTIPLES = 64;
    /** if a beans attribute is a relation, show its details in a nested panel */
    static final int MODE_SHOW_NESTINGDETAILS = 128;

    static final int MODE_ALL_SINGLE = MODE_EDITABLE | MODE_CREATABLE | MODE_DELETABLE | MODE_SEARCHABLE | MODE_ASSIGNABLE;
    static final int MODE_ALL = MODE_ALL_SINGLE | MODE_MULTISELECTION | MODE_SHOW_MULTIPLES;
    
    /**
     * setWorkingMode
     * 
     * @param mode
     */
    void setMode(int mode);

    /**
     * hasWorkingMode
     * 
     * @param mode - one of {@link #MODE_EDITABLE}, {@link #MODE_CREATABLE} etc.
     * @return whether the bean collector has the given mode
     */
    boolean hasMode(int mode);

    /**
     * adds a mode bit to the existing
     * @param modebit - one of {@link #MODE_EDITABLE}, {@link #MODE_CREATABLE}, etc.
     */
    void addMode(int modebit);
    
    void removeMode(int modebit);
    
    /**
     * @return Returns the beanFinder.
     */
    abstract IBeanFinder<T, ?> getBeanFinder();

    /**
     * @return Returns the selectionProvider.
     */
    ISelectionProvider<T> getSelectionProvider();
    
    /**
     * refresh
     */
    abstract void refresh();

    /**
     * override this method to create an edit dialog (executeOpenCommand).
     * 
     * @param bean bean to present in new dialog
     * @return command result
     */
    abstract Object editItem(Object bean);

    /**
     * will be called, before object deletion. overwrite this method to do an alternate deletion or throw an exception
     * if check fails, or show a question messages before.
     * 
     * @param selection objects to delete
     */
    @SuppressWarnings("rawtypes")
    abstract void checkBeforeDelete(Collection selection);

    /**
     * deleteItem
     * 
     * @param item item to remove
     */
    abstract void deleteItem(T item);

    /**
     * ceates a new object
     * 
     * @param selectedItem selected item
     * 
     * @return new object
     */
    abstract T createItem(T selectedItem);

    /**
     * @return Returns the collection.
     */
    COLLECTIONTYPE getCurrentData();
    
  /**
     * getSearchStatus
     * @return search status as text to be used as summary/footer for the table
     */
    String getSummary();
}