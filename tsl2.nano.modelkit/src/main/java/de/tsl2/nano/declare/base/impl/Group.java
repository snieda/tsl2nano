package de.tsl2.nano.modelkit.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.tsl2.nano.modelkit.Identified;
import de.tsl2.nano.modelkit.Selectable;

/**
 * Sorting Group, selecting its items through a selector and providing a comparator chain. Each comparator is asked, if it is the
 * right one (canSelect) and can have sub-comparators used on compare-equality. For more informations,
 * <p/>
 * see {@link #Group(String, int, String, int, String, boolean, String...)}<br>
 * see {@link #sort(List)}<br>
 * see {@link #groupByLimit(List)}<br>
 * see {@link #insertion(List, Group, Group)}
 */
public class Group<T> extends AbstractIdentified implements Comparator<T>, Selectable<T> {
    int limit;
    int step;
    String selectorFact;
    List<String> comparatorNames;
    private String groupFunction;
    private boolean insertionLimitFallDown;

    public Group(String name, int limit, String groupFunction, int step, String selectorFact, String... comparatorNames) {
        this(name, limit, groupFunction, step, selectorFact, false, comparatorNames);
    }

    /**
     * creates a new Group. a group sorts elements through comparators, splits subgroups through given groupFunction and limit,
     * and inserts group items on higher positions given by step/limit/insertionLimitFallDown.
     *
     * @param name
     *            unique name
     * @param limit
     *            primarily used by group splitting to a max of given limit. secondarily used by insertion to indicate limit of
     *            equal insertions
     * @param groupFunction
     *            defines group splitting and is used on insertion limit
     * @param step
     *            used by insertion for insertion position - and shifting index on reached limit
     * @param selectorFact
     *            defines the filter for this group
     * @param insertionLimitFallDown
     *            used by insertion to indicate if an insertion item falls to end of group on reached limit
     * @param comparatorNames
     *            selectable comparators to be used by group sorting
     */
    public Group(String name, int limit, String groupFunction, int step, String selectorFact, boolean insertionLimitFallDown,
        String... comparatorNames) {
        super(name);
        this.limit = limit;
        this.groupFunction = groupFunction;
        this.step = step;
        this.insertionLimitFallDown = insertionLimitFallDown;
        this.selectorFact = selectorFact;
        this.comparatorNames = List.of(comparatorNames);
    }

    @Override
    public void validate() {
        checkExistence(groupFunction, Func.class);
        checkExistence(selectorFact, Fact.class);
        checkExistence(NamedComparator.class, comparatorNames);
    }

    @Override
    public boolean canSelect(T item) {
        return canSelect(selectorFact, item);
    }

    @Override
    public int compare(T o1, T o2) {
        int result = 0;
        // TODO: not performance optimized
        List<NamedComparator> comparators = get(NamedComparator.class);
        for (String name : comparatorNames) {
            NamedComparator comparator = Identified.get(comparators, name);
            if (comparator.canSelect(Arrays.asList(o1, o2))) {
                result = comparator.compare(o1, o2);
                if (result != 0) {
                    break;
                }
            }
        }
        return result;
    }

    public int getLimit() {
        return limit;
    }

    public int getStep() {
        return step;
    }

    public List<T> filter(List<T> items) {
        return items.stream().filter(t -> canSelect(t)).collect(Collectors.toList());
    }

    /** main action for group. does the sorting wich comparators, splitting subgroups and insertions. */
    public List<T> sort(List<T> items) {
        visited();
        List<T> groupItems = filter(items);
        groupItems.sort((c1, c2) -> compare(c1, c2));
        return groupFunction != null && limit != -1 ? groupByLimit(groupItems) : groupItems;
    }

    /**
     * the group limit will be accessed on the given group by expression. E.g. if the groupByExpression targets a field, a hashmap on all groups pointing to list of items with that groupName will be
     * splitted after group limit (f.e. limit=3) and the rest will be shift to the bottom of the list.
     */
    List<T> groupByLimit(List<T> groupItems) {
        return splitGroups(groupBy(groupItems, (Function<T, Object>) get(groupFunction, Func.class).getFunction()), limit);
    }

    private Map<String, List<T>> groupBy(List<T> groupItems, Function<T, Object> groupByExpression) {
        Map<String, List<T>> groupBy = new LinkedHashMap<>(groupItems.size());
        groupItems.forEach(t -> {
            Object name = groupByExpression.apply(t);
            List<T> list = groupBy.get(name);
            if (list == null) {
                list = new LinkedList<>();
                groupBy.put(name.toString(), list);
            }
            list.add(t);
        });
        return groupBy;
    }

    /** item groups > limit will be splitted with rest to bottom */
    private List<T> splitGroups(Map<String, List<T>> grouped, int groupLimit) {
        List<T> items = new ArrayList<>();
        boolean iterationNeeded = false;
        for (var entry : grouped.entrySet()) {
            List<T> groupItems = entry.getValue();
            if (!iterationNeeded && groupItems.size() > groupLimit) {
                iterationNeeded = true;
            }
            List<T> fistElements = groupItems.stream().limit(groupLimit).toList();
            groupItems.removeAll(fistElements);
            items.addAll(fistElements);
        }
        if (iterationNeeded) {
            items = Stream.concat(items.stream(), splitGroups(grouped, groupLimit).stream()).toList();
        }
        return items;
    }

    public List<T> insertion(List<T> allItems) {
        if (step <= 0 || groupFunction == null) {
            return allItems;
        }
        Group<T> previous = config.getPrevious(this);
        previous = previous != null && previous.step > 0 ? previous : null;
        return insertion(allItems, previous, config.getNext(this));
    }

    /**
     * replacement of group items on specific positions given by member @step.
     *
     * @param allItems
     *            origin list to position the items of current group upper
     * @param insertedBefore
     *            if a higher group has call insertion() for its items before, don't overwrite them. normally the previous group.
     * @param lowerGroupToStop
     *            do insertions until items of given group are reached. normally the following group.
     * @return new list with replacements of current group items
     */
    public List<T> insertion(List<T> allItems, Group<T> insertedBefore, Group<T> lowerGroupToStop) {
        Objects.requireNonNull(groupFunction, toString() + " must have a groupFunction to call replace()");
        visited();
        List<T> groupItems = filter(allItems);
        List<T> replItems = new ArrayList<>(allItems.size());
        allItems.removeAll(groupItems);
        Func func = get(groupFunction, Func.class);
        int i = 1;
        T last = null;
        T t;
        while (!allItems.isEmpty()) {
            if (lowerGroupToStop.canSelect(allItems.get(0))) {
                break;
            }
            t = allItems.remove(0);
            if (i % step == 0 && !groupItems.isEmpty() && (insertedBefore == null || !insertedBefore.canSelect(t))) {
                T insert = groupItems.remove(0);
                insert = respectGroupingLimit(groupItems, func, last, insert);
                replItems.add(insert);
                last = insert;
                i++;
            }
            replItems.add(t);
            i++;
        }
        replItems.addAll(groupItems);
        replItems.addAll(allItems);
        // retain the origin list object
        allItems.clear();
        allItems.addAll(replItems);
        return allItems;
    }

    private T respectGroupingLimit(List<T> groupItems, Func func, T last, T insert) {
        int x = 0, count = groupItems.size();
        while (last != null && func.eval(insert).equals(func.eval(last)) && x++ < count) {
            if (!insertionLimitFallDown && limit > 0 && x + step < count) {
                groupItems.set(x + limit, insert);
            } else {
                groupItems.add(insert);
            }
            insert = groupItems.remove(0);
        }
        return insert;
    }

    @Override
    public String toString() {
        return super.toString() + " (" + selectorFact + " -> " + comparatorNames.toString() + ")";
    }

    String describe(String prefix) {
        StringBuilder b = new StringBuilder(prefix + super.toString() + " (if:" + selectorFact
            + ", groupBy: " + groupFunction + "/" + limit + ", insertion: step:" + step
            + (insertionLimitFallDown ? "/insertionLimitFallDown" : "") + ")\n");
        comparatorNames.forEach(c -> b.append(get(c, NamedComparator.class).describe(prefix + "\t\t")));
        return b.toString();
    }
}
