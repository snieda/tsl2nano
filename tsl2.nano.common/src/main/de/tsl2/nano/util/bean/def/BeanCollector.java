/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jul 20, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.util.bean.def;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;

import de.tsl2.nano.Environment;
import de.tsl2.nano.Messages;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.execution.Profiler;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.util.DateUtil;
import de.tsl2.nano.util.DelegatorProxy;
import de.tsl2.nano.util.NumberUtil;
import de.tsl2.nano.util.bean.BeanAttribute;
import de.tsl2.nano.util.bean.BeanContainer;
import de.tsl2.nano.util.bean.BeanUtil;

/**
 * see {@link IBeanCollector}. the collector inherits from {@link BeanDefinition}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Default(value = DefaultType.FIELD, required = false)
public class BeanCollector<COLLECTIONTYPE extends Collection<T>, T> extends BeanDefinition<T> implements
        IBeanCollector<COLLECTIONTYPE, T> {
    /** serialVersionUID */
    private static final long serialVersionUID = -5260557109700841750L;

    private static final Log LOG = LogFactory.getLog(BeanCollector.class);

    protected COLLECTIONTYPE collection;
    protected IBeanFinder<T, ?> beanFinder;
    transient ISelectionProvider<T> selectionProvider;

    IAction<?> newAction;
    IAction<?> editAction;
    IAction<?> deleteAction;
    /** the extending class has to set an instance for the ok action to set it as default button */
    protected IAction<?> openAction;

    protected int workingMode = MODE_EDITABLE | MODE_CREATABLE | MODE_SEARCHABLE;
    /**
     * search panel instructions.
     */
    IAction<COLLECTIONTYPE> searchAction;
    IAction<?> resetAction;
    String searchStatus = Messages.getString("swartifex.searchdialog.nosearch");

    /** whether to refresh data from beancontainer before opening edit-dialog */
    protected boolean reloadBean = true;

    private Collection<IPresentableColumn> columnDefinitions;

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
        this(new BeanFinder<T, Object>(beanType), workingMode);
    }

    /**
     * see constructor {@link #CollectionEditorBean(IBeanFinder, boolean, boolean, boolean)}.
     */
    public BeanCollector(Class<T> beanType, final COLLECTIONTYPE collection, int workingMode) {
        this(new BeanFinder<T, Object>(beanType), workingMode);
        this.collection = collection;
    }

    /**
     * see constructor {@link #CollectionEditorBean(IBeanFinder, boolean, boolean, boolean)}.
     */
    public BeanCollector(final COLLECTIONTYPE collection, int workingMode) {
        this(new BeanFinder(collection.iterator().next().getClass()) /*{
                                                                     @Override
                                                                     public Collection getData(Object fromFilter, Object toFilter) {
                                                                     return collection;
                                                                     }

                                                                     }*/, workingMode);
        this.collection = collection;
    }

    /**
     * constructor
     * 
     * @param beanFinder implementation to evaluate the data
     * @param workingMode one of {@link IBeanCollector#MODE_EDITABLE}, {@link IBeanCollector#MODE_CREATABLE} etc. Please
     *            see {@link IBeanCollector} for more modes.
     */
    public BeanCollector(IBeanFinder<T, Object> beanFinder, int workingMode) {
        super(beanFinder.getType());
        init(beanFinder, workingMode);
    }

    /**
     * init
     * 
     * @param beanFinder implementation to evaluate the data
     * @param workingMode one of {@link IBeanCollector#MODE_EDITABLE}, {@link IBeanCollector#MODE_CREATABLE} etc. Please
     *            see {@link IBeanCollector} for more modes.
     */
    private void init(IBeanFinder<T, ?> beanFinder, int workingMode) {
        setName(Messages.getString("swartifex.list") + " " + getName());
        this.collection = (COLLECTIONTYPE) new LinkedList<T>();
        setBeanFinder(beanFinder);
        this.workingMode = workingMode;

//        if (beanFinder != null) {
        actions = new LinkedHashSet<IAction>();
        if (hasMode(MODE_SEARCHABLE)) {
            createSearchAction();
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
     * Extension for {@link Serializable}
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        //actions are transient - we reconstruct the default actions.
        in.defaultReadObject();
        init(beanFinder, workingMode);
    }

    /**
     * getBeanType
     * 
     * @return optional beanType (content type of collection)
     */
    protected Class<T> getType() {
        //try to evaluate the collection content type
        if (beanFinder == null) {
            if (collection.size() > 0) {
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
            Object internalBeanFinder = new Serializable() {
                boolean betweenFinderCreated = false;

                @SuppressWarnings("unused")
                //used by proxy!
                Collection<T> getData() {
                    Collection<T> searchPanelBeans = getSearchPanelBeans();
                    Iterator<T> it = searchPanelBeans.iterator();
                    T from = searchPanelBeans.size() > 0 ? it.next() : null;
                    T to = searchPanelBeans.size() > 1 ? it.next() : null;
                    return (COLLECTIONTYPE) getData(from, to);
                }

                public Collection<T> getData(T from, Object to) {
                    if (BeanContainer.isInitialized() && BeanContainer.instance().isPersistable(getType()))
                        collection = (COLLECTIONTYPE) ((IBeanFinder<T, Object>) beanFinder).getData(from, to);
                    else if (!betweenFinderCreated) {
                        collection = (COLLECTIONTYPE) CollectionUtil.getFilteringBetween(collection, from, (T) to, true);
                        betweenFinderCreated = true;
                    }
                    return collection;
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
    protected boolean hasSelection() {
        return getFirstSelectedElement() != null;
    }

    /**
     * setSelectedElement
     * 
     * @param selected
     */
    protected void setSelectedElement(T selected) {
        Collection<T> selection = getSelectionProvider().getValue();
        selection.clear();
        if (selected != null)
            selection.add(selected);
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
    public Object editItem(Object bean) {
        //TODO: use incubation class Environment to get an optional command handler/service
//        final Map par = new HashMap<String, Object>();
//        par.put("bean", wrapBean(bean));
//        return WorkbenchUtil.executeCommand(OpenDialogCommandHandler.CMD_OPEN_DIALOG,
//            OpenDialogCommandHandler.CMD_OPEN_DIALOG_PARAMETER,
//            par);
        return bean;
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
        if (selectedItem != null) {
            try {
                /*
                 * we don't use the apache util to be compatible on all platforms (e.g. without java.bean package) - but the swartifex util may cause a classloader exception.
                 */
                final T cloneBean = (T) BeanUtil.clone(selectedItem);
                //the id attribute of the selected bean must not be copied!!!
                final BeanAttribute idAttribute = BeanContainer.getIdAttribute(cloneBean);
                if (idAttribute != null) {
                    idAttribute.setValue(cloneBean, null);
                }
                return cloneBean;
            } catch (final Exception e) {
                ForwardedException.forward(e);
                return null;
            }
        } else {
            /*
             * there is no information how to create a new bean - at least one stored bean instance must exist!
             */
            if (getType() != null && Collection.class.isAssignableFrom(getType())) {
                return null;
            }
            return BeanContainer.instance().createBean(getType());
        }
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
                    throw new FormattedException("swartifex.no_type_available");
                }
                final Object result = editItem(newBean);
                if (!IAction.CANCELED.equals(result)) {
                    newBean = (T) beanFinder.unwrapToSelectableBean(result);
                    final COLLECTIONTYPE values = collection;
                    values.add(newBean);
                    refresh();
                    setSelectedElement(newBean);
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
                return "config/icons/new.png";
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
                setSelectedElement(null);
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
                return "config/icons/blocked.png";
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
            public String getImagePath() {
                return "config/icons/open.png";
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
            List<IAttributeDefinition<?>> attributes = getBeanAttributes();
            columnDefinitions = new ArrayList<IPresentableColumn>(attributes.size());
            int i = 0;
            for (IAttributeDefinition<?> attr : attributes) {
                IColumn col = attr.getColumnDefinition();
                if (col == null) {
                    attr.setColumnDefinition(i, i, true, 100);
                    col = attr.getColumnDefinition();
                }
                columnDefinitions.add((IPresentableColumn) col);
                i++;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnText(Object element, int columnIndex) {
        IAttributeDefinition attribute = getAttribute(getAttributeNames()[columnIndex]);
        if (element == null)
            return "";
        Object value;
        try {
            value = attribute.getValue(element);
        } catch (Exception e) {
            LOG.warn("beancollector can't create a column text for column index " + columnIndex
                + "! exception: "
                + e.toString());
            value = null;
        }
        if (value == null)
            return "";
        return attribute.getFormat() != null ? attribute.getFormat().format(value) : value.toString();
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
            if (hasMode(MODE_SEARCHABLE) && filterRange != null)
                return Arrays.asList((T) filterRange.getValue("from"), (T) filterRange.getValue("to"));
            else
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
                searchStatus = Messages.getFormattedString("swartifex.searchdialog.searchrunning");
                Environment.get(Profiler.class).starting(this, getName());
                COLLECTIONTYPE result = (COLLECTIONTYPE) getBeanFinder().getData();
                long time = Environment.get(Profiler.class).ending(this, getName());
                searchStatus = Messages.getFormattedString("swartifex.searchdialog.searchresultdetails",
                    result.size(),
                    DateUtil.getFormattedTimeStamp(),
                    DateUtil.getFormattedMinutes(time));
                return result;
            }
            @Override
            public String getImagePath() {
                return "config/icons/find.png";
            }
        };
        searchAction.setDefault(true);
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
                return null;
            }
            @Override
            public String getImagePath() {
                return "config/icons/reload.png";
            }
        };
        actions.add(resetAction);
        return resetAction;
    }

    @Override
    public String getSummary() {
        return searchStatus;
    }
}
