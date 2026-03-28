# 农历日期距离计算器

## 功能描述
这个工具类可以接收农历日期（例如"腊月初八"、"腊月十八"），并计算该日期距离今天的天数。

## 使用方法

### 1. 直接调用API
```java
// 解析农历日期
int[] parsedDate = LunarDateDistanceCalculator.parseLunarDate("腊月初八");
int month = parsedDate[0];  // 农历月份
int day = parsedDate[1];    // 农历日期

// 计算距离天数
long days = LunarDateDistanceCalculator.calculateDaysToLunarDate(month, day);
```

### 2. 支持的日期格式
- 正月初一、二月初二、三月初三... etc.
- 腊月初八、腊月十八
- 一月、二月...十一月、十二月（或腊月）
- 日期支持：初一至三十，以及一、二、三...十、十一...十九、二十...二十九、三十

### 3. 返回值说明
- 如果返回值为0：表示今天就是该农历日期
- 如果返回值大于0：表示距离该农历日期还有N天
- 如果返回值小于0：表示该农历日期已在N天前

## 示例
```java
String input = "腊月初八";
int[] parsedDate = LunarDateDistanceCalculator.parseLunarDate(input);
long days = LunarDateDistanceCalculator.calculateDaysToLunarDate(parsedDate[0], parsedDate[1]);

System.out.println("距离农历" + input + "还有 " + days + " 天");
```

## 注意事项
- 某些农历日期在特定年份可能不存在（如闰月情况），程序会自动寻找最近的有效日期
- 农历月份的天数不是固定的，有些月份可能只有29天