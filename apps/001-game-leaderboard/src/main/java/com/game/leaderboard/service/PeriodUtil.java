package com.game.leaderboard.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.time.temporal.WeekFields;

public final class PeriodUtil {

    private PeriodUtil() {
    }

    public static String computePeriodId(Instant timestamp) {
        LocalDate date = timestamp.atZone(ZoneOffset.UTC).toLocalDate();
        int year = date.get(WeekFields.ISO.weekBasedYear());
        int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
        return String.format("%d-W%02d", year, week);
    }

    public static String getCurrentPeriod() {
        return computePeriodId(Instant.now());
    }
}
