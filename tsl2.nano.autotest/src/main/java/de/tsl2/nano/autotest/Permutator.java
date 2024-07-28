/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 31.03.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.autotest;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** 
 * permutator to iterate through all given possibilities.<p/>
 * useful for tests, checking all possibilities or creating test data...
 * 
 * Usage:<br/>
 * 1. simply iterate by yourself
 *   Example:
 *      Permutator p = new Permutator("test-3x3", 3, 3, 3);
        int[] c;
        while ((c = p.next() ) != null) {
            System.out.println(Arrays.toString(c));
        }
 *
 * 2. delegate the whole process to the permutator
 *  Example:
 *  Permutator.run("item-search",
    (() -> TimedRunner.runAt(currentTestTimeString, () -> TestController.search(items3))),
    items3,
    Map.of(
            (c, v) -> c.setName((String) v), allNames,
            (c, v) -> c.setStatus((Item.Status) v), allStatus,
            (c, v) -> c.setValidFrom((LocalDate) v), allDates,
            (c, v) -> c.setValidTo((LocalDate) v), allDates,
            (c, v) -> c.setPublished((LocalDateTime) v), allTimes,
            (c, v) -> c.setAvailable((int) v), allNumbers,
            (c, v) -> c.setHidden((Boolean) v), trueFalse));
 */
public class Permutator {
    private static final int PROGRESS_STEPS = Integer.getInteger("permutator.progress.steps", 1000);
    int[] maxItemsPerField;
    int[] currentFieldValues;
    private long start;
    private long counter;
    private long maxCount;

    public Permutator(String name, int... maxItemsPerField) {
        init(name, maxItemsPerField);
    }

    private void init(String name, int... maxItemsPerField) {
        this.maxItemsPerField = maxItemsPerField;
        currentFieldValues = new int[maxItemsPerField.length];
        maxCount = getMaxCount();
        log(name + ": creating " + maxCount + " permutations");
    }

    public int[] next() {
        counter++;
        if (start == 0) {
            start = System.currentTimeMillis();
            return currentFieldValues;
        }

        printProgress(counter, maxCount);

        for (int i = 0; i < maxItemsPerField.length; i++) {
            if (currentFieldValues[i] < maxItemsPerField[i] - 1) {
                currentFieldValues[i]++;
                return currentFieldValues;
            } else {
                currentFieldValues[i] = 0;
            }
        }
        log(" permutations finished in " + (System.currentTimeMillis() - start) / 1000 + " sec");
        return null;
    }

    public long getCounter() {
        return counter;
    }

    public long getMaxCount() {
        long c = 1;
        for (int i = 0; i < maxItemsPerField.length; i++) {
            c *= maxItemsPerField[i];
        }
        return c;
    }

    void printProgress(long i, long len) {
        if (i % ((len < PROGRESS_STEPS ? PROGRESS_STEPS : len) / PROGRESS_STEPS) == 0) {
            long estimatedTime = ((System.currentTimeMillis() - start) * len / i) / 1000;
            long estimatedTimeLeft = estimatedTime - estimatedTime * (len - i);
            System.out.print("\r" + i + " / " + len + " (estimated time left: " + estimatedTimeLeft + " sec)");
        }
    }

    static void log(Object t) {
        System.out.println(t);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <O> void permuteValues(List<O> instances, Map<BiConsumer<O, ?>, Object[]> valueMethodSets) {
        O instance = instances.get(currentFieldValues[0]);
        int i = 1;
        for (BiConsumer c : valueMethodSets.keySet()) {
            c.accept(instance, valueMethodSets.get(c)[currentFieldValues[i++]]);
        }
    }

    public <T, O> T runAll(String name, Supplier<T> runOnEach, List<O> instances,
            Map<BiConsumer<O, ?>, Object[]> valueMethodSets) {
        int[] valueMethodsSetsLength = valueMethodSets.values().stream().mapToInt(vs -> vs.length).toArray();
        init(name, ValueSet.concatIntArrays(new int[] { instances.size() }, valueMethodsSetsLength));
        T result = null;
        while (next() != null) {
            permuteValues(instances, (valueMethodSets));
            try {
                result = runOnEach.get();
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage() + " on:\n" + Arrays.toString(instances.toArray()), ex);
            }
        }
        return result;
    }

    public static <T, O> T run(String name, Supplier<T> runOnEach, List<O> instances,
            Map<BiConsumer<O, ?>, Object[]> valueMethodSets) {
        return new Permutator("init").runAll(name, runOnEach, instances, valueMethodSets);
    }
}

class ValueSet {
    static LocalDateTime[] getTimesAround(LocalDateTime userTime) {
        return new LocalDateTime[] { null, userTime, userTime.minusDays(1), userTime.plusDays(1),
                userTime.minusSeconds(1), userTime.plusSeconds(1) };
    }

    public static Boolean[] getAllBooleans(boolean withNull) {
        return withNull ? new Boolean[] { null, Boolean.TRUE, Boolean.FALSE } : new Boolean[] { true, false };
    }

    static LocalDate[] getDatesAround(LocalDate userDate) {
        return new LocalDate[] { null, userDate, userDate.minusDays(1), userDate.plusDays(1) };
    }

    static <T extends Enum<T>> T[] getAllEnums(Class<T> enumType, boolean withNull) {
        return withNull ? addNull(enumType.getEnumConstants()) : enumType.getEnumConstants();
    }

    @SuppressWarnings("unchecked")
    static <T> T[] addNull(T[] values) {
        return concat(values, (T[]) Array.newInstance(values.getClass().getComponentType(), 1));
    }

    static <T> T[] concat(T[] values, T... additionalValues) {
        return (T[]) Stream.concat(Arrays.stream(values), Arrays.stream(additionalValues))
                .toArray(l -> (T[]) Array.newInstance(values.getClass().getComponentType(), l));
    }

    static int[] concatIntArrays(int[] values, int... additionalValues) {
        return IntStream.concat(Arrays.stream(values), Arrays.stream(additionalValues)).toArray();
    }

    public static Integer[] getNumbers(int i, int j) {
        return (Integer[]) IntStream.range(i, j).boxed().toArray(Integer[]::new);
    }
}
