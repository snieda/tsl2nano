/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 20, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.Environment;
import de.tsl2.nano.Messages;
import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanAttribute;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.IAttributeDef;
import de.tsl2.nano.bean.IBeanContainer;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.collection.IPredicate;
import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.execution.Profiler;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.util.DateUtil;
import de.tsl2.nano.util.DelegatorProxy;
import de.tsl2.nano.util.NumberUtil;
import de.tsl2.nano.util.StringUtil;

/**
 * see {@link IBeanCollector}. the collector inherits from {@link BeanDefinition}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
@Default(value = DefaultType.FIELD, required = false)
public class BeanCollector<COLLECTIONTYPE extends Collection<T>, T> extends BeanDefinition<T> implements
        IBeanCollector<COLLECTIONTYPE, T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -5260557109700841750L;

    private static final Log LOG = LogFactory.getLog(BeanCollector.class);

    public static final String POSTFIX_COLLECTOR = ".collector";

    public static final String KEY_COMMANDHANDLER = "bean.commandhandler";

    /** static data or current data representation of this beancollector */
    protected transient COLLECTIONTYPE collection;
    boolean isStaticCollection = false;

    /** defines the data for this collector through it's getData() method */
    protected transient IBeanFinder<T, ?> beanFinder;
    /** holds the current selection */
    protected transient ISelectionProvider<T> selectionProvider;

    /**
     * holds the connection to the composition parent. if not null, the beancollector will work only on items having a
     * connection to this composition (uml-composition where childs can't exist without it's parent!).
     */
    protected Composition composition;

    protected transient IAction<?> newAction;
    protected transient IAction<?> editAction;
    protected transient IAction<?> deleteAction;
    /** the extending class has to set an instance for the ok action to set it as default button */
    protected transient IAction<?> openAction;

    /** defines the behaviour and the actions of the beancollector */
    protected int workingMode = MODE_EDITABLE | MODE_CREATABLE | MODE_SEARCHABLE;
    /**
     * search panel instructions.
     */
    protected transient IAction<COLLECTIONTYPE> searchAction;
    protected transient IAction<?> resetAction;
    String searchStatus = Messages.getString("tsl2nano.searchdialog.nosearch");

    /** whether to refresh data from beancontainer before opening edit-dialog */
    protected boolean reloadBean = true;

    @ElementList(name = "column", inline = true, required = false/*, type=ValueColumn.class*/)
    private Collection<IPresentableColumn> columnDefinitions;

    /** temporary variable to hold the {@link #toString()} output (--> performance) */
    private String asString;

    /**
     * the beancollector should listen to any change of the search-panel (beanfinders range-bean) to know which action
     * should have the focus
     */
    private Boolean hasSearchRequestChanged;

    /**
     * constructor. should only used by framework - for de-serialization.
     */
    public BeanCollector() {
        super();
    }

    /**
     * see constructor {@link #CollectionEditorBean(IBeanFinder, boolean, boolean, boolean)}.
     */
    public BeanCollector(Class<T> beanType, int workingMode) {
        this(new BeanFinder<T, Object>(beanType), workingMode, null);
    }

    /**
     * see constructor {@link #CollectionEditorBean(IBeanFinder, boolean, boolean, boolean)}.
     */
    public BeanCollector(Class<T> beanType, final COLLECTIONTYPE collection, int workingMode, Composition composition) {
        this(new BeanFinder<T, Object>(beanType), workingMode, composition);
        this.collection = collection;
        this.isStaticCollection = collection != null;
    }

    /**
     * see constructor {@link #CollectionEditorBean(IBeanFinder, boolean, boolean, boolean)}.
     */
    public BeanCollector(final COLLECTIONTYPE collection, int workingMode) {
        this(collection, workingMode, null);
    }

    /**
     * see constructor {@link #CollectionEditorBean(IBeanFinder, boolean, boolean, boolean)}.
     */
    public BeanCollector(final COLLECTIONTYPE collection, int workingMode, Composition composition) {
        this(new BeanFinder(collection.iterator().next().getClass()), workingMode, composition);
        this.collection = collection;
        this.isStaticCollection = collection != null;
    }

    /**
     * constructor
     * 
     * @param beanFinder implementation to evaluate the data
     * @param workingMode one of {@link IBeanCollector#MODE_EDITABLE}, {@link IBeanCollector#MODE_CREATABLE} etc. Please
     *            see {@link IBeanCollector} for more modes.
     */
    public BeanCollector(IBeanFinder<T, Object> beanFinder, int workingMode, Composition composition) {
        super(beanFinder.getType());
        init(null, beanFinder, workingMode, composition);
    }

    /**
     * init
     * 
     * @param beanFinder implementation to evaluate the data
     * @param workingMode one of {@link IBeanCollector#MODE_EDITABLE}, {@link IBeanCollector#MODE_CREATABLE} etc. Please
     *            see {@link IBeanCollector} for more modes.
     */
    private void init(COLLECTIONTYPE collection, IBeanFinder<T, ?> beanFinder, int workingMode, Composition composition) {
//        setName(Messages.getString("tsl2nano.list") + " " + getName());
        this.collection = collection != null ? collection : (COLLECTIONTYPE) new LinkedList<T>();
        setBeanFinder(beanFinder);
        //TODO: the check for attribute can't be done here - perhaps attributes will be added later!
//        if (getAttributeDefinitions().size() == 0) {
//            LOG.warn("bean-collector without showing any attribute");
//            this.workingMode = 0;
//            searchStatus = Messages.getFormattedString("tsl2nano.login.noprincipal", "...", "...");
//        } else {
        this.workingMode = workingMode;
//        }
        if (collection != null)
            searchStatus = Messages.getFormattedString("tsl2nano.searchdialog.searchresultcount", collection.size());
        this.composition = composition;
        if (composition != null && composition.getTargetType() == null) {
            if (hasMode(MODE_SEARCHABLE)) {
                LOG.warn("removing MODE_SEARCHABLE - because it is a composition");
                removeMode(MODE_SEARCHABLE);
            }
        }

//        if (beanFinder != null) {
        actions = new LinkedHashSet<IAction>();
        if (hasMode(MODE_SEARCHABLE)) {
            createSearchAction();
            if (getBeanFinder().getFilterRange() != null)
                createResetAction();
        }
        if (hasMode(MODE_EDITABLE)) {
            createOpenAction();
        }
        if (hasMode(MODE_CREATABLE)) {
            createNewAction();
            createDeleteAction();
        }
//        }
    }

    /**
     * getBeanType
     * 
     * @return optional beanType (content type of collection)
     */
    protected Class<T> getType() {
        //try to evaluate the collection content type
        if (beanFinder == null) {
            if (collection != null && collection.size() > 0) {
                setBeanFinder(new BeanFinder((Class<T>) collection.iterator().next().getClass()));
            }
        }
        return beanFinder.getType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IBeanFinder<T, ?> getBeanFinder() {
        return beanFinder;
    }

    /**
     * @param beanFinder The beanFinder to set.
     */
    public void setBeanFinder(final IBeanFinder<T, ?> beanFinder) {
        if (beanFinder != null) {
            /*
             * to use the beancollectors collection, we wrap the given beanfinder
             */
            @SuppressWarnings("unused")
            Object internalBeanFinder = new Serializable() {
                boolean betweenFinderCreated = false;

                //used by proxy!
                Collection<T> getData() {
                    Collection<T> searchPanelBeans = getSearchPanelBeans();
                    Iterator<T> it = searchPanelBeans.iterator();
                    T from = searchPanelBeans.size() > 0 ? it.next() : null;
                    T to = searchPanelBeans.size() > 1 ? it.next() : null;
                    return (COLLECTIONTYPE) getData(from, to);
                }

                public Collection<T> getData(T from, Object to) {
                    if (BeanContainer.isInitialized() && BeanContainer.instance().isPersistable(getType())) {
                        collection = (COLLECTIONTYPE) ((IBeanFinder<T, Object>) beanFinder).getData(from, to);
                        /*
                         * if it is a composition, all data has to be found in the compositions-parent-container
                         */
                        if (composition != null) {
                            for (Iterator<T> it = collection.iterator(); it.hasNext();) {
                                T item = (T) it.next();
                                if (!composition.getParentContainer().contains(item))
                                    it.remove();
                            }
                        }
                    } else if (!betweenFinderCreated) {
                        collection =
                            (COLLECTIONTYPE) CollectionUtil.getFilteringBetween(collection, from, (T) to, true);
                        betweenFinderCreated = true;
                    }
                    /*
                     * respect authorization/permissions
                     */
                    if (Environment.get("check.permission.data", true)) {
                        return CollectionUtil.getFiltering(collection, new IPredicate<T>() {
                            @Override
                            public boolean eval(T arg0) {
                                return getAttributeDefinitions().size() > 0 && BeanContainer.instance()
                                    .hasPermission(arg0.getClass().getName() + arg0.toString(), "read");
                            }
                        });
                    } else {
                        return collection;
                    }
                }

                public Collection<T> previous() {
                    return (collection = (COLLECTIONTYPE) beanFinder.previous());
                }

                public Collection<T> next() {
                    return (collection = (COLLECTIONTYPE) beanFinder.next());
                }
            };
            this.beanFinder = DelegatorProxy.delegator(IBeanFinder.class, internalBeanFinder, beanFinder);
        } else {
            this.beanFinder = null;
        }
    }

    /**
     * @return Returns the selectionProvider.
     */
    public ISelectionProvider<T> getSelectionProvider() {
        if (selectionProvider == null)
            selectionProvider = new SelectionProvider<T>(new LinkedList<T>());
        return selectionProvider;
    }

    /**
     * @param selectionProvider The selectionProvider to set.
     */
    public void setSelectionProvider(ISelectionProvider<T> selectionProvider) {
        this.selectionProvider = selectionProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasMode(int mode) {
        return NumberUtil.hasBit(workingMode, mode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMode(int mode) {
        this.workingMode = mode;
    }

    @Override
    public void addMode(int modebit) {
        this.workingMode |= modebit;
    }

    @Override
    public void removeMode(int modebit) {
        workingMode = NumberUtil.filterBits(workingMode, modebit);
    }

    /**
     * getWorkingMode
     * 
     * @return
     */
    public int getWorkingMode() {
        return workingMode;
    }

    /**
     * getFirstSelectedElement
     * 
     * @return first selection element or null
     */
    protected T getFirstSelectedElement() {
        if (getSelectionProvider() == null)
            return null;
        Collection<T> selection = getSelectionProvider().getValue();
        return selection.isEmpty() ? null : selection.iterator().next();
    }

    /**
     * hasSelection
     * 
     * @return tgrue, if at least one element was selected
     */
    public boolean hasSelection() {
        return getFirstSelectedElement() != null;
    }

    /**
     * hasSearchRequestChanged
     * 
     * @return true, if at least one value of range-bean from or to changed.
     */
    protected boolean hasSearchRequestChanged() {
        return hasSearchRequestChanged != null ? hasSearchRequestChanged : false;
    }

    /**
     * setSelectedElement
     * 
     * @param selected
     */
    public void setSelected(T... selected) {
        Collection<T> selection = getSelectionProvider().getValue();
        selection.clear();
        if (selected != null) {
            for (int i = 0; i < selected.length; i++) {
                selection.add(selected[i]);
            }
        }
    }

    public void selectFirstElement() {
        if (getSelectionProvider() == null || collection == null || collection.isEmpty()) {
            LOG.warn("couldn't select first element - no data or selection-provider available yet!");
            return;
        }
        setSelected(collection.iterator().next());
    }

    /**
     * @return Returns the reloadBean.
     */
    public boolean isReloadBean() {
        return reloadBean;
    }

    /**
     * @param reloadBean The reloadBean to set.
     */
    public void setReloadBean(boolean reloadBean) {
        this.reloadBean = reloadBean;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        //nothing to do on default implementation
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object editItem(Object item) {
        return getPresentationHelper().startUICommandHandler(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkBeforeDelete(Collection selection) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteItem(T item) {
        BeanContainer.instance().delete(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T createItem(T selectedItem) {
        final T newItem;
        if (selectedItem != null && Environment.get("collector.new.clone.selected", true)) {
            try {
                /*
                 * we don't use the apache util to be compatible on all platforms (e.g. without java.bean package)
                 * but the tsl2nano util may cause a classloader exception.
                 * we don't use a deep copy to avoid lazyloading problems
                 */
                newItem = (T) BeanUtil.clone(selectedItem);
                BeanUtil.createOwnCollectionInstances(newItem);
            } catch (final Exception e) {
                ForwardedException.forward(e);
                return null;
            }
        } else {
            /*
             * there is no information how to create a new bean - at least one stored bean instance must exist!
             */
            if (getType() != null && Collection.class.isAssignableFrom(getType())) {
                LOG.warn("There is no information how to create a new bean - at least one stored bean instance must exist!");
                return null;
            }
            newItem = BeanContainer.instance().createBean(getType());
        }
        //assign the new item to the composition parent
        if (composition != null) {
            composition.add(newItem);
        }
        /*
         * if timestamp fields are not shown, generate new timestamps
         */
        if (Environment.get("default.attribute.timestamp", true)) {
            Map<String, IAttributeDefinition<?>> attrs = getAttributeDefinitions();
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            for (IAttributeDefinition<?> a : attrs.values()) {
                if (a instanceof IValueAccess)
                    if (a.temporalType() != null && Timestamp.class.isAssignableFrom(a.temporalType())) {
                        ((IValueAccess) a).setValue(ts);
                    }
            }
        }
        /*
         * the id attribute of the selected bean must not be copied!!!
         * we create an generated value for the id. if jpa annotation @GenerateValue
         * is present, it will overwrite this id.
         */
        final BeanAttribute idAttribute = BeanContainer.getIdAttribute(newItem);
        if (idAttribute != null) {
            IAttributeDef def = Environment.get(IBeanContainer.class).getAttributeDef(newItem,
                idAttribute.getName());
            idAttribute.setValue(newItem,
                StringUtil.fixString(BeanUtil.createUUID(), def.length(), ' ', true));
        }
        return newItem;
    }

    /**
     * creates the new button with listeners inside the parent component
     * 
     * @param dform parent of search button
     */
    protected void createNewAction() {
        final String actionId = BeanContainer.getActionId(getType(), true, "new");
        newAction = new SecureAction<Object>(actionId,
            BeanContainer.getActionText(actionId, false),
            BeanContainer.getActionText(actionId, true),
            IAction.MODE_UNDEFINED) {
            public Object action() throws Exception {
                T newBean = createItem(getFirstSelectedElement());
                if (newBean == null) {
                    throw new FormattedException("tsl2nano.no_type_available");
                }
                final Object result = editItem(newBean);
                if (!IAction.CANCELED.equals(result)) {
                    newBean = (T) beanFinder.unwrapToSelectableBean(result);
                    final T finalBean = newBean;
                    final COLLECTIONTYPE values = collection;
                    values.add(newBean);
                    Bean.getBean((Serializable) newBean).attach(new Runnable() {
                        @Override
                        public void run() {
                            values.remove(finalBean);
                        }
                    });
                    refresh();
                    setSelected(newBean);
                }

                return newBean;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isEnabled() {
                return super.isEnabled() && hasMode(MODE_CREATABLE);
            }

            @Override
            public String getImagePath() {
                return "icons/new.png";
            }
        };
        actions.add(newAction);
    }

    /**
     * creates the delete button with listeners inside the parent component
     * 
     * @param dform parent of search button
     */
    protected void createDeleteAction() {
        final String actionId = BeanContainer.getActionId(getType(), true, "delete");
        deleteAction = new SecureAction<Object>(actionId,
            BeanContainer.getActionText(actionId, false),
            BeanContainer.getActionText(actionId, true),
            IAction.MODE_UNDEFINED) {
            public Object action() throws Exception {
                checkBeforeDelete(getSelectionProvider().getValue());
                final COLLECTIONTYPE values = collection;
                for (final Iterator<T> iterator = getSelectionProvider().getValue().iterator(); iterator.hasNext();) {
                    final T element = iterator.next();
                    deleteItem(element);
                    values.remove(element);
                }
                setSelected(null);
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isEnabled() {
                return super.isEnabled() && hasMode(MODE_CREATABLE) && hasSelection();
            }

            @Override
            public String getImagePath() {
                return "icons/blocked.png";
            }
        };
        actions.add(deleteAction);
//        deleteButton.setEnabled(deleteAction.isEnabled() && selection != null && !selection.isEmpty());
    }

    /**
     * creates the open button with listeners inside the parent component
     * 
     * @param dform parent of search button
     */
    protected void createOpenAction() {
        final String actionId = BeanContainer.getActionId(getType(), true, "open");
        openAction = new SecureAction<Object>(actionId,
            BeanContainer.getActionText(actionId, false),
            BeanContainer.getActionText(actionId, true),
            /*(closeDialogOnOpenAction ? */IAction.MODE_DLG_OK/* : IAction.MODE_UNDEFINED)*/) {
            public Object action() throws Exception {
                if (!hasSelection())
                    return null;
                //eval. the bean, get relations, and couple (after OK) it with table
                final T selectedBean = getFirstSelectedElement();
                T fullBean = reloadBean ? BeanContainer.instance().resolveLazyRelations(selectedBean) : selectedBean;
                fullBean = (T) beanFinder.wrapToDetailBean(fullBean);

                //create the command call
                Object result = editItem(fullBean);
                if (!IAction.CANCELED.equals(result)) {
                    result = beanFinder.unwrapToSelectableBean(result);
                    replaceBean(selectedBean, (T) result);
                    //no selection-change will be fired - but the instance was changed!
                    return result;
                }
                return null;
            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled() && hasMode(MODE_EDITABLE) && hasSelection();
            }

            @Override
            public boolean isDefault() {
                return isEnabled();
            }

            @Override
            public String getImagePath() {
                return "icons/open.png";
            }
        };
        openAction.setDefault(true);
        actions.add(openAction);
    }

    /**
     * will replace the old selection with the new, saved and unwrapped bean
     * 
     * @param selectedBean old selection
     * @param newBean saved and unwrapped bean
     */
    protected void replaceBean(T selectedBean, T newBean) {
        //TODO: the new item will be added to the end - but should be at the same position as the old one
        if (collection.remove(selectedBean))
            collection.add(newBean);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<IPresentableColumn> getColumnDefinitions() {
        if (columnDefinitions == null) {
            final List<IAttributeDefinition<?>> attributes = getBeanAttributes();
            columnDefinitions = new ArrayList<IPresentableColumn>(attributes.size());
            int i = 0;
            for (IAttributeDefinition<?> attr : attributes) {
                IPresentableColumn col = attr.getColumnDefinition();
                if (col == null) {
                    attr.setColumnDefinition(i, IPresentable.UNDEFINED, true, 100);
                    col = attr.getColumnDefinition();
                } else {
                    //if derived from deserialization, the cycling attributeDefinition is null
                    ValueColumn vc = (ValueColumn) col;
                    if (vc.attributeDefinition == null)
                        vc.attributeDefinition = attr;
                }
                columnDefinitions.add((IPresentableColumn) col);
                i++;
            }
            if (Environment.get("collector.use.multiple.filter", true)) {
                //TODO: filtering ids, and invsisbles, too --> don't ask multiple-flag
                columnDefinitions = CollectionUtil.getFiltering(columnDefinitions,
                    new IPredicate<IPresentableColumn>() {
                        @Override
                        public boolean eval(IPresentableColumn arg0) {
                            return (arg0.getPresentable() == null || arg0.getPresentable().isVisible())
                                && hasMode(MODE_SHOW_MULTIPLES)
                                || (getAttribute(arg0.getName()) == null || !getAttribute(arg0.getName())
                                    .isMultiValue());
                        }
                    });
            }
        }
        return columnDefinitions;
    }

    /**
     * setColumnDefinitions
     * 
     * @param columnDefinitions to set
     */
    public void setColumnDefinitions(Collection<IPresentableColumn> columnDefinitions) {
        this.columnDefinitions = columnDefinitions;
    }

    private IPresentableColumn getColumn(int i) {
        //try the fastest - all indexes as default
        String name = getAttributeNames()[i];
        IPresentableColumn column = getAttribute(name).getColumnDefinition();
        if (column != null && column.getIndex() == i)
            return column;
        Collection<IPresentableColumn> colDefs = getColumnDefinitions();
        for (IPresentableColumn c : colDefs) {
            if (c.getIndex() == i)
                return c;
        }
        return null;
    }

    public Object getColumnValue(Object element, int columnIndex) {
        return getColumnValue(element, getAttribute(getColumn(columnIndex).getName()));
    }

    public <V> V getColumnValue(Object element, IAttributeDefinition<V> attribute) {
        if (element == null)
            return null;
        Object value;
        try {
            value = attribute instanceof IValueDefinition ? ((IValueDefinition) attribute).getValue()
                : attribute.getValue(element);
        } catch (Exception e) {
            LOG.warn("beancollector can't create a column text for column '" + attribute.getName()
                + "'! exception: "
                + e.toString());
            value = null;
        }
        return (V) value;
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
        IPresentableColumn col = getColumn(columnIndex);
        //column may be filtered (e.g. id-columns)
        if (col == null)
            return "";
        return getColumnText(element, getAttribute(col.getName()));
    }

    /**
     * {@inheritDoc}
     */
    public String getColumnText(Object element, IAttributeDefinition<?> attribute) {
        Object value = getColumnValue(element, attribute);
        if (value == null)
            return "";
        if (attribute.getFormat() != null) {
            try {
                return attribute.getFormat().format(value);
            } catch (Exception e) {
                // showing the existing values should not throw exceptions...
                LOG.error(e);
                return Messages.TOKEN_MSG_NOTFOUND + e.toString() + Messages.TOKEN_MSG_NOTFOUND;
            }
        }
        return value.toString();
    }

    @Override
    public void shiftSortIndexes() {
        Collection<IPresentableColumn> columns = getColumnDefinitions();
        for (IPresentableColumn c : columns) {
            if (c.getSortIndex() != IPresentable.UNDEFINED)
                if (c.getSortIndex() < columns.size())
                    ((ValueColumn) c).sortIndex++;
                else
                    ((ValueColumn) c).sortIndex = IPresentable.UNDEFINED;
        }
    }

    Integer[] getSortIndexes() {
        //important: do a copy of the origin - otherwise the next time the columns will be arranged through sortindex!
        List<IPresentableColumn> columns = new ArrayList((List<IPresentableColumn>) getColumnDefinitions());
        Collections.sort(columns, new Comparator<IPresentableColumn>() {
            @Override
            public int compare(IPresentableColumn o1, IPresentableColumn o2) {
                return Integer.valueOf(o1.getSortIndex() != IPresentable.UNDEFINED ? o1.getSortIndex()
                    : Integer.MAX_VALUE)
                    .compareTo(Integer.valueOf(o2.getSortIndex() != IPresentable.UNDEFINED ? o2.getSortIndex()
                        : Integer.MAX_VALUE));
            }

        });
        List<Integer> indexes = new ArrayList<Integer>(columns.size());
        for (IPresentableColumn c : columns) {
            if (c.getSortIndex() == IPresentable.UNDEFINED)
                break;//the following will be undefined, too (--> sorting)
            indexes.add(c.getIndex());
        }
        return indexes.toArray(new Integer[0]);
    }

    @Override
    public void sort() {
        final Integer[] sortIndexes = getSortIndexes();
        Comparator<T> comparator = new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                int ci, c;
                for (int i = 0; i < sortIndexes.length; i++) {
                    ci = sortIndexes[i];
//                    c = getColumnText(o1, ci).compareTo(getColumnText(o2, ci));
                    c =
                        BeanPresentationHelper.STRING_COMPARATOR
                            .compare(getColumnValue(o1, ci), getColumnValue(o2, ci));
                    if (c != 0) {
                        //TODO: check performance!
                        c = getColumn(ci).isSortUpDirection() ? c : -1 * c;
                        return c;
                    }
                }
                return 0;
            }
        };
        collection = (COLLECTIONTYPE) CollectionUtil.getSortedList(collection, comparator, getName(), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMultiValue() {
        return true;
    }

    /**
     * getSearchPanelBeans
     * 
     * @return from-bean and to-bean of bean findes filter-range-bean
     */
    public Collection<T> getSearchPanelBeans() {
        if (getBeanFinder() != null) {
            Bean<?> filterRange = getBeanFinder().getFilterRange();
            if (hasMode(MODE_SEARCHABLE) && filterRange != null) {
                List<T> rangeBeans = Arrays.asList((T) filterRange.getValue("from"), (T) filterRange.getValue("to"));
                if (hasSearchRequestChanged == null) {
                    for (T rb : rangeBeans) {
                        //we can't use the bean cache - if empty beans where stored they would be reused!
                        connect(new Bean((Serializable) rb), rb, new CommonAction() {
                            @Override
                            public Object action() throws Exception {
                                return hasSearchRequestChanged = true;
                            }
                        });
                    }
                    ;
                }
                return rangeBeans;
            } else
                return new LinkedList<T>();
        }
        return new LinkedList<T>();
    }

    /**
     * @return Returns the collection.
     */
    public COLLECTIONTYPE getCurrentData() {
        return collection;
    }

    /**
     * getSearchPanelActions
     * 
     * @return
     */
    protected Collection<IAction> getSearchPanelActions() {
        if (hasMode(MODE_SEARCHABLE)) {
            return Arrays.asList((IAction) searchAction, resetAction);
        } else
            return new LinkedList<IAction>();
    }

    public IAction<COLLECTIONTYPE> getSearchAction() {
        return searchAction;
    }

    /**
     * createSearchAction
     * 
     * @return new created search action
     */
    protected IAction<COLLECTIONTYPE> createSearchAction() {
        final String actionId = BeanContainer.getActionId(getType(), true, "search");
        searchAction = new SecureAction<COLLECTIONTYPE>(actionId,
            BeanContainer.getActionText(actionId, false),
            BeanContainer.getActionText(actionId, true),
            IAction.MODE_UNDEFINED) {
            @Override
            public COLLECTIONTYPE action() throws Exception {
                //TODO: fire refresh event
                searchStatus = Messages.getFormattedString("tsl2nano.searchdialog.searchrunning");
                Environment.get(Profiler.class).starting(this, getName());
                COLLECTIONTYPE result = (COLLECTIONTYPE) getBeanFinder().getData();
                long time = Environment.get(Profiler.class).ending(this, getName());
                searchStatus = Messages.getFormattedString("tsl2nano.searchdialog.searchresultdetails",
                    result.size(),
                    DateUtil.getFormattedTimeStamp(),
                    DateUtil.getFormattedMinutes(time));
                if (openAction != null)
                    openAction.setDefault(true);
                return result;
            }

            @Override
            public boolean isDefault() {
                return isEnabled() && (!hasSelection() || hasSearchRequestChanged());
            }

            @Override
            public String getImagePath() {
                return "icons/find.png";
            }
        };
        actions.add(searchAction);
        return searchAction;
    }

    /**
     * createResetAction
     * 
     * @return new created reset action
     */
    protected IAction<?> createResetAction() {
        final String actionId = BeanContainer.getActionId(getType(), true, "reset");
        resetAction = new SecureAction<Object>(actionId,
            BeanContainer.getActionText(actionId, false),
            BeanContainer.getActionText(actionId, true),
            IAction.MODE_UNDEFINED) {
            @Override
            public Object action() throws Exception {
                //TODO: fire refresh event
                BeanUtil.resetValues(getBeanFinder().getFilterRange().getInstance().getFrom());
                BeanUtil.resetValues(getBeanFinder().getFilterRange().getInstance().getTo());
                if (!isStaticCollection && collection != null)
                    collection.clear();
                setSelected();
                return null;
            }

            @Override
            public String getImagePath() {
                return "icons/reload.png";
            }
        };
        actions.add(resetAction);
        return resetAction;
    }

    @Override
    public String getSummary() {
        return searchStatus;
    }

    /**
     * setCompositionParent
     * 
     * @param composition see {@link #composition}
     */
    public void setCompositionParent(Composition composition) {
        this.composition = composition;
    }

    public static final <C extends Collection<I>, I/* extends Serializable*/> BeanCollector<C, I> getBeanCollector(
            Collection<I> collection,
            int workingMode) {
        assert collection != null && collection.size() > 0 : "collection must contain at least one item";
        return getBeanCollector((Class<I>)collection.iterator().next().getClass(), collection, workingMode,
            null);
    }

    /**
     * searches for an existing bean-definition and creates a bean-collector.
     * 
     * @param beanType bean-collectors bean-type
     * @param collection optional prefilling collection (normally done by {@link IBeanFinder#getData(Object, Object)}.
     * @param workingMode working mode of desired collector (see {@link IBeanCollector} for available modes.
     * @return
     */
    public static final <C extends Collection<I>, I/* extends Serializable*/> BeanCollector<C, I> getBeanCollector(Class<I> beanType,
            Collection<I> collection,
            int workingMode,
            Composition composition) {
        BeanDefinition<I> beandef =
            getBeanDefinition(beanType.getSimpleName() + (useExtraCollectorDefinition() ? POSTFIX_COLLECTOR
                : ""),
                beanType,
                false);
        return (BeanCollector<C, I>) createCollector(collection, workingMode, composition, beandef);
    }

    private static boolean useExtraCollectorDefinition() {
        return Environment.get("collector.use.extra.definition", false);
    }

    /**
     * creates a bean-collector through informations of a bean-definition
     * 
     * @param <I>
     * @param collection optional collection for collector
     * @param beandef bean description
     * @return new created bean-collector holding given collection
     */
    protected static <C extends Collection<I>, I/* extends Serializable*/> BeanCollector<C, I> createCollector(C collection,
            int workingMode,
            Composition composition,
            BeanDefinition<I> beandef) {
        BeanCollector<C, I> bc = new BeanCollector<C, I>();
        copy(beandef, bc, "asString");
        //use an own map instance to be independent of changes by other beans or beancollectors.
        bc.attributeDefinitions = new LinkedHashMap<String, IAttributeDefinition<?>>(bc.attributeDefinitions);
        bc.init(collection, new BeanFinder(beandef.getClazz()), workingMode, composition);
        //while deserialization was done on BeanDefinition, we have to do this step manually
        bc.initDeserializing();
        return bc;
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        asString = null;
    }

    @Override
    public String toString() {
        if (asString == null && name != null) {
            //empty search-beans are possible
            asString =
                (useExtraCollectorDefinition() ? Environment.translate("tsl2nano.list", false) + " " : "")
                    + StringUtil.substring(name,
                        null,
                        POSTFIX_COLLECTOR);
        }
        return asString;
    }

    /**
     * Extension for {@link Serializable}
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initDeserialization();
    }

    /**
     * Extension for {@link Serializable}
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        initSerialization();
        out.defaultWriteObject();
    }

    @Persist
    protected void initSerialization() {
        super.initSerialization();
    }

    @Commit
    protected void initDeserializing() {
        init(collection, beanFinder, workingMode, composition);
        if (columnDefinitions != null) {
            for (IPresentableColumn c : columnDefinitions) {
                ((ValueColumn) c).attributeDefinition = getAttribute(c.getName());
            }
        }
    }

    /**
     * getColumnSortingActions
     * 
     * @return
     */
    public Collection<IAction<?>> getColumnSortingActions() {
        Collection<IPresentableColumn> columns = getColumnDefinitions();
        Collection<IAction<?>> actions = new ArrayList<IAction<?>>(columns.size());
        for (IPresentableColumn c : columns) {
            actions.add(c.getSortingAction(this));
        }
        return actions;
    }

    public void addColumnDefinition(String attributeName, int index, int sortIndex, boolean sortUpDirection, int width) {
        getAttribute(attributeName).setColumnDefinition(index, sortIndex, sortUpDirection, width);
    }
}
