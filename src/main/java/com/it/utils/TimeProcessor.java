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

    // 默认时区：中国大陆时区
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    // 显式日期格式匹配模式
    private static final Pattern DATE_PATTERN_FULL_CN = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})[日号]");
    private static final Pattern DATE_PATTERN_SHORT_CN = Pattern.compile("(\\d{1,2})月(\\d{1,2})[日号]");
    private static final Pattern DATE_PATTERN_STD = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");

    // 显式时间格式匹配模式
    private static final Pattern TIME_PATTERN_HH_MM = Pattern.compile("(\\d{1,2})[:：](\\d{1,2})");
    private static final Pattern TIME_PATTERN_CN = Pattern.compile("(凌晨|早上|上午|中午|下午|晚上|夜里)?(\\d{1,2})[点时](\\d{1,2})?分?");

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
        return parseNaturalLanguage(text, null);
    }

    /**
     * 6. 自然语言时间解析 (带参考时间)
     * 从文本中提取相对时间并转换为 LocalDateTime
     *
     * @param text 输入文本
     * @param referenceTime 参考基准时间 (若为空则默认为当前时间)
     * @return 解析出的时间，如果无法解析则返回 null
     */
    public static LocalDateTime parseNaturalLanguage(String text, LocalDateTime referenceTime) {
        if (!StringUtils.hasText(text)) {
            return null;
        }

        LocalDateTime now = referenceTime != null ? referenceTime : LocalDateTime.now(DEFAULT_ZONE);
        
        // 1. 尝试解析日期部分
        LocalDate parsedDate = extractDate(text, now.toLocalDate());
        
        // 2. 尝试解析时间部分
        LocalTime parsedTime = extractTime(text, now.toLocalTime());

        // 3. 组合日期和时间
        if (parsedDate != null) {
            // 如果只有日期没有时间，且包含用餐关键词，则设置默认用餐时间
            if (parsedTime == null) {
                parsedTime = extractMealTime(text);
            }
            // 如果还是没有时间，默认使用参考时间的小时分钟 (或 00:00，取决于需求，这里保持与参考时间一致以体现“今天”等含义)
            if (parsedTime == null) {
                parsedTime = now.toLocalTime();
            }
            return LocalDateTime.of(parsedDate, parsedTime);
        }

        // 4. Natty兜底解析 (针对英文或复杂中文描述)
        try {
            List<DateGroup> groups = NATTY_PARSER.parse(text);
            if (!groups.isEmpty() && !groups.get(0).getDates().isEmpty()) {
                Date date = groups.get(0).getDates().get(0);
                return LocalDateTime.ofInstant(date.toInstant(), DEFAULT_ZONE);
            }
        } catch (Exception e) {
            log.warn("Natty parse failed for text: {}", text);
        }

        // 5. 如果仅包含用餐关键词但没日期，默认今天
        LocalTime mealTime = extractMealTime(text);
        if (mealTime != null) {
            return LocalDateTime.of(now.toLocalDate(), mealTime);
        }

        return null;
    }

    private static LocalDate extractDate(String text, LocalDate baseDate) {
        // 1. 优先匹配明确的日期格式 (Regex)
        // 1.1 YYYY年MM月DD日/号
        java.util.regex.Matcher m1 = DATE_PATTERN_FULL_CN.matcher(text);
        if (m1.find()) {
            try {
                return LocalDate.of(Integer.parseInt(m1.group(1)), Integer.parseInt(m1.group(2)), Integer.parseInt(m1.group(3)));
            } catch (Exception e) { log.warn("Regex parse failed for full CN date: {}", text); }
        }
        
        // 1.2 MM月DD日/号 (默认为当年)
        java.util.regex.Matcher m2 = DATE_PATTERN_SHORT_CN.matcher(text);
        if (m2.find()) {
            try {
                return LocalDate.of(baseDate.getYear(), Integer.parseInt(m2.group(1)), Integer.parseInt(m2.group(2)));
            } catch (Exception e) { log.warn("Regex parse failed for short CN date: {}", text); }
        }
        
        // 1.3 YYYY-MM-DD
        java.util.regex.Matcher m3 = DATE_PATTERN_STD.matcher(text);
        if (m3.find()) {
            try {
                return LocalDate.of(Integer.parseInt(m3.group(1)), Integer.parseInt(m3.group(2)), Integer.parseInt(m3.group(3)));
            } catch (Exception e) { log.warn("Regex parse failed for std date: {}", text); }
        }

        // 2. 绝对日期描述处理
        if (text.contains("前天")) return baseDate.minusDays(2);
        if (text.contains("昨天") || text.contains("昨晚") || text.contains("昨夜")) return baseDate.minusDays(1);
        if (text.contains("今天") || text.contains("今日") || text.contains("今晚")) return baseDate;
        if (text.contains("明天") || text.contains("明日") || text.contains("明晚")) return baseDate.plusDays(1);
        if (text.contains("后天")) return baseDate.plusDays(2);

        // 3. 相对日期描述处理 (周/月)
        if (text.contains("上周")) {
            return adjustDayOfWeek(text, baseDate.minusWeeks(1));
        } else if (text.contains("下周")) {
            return adjustDayOfWeek(text, baseDate.plusWeeks(1));
        } else if (text.contains("上个月") || text.contains("上月")) {
            return baseDate.minusMonths(1);
        } else if (text.contains("下个月") || text.contains("下月")) {
            return baseDate.plusMonths(1);
        } else if (text.matches(".*(周|星期)[一二三四五六日天].*")) {
            return adjustDayOfWeek(text, baseDate);
        }

        return null;
    }

    private static LocalTime extractTime(String text, LocalTime baseTime) {
        // 1. 匹配 HH:mm
        java.util.regex.Matcher m1 = TIME_PATTERN_HH_MM.matcher(text);
        if (m1.find()) {
            try {
                return LocalTime.of(Integer.parseInt(m1.group(1)), Integer.parseInt(m1.group(2)));
            } catch (Exception e) { log.warn("Regex parse failed for HH:mm: {}", text); }
        }

        // 2. 匹配 XX点XX分
        java.util.regex.Matcher m2 = TIME_PATTERN_CN.matcher(text);
        if (m2.find()) {
            try {
                String period = m2.group(1);
                int hour = Integer.parseInt(m2.group(2));
                int minute = m2.group(3) != null ? Integer.parseInt(m2.group(3)) : 0;
                
                // 处理上下午逻辑
                if ("下午".equals(period) || "晚上".equals(period) || "夜里".equals(period)) {
                    if (hour < 12) hour += 12;
                } else if ("凌晨".equals(period) || "早上".equals(period) || "上午".equals(period)) {
                    if (hour == 12) hour = 0;
                }
                
                return LocalTime.of(hour, minute);
            } catch (Exception e) { log.warn("Regex parse failed for CN time: {}", text); }
        }

        return null;
    }

    private static LocalTime extractMealTime(String text) {
        if (text.contains("早餐") || text.contains("早饭") || text.contains("breakfast")) {
            return LocalTime.of(8, 0);
        } else if (text.contains("午餐") || text.contains("午饭") || text.contains("lunch")) {
            return LocalTime.of(12, 0);
        } else if (text.contains("晚餐") || text.contains("晚饭") || text.contains("dinner") || text.contains("晚") || text.contains("夜")) {
            return LocalTime.of(20, 0);
        }
        return null;
    }

    private static LocalDate adjustDayOfWeek(String text, LocalDate baseDate) {
        DayOfWeek targetDay;
        if (text.contains("周一") || text.contains("星期一")) targetDay = DayOfWeek.MONDAY;
        else if (text.contains("周二") || text.contains("星期二")) targetDay = DayOfWeek.TUESDAY;
        else if (text.contains("周三") || text.contains("星期三")) targetDay = DayOfWeek.WEDNESDAY;
        else if (text.contains("周四") || text.contains("星期四")) targetDay = DayOfWeek.THURSDAY;
        else if (text.contains("周五") || text.contains("星期五")) targetDay = DayOfWeek.FRIDAY;
        else if (text.contains("周六") || text.contains("星期六")) targetDay = DayOfWeek.SATURDAY;
        else if (text.contains("周日") || text.contains("星期日") || text.contains("星期天")) targetDay = DayOfWeek.SUNDAY;
        else return baseDate;

        // 获取本周的 targetDay
        LocalDate result = baseDate.with(java.time.temporal.TemporalAdjusters.nextOrSame(targetDay));
        // 如果 nextOrSame 导致跨周（即 baseDate 的 targetDay 已经在过去），需要根据 baseDate 所在的周来调整
        // 但其实 with(DayOfWeek) 在 LocalDate 中就是调整到那一周的那一天
        return baseDate.with(targetDay);
    }
}
