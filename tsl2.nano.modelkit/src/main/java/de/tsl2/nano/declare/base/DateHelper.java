package de.tsl2.nano.modelkit;

import java.time.LocalDateTime;

public interface DateHelper {
    static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /** respects first=null to be before on any second time */
    static boolean isBefore(LocalDateTime first, LocalDateTime second) {
        return first == null || second != null && first.isBefore(second);
    }

    /** respects second=null to be behind on any first time */
    static boolean isEndingBefore(LocalDateTime first, LocalDateTime second) {
        return second == null || first != null && first.isBefore(second);
    }

    /** respects first=null to be behind on any second time */
    static boolean isAfter(LocalDateTime first, LocalDateTime second) {
        return first == null || second != null && first.isAfter(second);
    }

    /** respects null values to always inside period */
    static boolean isInside(LocalDateTime from, LocalDateTime until, LocalDateTime time) {
        return (from == null || !isAfter(from, time)) && (until == null || !isEndingBefore(until, time));
    }

}
