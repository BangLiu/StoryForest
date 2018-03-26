package edu.ualberta.storyteller.core.util;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * This class contains methods that manipulate time or date.
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1219
 */
public class TimeUtils {

    /**
     * 时间元素列表: 年,月,日,时,分,秒
     */
    private final static List<Integer> TIMEUNITS = new ArrayList<>();

    /**
     * 设置时间元素列表
     */
    static {
        TIMEUNITS.add(Calendar.YEAR);
        TIMEUNITS.add(Calendar.MONTH);
        TIMEUNITS.add(Calendar.DATE);
        TIMEUNITS.add(Calendar.HOUR);
        TIMEUNITS.add(Calendar.MINUTE);
        TIMEUNITS.add(Calendar.SECOND);
    }

    /**
     * 日期格式为: 年 月 日；如：2016年04月06日
     */
    public static final String FORMAT_CALENDAR_DATE = "yyyy\u5E74MM\u6708dd\u65E5E";

    /**
     * 时间格式 为: 小时：分; 如：12:30
     */
    public static final String FORMAT_CALENDAR_TIME = "HH:mm";

    /**
     * 默认的日期格式
     */
    private static String defaultDatePattern = "yyyy-MM-dd";

    /**
     * 不同时间单位的等价微秒数
     */
    private static final long ONE_MINUTE_MILLISECOND = 60000L;
    private static final long ONE_HOUR_MILLISECOND = 3600000L;
    private static final long ONE_DAY_MILLISECOND = 86400000L;
    private static final long ONE_WEEK_MILLISECOND = 604800000L;
    private static final long ONE_MONTH_MILLISECOND = 2592000000L;
    private static final long ONE_YEAR_MILLISECOND = 31536000000L;

    /**
     * 不同的日期格式
     */
    private static final String[] SMART_DATE_FORMATS = {
        "yyyy-MM-dd HH:mm:ss", "yyyy.MM.dd HH:mm:ss",
        "yyyy-MM-dd HH:mm", "yyyy.MM.dd HH:mm",
        "yyyyMMddHHmmss", "yyyyMMddHHmm",
        "yyyy-MM-dd", "yyyy.MM.dd", "yyyyMMdd"
    };

    /**
     * 生肖
     */
    public static final String[] zodiacArray = { "猴", "鸡", "狗", "猪", "鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊" };

    /**
     * 星座
     */
    private static final String[] constellationArray = {
        "水瓶座", "双鱼座", "牡羊座", "金牛座", "双子座", "巨蟹座",
        "狮子座", "处女座", "天秤座", "天蝎座", "射手座", "魔羯座"
    };

    /**
     * 星座边界日期
     */
    private static final int[] constellationEdgeDay = {20, 19, 21, 21, 21, 22, 23, 23, 23, 23, 22, 22};

    /**
     * Convert a string to timestamp according to given string format.
     * <p>
     * @param str_date The date string.
     * @param format The format of date string.
     * @return Timestamp.
     */
    public static Timestamp convertStringToTimestamp(String str_date, String format) {
        try {
            DateFormat formatter;
            formatter = new SimpleDateFormat(format);
            Date date = formatter.parse(str_date);
            java.sql.Timestamp timeStampDate = new Timestamp(date.getTime());
            return timeStampDate;
        } catch (ParseException e) {
            System.out.println("Exception :" + e);
            return null;
        }
    }

    /**
     * Get the timestamp of n days after ts.
     * <p>
     * @param ts Timestamp to change.
     * @param n Number of days.
     * @return Result timestamp.
     */
    public static Timestamp addDays(Timestamp ts, int n) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(ts);
        cal.add(Calendar.DAY_OF_WEEK, n);
        return new Timestamp(cal.getTime().getTime());
    }

    /**
     * Calculate timestamp difference. Input in millisecond format.
     * <p>
     * @param currentTime Current timestamp.
     * @param oldTime Compared timestamp.
     * @param unit Time unit such as hour, minute, day, etc.
     * @return Time difference.
     */
    public static double calcTimestampDiff(Timestamp currentTime, Timestamp oldTime, String unit)
    {
        long milliseconds1 = oldTime.getTime();
        long milliseconds2 = currentTime.getTime();
        double diff = milliseconds2 - milliseconds1;
        if ("s".equals(unit) || "S".equals(unit)) {
            diff = diff / 1000;
        } else if ("m".equals(unit) || "M".equals(unit)) {
            diff = diff / (60 * 1000);
        } else if ("h".equals(unit) || "H".equals(unit)) {
            diff = diff / (60 * 60 * 1000);
        } else if ("d".equals(unit) || "D".equals(unit)) {
            diff = diff / (24 * 60 * 60 * 1000);
        } else {
            System.out.println("Invalid time unit parameter!");
            System.exit(-1);
        }
        return diff;
    }

    /**
     * Convert long int timestamp to string datetime.
     * <p>
     * @param time long timestamp.
     * @return string datetime.
     */
    public static String convertTime(long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }

    /**
     * 获取日期格式
     * <p>
     * @return 日期格式字符串
     */
    public static String getDatePattern() {
        return defaultDatePattern;
    }

    /**
     * 获取年
     * <p>
     * @param date 输入日期
     * @return 年份数字
     */
    public static int getYear(Date date) {
        return getCalendar(date).get(Calendar.YEAR);
    }

    /**
     * 获取月
     * <p>
     * @param date 输入日期
     * @return 月份数字
     */
    public static int getMonth(Date date) {
        return getCalendar(date).get(Calendar.MONTH);
    }

    /**
     * 获取日
     * <p>
     * @param date 输入日期
     * @return 日期数字
     */
    public static int getDay(Date date) {
        return getCalendar(date).get(Calendar.DATE);
    }

    /**
     * 获取周
     * <p>
     * @param date 输入日期
     * @return 周几
     */
    public static int getWeek(Date date) {
        return getCalendar(date).get(Calendar.DAY_OF_WEEK);
    }

    /**
     * 获取本月首日的周
     * <p>
     * @param date 输入日期
     * @return 周几
     */
    public static int getWeekOfFirstDayOfMonth(Date date) {
        return getWeek(getFirstDayOfMonth(date));
    }

    /**
     * 获取本月末日的周
     * <p>
     * @param date 输入日期
     * @return 周几
     */
    public static int getWeekOfLastDayOfMonth(Date date) {
        return getWeek(getLastDayOfMonth(date));
    }

    /**
     * 给定日期字符串和格式字符串, 得到日期
     * <p>
     * @param strDate 日期字符串
     * @param format 格式字符串
     * @return 日期
     */
    public static final Date parseDate(String strDate, String format) {
        SimpleDateFormat df = new SimpleDateFormat(format);
        try {
            return df.parse(strDate);
        } catch (ParseException pe) {
        }
        return null;
    }

    /**
     * 给定日期字符串, 尝试不同的格式来得到日期
     * <p>
     * @param strDate 日期字符串
     * @return 日期
     */
    public static final Date parseDateSmart(String strDate) {
        if (StringUtils.isEmpty(strDate)) {
            return null;
        }
        for (String fmt : SMART_DATE_FORMATS) {
            Date d = parseDate(strDate, fmt);
            if (d != null) {
                String s = formatDate(d, fmt);
                if (strDate.equals(s)) {
                    return d;
                }
            }
        }
        try {
            long time = Long.parseLong(strDate);
            return new Date(time);
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * 给定日期字符串, 利用默认格式来得到日期
     * <p>
     * @param strDate 日期字符串
     * @return 日期
     */
    public static Date parseDate(String strDate) {
        return parseDate(strDate, getDatePattern());
    }

    /**
     * 判断是否是闰年
     * <p>
     * @param year 年份
     * @return true or false
     */
    public static boolean isLeapYear(int year) {
        if (year / 4 * 4 != year) {
            return false;
        }
        if (year / 100 * 100 != year) {
            return true;
        }
        return (year / 400 * 400 == year);
    }

    /**
     * 判断是否是周末
     * <p>
     * @param date 日期
     * @return true or false
     */
    public static boolean isWeekend(Date date) {
        Calendar c = Calendar.getInstance();
        if (date != null) {
            c.setTime(date);
        }
        int weekDay = c.get(Calendar.DAY_OF_WEEK);
        return ((weekDay == Calendar.SATURDAY) || (weekDay == Calendar.SUNDAY));
    }

    /**
     * 判断今天是否是周末
     * <p>
     * @return true or false
     */
    public static boolean isWeekend() {
        return isWeekend(null);
    }

    /**
     * 获取当前时间的字符串
     * <p>
     * @return 当前时间
     */
    public static String getCurrentTime() {
        return formatDate(new Date());
    }

    /**
     * 获取当前时间并按照给定格式设置字符串
     * <p>
     * @param format 格式
     * @return 当前时间
     */
    public static String getCurrentTime(String format) {
        return formatDate(new Date(), format);
    }

    /**
     * 按照给定格式设置时间字符串
     * <p>
     * @param date 日期
     * @param format 格式
     * @return 格式化的日期
     */
    public static String formatDate(Date date, String format) {
        if (date == null) {
            date = new Date();
        }
        if (format == null) {
            format = getDatePattern();
        }
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

    /**
     * 格式化时间.如果超过一年,按照默认格式输出.如果没有超过,按简单格式输出,例如"3个月后".
     * <p>
     * @param date 日期
     * @return 格式化日期
     */
    public static String formatDate(Date date) {
        long offset = System.currentTimeMillis() - date.getTime();
        String pos = "前";
        if (offset < 0L) {
            pos = "后";
            offset = -offset;
        }
        if (offset >= ONE_YEAR_MILLISECOND) {
            return formatDate(date, getDatePattern());
        }
        if (offset >= 2 * ONE_MONTH_MILLISECOND) {
            return ((offset + 0.5 * ONE_MONTH_MILLISECOND) / ONE_MONTH_MILLISECOND) + "个月" + pos;
        }
        if (offset > ONE_WEEK_MILLISECOND) {
            return ((offset + 0.5 * ONE_WEEK_MILLISECOND) / ONE_WEEK_MILLISECOND) + "周" + pos;
        }
        if (offset > ONE_DAY_MILLISECOND) {
            return ((offset + 0.5 * ONE_DAY_MILLISECOND) / ONE_DAY_MILLISECOND) + "天" + pos;
        }
        if (offset > ONE_HOUR_MILLISECOND) {
            return ((offset + 0.5 * ONE_HOUR_MILLISECOND) / ONE_HOUR_MILLISECOND) + "小时" + pos;
        }
        if (offset > ONE_MINUTE_MILLISECOND) {
            return ((offset + 0.5 * ONE_MINUTE_MILLISECOND) / ONE_MINUTE_MILLISECOND) + "分钟" + pos;
        }
        return (offset / 1000L) + "秒" + pos;
    }

    /**
     * 默认时间格式化
     * <p>
     * @param date 日期
     * @return 格式化日期
     */
    public static String formatDateDefault(Date date) {
        return TimeUtils.formatDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 检测日期格式字符串是否符合format
     * 先把字符串parse为该format的Date对象，再将Date对象按format转换为string. 如果此string与初始字符串一致，则日期符合format.
     * 之所以用来回双重逻辑校验，是因为假如把一个非法字符串parse为某format的Date对象是不一定会报错的.
     * 比如 2015-06-29 13:12:121，不符合yyyy-MM-dd HH:mm:ss，但可以正常parse成Date对象，但时间变为了2015-06-29 13:14:01.
     * 增加多一重校验则可检测出这个问题.
     * <p>
     * @param strDateTime 日期
     * @param format 日期格式
     * @return true or false
     */
    public static boolean checkDateFormatAndValite(String strDateTime, String format) {
        if (strDateTime == null || strDateTime.length() == 0) {
            return false;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            Date ndate = sdf.parse(strDateTime);
            String str = sdf.format(ndate);
            if (str.equals(strDateTime)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取干净格式(如:Mon Dec 26 00:00:00 CST 2016)日期
     * <p>
     * @param day 日期
     * @return 日历日期
     */
    public static Date getCleanDay(Date day) {
        return getCleanDay(getCalendar(day));
    }

    /**
     * 获取给定日期的日历
     * <p>
     * @param day 日期
     * @return 日历
     */
    public static Calendar getCalendar(Date day) {
        Calendar c = Calendar.getInstance();
        if (day != null) {
            c.setTime(day);
        }
        return c;
    }

    /**
     * 获取干净格式(如:Mon Dec 26 00:00:00 CST 2016)日期
     * <p>
     * @param c 日历
     * @return 日历日期
     */
    private static Date getCleanDay(Calendar c) {
        c.set(11, 0);
        c.clear(12);
        c.clear(13);
        c.clear(14);
        return c.getTime();
    }

    /**
     * 根据年月日得到日期
     * <p>
     * @param year 年
     * @param month 月
     * @param day 日
     * @return 日期
     */
    public static Date makeDate(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        getCleanDay(c);
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DATE, day);
        return c.getTime();
    }

    /**
     * 将给定日期的给定部分(如:年,月,日)设为1
     * <p>
     * @param datePart 日期字段
     * @param date 日期
     * @return 修改后的日期
     */
    private static Date getFirstCleanDay(int datePart, Date date) {
        Calendar c = Calendar.getInstance();
        if (date != null) {
            c.setTime(date);
        }
        c.set(datePart, 1);
        return getCleanDay(c);
    }

    /**
     * 将给定日期的给定部分(如:年,月,日)改变一定数值
     * <p>
     * @param datePart 日期字段
     * @param delta 改变量
     * @param date 日期
     * @return 修改后的日期
     */
    private static Date add(int datePart, int delta, Date date) {
        Calendar c = Calendar.getInstance();
        if (date != null) {
            c.setTime(date);
        }
        c.add(datePart, delta);
        return c.getTime();
    }

    /**
     * 获取给定日期所在周的第一天
     * <p>
     * @param date 给定日期
     * @return 该日期的周一
     */
    public static Date getFirstDayOfWeek(Date date) {
        return getFirstCleanDay(Calendar.DAY_OF_WEEK, date);
    }

    /**
     * 获取今天所在周的第一天
     * <p>
     * @return 这周的周一
     */
    public static Date getFirstDayOfWeek() {
        return getFirstDayOfWeek(null);
    }

    /**
     * 获取给定日期所在月的第一天
     * <p>
     * @param date 给定日期
     * @return 该日期的月首日
     */
    public static Date getFirstDayOfMonth(Date date) {
        return getFirstCleanDay(Calendar.DAY_OF_MONTH, date);
    }

    /**
     * 获取今天所在月的第一天
     * <p>
     * @return 本月的月首日
     */
    public static Date getFirstDayOfMonth() {
        return getFirstDayOfMonth(null);
    }

    /**
     * 获取给定日期所在月的最后一天
     * <p>
     * @param date 给定日期
     * @return 该日期的月末日
     */
    public static Date getLastDayOfMonth(Date date) {
        Calendar c = getCalendar(getFirstDayOfMonth(date));
        c.add(Calendar.MONTH, 1);
        c.add(Calendar.DATE, -1);
        return getCleanDay(c);
    }

    /**
     * 获取今天所在月的最后一天
     * <p>
     * @return 本月的月末日
     */
    public static Date getLastDayOfMonth() {
        return getLastDayOfMonth(null);
    }

    /**
     * 获取给定日期所在季节的第一天
     * <p>
     * @param date 给定日期
     * @return 该季第一天
     */
    public static Date getFirstDayOfSeason(Date date) {
        Date d = getFirstDayOfMonth(date);
        int delta = getMonth(d) % 3;
        if (delta > 0) {
            d = getDateAfterMonths(d, -delta);
        }
        return d;
    }

    /**
     * 获取今天所在季节的第一天
     * <p>
     * @return 本季第一天
     */
    public static Date getFirstDayOfSeason() {
        return getFirstDayOfSeason(null);
    }

    /**
     * 获取给定日期所在年份的第一天
     * <p>
     * @param date 给定日期
     * @return 该年第一天
     */
    public static Date getFirstDayOfYear(Date date) {
        return makeDate(getYear(date), 1, 1);
    }

    /**
     * 获取今年第一天
     * <p>
     * @return 今年第一天
     */
    public static Date getFirstDayOfYear() {
        return getFirstDayOfYear(new Date());
    }

    /**
     * 获取距离起始日期几周的日期
     * <p>
     * @param start 起始日期
     * @param weeks 距离的周数
     * @return 目标日期
     */
    public static Date getDateAfterWeeks(Date start, int weeks) {
        return getDateAfterMs(start, weeks * ONE_WEEK_MILLISECOND);
    }

    /**
     * 获取距离起始日期几月的日期
     * <p>
     * @param start 起始日期
     * @param months 距离的月数
     * @return 目标日期
     */
    public static Date getDateAfterMonths(Date start, int months) {
        return add(Calendar.MONTH, months, start);
    }

    /**
     * 获取距离起始日期几年的日期
     * <p>
     * @param start 起始日期
     * @param years 距离的年数
     * @return 目标日期
     */
    public static Date getDateAfterYears(Date start, int years) {
        return add(Calendar.YEAR, years, start);
    }

    /**
     * 计算距离起始日期几天的日期
     * <p>
     * @param start 起始日期
     * @param days 距离的天数
     * @return 目标日期
     */
    public static Date getDateAfterDays(Date start, int days) {
        return getDateAfterMs(start, days * ONE_DAY_MILLISECOND);
    }

    /**
     * 计算距离起始日期几毫秒的日期
     * <p>
     * @param start 起始日期
     * @param ms 距离的毫秒数
     * @return 目标日期
     */
    public static Date getDateAfterMs(Date start, long ms) {
        return new Date(start.getTime() + ms);
    }

    /**
     * 计算开始与结束日期间有多少段给定时间长度
     * <p>
     * @param start 开始日期
     * @param end 结束日期
     * @param msPeriod 时间段毫秒数
     * @return 几段
     */
    public static long getPeriodNum(Date start, Date end, long msPeriod) {
        return (getIntervalMs(start, end) / msPeriod);
    }

    /**
     * 计算开始与结束日期间有多少毫秒
     * <p>
     * @param start 开始日期
     * @param end 结束日期
     * @return 几毫秒
     */
    public static long getIntervalMs(Date start, Date end) {
        return (end.getTime() - start.getTime());
    }

    /**
     * 计算开始与结束日期间有多少天
     * <p>
     * @param start 开始日期
     * @param end 结束日期
     * @return 几天
     */
    public static int getIntervalDays(Date start, Date end) {
        return (int) getPeriodNum(start, end, ONE_DAY_MILLISECOND);
    }

    /**
     * 计算开始与结束日期间有多少周
     * <p>
     * @param start 开始日期
     * @param end 结束日期
     * @return 几周
     */
    public static int getIntervalWeeks(Date start, Date end) {
        return (int) getPeriodNum(start, end, ONE_WEEK_MILLISECOND);
    }

    /**
     * 判断日期是否在基准日期之前或相同
     * <p>
     * @param base 基准日前
     * @param date 待判日期
     * @return true or false
     */
    public static boolean before(Date base, Date date) {
        return ((date.before(base)) || (date.equals(base)));
    }

    /**
     * 判断日期是否在基准日期之后或相同
     * <p>
     * @param base 基准日期
     * @param date 待判日期
     * @return true or false
     */
    public static boolean after(Date base, Date date) {
        return ((date.after(base)) || (date.equals(base)));
    }

    /**
     * 获取两日期中较大的那个
     * <p>
     * @param date1 日期1
     * @param date2 日期2
     * @return 较大的日期
     */
    public static Date max(Date date1, Date date2) {
        if (date1.getTime() > date2.getTime()) {
            return date1;
        }
        return date2;
    }

    /**
     * 获取两日期中较小的那个
     * <p>
     * @param date1 日期1
     * @param date2 日期2
     * @return 较小的日期
     */
    public static Date min(Date date1, Date date2) {
        if (date1.getTime() < date2.getTime()) {
            return date1;
        }
        return date2;
    }

    /**
     * 判断某日期是否在两日期之间 (包括边界)
     * <p>
     * @param start 开始日期
     * @param end 结束日期
     * @param date 待判定日期
     * @return true or false
     */
    public static boolean inPeriod(Date start, Date end, Date date) {
        return ((((end.after(date)) || (end.equals(date)))) &&
                (((start.before(date)) || (start.equals(date)))));
    }

    /**
     * 获取给定日期的生肖
     * <p>
     * @param time 输入日期
     * @return 生肖
     */
    public static String date2Zodica(Date time) {
        Calendar c = Calendar.getInstance();
        c.setTime(time);
        return year2Zodica(c.get(1));
    }

    /**
     * 获取给定年份的生肖
     * <p>
     * @param year 输入年份
     * @return 生肖
     */
    public static String year2Zodica(int year) {
        return zodiacArray[(year % 12)];
    }

    /**
     * 获取给定日期的星座
     * <p>
     * @param time 输入日期
     * @return 星座
     */
    public static String date2Constellation(Date time) {
        Calendar c = Calendar.getInstance();
        c.setTime(time);
        int month = c.get(2);
        int day = c.get(5);
        if (day < constellationEdgeDay[month]) {
            --month;
        }
        if (month >= 0) {
            return constellationArray[month];
        }
        return constellationArray[11];
    }

    /**
     * 获得指定时间那天的某个小时（24小时制）的整点时间
     * <p>
     * @param date 输入时间
     * @param hourIn24 小时
     * @return 整点时间
     */
    public static Date getSpecificHourInTheDay(final Date date, int hourIn24) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, hourIn24);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * 获取指定时间的那天 00:00:00.000 的时间
     * <p>
     * @param date 输入时间
     * @return 零点时间
     */
    public static Date dayBegin(final Date date) {
        return getSpecificHourInTheDay(date, 0);
    }

    /**
     * 获取指定时间的那天 23:59:59.999 的时间
     * <p>
     * @param date 输入时间
     * @return 终点时间
     */
    public static Date dayEnd(final Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    /**
     * 是否是指定日期
     * <p>
     * @param date 输入时间
     * @param day 比较时间
     * @return true or false
     */
    public static boolean isTheDay(final Date date, final Date day) {
        return date.getTime() >= dayBegin(day).getTime() && date.getTime() <= dayEnd(day).getTime();
    }

    /**
     * 是否是今天
     * <p>
     * @param date 输入时间
     * @return true or false
     */
    public static boolean isToday(final Date date) {
        return isTheDay(date, new Date());
    }

    /**
     * 检测时间表达式语句是否是有效的日期时间
     * <p>
     * @param dateToValidate 待检测的语句
     * @param dateFromat 时间表达式格式
     * @return true or false
     */
    public boolean isThisDateValid(String dateToValidate, String dateFromat){
        if(dateToValidate == null){
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(dateFromat);
        sdf.setLenient(false);

        try {
            //if not valid, it will throw ParseException
            Date date = sdf.parse(dateToValidate);
            System.out.println(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * main
     * @param args
     */
    public static void main(String[] args) {
        System.out.println(year2Zodica(1973));
        System.out.println(date2Zodica(new Date()));
        System.out.println(date2Constellation(makeDate(1973, 5, 12)));
        System.out.println(Calendar.getInstance() == Calendar.getInstance());
        System.out.println(getCleanDay(new Date()));
        System.out.println(new Date());
        Calendar c = Calendar.getInstance();
        c.set(5, 1);
        System.out.println(getFirstDayOfMonth());
        System.out.println(getLastDayOfMonth(makeDate(1996, 2, 1)));

        System.out.println(formatDate(makeDate(2009, 5, 1)));
        System.out.println(formatDate(makeDate(2010, 5, 1)));
        System.out.println(formatDate(makeDate(2010, 12, 21)));
        System.out.println(before(makeDate(2009, 5, 1), new Date()));
        System.out.println(after(makeDate(2009, 5, 1), new Date()));
        System.out.println(inPeriod(makeDate(2009, 11, 24), makeDate(2009, 11, 30), makeDate(2009, 11, 24)));

        System.out.println("Calendar.YEAR " + Calendar.YEAR);
        System.out.println("Calendar.MONTH " + Calendar.MONTH);
        System.out.println("Calendar.DATE " + Calendar.DATE);
        System.out.println("Calendar.DAY_OF_MONTH " + Calendar.DAY_OF_MONTH);
        System.out.println("Calendar.DAY_OF_WEEK " + Calendar.DAY_OF_WEEK);
        System.out.println("Calendar.DAY_OF_WEEK_IN_MONTH " + Calendar.DAY_OF_WEEK_IN_MONTH);
        System.out.println("Calendar.DAY_OF_YEAR " + Calendar.DAY_OF_YEAR);
        System.out.println("Calendar.FEBRUARY " + Calendar.FEBRUARY);
        System.out.println("Calendar.DATE " + Calendar.DATE);
        System.out.println("Calendar.FRIDAY " + Calendar.FRIDAY);
        System.out.println("Calendar.HOUR " + Calendar.HOUR);
        System.out.println("Calendar.LONG_FORMAT " + Calendar.LONG_FORMAT);
        System.out.println("Today " + Calendar.getInstance().toString());
    }
}
