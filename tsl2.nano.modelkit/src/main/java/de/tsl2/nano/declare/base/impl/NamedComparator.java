package de.tsl2.nano.modelkit.impl;

import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.tsl2.nano.modelkit.Identified;
import de.tsl2.nano.modelkit.Selectable;
import de.tsl2.nano.modelkit.TriFunction;

/**
 * a comparator to be identified through its owning configuration. checks, if available for the current constalation and has a
 * comparator chain on equality of compared objects.
 */
public class NamedComparator<T> extends AbstractIdentified implements Comparator<T>, Selectable<List<T>> {
    @JsonIgnore
    TriFunction<ModelKit, T, T, Integer> comparator;
    private List<String> onEqualsThen;
    private String selectorFact;

    public NamedComparator(String name, String selectorFact, TriFunction<ModelKit, T, T, Integer> comparator,
        String... onEqualsThen) {
        super(name);
        this.selectorFact = selectorFact;
        this.comparator = comparator;
        this.onEqualsThen = List.of(onEqualsThen);
    }

    @Override
    public void validate() {
        checkExistence(selectorFact, Fact.class);
        checkExistence(NamedComparator.class, onEqualsThen);
    }

    @Override
    public boolean canSelect(List<T> current) {
        if (selectorFact == null) {
            return true;
        }
        return canSelect(selectorFact, current.get(0)) || canSelect(selectorFact, current.get(1));
    }

    @Override
    public int compare(T o1, T o2) {
        visited();
        int c = comparator.apply(config, o1, o2);
        if (c == 0) {
            List<NamedComparator> comparators = get(NamedComparator.class);
            for (String nextComparatorName : onEqualsThen) {
                c = Identified.get(comparators, nextComparatorName).compare(o1, o2);
                if (c != 0) {
                    break;
                }
            }
        }
        return c;
    }

    /**
     * convenience for three way comparators inside lambda function. if resultToNegative (=-1) and resultToPositive (=1) both
     * false, returns 0
     */
    public static int threeWayResult(boolean resultToNegative, boolean resultToPositive) {
        return threeWay(resultToNegative, -1, resultToPositive, 1, 0);
    }

    public static int threeWay(boolean first, int firstResult, boolean second, int secondResult, int thirdResult) {
        if (first) {
            return firstResult;
        } else if (second) {
            return secondResult;
        } else {
            return thirdResult;
        }
    }

    @Override
    public String toString() {
        return super.toString() + " [" + selectorFact + " -> onequal:chain: " + onEqualsThen + "]";
    }

    public Object describe(String prefix) {
        StringBuilder b = new StringBuilder(prefix + toString() + "\n");
        onEqualsThen.forEach(c -> b.append(get(c, NamedComparator.class).describe(prefix + "\t")));
        return b.toString();
    }
}
