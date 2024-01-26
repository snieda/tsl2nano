package de.tsl2.nano.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static de.tsl2.nano.filter.CollectionFilter.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CollectionFilterTest {
    @BeforeAll
    public static final void setUpClass() {
        // perhaps set logger to debug...
    }

    @Test
    void testWithTwoAndEqualsFilter() {
        testFilter3With(1,
                eq("str", "A"),
                eq("number", 1));
    }

    @Test
    void testWithChainedOrEqualsFilter() {
        testFilter3With(2,
                (ValueMapPredicate) eq("str", "A")
                        .or(
                                eq("str", "B"))
                        .and(between("date", LocalDate.now().plusDays(2), LocalDate.now().plusDays(3)).negate()));
    }

    @Test
    void testWithInFilter() {
        testFilter3With(2, in("str", "A", "B"));
    }

    @Test
    void testWithBetweenFilter() {
        testFilter3With(2, between("str", "A", "B"));
    }

    // simple test helper: the expected items have to start from first in collection!
    void testFilter3With(int expectedCount, ValueMapPredicate... filters) {
        Collection<TestBean> items = createItems();
        List<TestBean> filtered = CollectionFilter.filter(items, filters);

        assertEquals(expectedCount, filtered.size());
        int i = 0;
        for (TestBean item : items) {
            assertEquals(item, filtered.get(i++));
            if (expectedCount <= i) {
                break;
            }
        }
    }

    private Collection<TestBean> createItems() {
        return Arrays.asList(
                new TestBean("A", 1, LocalDate.now()),
                new TestBean("B", 2, LocalDate.now().plusDays(1)),
                new TestBean("C", 3, LocalDate.now().plusDays(2)));
    }
}

class TestBean {
    String str;
    int number;
    LocalDate date;

    public TestBean(String string, int i, LocalDate now) {
        str = string;
        number = i;
        date = now;
    }

    /**
     * @return the str
     */
    public String getStr() {
        return str;
    }

    /**
     * @param str the str to set
     */
    public void setStr(String str) {
        this.str = str;
    }

    /**
     * @return the number
     */
    public int getNumber() {
        return number;
    }

    /**
     * @param number the number to set
     */
    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * @return the date
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * @param date the date to set
     */
    public void setDate(LocalDate date) {
        this.date = date;
    }
}
