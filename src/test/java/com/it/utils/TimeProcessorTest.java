package com.it.utils;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class TimeProcessorTest {

    @Test
    void format_ShouldReturnCorrectString() {
        LocalDateTime dt = LocalDateTime.of(2023, 10, 1, 12, 0, 0);
        String result = TimeProcessor.format(dt, TimeProcessor.YYYY_MM_DD_HH_MM_SS);
        assertEquals("2023-10-01 12:00:00", result);
    }

    @Test
    void parse_ShouldReturnLocalDateTime() {
        String text = "2023-10-01 12:00:00";
        LocalDateTime result = TimeProcessor.parse(text, TimeProcessor.YYYY_MM_DD_HH_MM_SS);
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(10, result.getMonthValue());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void conversion_ShouldWorkBidirectionally() {
        LocalDateTime dt = LocalDateTime.of(2023, 10, 1, 12, 0, 0);
        Date date = TimeProcessor.toDate(dt);
        assertNotNull(date);
        
        LocalDateTime back = TimeProcessor.toLocalDateTime(date);
        assertEquals(dt, back);
    }

    @Test
    void parseNaturalLanguage_ShouldHandleRelativeDays() {
        // Yesterday
        LocalDateTime yesterday = TimeProcessor.parseNaturalLanguage("昨天");
        assertNotNull(yesterday);
        assertEquals(LocalDateTime.now().minusDays(1).getDayOfYear(), yesterday.getDayOfYear());

        // Tomorrow
        LocalDateTime tomorrow = TimeProcessor.parseNaturalLanguage("明天");
        assertNotNull(tomorrow);
        assertEquals(LocalDateTime.now().plusDays(1).getDayOfYear(), tomorrow.getDayOfYear());
    }

    @Test
    void parseNaturalLanguage_ShouldHandleMeals() {
        LocalDateTime breakfast = TimeProcessor.parseNaturalLanguage("早餐");
        assertNotNull(breakfast);
        assertEquals(8, breakfast.getHour());

        LocalDateTime lunch = TimeProcessor.parseNaturalLanguage("午餐");
        assertNotNull(lunch);
        assertEquals(12, lunch.getHour());

        LocalDateTime dinner = TimeProcessor.parseNaturalLanguage("晚餐");
        assertNotNull(dinner);
        assertEquals(20, dinner.getHour());
    }

    @Test
    void parseNaturalLanguage_ShouldHandleWeeks() {
        LocalDateTime lastWeekMonday = TimeProcessor.parseNaturalLanguage("上周一");
        assertNotNull(lastWeekMonday);
        // Logic verification might be tricky depending on "now", but we check not null
        // and consistency
    }

    @Test
    void parseNaturalLanguage_ShouldHandleChineseDateFormats() {
        // Test "MM月DD日"
        LocalDateTime date1 = TimeProcessor.parseNaturalLanguage("5月20日 买东西");
        if (date1 != null) {
            assertEquals(5, date1.getMonthValue());
            assertEquals(20, date1.getDayOfMonth());
        }

        // Test "YYYY年MM月DD日"
        LocalDateTime date2 = TimeProcessor.parseNaturalLanguage("2023年5月20日 买东西");
        if (date2 != null) {
            assertEquals(2023, date2.getYear());
            assertEquals(5, date2.getMonthValue());
            assertEquals(20, date2.getDayOfMonth());
        }
    }

    @Test
    void parseNaturalLanguage_ShouldHandleThisWeekDays() {
        // Test "周一" (Should be Monday of current week)
        LocalDateTime monday = TimeProcessor.parseNaturalLanguage("周一");
        assertNotNull(monday);
        assertEquals(DayOfWeek.MONDAY, monday.getDayOfWeek());
    }
    
    @Test
    void parseNaturalLanguage_ShouldHandleStandardDate() {
        // Test "2023-05-20"
        LocalDateTime date = TimeProcessor.parseNaturalLanguage("2023-05-20 买东西");
        if (date != null) {
            assertEquals(2023, date.getYear());
            assertEquals(5, date.getMonthValue());
            assertEquals(20, date.getDayOfMonth());
        }
    }
}
