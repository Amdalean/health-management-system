package com.hms.common.utils;

import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 农历日期距离计算器
 * 
 * @author Qwen
 * @date 2026/1/27
 */
public class LunarDateDistanceCalculator {

    // 中文月份映射
    private static final Map<String, Integer> MONTH_MAP = new HashMap<>();
    
    // 中文日期映射
    private static final Map<String, Integer> DAY_MAP = new HashMap<>();

    static {
        // 月份映射
        MONTH_MAP.put("正月", 1);
        MONTH_MAP.put("一月", 1);
        MONTH_MAP.put("二月", 2);
        MONTH_MAP.put("三月", 3);
        MONTH_MAP.put("四月", 4);
        MONTH_MAP.put("五月", 5);
        MONTH_MAP.put("六月", 6);
        MONTH_MAP.put("七月", 7);
        MONTH_MAP.put("八月", 8);
        MONTH_MAP.put("九月", 9);
        MONTH_MAP.put("十月", 10);
        MONTH_MAP.put("冬月", 11);
        MONTH_MAP.put("十一月", 11);
        MONTH_MAP.put("腊月", 12);
        MONTH_MAP.put("十二月", 12);

        // 日期映射
        DAY_MAP.put("初一", 1);
        DAY_MAP.put("初二", 2);
        DAY_MAP.put("初三", 3);
        DAY_MAP.put("初四", 4);
        DAY_MAP.put("初五", 5);
        DAY_MAP.put("初六", 6);
        DAY_MAP.put("初七", 7);
        DAY_MAP.put("初八", 8);
        DAY_MAP.put("初九", 9);
        DAY_MAP.put("初十", 10);
        DAY_MAP.put("十一", 11);
        DAY_MAP.put("十二", 12);
        DAY_MAP.put("十三", 13);
        DAY_MAP.put("十四", 14);
        DAY_MAP.put("十五", 15);
        DAY_MAP.put("十六", 16);
        DAY_MAP.put("十七", 17);
        DAY_MAP.put("十八", 18);
        DAY_MAP.put("十九", 19);
        DAY_MAP.put("二十", 20);
        DAY_MAP.put("廿一", 21);
        DAY_MAP.put("廿二", 22);
        DAY_MAP.put("廿三", 23);
        DAY_MAP.put("廿四", 24);
        DAY_MAP.put("廿五", 25);
        DAY_MAP.put("廿六", 26);
        DAY_MAP.put("廿七", 27);
        DAY_MAP.put("廿八", 28);
        DAY_MAP.put("廿九", 29);
        DAY_MAP.put("三十", 30);
        DAY_MAP.put("三十一", 31);

        // 为了兼容性，也添加一些简写形式
        DAY_MAP.put("一", 1);
        DAY_MAP.put("二", 2);
        DAY_MAP.put("三", 3);
        DAY_MAP.put("四", 4);
        DAY_MAP.put("五", 5);
        DAY_MAP.put("六", 6);
        DAY_MAP.put("七", 7);
        DAY_MAP.put("八", 8);
        DAY_MAP.put("九", 9);
        DAY_MAP.put("十", 10);
    }

    /**
     * 解析农历日期字符串，如 "腊月初八"、"腊月十八"
     *
     * @param input 输入字符串
     * @return int[2] {month, day}
     * @throws IllegalArgumentException 如果无法解析
     */
    public static int[] parseLunarDate(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("输入不能为空");
        }

        String s = input.trim();

        // 尝试匹配各种可能的格式
        for (String monthStr : MONTH_MAP.keySet()) {
            if (s.startsWith(monthStr)) {
                String dayPart = s.substring(monthStr.length()).trim();

                // 按长度降序排列日期键，确保长匹配优先
                return new int[]{MONTH_MAP.get(monthStr), findBestDayMatch(dayPart)};
            }
        }

        // 如果没有找到匹配的月份，抛出异常
        throw new IllegalArgumentException("无法解析农历日期中的月部分: " + s);
    }

    /**
     * 查找最佳日期匹配
     *
     * @param dayPart 日期部分字符串
     * @return 匹配的日期数值
     * @throws IllegalArgumentException 如果无法解析
     */
    private static int findBestDayMatch(String dayPart) {
        if (dayPart.isEmpty()) {
            throw new IllegalArgumentException("日期部分不能为空");
        }

        // 按长度降序排列日期键，确保长匹配优先
        return DAY_MAP.entrySet().stream()
            .filter(entry -> dayPart.equals(entry.getKey()))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElseThrow(() -> new IllegalArgumentException("无法解析农历日期中的日部分: " + dayPart));
    }

    /**
     * 计算指定农历日期距离今天的天数
     * 
     * @param lunarMonth 农历月份
     * @param lunarDay 农历日期
     * @return 距离今天的天数（如果日期在今天之前则为负数，在今天之后则为正数，今天为0）
     */
    public static long calculateDaysToLunarDate(int lunarMonth, int lunarDay) {
        LocalDate today = LocalDate.now();
        
        // 尝试当前年份和下一年份，找出最接近的日期
        int currentYear = today.getYear();

        // 先尝试去年
        try {
            Lunar lunarCurrentYear = new Lunar(currentYear-1, lunarMonth, lunarDay);
            Solar solarCurrentYear = lunarCurrentYear.getSolar();
            LocalDate targetDateCurrentYear = LocalDate.of(solarCurrentYear.getYear(),
                    solarCurrentYear.getMonth(),
                    solarCurrentYear.getDay());

            long daysDiffCurrentYear = ChronoUnit.DAYS.between(today, targetDateCurrentYear);

            // 如果当前年份的目标日期还未到，则返回该日期
            if (daysDiffCurrentYear >= 0) {
                return daysDiffCurrentYear;
            }
        } catch (Exception e) {
            // 当前年份的农历日期可能不存在，忽略并尝试下一年
        }
        // 再尝试当前年份
        try {
            Lunar lunarCurrentYear = new Lunar(currentYear, lunarMonth, lunarDay);
            Solar solarCurrentYear = lunarCurrentYear.getSolar();
            LocalDate targetDateCurrentYear = LocalDate.of(solarCurrentYear.getYear(),
                                                          solarCurrentYear.getMonth(),
                                                          solarCurrentYear.getDay());

            long daysDiffCurrentYear = ChronoUnit.DAYS.between(today, targetDateCurrentYear);

            // 如果当前年份的目标日期还未到，则返回该日期
            if (daysDiffCurrentYear >= 0) {
                return daysDiffCurrentYear;
            }
        } catch (Exception e) {
            // 当前年份的农历日期可能不存在，忽略并尝试下一年
        }

        // 尝试下一年份
        try {
            Lunar lunarNextYear = new Lunar(currentYear + 1, lunarMonth, lunarDay);
            Solar solarNextYear = lunarNextYear.getSolar();
            LocalDate targetDateNextYear = LocalDate.of(solarNextYear.getYear(),
                                                       solarNextYear.getMonth(),
                                                       solarNextYear.getDay());

            return ChronoUnit.DAYS.between(today, targetDateNextYear);
        } catch (Exception e) {
            throw new RuntimeException("无法计算农历日期对应的公历日期", e);
        }
    }
    //传入农历日期，返回距离今天多少天
    public static long getNextDay(String input){
        int[] parsedDate = parseLunarDate(input);
        int month = parsedDate[0];
        int day = parsedDate[1];

        return calculateDaysToLunarDate(month, day);
    }

    /**
     * main方法，用于测试
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("农历日期距离计算器");
        System.out.println("===================");

        while (true) {
            System.out.print("请输入农历日期（例如：腊月初八，腊月十八，或输入'quit'退出）: ");
            String input = scanner.nextLine().trim();

            if ("quit".equalsIgnoreCase(input)) {
                System.out.println("程序已退出。");
                break;
            }

            try {
                int[] parsedDate = parseLunarDate(input);
                int month = parsedDate[0];
                int day = parsedDate[1];

                long days = calculateDaysToLunarDate(month, day);

                if (days == 0) {
                    System.out.println("今天就是农历" + input + "！");
                } else if (days > 0) {
                    System.out.println("距离下一个农历" + input + "还有 " + days + " 天。");
                } else {
                    System.out.println("上一个农历" + input + "是在 " + Math.abs(days) + " 天前。");
                }

                System.out.println();
            } catch (Exception e) {
                System.err.println("错误: " + e.getMessage());
                System.out.println();
            }
        }

        scanner.close();
    }
}