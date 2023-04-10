package de.tsl2.nano.modelkit.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.tsl2.nano.modelkit.ExceptionHandler;
import de.tsl2.nano.modelkit.ObjectUtil;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ModelKitTest {
    static final String CURRENTTIME = "2023-04-08T00:00:00";

    @Before
    public void setUp() {
        ModelKit.enableDebugLog("name", "type", "value");
        ModelKit.resetAndDelete();
        System.setProperty("tsl2.modelkit.test.items.json.file", "modelkit-test-validation.json");
        System.setProperty("tsl2.modelkit.test.item.type", TestItem.class.getName());
        SortModel.create();
        SortModel.create2();
    }

    @Test
    public void testGroupsDistinct() throws StreamReadException, DatabindException, IOException {
        List<TestItem> items = ModelKitTester.loadTestItems(TestItem.class,
                "modelkit-test-validation.json");
        final List<TestItem> groupedTestItems = new ArrayList<>(items);
        ModelKit<TestItem> kit = SortModel.create();
        kit.forEachGroup(g -> groupedTestItems.removeAll(g.filter(items)));
        assertEquals(0, groupedTestItems.size());

        List<TestItem> groupedTestItems2 = new ArrayList();
        kit.forEachGroup(g -> groupedTestItems2.addAll(g.filter(items)));
        assertEquals(items.size(), groupedTestItems2.size());
    }

    @Test
    public void testModelKit() {
        ModelKit week = SortModel.create();
        ModelKit end = SortModel.create2();
        assertEquals(end,
                ExceptionHandler.trY(() -> ModelKit.getActiveModelKit(ZonedDateTime.parse("2023-04-08T00:00:00Z")),
                        NoSuchElementException.class));
        assertEquals(end,
                ExceptionHandler.trY(() -> ModelKit.getActiveModelKit(ZonedDateTime.parse("2023-04-09T00:00:00Z")),
                        NoSuchElementException.class));
        assertEquals(week, ModelKit.getActiveModelKit(ZonedDateTime.parse("2023-04-07T00:00:00Z")));
    }

    <T> T runAt(String timeString, Supplier<T> callback) {
        LocalDateTime dateTime = LocalDateTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(timeString + "Z");
        try (MockedStatic<LocalDateTime> mockedStatic = mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<ZonedDateTime> mockedStaticZ = mockStatic(ZonedDateTime.class,
                        Mockito.CALLS_REAL_METHODS)) {
            mockedStatic.when(LocalDateTime::now).thenReturn(dateTime);
            mockedStaticZ.when(ZonedDateTime::now).thenReturn(zonedDateTime);
            return callback.get();
        }
    }

    @Test
    public void testConfiguredMultiSort() throws Exception {

        List<TestItem> items = ModelKitTester.loadTestItems(TestItem.class, "modelkit-test-validation.json");

        ModelKit standard = SortModel.create();
        ModelKit weekend = SortModel.create2();

        // check active and start sort
        ModelKit weekendSort = ModelKit.getActiveModelKit(ZonedDateTime.parse("2023-03-05T00:00:00.000Z"));
        assertEquals(weekend, weekendSort);
        // do only a simple smoke test
        runAt(CURRENTTIME, () -> weekendSort.apply(items));

        ModelKit standardSort = ModelKit.getActiveModelKit(ZonedDateTime.parse("2023-03-06T00:00:00.000Z"));
        assertEquals(standard, standardSort);
        // do only a simple smoke test
        runAt(CURRENTTIME, () -> standardSort.apply(items));

    }

    @Test
    public void testUpdateConfiguration() {

        List<ModelKit> kits = ModelKit.getConfigurations();
        final String name = "sorter";
        ModelKit kit = kits.stream().filter(c -> c.getName().equals(name)).findFirst().orElseThrow();

        // all in
        ObjectUtil.setValue(kit, "description", "**");
        ModelKit.saveAsJSon(kits.toArray(new ModelKit[0]));

        kit.reset();
        kits = ModelKit.getConfigurations();
        kit = kits.stream().filter(c -> c.getName().equals(name)).findFirst().orElseThrow();
        assertEquals("**", kit.getDescription());

        // only a property of the kituration
        ModelKit.updateConfiguration(name, "description", "***");

        kit.reset();
        kits = ModelKit.getConfigurations();
        kit = kits.stream().filter(c -> c.getName().equals(name)).findFirst().orElseThrow();
        assertEquals("***", kit.getDescription());
    }

    @Test
    public void testUpdateConfigElementFailing() {

        final String name = "sorter";
        String jsonGroups = String.format("{\"name\": \"sorter.%s\", \"value\": [\"XXX\",\"YYY\"]}",
                "groupItems");
        String jsonIsType1 = "{\"name\": \"sorter.isType1\",\"andFacts\": [\"!sorter.isType1\"]}";
        String jsonIsType2 = "{\"name\":\"sorter.isType2\",\"andFacts\":[\"!sorter.isType1\"]}";

        try {
            ModelKit.updateConfigurationElement(name, "Def", jsonGroups);
            ModelKit.updateConfigurationElement(name, "Fact", jsonIsType1);
            ModelKit.updateConfigurationElement(name, "Fact", jsonIsType2);
            fail("updateConfigurationElement must fail with overlapping groups!");
        } catch (IllegalStateException e) {
            if (!e.getMessage().contains("The groups of this configuration are overlapping or miss some items"))
                fail("updateConfigurationElement must fail with overlapping groups!");
        }
    }

    @Test
    public void testUpdateConfigElement() {

        List<ModelKit> kits = ModelKit.getConfigurations();
        final String name = "sorter";
        ModelKit kit = kits.stream().filter(c -> c.getName().equals(name)).findFirst().orElseThrow();

        String jsonGroups = String.format("{\"name\": \"sorter.%s\", \"value\": [\"TYPE1\",\"TYPE2\",\"TYPE3\"]}",
                "groupItems");
        ModelKit.updateConfigurationElement(name, "Def", jsonGroups);

        kit.reset();
        kits = ModelKit.getConfigurations();
        kit = kits.stream().filter(c -> c.getName().equals(name)).findFirst().orElseThrow();
        List<?> groups = kit.getEnum("groupItems");

        assertEquals(3, groups.size());
        assertEquals("TYPE3", groups.get(2));
    }

}
