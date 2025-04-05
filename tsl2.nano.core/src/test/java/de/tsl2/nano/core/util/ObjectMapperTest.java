package de.tsl2.nano.core.util;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

public class ObjectMapperTest {

  static Map<Object, Object> convMap = new LinkedHashMap<>();

  @BeforeClass
  public static void setUp() {

    convMap.put((short) 100, "100");
    convMap.put((byte) 100, "100");
    convMap.put(1000000, "1000000");
    convMap.put(10000.11f, "10.000,11");
    convMap.put(10l, 10f);
    convMap.put((short) 10, 10);
    convMap.put(0.01f, "0,01");
    convMap.put(0.01d, "0,01");
    convMap.put(Boolean.TRUE, "true");
    convMap.put(false, "false");
    convMap.put(LocalDate.of(1899, 1, 1), "1899-01-01");
    convMap.put(LocalDate.of(1899, 12, 31), "1899-12-31");
    convMap.put("my-unique-key", new TObject("my-unique-key"));
    convMap.put((long) 100, 100);
    convMap.put((short) 100, 100);
    convMap.put((float) 100, 100);
    convMap.put((double) 100, 100);
    convMap.put((byte) 100, 100);
  }

  @Test
  public void testDefaultOnNull() {
    assertEquals(false, ObjectMapper.getDefaultOnNull(Boolean.class));
    assertEquals("", ObjectMapper.getDefaultOnNull(String.class));
    assertEquals(null, ObjectMapper.getDefaultOnNull(Object.class));
    assertEquals((Integer) 0, ObjectMapper.getDefaultOnNull(Integer.class));
    assertEquals((Float) 0f, ObjectMapper.getDefaultOnNull(Float.class));
  }

  @Test
  public void testWrapping() {
    convMap.entrySet().forEach(e -> assertEquals0(e, () -> ObjectMapper.wrap(e.getValue(), e.getKey().getClass())));
    convMap.entrySet().forEach(e -> assertEquals(e.getValue(), ObjectMapper.wrap(e.getKey(), e.getValue().getClass())));
}

  void assertEquals0(Map.Entry e, Supplier supplier) {
    System.out.println(e.getKey() + " => " + supplier.get());
    assertEquals(e.getKey().getClass(), supplier.get().getClass());
    assertEquals(e.getKey(), supplier.get());
  }

  public static <K, V> Map<V, K> reverseOneToOneMap(Map<K, V> map) {
    return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
  }
}

class TObject {
  String expression;

  TObject(String expression) {
    this.expression = expression;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof TObject && ((TObject) obj).expression == expression;
  }

  @Override
  public String toString() {
    return expression;
  }
}
