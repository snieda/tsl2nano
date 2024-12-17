package de.tsl2.nano.autotest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.Test;

import de.tsl2.nano.core.util.RegExUtil;
import de.tsl2.nano.core.util.StringUtil;

public class PermutatorTest {
    @Test
    public void testPermutatorSearchingItems() throws Exception {

        List<Item> items = Arrays.asList(new Item(), new Item(), new Item());
        List<Item> items3 = items.subList(0, 3);

        // evaluate all edge possibilities on all fields to shuffle their values
        String currentTestTimeString = "2023-01-01T08:30:00";
        LocalDateTime currentTime = LocalDateTime.parse(currentTestTimeString);
        LocalDate currentDate = currentTime.toLocalDate();

        LocalDateTime allTimes[] = ValueSet.getTimesAround(currentTime);
        LocalDate allDates[] = ValueSet.getDatesAround(currentDate);
        Item.Status[] allStatus = ValueSet.getAllEnums(Item.Status.class, false);
        String[] allNames = { "Item1", "Item2", "Item3" };
        Integer[] allNumbers = ValueSet.getNumbers(0, 3);
        Boolean[] trueFalse = ValueSet.getAllBooleans(false);

        Permutator.run("item-search",
                (() -> TimedRunner.runAt(currentTestTimeString, () -> TestController.process(items3))),
                items3,
                Map.of(
                        (c, v) -> c.setName((String) v), allNames,
                        (c, v) -> c.setStatus((Item.Status) v), allStatus,
                        (c, v) -> c.setValidFrom((LocalDate) v), allDates,
                        (c, v) -> c.setValidTo((LocalDate) v), allDates,
                        (c, v) -> c.setPublished((LocalDateTime) v), allTimes,
                        (c, v) -> c.setAvailable((int) v), allNumbers,
                        (c, v) -> c.setHidden((Boolean) v), trueFalse));
    }

    @Test
    public void testPermutator() {
        Permutator p = new Permutator("test-3x3", 3, 3, 3);
        int[] c;
        while ((c = p.next()) != null) {
            System.out.println(Arrays.toString(c));
        }
    }

    @Test
    public void testRegExGeneration() {
        checkRegEx("\\w+");
        checkRegEx("\\w{3}\\d{3}");
        checkRegEx("(https?|s?ftp|file)//(\\w+/)*\\w+");
        checkRegEx(StringUtil.VAR_REGEXP);
        checkRegEx("\\[[A-Z]{3}\s*[0-9]{3}\\];");
    }

    private void checkRegEx(String regex) {
        long t = System.currentTimeMillis();
        System.out.println(regex);
        String gen = RegExUtil.createFromRegEx(regex, 6, 10, 10000000);
        System.out.println("(millis:" + (System.currentTimeMillis() - t) + ") -> " + gen);
    }

}

class TimedRunner {
    /**
     * starts given callback mocking time to given timeString
     * @param <T> result type
     * @param timeString mocking time to use
     * @param callback test function
     * @return test function result
     */
    // TODO: add mockito to pom.xml
    public static <T> T runAt(String timeString, Supplier<T> callback) {
        //     LocalDateTime dateTime = LocalDateTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        //     ZonedDateTime zonedDateTime = ZonedDateTime.parse(timeString + "Z");
        //     try (MockedStatic<LocalDateTime> mockedStatic = mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS);
        //             MockedStatic<ZonedDateTime> mockedStaticZ = mockStatic(ZonedDateTime.class,
        //                     Mockito.CALLS_REAL_METHODS)) {
        //         mockedStatic.when(LocalDateTime::now).thenReturn(dateTime);
        //         mockedStaticZ.when(ZonedDateTime::now).thenReturn(zonedDateTime);
        return callback.get();
        //     }
    }
}

class Item {
    enum Status {
        NEW, REJECTED
    };

    String name;
    LocalDate validFrom;
    LocalDate validTo;
    LocalDateTime published;
    Status status;
    int available;
    boolean hidden;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public LocalDateTime getPublished() {
        return published;
    }

    public void setPublished(LocalDateTime published) {
        this.published = published;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}

class TestController {
    public static List<Item> process(List<Item> items) {
        System.out.println(items);
        return null;
    }
}
