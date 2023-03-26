package de.tsl2.nano.modelkit.sort;

import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;
import java.util.NoSuchElementException;

import org.junit.Test;

import de.tsl2.nano.modelkit.ExceptionHandler;
import de.tsl2.nano.modelkit.impl.ModelKit;

public class ModelKitTest {
    @Test
    public void testModelKit() {
        ModelKit sortKit = SortModel.create();
        assertEquals(null,
                ExceptionHandler.trY(() -> ModelKit.getActiveModelKit(ZonedDateTime.parse("2023-03-26T00:00:00Z")),
                        NoSuchElementException.class));
        assertEquals(null,
                ExceptionHandler.trY(() -> ModelKit.getActiveModelKit(ZonedDateTime.parse("2023-03-25T00:00:00Z")),
                        NoSuchElementException.class));
        assertEquals(sortKit, ModelKit.getActiveModelKit(ZonedDateTime.parse("2023-03-24T00:00:00Z")));
    }
}

class TestItem {
    String name;
    String type;
    Integer value;
}