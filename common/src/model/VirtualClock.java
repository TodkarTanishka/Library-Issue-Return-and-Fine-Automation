package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class VirtualClock {
    private static volatile long dayOffset = 0;

    public static synchronized void advanceDays(long days) {
        dayOffset += days;
    }

    public static synchronized void setDayOffset(long days) {
        dayOffset = days;
    }

    public static synchronized void reset() {
        dayOffset = 0;
    }

    public static long getDayOffset() {
        return dayOffset;
    }

    public static LocalDate getToday() {
        return LocalDate.now().plusDays(dayOffset);
    }

    public static LocalDateTime getNow() {
        return LocalDateTime.now().plusDays(dayOffset);
    }
}
