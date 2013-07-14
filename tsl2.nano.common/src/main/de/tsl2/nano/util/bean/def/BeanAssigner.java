package de.tsl2.nano.util.bean.def;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tsl2.nano.collection.ListSet;
import de.tsl2.nano.exception.FormattedException;
import de.tsl2.nano.util.StringUtil;
import de.tsl2.nano.util.bean.BeanAttribute;
import de.tsl2.nano.util.bean.BeanContainer;
import de.tsl2.nano.util.bean.BeanUtil;

/**
 * simple bean assigner usable for two-list-selection. on construction, the elements contained in source and dest item
 * collection will be removed in the source item collection. the source item collection will be additionally wrapped
 * into a {@link ListSet} to provide both interfaces - but losing the direct instance connection. the dest item list
 * wont be wrapped to obtain the original collection instance.
 * <p/>
 * be careful to implement the {@link Object#hashCode()} and {@link Object#equals(Object)} methods, because the moved
 * elements will be removed from the other list.
 * <p/>
 * if you work on a special gui implementation like richfaces, you have to wrap your dest-item collection into a new
 * instance of {@link ListSet} before calling the BeanAssigner constructor. giving the dest item collection to the gui
 * framework (through your managed bean) you have to add all dest items to your source item collection - otherwise they
 * wont be visible.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class BeanAssigner<T> {

    protected Collection<T> sourceItems;
    protected Collection<T> destItems;
    private String idAttributeName;
    protected static final Log LOG = LogFactory.getLog(BeanAssigner.class);

    /**
     * constructor
     * 
     * @param sourceItems source item list
     * @param destItems (optional) destination item list
     * @param idAttributeName (optional) attribute name containing the real id of the items to be filtered from
     *            sourceItems
     */
    public BeanAssigner(Collection<T> sourceItems, Collection<T> destItems, String idAttributeName) {
        LOG.debug("sourceItems: " + StringUtil.toFormattedString(sourceItems, 200));
        LOG.debug("destItems: " + StringUtil.toFormattedString(destItems, 200));
        this.sourceItems = new ListSet<T>(sourceItems);
        if (this.sourceItems.size() < sourceItems.size()) {
            LOG.warn("sourceItems in hashSet removed! please check your hashCode() and equals() methods!");
        }
        this.destItems = destItems != null ? destItems : new ListSet<T>();
        this.idAttributeName = idAttributeName;
        //filter currently assigned items from all items
        if (idAttributeName != null && sourceItems.size() > 0) {
            final Class<T> beanType = (Class<T>) sourceItems.iterator().next().getClass();
            final Collection<T> toRemove = new LinkedList<T>();
            for (final T d : destItems) {
                for (final T s : sourceItems) {
                    final BeanAttribute attr = BeanAttribute.getBeanAttribute(beanType, idAttributeName);
                    final T sValue = (T) attr.getValue(s);
                    final T dValue = (T) attr.getValue(d);
                    if (sValue == dValue || (sValue != null && BeanUtil.equals(sValue, dValue))) {
                        toRemove.add(s);
                    }
                }
            }
            LOG.info("removing already assigned objects from available objects: " + StringUtil.toFormattedString(toRemove,
                200));
            this.sourceItems.removeAll(toRemove);
        } else {
            if (destItems != null)
                this.sourceItems.removeAll(destItems);
        }
    }

    /**
     * override this method, if you work on entity beans with generic ids. you should assign the parent object.
     * 
     * @param selectedSourceItems items to move to the destination list.
     */
    public void moveToDest(Collection<T> selectedSourceItems) {
        assert sourceItems.containsAll(selectedSourceItems);
        if (!(sourceItems.removeAll(selectedSourceItems))) {
            throw new FormattedException("tsl2nano.unexpectederror", null);
        }

        destItems.addAll(selectedSourceItems);
    }

    /**
     * override this method, if you work on entity beans with generic ids. you should remove the ids.
     * 
     * @param destItem items to move to the source list.
     */
    public void moveToSource(Collection<T> selectedDestItems) {
        assert destItems.containsAll(selectedDestItems);
        if (!(destItems.removeAll(selectedDestItems))) {
            throw new FormattedException("tsl2nano.unexpectederror", null);
        }

        sourceItems.addAll(selectedDestItems);
    }

    /**
     * moveAllToSource
     */
    public void moveAllToSource() {
        moveToSource(new ArrayList<T>(destItems));
    }

    /**
     * moveAllToDest
     */
    public void moveAllToDest() {
        moveToDest(new ArrayList<T>(sourceItems));
    }

    /**
     * @return Returns the sourceItems. please read the description of {@link BeanAssigner}.
     */
    public Collection<T> getSourceItems() {
        return sourceItems;
    }

    /**
     * @return Returns the destItems. please read the description of {@link BeanAssigner}.
     */
    public Collection<T> getDestItems() {
        return destItems;
    }

    /**
     * @param sourceItems The sourceItems to set.
     */
    public void setSourceItems(Collection<T> sourceItems) {
        //do nothing
        //this.sourceItems = sourceItems;
    }

    /**
     * @param destItems The destItems to set.
     */
    public void setDestItems(Collection<T> destItems) {
        //do nothing
        //        this.destItems = destItems;
    }

    /**
     * @return Returns the idAttributeName.
     */
    public String getIdAttributeName() {
        return idAttributeName;
    }

    /**
     * wraps the a simple selection type into an assigned object type. through a 'findAll' of the source type a list
     * will be evaluated. for each item of that list, a new dest object (with uuid on idAttributeName) will be created.
     * on the dest object type, an attribute of type 'sourceType' will be searched, and the setter will be called to
     * assign the simple source object.
     * <p>
     * WARNING: The {@link BeanContainer} must be initialized - to create and find bean instances.
     * 
     * @param <SOURCE> source type
     * @param <DEST> wrapping type
     * @param sourceType source type
     * @param destType wrapping type
     * @param idAttributeName id attribute
     * @return wrapped collection
     */
    public static <SOURCE, DEST> Collection<DEST> getWrappedCollection(Class<SOURCE> sourceType,
            Class<DEST> destType,
            String idAttributeName,
            int maxCount) {
        final Collection<SOURCE> allItems = BeanContainer.instance().getBeans(sourceType, maxCount);
        return getWrappedCollection(allItems, destType, idAttributeName, maxCount);
    }

    /**
     * wraps the a simple selection type into an assigned object type. through a 'findAll' of the source type a list
     * will be evaluated. for each item of that list, a new dest object (with uuid on idAttributeName) will be created.
     * on the dest object type, an attribute of type 'sourceType' will be searched, and the setter will be called to
     * assign the simple source object.
     * <p>
     * WARNING: The {@link BeanContainer} must be initialized - to create bean instances.
     * 
     * @param <SOURCE> source type
     * @param <DEST> wrapping type
     * @param sourceType source type
     * @param destType wrapping type
     * @param idAttributeName id attribute
     * @return wrapped collection
     */
    public static <SOURCE, DEST> Collection<DEST> getWrappedCollection(Collection<SOURCE> availableItems,
            Class<DEST> destType,
            String idAttributeName,
            int maxCount) {
        //availableItems must have at least one item - otherwise the assigner is senseless!
        final Class<SOURCE> sourceType = (Class<SOURCE>) availableItems.iterator().next().getClass();
        final Collection<DEST> allWrappedItems = new LinkedList<DEST>();
        final BeanAttribute srcAttribute = BeanAttribute.getBeanAttribute(destType,
            BeanAttribute.getAttributeName(sourceType));
        final BeanAttribute idAttribute = BeanAttribute.getBeanAttribute(destType, idAttributeName);
        for (final SOURCE g : availableItems) {
            final DEST bg = BeanContainer.isInitialized() ? BeanContainer.instance().createBean(destType)
                : BeanContainer.createBeanInstance(destType);
            idAttribute.setValue(bg, UUID.randomUUID().toString());
            srcAttribute.setValue(bg, g);
            allWrappedItems.add(bg);
        }
        return allWrappedItems;
    }

    /**
     * createAssigner, uses {@link #getWrappedCollection(Class, Class, String, int)} to evaluate the available item
     * list. please read the description of {@link BeanAssigner}.
     * 
     * @param <T>
     * @param type list type
     * @param destItems (optional) destination item list
     * @param idAttributeName (optional) attribute name containing the real id of the items to be filtered from
     *            sourceItems
     * @param wrappedBeanAttributeName attribute name of wrapped bean
     * @param maxCount maximum count of available items to load
     * @return new filled BeanAssigner instance
     */
    public static <S, T> BeanAssigner<T> createAssigner(Collection<S> availableItems,
            Class<T> type,
            Collection<T> dest,
            String idAttributeName,
            String wrappedBeanAttributeName,
            int maxCount) {
        return new BeanAssigner<T>(getWrappedCollection(availableItems, type, idAttributeName, maxCount),
            dest,
            wrappedBeanAttributeName);
    }

    /**
     * createAssigner, uses {@link #getWrappedCollection(Class, Class, String, int)} to evaluate the available item
     * list.please read the description of {@link BeanAssigner}.
     * 
     * @param <T>
     * @param type list type
     * @param destItems (optional) destination item list
     * @param idAttributeName (optional) attribute name containing the real id of the items to be filtered from
     *            sourceItems
     * @param wrappedBeanAttributeName attribute name of wrapped bean
     * @param maxCount maximum count of available items to load
     * @return new filled BeanAssigner instance
     */
    public static <T> BeanAssigner<T> createAssigner(Class<T> type,
            Collection<T> dest,
            String idAttributeName,
            String wrappedBeanAttributeName,
            int maxCount) {
        return new BeanAssigner<T>(getWrappedCollection(type, type, idAttributeName, maxCount),
            dest,
            wrappedBeanAttributeName);
    }
}