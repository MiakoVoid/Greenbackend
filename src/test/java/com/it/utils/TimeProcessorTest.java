package com.it.utils;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import static org.junit.jupiter.api.Assertions.*;

public class TimeProcessorTest {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final LocalDateTime REFERENCE = LocalDateTime.parse("2024-03-15 10:00:00", formatter);

    @Test
    public void testYesterday() {
        LocalDateTime result = TimeProcessor.parseNaturalLanguage("昨天", REFERENCE);
        assertNotNull(result);
        assertEquals("2024-03-14 10:00:00", result.format(formatter));
    }

    @Test
    public void testLastFriday() {
        // 2024-03-15 is Friday
        LocalDateTime result = TimeProcessor.parseNaturalLanguage("上周五", REFERENCE);
        assertNotNull(result);
        assertEquals("2024-03-08 10:00:00", result.format(formatter));
    }

    @Test
    public void testSpecificDateAndTime() {
        LocalDateTime result = TimeProcessor.parseNaturalLanguage("3月15号下午3点", REFERENCE);
        assertNotNull(result);
        assertEquals("2024-03-15 15:00:00", result.format(formatter));
    }

    @Test
    public void testMealTime() {
        LocalDateTime result = TimeProcessor.parseNaturalLanguage("晚饭", REFERENCE);
        assertNotNull(result);
        assertEquals("2024-03-15 20:00:00", result.format(formatter));
        
        result = TimeProcessor.parseNaturalLanguage("明天早餐", REFERENCE);
        assertNotNull(result);
        assertEquals("2024-03-16 08:00:00", result.format(formatter));
    }

    @Test
    public void testCombinedRelative() {
        LocalDateTime result = TimeProcessor.parseNaturalLanguage("昨天晚上8点", REFERENCE);
        assertNotNull(result);
        assertEquals("2024-03-14 20:00:00", result.format(formatter));
    }

    @Test
    public void testStandardFormat() {
        LocalDateTime result = TimeProcessor.parseNaturalLanguage("2024-01-01", REFERENCE);
        assertNotNull(result);
        assertEquals("2024-01-01 10:00:00", result.format(formatter));
    }

    @Test
    public void testLastMonth() {
        LocalDateTime result = TimeProcessor.parseNaturalLanguage("上月", REFERENCE);
        assertNotNull(result);
        assertEquals("2024-02-15 10:00:00", result.format(formatter));
    }
}
