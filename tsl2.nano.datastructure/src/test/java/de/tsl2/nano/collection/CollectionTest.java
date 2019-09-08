package de.tsl2.nano.collection;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import de.tsl2.nano.core.ITransformer;
import de.tsl2.nano.core.execution.Profiler;
import de.tsl2.nano.core.util.Util;

/**
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class CollectionTest {

//    @Ignore("...running long...")
@Test
public void testArrayPerformance() {
    final int c = 2;//1000;
    String description =
        "Test adding and getting one million elements on:\n0. FloatArray\n1. ArrayList\n2. Typed ArrayList\n3. ArrSegList\n4. LinkedList";
    Profiler.si().compareTests(description, 1000, new Runnable() {
        @Override
        public void run() {
            FloatArray numArrayList = new FloatArray();
            for (int i = 0; i < c; i++) {
                numArrayList.add(i);
            }
            for (int i = 0; i < c; i++) {
                numArrayList.get(i);
            }
            numArrayList.toArray();
        }
    }, new Runnable() {
        @Override
        public void run() {
            ArrayList numArrayList = new ArrayList();
            for (int i = 0; i < c; i++) {
                numArrayList.add(i);
            }
            for (int i = 0; i < c; i++) {
                numArrayList.get(i);
            }
            numArrayList.toArray();
        }

    }, new Runnable() {
        @Override
        public void run() {
            ArrayList<Float> numArrayList = new ArrayList<Float>();
            for (int i = 0; i < c; i++) {
                numArrayList.add((float) i);
            }
            for (int i = 0; i < c; i++) {
                numArrayList.get(i);
            }
            numArrayList.toArray(new Float[0]);
        }

    }, new Runnable() {
        @Override
        public void run() {
            ArrSegList<float[], Float> numArrayList = new ArrSegList<float[], Float>(float.class);
            for (int i = 0; i < c; i++) {
                numArrayList.add((float) i);
            }
            for (int i = 0; i < c; i++) {
                numArrayList.get(i);
            }
            numArrayList.toSegmentArray();
        }

    }, new Runnable() {
        @Override
        public void run() {
            LinkedList numArrayList = new LinkedList();
            for (int i = 0; i < c; i++) {
                numArrayList.add(i);
            }
            for (int i = 0; i < c; i++) {
                numArrayList.get(i);
            }
            numArrayList.toArray();
        }
    });
}

@Test
public void testCollectionUtil() throws Exception {
    //1. converting from arrays to a list
    Object[] arr1 = new Object[] { "Hans Müller", "Hans Mueller" };
    String[] arr2 = new String[] { "Carsten1", "Carsten0" };
    String[] arr3 = new String[] { "Berta", "Anton" };
    String[] arr4 = new String[] { "1100", "11", "111", "101", "1" };
    List<String> list = CollectionUtil.asListCombined(arr1, arr2, arr3, arr4);
    assertEquals(arr1.length + arr2.length + arr3.length + arr4.length, list.size());

    //2. sorting the list for numbers and strings
    Collection<?> sortedList = CollectionUtil.getSortedList(list);
    assertEquals(Arrays.asList("1",
        "11",
        "101",
        "111",
        "1100",
        "Anton",
        "Berta",
        "Carsten0",
        "Carsten1",
        "Hans Mueller",
        "Hans Müller"), sortedList);

    //3. filtering data
    Collection<String> filteredBetween = CollectionUtil.getFilteringBetween(list, "Anton", "Carsten1");
    assertEquals(Arrays.asList("Anton", "Berta", "Carsten0", "Carsten1"),
        CollectionUtil.getList(filteredBetween.iterator()));

    //do it again
    filteredBetween = CollectionUtil.getFilteringBetween(list, "Anton", "Carsten1");
    assertEquals(Arrays.asList("Anton", "Berta", "Carsten0", "Carsten1"),
        CollectionUtil.getList(filteredBetween.iterator()));

    //4. concatenation
    String[] concat = CollectionUtil.concat(String[].class, arr2, arr3);
    assertArrayEquals(new String[] { "Carsten1", "Carsten0", "Berta", "Anton" }, concat);

    //5. transforming data
    MyBean tb1 = new MyBean("Anton");
    MyBean tb2 = new MyBean("Berta");
    List<MyBean> transforming =
        (List<MyBean>) Util.untyped(CollectionUtil.getTransforming(Arrays.asList(tb1, tb2),
            new ITransformer<MyBean, String>() {
                @Override
                public String transform(MyBean toTransform) {
                    return toTransform.name;
                }
            }));
    assertEquals(Arrays.asList("Anton", "Berta"), CollectionUtil.getList(transforming.iterator()));
}
}

class MyBean {
    String name;
    public MyBean(String name) {
        this.name = name;
    }
}