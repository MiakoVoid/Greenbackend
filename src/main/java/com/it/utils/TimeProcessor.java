package com.it.utils;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 时间处理工具类
 * 提供统一的日期时间格式化、转换、计算、验证及自然语言解析功能
 */
@Slf4j
public class TimeProcessor {

    // 常用日期时间格式
    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    public static final String YYYY_MM_DD_SLASH = "yyyy/MM/dd";
    public static final String YYYY_MM_DD_HH_MM_SS_SLASH = "yyyy/MM/dd HH:mm:ss";

    // 默认时区
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    // 显式日期格式匹配模式
    private static final Pattern DATE_PATTERN_FULL_CN = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})日");
    private static final Pattern DATE_PATTERN_SHORT_CN = Pattern.compile("(\\d{1,2})月(\\d{1,2})日");
    private static final Pattern DATE_PATTERN_STD = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");

    // Natty Parser is thread-safe and expensive to create, so we make it static
    private static final Parser NATTY_PARSER = new Parser();

    private TimeProcessor() {
        // 私有构造函数，防止实例化
    }

    /**
     * 1. 格式化 LocalDateTime
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null || !StringUtils.hasText(pattern)) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 格式化 Date
     */
    public static String format(Date date, String pattern) {
        if (date == null || !StringUtils.hasText(pattern)) {
            return null;
        }
        return format(toLocalDateTime(date), pattern);
    }

    /**
     * 解析字符串为 LocalDateTime
     */
    public static LocalDateTime parse(String text, String pattern) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(pattern)) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            log.error("Parse date failed: text={}, pattern={}", text, pattern);
            return null;
        }
    }

    /**
     * 2. Date 转 LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return null;
        return LocalDateTime.ofInstant(date.toInstant(), DEFAULT_ZONE);
    }

    /**
     * LocalDateTime 转 Date
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return Date.from(localDateTime.atZone(DEFAULT_ZONE).toInstant());
    }

    /**
     * 时间戳转 LocalDateTime
     */
    public static LocalDateTime fromTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), DEFAULT_ZONE);
    }

    /**
     * LocalDateTime 转时间戳
     */
    public static long toTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null) return 0;
        return localDateTime.atZone(DEFAULT_ZONE).toInstant().toEpochMilli();
    }

    /**
     * 3. 时间计算: 加减天数
     */
    public static LocalDateTime plusDays(LocalDateTime dateTime, long days) {
        return dateTime == null ? null : dateTime.plusDays(days);
    }

    /**
     * 时间差计算 (单位: 分钟)
     */
    public static long diffMinutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * 4. 时区转换
     */
    public static LocalDateTime convertZone(LocalDateTime dateTime, ZoneId fromZone, ZoneId toZone) {
        if (dateTime == null || fromZone == null || toZone == null) return null;
        ZonedDateTime zonedDateTime = dateTime.atZone(fromZone);
        return zonedDateTime.withZoneSameInstant(toZone).toLocalDateTime();
    }

    /**
     * 5. 时间验证
     */
    public static boolean isValid(String text, String pattern) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(pattern)) return false;
        try {
            LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * 6. 自然语言时间解析
     * 从文本中提取相对时间并转换为 LocalDateTime
     *
     * @param text 输入文本
     * @return 解析出的时间，如果无法解析则返回 null
     */
    public static LocalDateTime parseNaturalLanguage(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }

        LocalDateTime parsedTime = null;
        LocalDateTime now = LocalDateTime.now();
        
        // 1. 优先匹配明确的日期格式 (Regex)
        // 1.1 YYYY年MM月DD日
        java.util.regex.Matcher m1 = DATE_PATTERN_FULL_CN.matcher(text);
        if (m1.find()) {
            try {
                int year = Integer.parseInt(m1.group(1));
                int month = Integer.parseInt(m1.group(2));
                int day = Integer.parseInt(m1.group(3));
                parsedTime = LocalDateTime.of(year, month, day, 0, 0);
            } catch (Exception e) {
                log.warn("Regex parse failed for full CN date: {}", text);
            }
        }
        
        // 1.2 MM月DD日 (默认为当年)
        if (parsedTime == null) {
            java.util.regex.Matcher m2 = DATE_PATTERN_SHORT_CN.matcher(text);
            if (m2.find()) {
                try {
                    int month = Integer.parseInt(m2.group(1));
                    int day = Integer.parseInt(m2.group(2));
                    parsedTime = LocalDateTime.of(now.getYear(), month, day, 0, 0);
                } catch (Exception e) {
                    log.warn("Regex parse failed for short CN date: {}", text);
                }
            }
        }
        
        // 1.3 YYYY-MM-DD
        if (parsedTime == null) {
            java.util.regex.Matcher m3 = DATE_PATTERN_STD.matcher(text);
            if (m3.find()) {
                try {
                    int year = Integer.parseInt(m3.group(1));
                    int month = Integer.parseInt(m3.group(2));
                    int day = Integer.parseInt(m3.group(3));
                    parsedTime = LocalDateTime.of(year, month, day, 0, 0);
                } catch (Exception e) {
                    log.warn("Regex parse failed for std date: {}", text);
                }
            }
        }

        // 2. 绝对时间描述处理
        if (parsedTime == null) {
            if (text.contains("前天")) {
                parsedTime = now.minusDays(2);
            } else if (text.contains("昨天") || text.contains("昨晚") || text.contains("昨夜")) {
                parsedTime = now.minusDays(1);
            } else if (text.contains("今天") || text.contains("今日") || text.contains("今晚")) {
                parsedTime = now;
            } else if (text.contains("明天") || text.contains("明日") || text.contains("明晚")) {
                parsedTime = now.plusDays(1);
            } else if (text.contains("后天")) {
                parsedTime = now.plusDays(2);
            }
        }

        // 3. 相对时间描述处理 (周/月)
        if (parsedTime == null) {
            if (text.contains("上周")) {
                parsedTime = now.minusWeeks(1);
                parsedTime = adjustDayOfWeek(text, parsedTime);
            } else if (text.contains("下周")) {
                parsedTime = now.plusWeeks(1);
                parsedTime = adjustDayOfWeek(text, parsedTime);
            } else if (text.contains("上个月")) {
                parsedTime = now.minusMonths(1);
            } else if (text.contains("下个月")) {
                parsedTime = now.plusMonths(1);
            } 
            // 处理 "本周" 或 隐含的 "周X" (如 "周一开会") -> 默认为本周
            else if (text.matches(".*(周|星期)[一二三四五六日天].*")) {
                 parsedTime = adjustDayOfWeek(text, now);
            }
        }

        // 4. Natty兜底解析
        if (parsedTime == null) {
            try {
                List<DateGroup> groups = NATTY_PARSER.parse(text);
                if (!groups.isEmpty() && !groups.get(0).getDates().isEmpty()) {
                    Date date = groups.get(0).getDates().get(0);
                    parsedTime = LocalDateTime.ofInstant(date.toInstant(), DEFAULT_ZONE);
                }
            } catch (Exception e) {
                log.warn("Natty parse failed for text: {}", text);
            }
        }

        // 如果仍未解析出时间，但包含用餐关键词，则默认基于当前日期
        if (parsedTime == null) {
             if (text.contains("早餐") || text.contains("breakfast") ||
                 text.contains("午餐") || text.contains("午饭") || text.contains("lunch") ||
                 text.contains("晚餐") || text.contains("晚饭") || text.contains("dinner") || text.contains("晚") || text.contains("夜")) {
                 parsedTime = now;
             } else {
                 return null;
             }
        }

        // 4. 时间段细节调整 (早/午/晚/夜)
        if (text.contains("早餐") || text.contains("breakfast")) {
            parsedTime = parsedTime.with(LocalTime.of(8, 0));
        } else if (text.contains("午餐") || text.contains("午饭") || text.contains("lunch")) {
            parsedTime = parsedTime.with(LocalTime.of(12, 0));
        } else if (text.contains("晚餐") || text.contains("晚饭") || text.contains("dinner") || text.contains("晚") || text.contains("夜")) {
            // "昨晚", "今晚", "前晚", "晚上" -> 20:00
            parsedTime = parsedTime.with(LocalTime.of(20, 0));
        }

        return parsedTime;
    }

    private static LocalDateTime adjustDayOfWeek(String text, LocalDateTime baseTime) {
        if (text.contains("周一") || text.contains("星期一")) return baseTime.with(DayOfWeek.MONDAY);
        if (text.contains("周二") || text.contains("星期二")) return baseTime.with(DayOfWeek.TUESDAY);
        if (text.contains("周三") || text.contains("星期三")) return baseTime.with(DayOfWeek.WEDNESDAY);
        if (text.contains("周四") || text.contains("星期四")) return baseTime.with(DayOfWeek.THURSDAY);
        if (text.contains("周五") || text.contains("星期五")) return baseTime.with(DayOfWeek.FRIDAY);
        if (text.contains("周六") || text.contains("星期六")) return baseTime.with(DayOfWeek.SATURDAY);
        if (text.contains("周日") || text.contains("星期日") || text.contains("星期天")) return baseTime.with(DayOfWeek.SUNDAY);
        return baseTime;
    }
}
