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
import java.lang.reflect.Proxy;
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
import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.action.IActivable;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.IAttributeDef;
import de.tsl2.nano.bean.IBeanContainer;
import de.tsl2.nano.bean.IValueAccess;
import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.collection.Entry;
import de.tsl2.nano.collection.FilteringIterator;
import de.tsl2.nano.collection.MapEntrySet;
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.IPredicate;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.execution.Profiler;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.format.FormatUtil;
import de.tsl2.nano.util.DelegatorProxy;
import de.tsl2.nano.util.NumberUtil;

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

    public static final String POSTFIX_QUICKSEARCH = "quicksearch";

    public static final String KEY_COMMANDHANDLER = "bean.commandhandler";

    /** static data or current data representation of this beancollector */
    protected transient COLLECTIONTYPE collection;
    protected transient boolean isStaticCollection = false;

    /** defines the data for this collector through it's getData() method */
    protected transient IBeanFinder<T, ?> beanFinder;
    /** holds the current selection */
    protected transient ISelectionProvider<T> selectionProvider;

    /**
     * holds the connection to the composition parent. if not null, the beancollector will work only on items having a
     * connection to this composition (uml-composition where childs can't exist without it's parent!).
     */
    @Transient
    protected Composition composition;

    protected transient IAction<?> newAction;
    protected transient IAction<?> editAction;
    protected transient IAction<?> deleteAction;
    /** the extending class has to set an instance for the ok action to set it as default button */
    protected transient IAction<?> openAction;

    /** defines the behavior and the actions of the beancollector */
    @Transient
    protected int workingMode = MODE_EDITABLE | MODE_CREATABLE | MODE_SEARCHABLE;
    /**
     * search panel instructions.
     */
    protected transient IAction<COLLECTIONTYPE> searchAction;
    protected transient IAction<COLLECTIONTYPE> quickSearchAction;
    protected transient IAction<?> resetAction;
    transient String searchStatus = Messages.getString("tsl2nano.searchdialog.nosearch");

    /** whether to refresh data from beancontainer before opening edit-dialog */
    @Transient
    protected boolean reloadBean = true;

    @ElementList(name = "column", inline = true, required = false/*, type=ValueColumn.class*/)
    private transient Collection<IPresentableColumn> columnDefinitions;

    /** temporary variable to hold the {@link #toString()} output (--> performance) */
    private transient String asString;

    /**
     * the beancollector should listen to any change of the search-panel (beanfinders range-bean) to know which action
     * should have the focus
     */
    private transient Boolean hasSearchRequestChanged;

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
    public BeanCollector(final COLLECTIONTYPE collection, int workingMode) {
        this(collection, workingMode, null);
    }

    /**
     * see constructor {@link #CollectionEditorBean(IBeanFinder, boolean, boolean, boolean)}.
     */
    public BeanCollector(final COLLECTIONTYPE collection, int workingMode, Composition composition) {
        this((Class<T>) collection.iterator().next().getClass(), collection, workingMode, composition);
    }

    /**
     * see constructor {@link #CollectionEditorBean(IBeanFinder, boolean, boolean, boolean)}.
     */
    public BeanCollector(Class<T> beanType, final COLLECTIONTYPE collection, int workingMode, Composition composition) {
        this(new BeanFinder<T, Object>(beanType), workingMode, composition);
        this.collection = collection;
        if (isStaticCollection || !hasMode(MODE_SEARCHABLE))
            this.searchStatus = "";
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
    protected void init(COLLECTIONTYPE collection,
            IBeanFinder<T, ?> beanFinder,
            int workingMode,
            Composition composition) {
//        setName(Messages.getString("tsl2nano.list") + " " + getName());
        this.isStaticCollection = !Util.isEmpty(collection) ||
            beanFinder == null || (!isPersistable());
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
        if (isStaticCollection)
            searchStatus =
                Messages.getFormattedString("tsl2nano.searchdialog.searchresultcount", this.collection.size());
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
            if (hasFilter())
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
            } else {
                setBeanFinder(new BeanFinder(super.getClazz()));
            }
        }
        return beanFinder.getType();
    }

    @Override
    public boolean isPersistable() {
        return BeanContainer.isInitialized() && BeanContainer.instance().isPersistable(getType());
    }

    @Override
    public void onActivation() {
        super.onActivation();
    }

    @Override
    public void onDeactivation() {
        super.onDeactivation();
        /*
         * if a bean-collector was left through cancel, new created compositions must be removed!
         */
        if (composition != null)
            for (T instance : collection) {
                Bean.getBean((Serializable) instance).onDeactivation();
            }
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
                String lastExpression = null;

                //used by proxy!
                Collection<T> getData() {
                    if (lastExpression != null) {
                        return getData(lastExpression);
                    } else {
                        Collection<T> searchPanelBeans = getSearchPanelBeans();
                        Iterator<T> it = searchPanelBeans.iterator();
                        T from = searchPanelBeans.size() > 0 ? it.next() : null;
                        T to = searchPanelBeans.size() > 1 ? it.next() : null;
                        return (COLLECTIONTYPE) getData(from, to);
                    }
                }

                public Collection<T> getData(T from, Object to) {
                    if (!isStaticCollection || (isPersistable() && composition == null)) {
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
                    return authorized(collection);
                }

                public Collection<T> getData(String expression) {
                    if (!isStaticCollection) {
                        collection =
                            (COLLECTIONTYPE) getValueExpression().matchingObjects(expression);
                    } else if (!betweenFinderCreated) {
                        collection =
                            (COLLECTIONTYPE) CollectionUtil.getFiltering(collection, new StringBuilder(expression));
                        betweenFinderCreated = true;
                    }
                    Collections.sort((List) collection, getValueExpression().getComparator());
                    return authorized(collection);
                }

                private Collection<T> authorized(COLLECTIONTYPE collection) {
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
     * hasFilter
     * 
     * @return true, if bean-finder could create an filter-range instance
     */
    public boolean hasFilter() {
        return getBeanFinder().getFilterRange() != null;
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
        if (!CompositionFactory.markToPersist(item) && Bean.getBean((Serializable) item).getId() != null)
            BeanContainer.instance().delete(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T createItem(T selectedItem) {
        T newItem = null;
        Class<T> type = getType();
        /*
         * do a copy of the selected element (only if exactly one element was selected) and the property was set.
         */
        if (selectedItem != null && getSelectionProvider() != null && getSelectionProvider().getValue().size() == 1
            && Environment.get("collector.new.clone.selected", true)
            && !(Entry.class.isAssignableFrom(type))) {
            try {
                /*
                 * we don't use the apache util to be compatible on all platforms (e.g. without java.bean package like the dalvik vm)
                 * but the tsl2nano util may cause a classloader exception.
                 * we don't use a deep copy to avoid lazyloading problems
                 */
                newItem = (T) BeanUtil.clone(selectedItem);
                BeanUtil.createOwnCollectionInstances(newItem);
            } catch (final Exception e) {
                LOG.error(e);
                Message.send("Couldn't copy selected element!");
            }
        }
        if (newItem == null) {
            /*
             * there is no information how to create a new bean - at least one stored bean instance must exist!
             */
            if (type != null && Collection.class.isAssignableFrom(type)) {
                LOG.warn("There is no information how to create a new bean - at least one stored bean instance must exist!");
                return null;
            } else if (Entry.class.isAssignableFrom(type)) {
                // normally we would handle this inside the generic else block, but we need
                // the generics key and value type informations
                if (Proxy.isProxyClass(collection.getClass()))
                    newItem = (T) ((MapEntrySet) (FilteringIterator.getIterable((Proxy) collection))).add(null, null);
                else
                    newItem = (T) ((MapEntrySet) (collection)).add(null, null);
            } else {
                newItem = BeanContainer.instance().createBean(getType());
            }
            setDefaultValues(newItem);
        }
        /*
         * if timestamp fields are not shown, generate new timestamps
         */
        if (Environment.get("default.attribute.timestamp", true)) {
            //respect all attributes
            BeanClass<T> bc = BeanClass.getBeanClass(getDeclaringClass());
            List<IAttribute> attrs = bc.getAttributes();
//            Map<String, IAttributeDefinition<?>> attrs = getAttributeDefinitions();
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            for (IAttribute a : attrs) {
                IAttributeDef def = BeanContainer.instance().getAttributeDef(newItem, a.getName());
                if (!def.nullable() && def.temporalType() != null
                    && Timestamp.class.isAssignableFrom(def.temporalType())) {
//                    if (a instanceof IValueAccess) {
//                        ((IValueAccess) a).setValue(ts);
//                    } else {
                    a.setValue(newItem, ts);
//                    }
                }
            }
        }
        /*
         * the id attribute of the selected bean must not be copied!!!
         * we create a generated value for the id. if jpa annotation @GenerateValue
         * is present, it will overwrite this id.
         */
        final BeanAttribute idAttribute = BeanContainer.getIdAttribute(newItem);
        if (idAttribute != null) {
            IAttributeDef def = Environment.get(IBeanContainer.class).getAttributeDef(newItem,
                idAttribute.getName());
            idAttribute.setValue(newItem, /*composition != null ?
                                          StringUtil.fixString(BeanUtil.createUUID(), (def.length() > -1 ? def.length() : 0), ' ', true) : */
                null);
        }

        /*
         * assign the new item to the composition parent. this should be done last, 
         * because of the equals/hash difference to existing items.
         */
        if (composition != null) {
            composition.add(newItem);
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
                    throw new ManagedException("tsl2nano.no_type_available");
                }
                final Object result = editItem(newBean);
                if (!IAction.CANCELED.equals(result)) {
                    newBean = (T) beanFinder.unwrapToSelectableBean(result);
                    final T finalBean = newBean;
                    final COLLECTIONTYPE values = collection;
                    values.add(newBean);
                    final Bean b = Bean.getBean((Serializable) newBean);
                    b.attach(new CommonAction<Void>() {
                        @Override
                        public Void action() throws Exception {
                            if (!values.remove(finalBean)) {
                                /*
                                 * Workaround for a HashSet on Beans with changing hashcodes.
                                 * Here we use equals() instead of hashcode()
                                 */
                                for (Iterator it = values.iterator(); it.hasNext();) {
                                    if (it.next().equals(finalBean))
                                        it.remove();
                                }
                            }
                            getSelectionProvider().getValue().remove(finalBean);
                            //on an error after persisting the new persisted bean may be already added
                            values.remove(b.getInstance());
                            getSelectionProvider().getValue().remove(b.getInstance());
                            if (!"remove".equals(getParameter(0))) {
                                values.add((T) b.getInstance());
                                getSelectionProvider().getValue().add((T) b.getInstance());
                            } else {
                                if (composition != null)
                                    composition.remove(finalBean);
                            }
                            return null;
                        }
                    });
                    refresh();
                    getSelectionProvider().getValue().add(newBean);
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
                return isEnabled()/* && !isconnected*/;
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

    @Override
    public void setAttributeFilter(String... availableAttributes) {
        super.setAttributeFilter(availableAttributes);
        columnDefinitions = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<IPresentableColumn> getColumnDefinitions() {
        if (columnDefinitions == null) {
            columnDefinitions = createColumnDefinitions(this, new IActivable() {
                @Override
                public boolean isActive() {
                    return hasMode(MODE_SHOW_MULTIPLES);
                }
            });
        }
        return columnDefinitions;
    }

    /**
     * getColumnDefinitionsIndexSorted
     * 
     * @return visible columns index-sorted
     */
    public List<IPresentableColumn> getColumnDefinitionsIndexSorted() {
        Collection<IPresentableColumn> colDefs = getColumnDefinitions();
        List<IPresentableColumn> colDefCopy = new ArrayList<IPresentableColumn>(colDefs.size());
        IPresentableColumn c = null;
        //we have to use the iterator to get the filtered data!
        for (Iterator<IPresentableColumn> it = colDefs.iterator(); it.hasNext();) {
            c = it.next();
            if (c.getPresentable().isVisible())
                colDefCopy.add(c);
        }
        Collections.sort(colDefCopy);
        return colDefCopy;
    }

    /**
     * createColumnDefinitions
     * 
     * @param def
     * @param showMultiples
     * @return
     */
    static <T> Collection<IPresentableColumn> createColumnDefinitions(final BeanDefinition<T> def,
            final IActivable showMultiples) {
        final List<IAttributeDefinition<?>> attributes = def.getBeanAttributes();
        Collection<IPresentableColumn> columnDefinitions = new ArrayList<IPresentableColumn>(attributes.size());
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
            //TODO: filtering ids, and invisibles, too --> don't ask multiple-flag
            columnDefinitions = CollectionUtil.getFiltering(columnDefinitions,
                new IPredicate<IPresentableColumn>() {
                    @Override
                    public boolean eval(IPresentableColumn arg0) {
                        return (arg0.getPresentable() == null || arg0.getPresentable().isVisible())
                            && showMultiples.isActive()
                            || (def.getAttribute(arg0.getName()) == null || !def.getAttribute(arg0.getName())
                                .isMultiValue());
                    }
                });
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
     * evaluates the column-index sorted column descriptions
     * 
     * @return column labels to be presented as table header
     */
    public List<String> getColumnLabels() {
        List<IPresentableColumn> colDefs = new ArrayList<IPresentableColumn>(getColumnDefinitions());
        Collections.sort(colDefs);
        List<String> cnames = new ArrayList<String>(colDefs.size());

        for (IPresentableColumn c : colDefs) {
            if (c.getPresentable().isVisible())
                cnames.add(c.getDescription());
        }
        return cnames;
    }

    private IPresentableColumn getColumn(int i) {
        //try the fastest - all indexes as default
        String[] names = getAttributeNames();
        if (names.length > i) {
            String name = names[i];
            IPresentableColumn column = getAttribute(name).getColumnDefinition();
            if (column != null && column.getIndex() == i)
                return column;
        }
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

    /**
     * delegates to {@link #getColumnValueEx(Object, IAttributeDefinition)} catching the exception.
     * 
     * @param element bean instance
     * @param attribute bean attribute
     * @return column value
     */
    public <V> V getColumnValue(Object element, IAttributeDefinition<V> attribute) {
        try {
            return getColumnValueEx(element, attribute);
        } catch (Exception e) {
            LOG.error("beancollector can't create a column text for column '" + attribute.getName()
                + "'!", e);
            return null;
        }
    }

    /**
     * @param element bean instance
     * @param attribute bean attribute
     * @return column value
     */
    protected <V> V getColumnValueEx(Object element, IAttributeDefinition<V> attribute) {
        if (element == null)
            return null;
        Object value;
        value = attribute instanceof IValueDefinition ? ((IValueDefinition) attribute).getValue()
            : attribute.getValue(element);
        return (V) value;
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
        IPresentableColumn col = getColumn(columnIndex);
        //column may be filtered (e.g. id-columns)
        if (col == null)
            return "";
        else if (col instanceof ValueColumn)
            return getColumnText(element, ((ValueColumn) col).attributeDefinition);
        return getColumnText(element, getAttribute(col.getName()));
    }

    /**
     * {@inheritDoc}
     */
    public String getColumnText(Object element, IAttributeDefinition<?> attribute) {
        Object value;
        try {
            value = getColumnValueEx(element, attribute);
            if (value == null)
                return "";
            if (attribute.getFormat() != null) {
                return attribute.getFormat().format(value);
            } else {
                FormatUtil.getDefaultFormat(value, false).format(value);
            }
        } catch (Exception e) {
            // showing the existing values should not throw exceptions...
            LOG.error(e);
            return Messages.TOKEN_MSG_NOTFOUND + e.toString() + Messages.TOKEN_MSG_NOTFOUND;
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
                        //we can't use the bean cache - if empty beans were stored they would be reused!
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
        final String actionId =
            isVirtual() ? getName() + ".search" : BeanContainer.getActionId(getType(), true, "search");
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
                searchStatus = Messages.getFormattedString("tsl2nano.searchdialog.searchresultcount",
                    result.size());
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
     * getQuickSearchAction
     * 
     * @return search action for value expressions
     */
    public IAction<COLLECTIONTYPE> getQuickSearchAction() {
        if (quickSearchAction == null) {
            final String actionId =
                isVirtual() ? getName() + "." + POSTFIX_QUICKSEARCH : BeanContainer.getActionId(getType(), true,
                    POSTFIX_QUICKSEARCH);
            quickSearchAction = new SecureAction<COLLECTIONTYPE>(actionId,
                "",
                BeanContainer.getActionText(actionId, true),
                IAction.MODE_UNDEFINED) {
                @Override
                public COLLECTIONTYPE action() throws Exception {
                    //TODO: fire refresh event
                    searchStatus = Messages.getFormattedString("tsl2nano.searchdialog.searchrunning");
                    Environment.get(Profiler.class).starting(this, getName());
                    COLLECTIONTYPE result = (COLLECTIONTYPE) getBeanFinder().getData((String) getParameter(0));
                    searchStatus = Messages.getFormattedString("tsl2nano.searchdialog.searchresultcount",
                        result.size());
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
        }
        return quickSearchAction;
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
                searchStatus =
                    isStaticCollection || !hasMode(MODE_SEARCHABLE) ? "" : Messages
                        .getString("tsl2nano.searchdialog.nosearch");
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
     * getComposition
     * @return
     */
    public Composition getComposition() {
        return composition;
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
        ManagedException.assertion(collection != null && collection.size() > 0,
            "collection is empty but must contain at least one item");
        return getBeanCollector((Class<I>) BeanClass.getDefiningClass(collection.iterator().next().getClass()),
            collection, workingMode,
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
            (BeanDefinition<I>) (beanType.isArray() && !Util.isEmpty(collection) ? Bean
                .getBean((Serializable) collection.iterator().next()) :
                getBeanDefinition(beanType.getSimpleName() + (useExtraCollectorDefinition() ? POSTFIX_COLLECTOR
                    : ""),
                    beanType,
                    false));
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
        copy(beandef, bc, "asString", "presentationHelper");
        //use an own map instance to be independent of changes by other beans or beancollectors.
        bc.attributeDefinitions = new LinkedHashMap<String, IAttributeDefinition<?>>(bc.attributeDefinitions);
        bc.init(collection, new BeanFinder(beandef.getClazz()), workingMode, composition);
        //while deserialization was done on BeanDefinition, we have to do this step manually
        bc.initDeserialization();
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

    @Commit
    protected void initDeserialization() {
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

    public static <T> ValueHolder<BeanCollector<?, T>> createBeanCollectorHolder(Collection<T> instance, int mode) {
        return new ValueHolder<BeanCollector<?, T>>(getBeanCollector(instance, mode));
    }
}
