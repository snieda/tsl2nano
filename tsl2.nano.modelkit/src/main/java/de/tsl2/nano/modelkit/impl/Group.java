package de.tsl2.nano.modelkit.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import de.tsl2.nano.modelkit.Identified;
import de.tsl2.nano.modelkit.Selectable;
import lombok.Getter;
import lombok.Setter;

/**
 * Group, selecting its items through a selector and providing a
 * comparator chain. Each comparator is asked, if it is the
 * right one (canSelect) and can have sub-comparators used on compare-equality.
 * For more informations,
 * <p/>
 * see {@link #Group(String, int, String, int, String, boolean, String...)}<br>
 * see {@link #sort(List)}<br>
 */
public class Group<T> extends AIdentified
        implements Comparator<T>, Selectable<T>, BiFunction<Integer, List<T>, List<T>> {
    @Getter
    @Setter
    String selectorFact;
    @Getter
    @Setter
    private List<String> passFuncs;
    @Getter
    @Setter
    private List<String> compNames;

    protected Group() {
    }

    /**
     * creates a new Group. a group does actions in several passes on given items
     *
     * @param name unique name
     * @param propertyNames any identified definitions/comparators, facts, funcs to be used as properties for your action.
     */
    public Group(String name, String selectorFact, String... passFuncs) {
        super(name);
        this.selectorFact = selectorFact;
        this.passFuncs = Arrays.asList(passFuncs);
    }

    public Group setComparators(String... compNames) {
        this.compNames = Arrays.asList(compNames);
        return this;
    }

    public Integer getPassCount() {
        return passFuncs.size();
    }

    @Override
    public List<T> apply(Integer pass, List<T> items) {
        if (pass >= passFuncs.size())
            return items;
        return (List<T>) get(passFuncs.get(pass), Func.class).eval(this, items);
    }
    @Override
    public void validate() {
        Objects.requireNonNull(passFuncs, "funcName must not be null: " + toString());
        checkExistence(Func.class, passFuncs);
        checkExistence(selectorFact, Fact.class);
        checkExistence(Comp.class, compNames);
    }

    @Override
    public void tagNames(String parent) {
        super.tagNames(parent);
        selectorFact = tag(parent, selectorFact);
        tag(parent, passFuncs);
        tag(parent, compNames);
    }

    @Override
    public boolean canSelect(T item) {
        return canSelect(selectorFact, item);
    }

    /**
     * sorts its filtered items through its comparators
     */
    public List<T> sort(List<T> items) {
        List<T> groupItems = filter(items);
        groupItems.sort((c1, c2) -> compare(c1, c2));
        return groupItems;
    }

    @Override
    public int compare(T o1, T o2) {
        int result = 0;
        // TODO: not performance optimized
        List<Comp> comparators = get(Comp.class);
        for (String name : compNames) {
            Comp comparator = Identified.get(comparators, name);
            if (comparator.canSelect(Arrays.asList(o1, o2))) {
                result = comparator.compare(o1, o2);
                if (result != 0) {
                    break;
                }
            }
        }
        return result;
    }

    public List<T> filter(List<T> items) {
        return items.stream().filter(t -> canSelect(t)).collect(Collectors.toList());
    }


    @Override
    public String toString() {
        return super.toString() + " (" + selectorFact + " -> " + compNames.toString() + ")";
    }

    String describe(String prefix) {
        StringBuilder b = new StringBuilder(
                prefix + super.toString() + " (if:" + selectorFact + " -> " + passFuncs + ")\n");
        compNames.forEach(c -> b.append(get(c, Comp.class).describe(prefix + "\t\t")));
        return b.toString();
    }
}
