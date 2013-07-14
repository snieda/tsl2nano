package de.tsl2.nano.util.bean.def;

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
    public static final int MODE_EDITABLE = 1;
    /** whether new items can be added to the collection of bean-collector  */
    public static final int MODE_CREATABLE = 2;
    /** whether items can be deleted from collection of bean-collector  */
    public static final int MODE_DELETABLE = 4;

    /** whether more than one item of the collection can be selected */
    public static final int MODE_MULTISELECTION = 8;
    /** whether a filter/search panel should be available */
    public static final int MODE_SEARCHABLE = 16;
    /** whether the selection should be returned to the parent caller - to be assigned as result */
    public static final int MODE_ASSIGNABLE = 32;
    /** show one-to-many relations in table (will have effects on {@link BeanDefinition#attributeFilter} and {@link BeanDefinition#attributeDefinitions} */
    public static final int MODE_SHOW_MULTIPLES = 64;

    public static final int MODE_ALL_SINGLE = MODE_EDITABLE | MODE_CREATABLE | MODE_DELETABLE | MODE_SEARCHABLE | MODE_ASSIGNABLE;
    public static final int MODE_ALL = MODE_ALL_SINGLE | MODE_MULTISELECTION | MODE_SHOW_MULTIPLES;
    
    /**
     * setWorkingMode
     * 
     * @param mode
     */
    public void setMode(int mode);

    /**
     * hasWorkingMode
     * 
     * @param mode - one of {@link #MODE_EDITABLE}, {@link #MODE_CREATABLE} etc.
     * @return whether the bean collector has the given mode
     */
    public boolean hasMode(int mode);

    /**
     * adds a mode bit to the existing
     * @param modebit - one of {@link #MODE_EDITABLE}, {@link #MODE_CREATABLE}, etc.
     */
    public void addMode(int modebit);
    
    public void removeMode(int modebit);
    
    /**
     * @return Returns the beanFinder.
     */
    public abstract IBeanFinder<T, ?> getBeanFinder();

    /**
     * @return Returns the selectionProvider.
     */
    public ISelectionProvider<T> getSelectionProvider();
    
    /**
     * refresh
     */
    public abstract void refresh();

    /**
     * override this method to create an edit dialog (executeOpenCommand).
     * 
     * @param bean bean to present in new dialog
     * @return command result
     */
    public abstract Object editItem(Object bean);

    /**
     * will be called, before object deletion. overwrite this method to do an alternate deletion or throw an exception
     * if check fails, or show a question messages before.
     * 
     * @param selection objects to delete
     */
    @SuppressWarnings("rawtypes")
    public abstract void checkBeforeDelete(Collection selection);

    /**
     * deleteItem
     * 
     * @param item item to remove
     */
    public abstract void deleteItem(T item);

    /**
     * ceates a new object
     * 
     * @param selectedItem selected item
     * 
     * @return new object
     */
    public abstract T createItem(T selectedItem);

    /**
     * @return Returns the collection.
     */
    public COLLECTIONTYPE getCurrentData();
    
  /**
     * getSearchStatus
     * @return search status as text to be used as summary/footer for the table
     */
    public String getSummary();
}