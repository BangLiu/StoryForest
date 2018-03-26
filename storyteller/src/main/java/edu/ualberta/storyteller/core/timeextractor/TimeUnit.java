package edu.ualberta.storyteller.core.timeextractor;

import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.HashMap;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 时间语句分析
 * 此类具体解析时间字符串
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1220
 */
public class TimeUnit {

    /**
     * 输入待解析字符串
     */
    public String timeExpression = null;

    /**
     * 输出时间字符串
     */
    public String timeNorm = "";

    /**
     * 识别的时间
     */
    private Date time;

    /**
     * 是否代表一天
     */
    private Boolean isAllDayTime = true;

    /**
     * 是否第一次解析时间上下文
     */
    private boolean isFirstTimeSolveContext = true;

    /**
     * 时间解析器
     */
    TimeExtractor normalizer = null;

    /**
     * 时间点
     */
    public TimePoint _tp = new TimePoint();

    /**
     * 原始时间点
     */
    public TimePoint _tp_origin = new TimePoint();

    /**
     * 时间点元素index到日历意义的map, 如: 0代表年, ..., 5代表秒
     */
    private static Map<Integer, Integer> TUNIT_MAP = new HashMap<>();
    static{
        TUNIT_MAP.put(0, Calendar.YEAR);
        TUNIT_MAP.put(1, Calendar.MONTH);
        TUNIT_MAP.put(2, Calendar.DAY_OF_MONTH);
        TUNIT_MAP.put(3, Calendar.HOUR_OF_DAY);
        TUNIT_MAP.put(4, Calendar.MINUTE);
        TUNIT_MAP.put(5, Calendar.SECOND);
    }

    /**
     * 时间表达式单元构造方法
     * 该方法作为时间表达式单元的入口，将时间表达式字符串传入
     * <p>
     * @param exp_time 时间表达式字符串
     * @param n 时间识别器
     */
    public TimeUnit(String exp_time, TimeExtractor n)
    {
        timeExpression = exp_time;
        normalizer = n;
        normTime();
    }

    /**
     * 时间表达式单元构造方法
     * 该方法作为时间表达式单元的入口，将时间表达式字符串传入
     * <p>
     * @param exp_time 时间表达式字符串
     * @param n 时间识别器
     * @param contextTp 上下文时间
     */
    public TimeUnit(String exp_time, TimeExtractor n, TimePoint contextTp)
    {
        timeExpression = exp_time;
        normalizer = n;
        _tp_origin = contextTp;
        normTime();
    }

    /**
     * 年-规范化方法
     * 该方法识别时间表达式单元的年字段
     */
    public void normYear()
    {
        // 匹配两位数表示的年份,如: 98年,10年...
        String rule = "[0-9]{2}(?=年)";
        Pattern pattern = Pattern.compile(rule);
        Matcher match = pattern.matcher(timeExpression);
        if(match.find()) {
            System.out.println("Execute normYear...");
            _tp.tUnit[0] = Integer.parseInt(match.group());
            if (_tp.tUnit[0] >= 0 && _tp.tUnit[0] < 100) {
                if (_tp.tUnit[0] < 30) {
                    // NOTICE: 30以下表示2000年以后的年份
                    _tp.tUnit[0] += 2000;
                } else {
                    // NOTICE: 否则表示1900年以后的年份
                    _tp.tUnit[0] += 1900;
                }
            }
        }

        // 匹配三位数和四位数表示的年份, 如: 1XXX年,2XXX年,XXX年
        // 如果有3位数和4位数的年份，则覆盖原来2位数识别出的年份
        rule = "[0-9]?[0-9]{3}(?=年)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if(match.find()) {
            System.out.println("Execute normYear...");
            _tp.tUnit[0]=Integer.parseInt(match.group());
        }
    }

    /**
     * 月-规范化方法
     * 该方法识别时间表达式单元的月字段
     */
    public void normMonth()
    {
        // 匹配月份,如: 4月,10月...
        String rule = "((10)|(11)|(12)|([0]?[1-9]))(?=月)";
        Pattern pattern=Pattern.compile(rule);
        Matcher match=pattern.matcher(timeExpression);
        if(match.find())
        {
            System.out.println("Execute normMonth...");
            _tp.tUnit[1]=Integer.parseInt(match.group());

            // 处理倾向于未来时间的情况
            preferFuture(1);
        }
    }

    /**
     * 月-日 兼容模糊写法
     * 该方法识别时间表达式单元的月、日字段
     */
    public void normMonthDay()
    {
        // 匹配月日,如:3月2, 10月25, 10.25, 10-25...
        String rule = "((?<!(\\d|\\.)))((10)|(11)|(12)|([0]?[1-9]))(月|\\.|\\-)([0-3][0-9]|[1-9])";
        Pattern pattern = Pattern.compile(rule);
        Matcher match = pattern.matcher(timeExpression);
        if(match.find())
        {
            System.out.println("Execute normMonthDay...");
            String matchStr = match.group();
            Pattern p = Pattern.compile("(月|\\.|\\-)");
            Matcher m = p.matcher(matchStr);
            if(m.find()){
                int splitIndex = m.start();
                String month = matchStr.substring(0, splitIndex);
                String date = matchStr.substring(splitIndex+1);

                _tp.tUnit[1] = Integer.parseInt(month);
                _tp.tUnit[2] = Integer.parseInt(date);

                // 处理倾向于未来时间的情况
                preferFuture(1);
            }
        }
    }

    /**
     *日-规范化方法
     *该方法识别时间表达式单元的日字段
     */
    public void normDay()
    {
        // 匹配日, 如: 3号, 29日...
        String rule = "((?<!\\d))([0-3][0-9]|[1-9])(?=(日|号))";
        Pattern pattern = Pattern.compile(rule);
        Matcher match = pattern.matcher(timeExpression);
        if(match.find())
        {
            System.out.println("Execute normDay...");
            _tp.tUnit[2] = Integer.parseInt(match.group());

            // 处理倾向于未来时间的情况
            preferFuture(2);
        }
    }

    /**
     *时-规范化方法
     *该方法识别时间表达式单元的时字段
     */
    public void normHour()
    {
        // 匹配时, 如: 3点, 22点, 14时...
        String rule = "(?<!(周|星期))([0-2]?[0-9])(?=(点|时))";
        Pattern pattern = Pattern.compile(rule);
        Matcher match = pattern.matcher(timeExpression);
        if(match.find())
        {
            System.out.println("Execute normHour...");
            _tp.tUnit[3] = Integer.parseInt(match.group());

            // 处理倾向于未来时间的情况
            preferFuture(3);
            isAllDayTime = false;
        }

        // 对关键字：早（包含早上/早晨/早间），上午，中午,午间,下午,午后,晚上,傍晚,晚间,晚,pm,PM的正确时间计算
        // 规约：
        // 1.中午/午间0-10点视为12-22点
        // 2.下午/午后0-11点视为12-23点
        // 3.晚上/傍晚/晚间/晚1-11点视为13-23点，12点视为0点
        // 4.0-11点pm/PM视为12-23点

        // 处理没有明确时间点，只写了“凌晨”这种情况
        rule = "凌晨";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normHour...");
            if (_tp.tUnit[3] == -1) {
                _tp.tUnit[3] = RangeTimeEnum.day_break.getHourTime();
            }

            // 处理倾向于未来时间的情况
            preferFuture(3);
            isAllDayTime = false;
        }

        // 处理没有明确时间点，只写了“早上|早晨|早间|晨间|今早|明早”这种情况
        rule = "早上|早晨|早间|晨间|今早|明早";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normHour...");
            if (_tp.tUnit[3] == -1) {
                _tp.tUnit[3] = RangeTimeEnum.early_morning.getHourTime();
            }

            // 处理倾向于未来时间的情况
            preferFuture(3);
            isAllDayTime = false;
        }

        // 处理没有明确时间点，只写了“上午”这种情况
        rule = "上午";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normHour...");
            if (_tp.tUnit[3] == -1) {
                _tp.tUnit[3] = RangeTimeEnum.morning.getHourTime();
            }

            // 处理倾向于未来时间的情况
            preferFuture(3);
            isAllDayTime = false;
        }

        // 处理没有明确时间点，只写了“中午/午间”这种情况
        rule = "(中午)|(午间)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normHour...");
            if (_tp.tUnit[3] >= 0 && _tp.tUnit[3] <= 10) {
                _tp.tUnit[3] += 12;
            }
            if (_tp.tUnit[3] == -1) {
                _tp.tUnit[3] = RangeTimeEnum.noon.getHourTime();
            }

            // 处理倾向于未来时间的情况
            preferFuture(3);
            isAllDayTime = false;
        }

        // 处理没有明确时间点，只写了“下午|午后”这种情况
        rule = "(下午)|(午后)|(pm)|(PM)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normHour...");
            if (_tp.tUnit[3] >= 0 && _tp.tUnit[3] <= 11) {
                _tp.tUnit[3] += 12;
            }
            if (_tp.tUnit[3] == -1) {
                _tp.tUnit[3] = RangeTimeEnum.afternoon.getHourTime();
            }

            // 处理倾向于未来时间的情况
            preferFuture(3);
            isAllDayTime = false;
        }

        // 处理没有明确时间点，只写了"晚上|夜间|夜里|今晚|明晚"这种情况
        rule = "晚上|夜间|夜里|今晚|明晚";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normHour...");
            if(_tp.tUnit[3] >= 1 && _tp.tUnit[3] <= 11) {
                _tp.tUnit[3] += 12;
            } else if (_tp.tUnit[3] == 12) {
                _tp.tUnit[3] = 0;
            } else if (_tp.tUnit[3] == -1) {
                _tp.tUnit[3] = RangeTimeEnum.night.getHourTime();
            }

            // 处理倾向于未来时间的情况
            preferFuture(3);
            isAllDayTime = false;
        }
    }

    /**
     * 分-规范化方法
     * 该方法识别时间表达式单元的分字段
     */
    public void normMinute()
    {
        // 匹配分, 如: 23分...
        String rule="([0-5]?[0-9](?=分(?!钟)))|((?<=((?<!小)[点时]))[0-5]?[0-9](?!刻))";

        Pattern pattern=Pattern.compile(rule);
        Matcher match=pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normMinute...");
            if (!match.group().equals("")) {
                _tp.tUnit[4]=Integer.parseInt(match.group());

                // 处理倾向于未来时间的情况
                preferFuture(4);
                isAllDayTime = false;
            }
        }

        // 匹配1刻
        rule = "(?<=[点时])[1一]刻(?!钟)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normMinute...");
            _tp.tUnit[4] = 15;

            // 处理倾向于未来时间的情况
            preferFuture(4);
            isAllDayTime = false;
        }

        // 匹配3刻
        rule = "(?<=[点时])[3三]刻(?!钟)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normMinute...");
            _tp.tUnit[4] = 45;

            // 处理倾向于未来时间的情况
            preferFuture(4);
            isAllDayTime = false;
        }

        // 匹配半 (半为30分)
        rule = "(?<=[点时])半";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normMinute...");
            _tp.tUnit[4] = 30;

            // 处理倾向于未来时间的情况
            preferFuture(4);
            isAllDayTime = false;
        }
    }

    /**
     * 秒-规范化方法
     * 该方法识别时间表达式单元的秒字段
     */
    public void normSecond()
    {
        // 匹配秒, 如: 38秒, X分54
        String rule="([0-5]?[0-9](?=秒))|((?<=分)[0-5]?[0-9])";
        Pattern pattern=Pattern.compile(rule);
        Matcher match=pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normSecond...");
            _tp.tUnit[5]=Integer.parseInt(match.group());
            isAllDayTime = false;
        }
    }

    /**
     * 特殊形式的规范化方法
     * 该方法识别特殊形式的时间表达式单元的各个字段
     */
    public void normAll()
    {
        String rule;
        Pattern pattern;
        Matcher match;
        String[] tmp_parser;
        String tmp_target;

        // 匹配时分秒, 如: 23:15:13, 1:23:6, 03:5:04...
        // TODO: 没有限制小时数大于 23的情况,比如 29:12
        // TODO: 其他类似情况同理.
        rule = "(?<!(周|星期))([0-2]?[0-9]):[0-5]?[0-9]:[0-5]?[0-9]";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normAll...");
            tmp_parser = new String[3];
            tmp_target = match.group();
            tmp_parser = tmp_target.split(":");
            _tp.tUnit[3] = Integer.parseInt(tmp_parser[0]);
            _tp.tUnit[4] = Integer.parseInt(tmp_parser[1]);
            _tp.tUnit[5] = Integer.parseInt(tmp_parser[2]);

            // 处理倾向于未来时间的情况
            preferFuture(3);
            isAllDayTime = false;
        } else {
            // 匹配时分, 如: 23:15, 1:23...
            rule = "(?<!(周|星期))([0-2]?[0-9]):[0-5]?[0-9]";
            pattern = Pattern.compile(rule);
            match = pattern.matcher(timeExpression);
            if (match.find()) {
                System.out.println("Execute normAll...");
                tmp_parser = new String[2];
                tmp_target = match.group();
                tmp_parser = tmp_target.split(":");
                _tp.tUnit[3] = Integer.parseInt(tmp_parser[0]);
                _tp.tUnit[4] = Integer.parseInt(tmp_parser[1]);

                // 处理倾向于未来时间的情况
                preferFuture(3);
                isAllDayTime = false;
            }
        }

        // 增加了:固定形式时间表达式的
        // 中午,午间,下午,午后,晚上,傍晚,晚间,晚,pm,PM
        // 的正确时间计算，规约同上

        // 处理没有明确时间点，只写了“中午/午间”这种情况
        rule = "(中午)|(午间)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normAll...");
            // 处理类似"午间4点"就是16点这种情况.
            // TODO: 这里并没有限制之前设置的时间是和 (中午)|(午间) 相连的. 需要修改
            // TODO: 其他类似情况同理.
            if (_tp.tUnit[3] >= 0 && _tp.tUnit[3] <= 10) {
                _tp.tUnit[3] += 12;
            }
            if (_tp.tUnit[3] == -1) {
                _tp.tUnit[3] = RangeTimeEnum.noon.getHourTime();
            }

            //处理倾向于未来时间的情况
            preferFuture(3);
            isAllDayTime = false;
        }

        // 处理没有明确时间点，只写了“(下午)|(午后)|(pm)|(PM)”这种情况
        rule = "(下午)|(午后)|(pm)|(PM)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normAll...");
            if (_tp.tUnit[3] >= 0 && _tp.tUnit[3] <= 11) {
                _tp.tUnit[3] += 12;
            }
            if (_tp.tUnit[3] == -1) {
                _tp.tUnit[3] = RangeTimeEnum.afternoon.getHourTime();
            }

            // 处理倾向于未来时间的情况
            preferFuture(3);
            isAllDayTime = false;
        }

        // 处理没有明确时间点，只写了"晚"这种情况
        rule = "晚";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normAll...");
            if (_tp.tUnit[3] >= 1 && _tp.tUnit[3] <= 11) {
                _tp.tUnit[3] += 12;
            } else if (_tp.tUnit[3] == 12) {
                _tp.tUnit[3] = 0;
            }
            if (_tp.tUnit[3] == -1) {
                _tp.tUnit[3] = RangeTimeEnum.night.getHourTime();
            }

            // 处理倾向于未来时间的情况
            preferFuture(3);
            isAllDayTime = false;
        }

        // 匹配年-月-日, 如: 1997-01-09, 2005-4-7...
        // TODO: 检测不对的日期,如1997-13-45等等
        rule="[0-9]?[0-9]?[0-9]{2}-((10)|(11)|(12)|([0]?[1-9]))-((?<!\\d))([0-3][0-9]|[1-9])";
        pattern=Pattern.compile(rule);
        match=pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normAll...");
            tmp_target = match.group();
            tmp_parser = tmp_target.split("-");
            _tp.tUnit[0] = Integer.parseInt(tmp_parser[0]);
            _tp.tUnit[1] = Integer.parseInt(tmp_parser[1]);
            _tp.tUnit[2] = Integer.parseInt(tmp_parser[2]);
        }

        // 匹配月/日/年, 如: 10/25/1998
        // TODO: 如果是日/月/年 怎么办? 需要添加智能识别判断
        // TODO: 或者给程序添加时间结果的确信度,区间等等
        rule="((10)|(11)|(12)|([0]?[1-9]))/((?<!\\d))([0-3][0-9]|[1-9])/[0-9]?[0-9]?[0-9]{2}";
        pattern=Pattern.compile(rule);
        match=pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normAll...");
            tmp_target=match.group();
            tmp_parser=tmp_target.split("/");
            _tp.tUnit[1]=Integer.parseInt(tmp_parser[0]);
            _tp.tUnit[2]=Integer.parseInt(tmp_parser[1]);
            _tp.tUnit[0]=Integer.parseInt(tmp_parser[2]);
        }

        // 匹配年.月.日, 如: 1997.2.13
        rule = "[0-9]?[0-9]?[0-9]{2}\\.((10)|(11)|(12)|([0]?[1-9]))\\.((?<!\\d))([0-3][0-9]|[1-9])";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute norm_setTotal333...");
            tmp_target = match.group();
            tmp_parser = tmp_target.split("\\.");
            _tp.tUnit[0] = Integer.parseInt(tmp_parser[0]);
            _tp.tUnit[1] = Integer.parseInt(tmp_parser[1]);
            _tp.tUnit[2] = Integer.parseInt(tmp_parser[2]);
        }

        // TODO: 增加更多形式的识别,例如不同的分隔符, 以及根据中文正则表达式的内容增加相应的情况处理
        // TODO: 增加对时间正确性的判断.有些日期不存在,例如2月31, 5月56, 16月89等等
    }

    /**
     * 设置以上文时间为基准的时间偏移计算
     */
    public void normBaseRelated() {
        String [] time_grid = new String[6];
        time_grid=normalizer.timeBase().split("-");
        int[] ini = new int[6];
        for (int i = 0 ; i < 6; i++) {
            ini[i] = Integer.parseInt(time_grid[i]);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(ini[0], ini[1]-1, ini[2], ini[3], ini[4], ini[5]);
        calendar.getTime();

        //观察时间表达式是否因当前相关时间表达式而改变时间
        boolean[] flag = {false,false,false};

        String rule="\\d+(?=天[以之]?前)";
        Pattern pattern=Pattern.compile(rule);
        Matcher match=pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normBaseRelated...");
            flag[2] = true;
            int day = Integer.parseInt(match.group());
            calendar.add(Calendar.DATE, -day);
        }

        rule="\\d+(?=天[以之]?后)";
        pattern=Pattern.compile(rule);
        match=pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normBaseRelated...");
            flag[2] = true;
            int day = Integer.parseInt(match.group());
            calendar.add(Calendar.DATE, day);
        }

        rule="\\d+(?=(个)?月[以之]?前)";
        pattern=Pattern.compile(rule);
        match=pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normBaseRelated...");
            flag[1] = true;
            int month = Integer.parseInt(match.group());
            calendar.add(Calendar.MONTH, -month);
        }

        rule="\\d+(?=(个)?月[以之]?后)";
        pattern=Pattern.compile(rule);
        match=pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normBaseRelated...");
            flag[1] = true;
            int month = Integer.parseInt(match.group());
            calendar.add(Calendar.MONTH, month);
        }

        rule="\\d+(?=年[以之]?前)";
        pattern=Pattern.compile(rule);
        match=pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normBaseRelated...");
            flag[0] = true;
            int year = Integer.parseInt(match.group());
            calendar.add(Calendar.YEAR, -year);
        }

        rule="\\d+(?=年[以之]?后)";
        pattern=Pattern.compile(rule);
        match=pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normBaseRelated...");
            flag[0] = true;
            int year = Integer.parseInt(match.group());
            calendar.add(Calendar.YEAR, year);
        }

        String s = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(calendar.getTime());
        String[] time_fin = s.split("-");
        if (flag[0]||flag[1]||flag[2]) {
            _tp.tUnit[0] = Integer.parseInt(time_fin[0]);
        }
        if (flag[1]||flag[2]) {
            _tp.tUnit[1] = Integer.parseInt(time_fin[1]);
        }
        if (flag[2]) {
            _tp.tUnit[2] = Integer.parseInt(time_fin[2]);
        }
    }

    /**
     * 设置当前时间相关的时间表达式
     */
    public void normCurrentRelated() {
        String [] time_grid = new String[6];
        time_grid=normalizer.oldTimeBase().split("-");
        int[] ini = new int[6];
        for (int i = 0 ; i < 6; i++) {
            ini[i] = Integer.parseInt(time_grid[i]);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(ini[0], ini[1]-1, ini[2], ini[3], ini[4], ini[5]);
        calendar.getTime();

        //观察时间表达式是否因当前相关时间表达式而改变时间
        boolean[] flag = {false, false, false};

        // TODO: 检查这些对不对. 是否要设置一个"今天"然后进行计算
        String rule = "前年";
        Pattern pattern = Pattern.compile(rule);
        Matcher match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[0] = true;
            calendar.add(Calendar.YEAR, -2);
        }

        rule="去年";
        pattern=Pattern.compile(rule);
        match=pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[0] = true;
            calendar.add(Calendar.YEAR, -1);
        }

        rule = "今年";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[0] = true;
            calendar.add(Calendar.YEAR, 0);
        }

        rule = "明年";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[0] = true;
            calendar.add(Calendar.YEAR, 1);
        }

        rule = "后年";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[0] = true;
            calendar.add(Calendar.YEAR, 2);
        }

        rule = "上(个)?月";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[1] = true;
            calendar.add(Calendar.MONTH, -1);
        }

        rule = "(本|这个)月";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[1] = true;
            calendar.add(Calendar.MONTH, 0);
        }

        rule = "下(个)?月";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[1] = true;
            calendar.add(Calendar.MONTH, 1);
        }

        rule = "大前天";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[2] = true;
            calendar.add(Calendar.DATE, -3);
        }

        rule = "(?<!大)前天";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[2] = true;
            calendar.add(Calendar.DATE, -2);
        }

        rule = "昨";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[2] = true;
            calendar.add(Calendar.DATE, -1);
        }

        rule = "今(天|日)";
        pattern=Pattern.compile(rule);
        match=pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[2] = true;
            calendar.add(Calendar.DATE, 0);
        }

        rule = "明(天|日)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[2] = true;
            calendar.add(Calendar.DATE, 1);
        }

        rule = "(?<!大)后天";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[2] = true;
            calendar.add(Calendar.DATE, 2);
        }

        rule = "大后天";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[2] = true;
            calendar.add(Calendar.DATE, 3);
        }

        rule = "(?<=(上上(周|星期)))[1-7]?";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[2] = true;
            int week;
            try {
                week = Integer.parseInt(match.group());
            } catch (NumberFormatException e) {
                week = 1;
            }
            if (week == 7) {
                week = 1;
            } else {
                week++;
            }
            calendar.add(Calendar.WEEK_OF_MONTH, -2);
            calendar.set(Calendar.DAY_OF_WEEK, week);
        }

        rule="(?<=((?<!上)上(周|星期)))[1-7]?";
        pattern=Pattern.compile(rule);
        match=pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[2] = true;
            int week;
            try {
                week = Integer.parseInt(match.group());
            } catch (NumberFormatException e) {
                week = 1;
            }
            if (week == 7) {
                week = 1;
            } else {
                week++;
            }
            calendar.add(Calendar.WEEK_OF_MONTH, -1);
            calendar.set(Calendar.DAY_OF_WEEK, week);
        }

        rule="(?<=((?<!下)下(周|星期)))[1-7]?";
        pattern=Pattern.compile(rule);
        match=pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[2] = true;
            int week;
            try {
                week = Integer.parseInt(match.group());
            } catch (NumberFormatException e) {
                week = 1;
            }
            if (week == 7) {
                week = 1;
            } else {
                week++;
            }
            calendar.add(Calendar.WEEK_OF_MONTH, 1);
            calendar.set(Calendar.DAY_OF_WEEK, week);
        }

        rule="(?<=(下下(周|星期)))[1-7]?";
        pattern=Pattern.compile(rule);
        match=pattern.matcher(timeExpression);
        if (match.find()) {
            flag[2] = true;
            int week;
            try {
                week = Integer.parseInt(match.group());
            } catch (NumberFormatException e) {
                week = 1;
            }
            if (week == 7) {
                week = 1;
            } else {
                week++;
            }
            calendar.add(Calendar.WEEK_OF_MONTH, 2);
            calendar.set(Calendar.DAY_OF_WEEK, week);
        }

        rule="(?<=((?<!(上|下))(周|星期)))[1-7]?";
        pattern=Pattern.compile(rule);
        match=pattern.matcher(timeExpression);
        if (match.find()) {
            System.out.println("Execute normCurrentRelated...");
            flag[2] = true;
            int week;
            try {
                week = Integer.parseInt(match.group());
            } catch (NumberFormatException e) {
                week = 1;
            }
            if (week == 7) {
                week = 1;
            } else {
                week++;
            }
            calendar.add(Calendar.WEEK_OF_MONTH, 0);
            calendar.set(Calendar.DAY_OF_WEEK, week);

            // 处理未来时间倾向
            // TODO: 为什么其他的上面的处理没有未来倾向处理?
            preferFutureWeek(week, calendar);
        }

        // TODO: 添加更多的支持
        // 根据相对时间处理结果来更新时间点
        String s = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(calendar.getTime());
        String[] time_fin = s.split("-");
        if (flag[0]||flag[1]||flag[2]) {
            _tp.tUnit[0] = Integer.parseInt(time_fin[0]);
        }
        if (flag[1]||flag[2]) {
            _tp.tUnit[1] = Integer.parseInt(time_fin[1]);
        }
        if (flag[2]) {
            _tp.tUnit[2] = Integer.parseInt(time_fin[2]);
        }
    }

    /**
     * 该方法用于更新timeBase使之具有上下文关联性
     * 根据_tp.tunit列表的值来更新timeBase字符串的值
     */
    public void modifyTimeBase(){
        String [] time_grid = new String[6];
        time_grid = normalizer.timeBase().split("-");

        String s = "";
        if (_tp.tUnit[0] != -1) {
            s += Integer.toString(_tp.tUnit[0]);
        } else {
            s += time_grid[0];
        }
        for (int i = 1; i < 6; i++) {
            s += "-";
            if (_tp.tUnit[i] != -1) {
                s += Integer.toString(_tp.tUnit[i]);
            } else {
                s += time_grid[i];
            }
        }
        normalizer.timeBase(s);
    }

    /**
     * 时间表达式规范化的入口
     * 时间表达式识别后，通过此入口进入规范化阶段，
     * 具体识别每个字段的值
     */
    public void normTime() {
        // 识别输入文本中的年月日时分秒
        // TODO: 确保以下操作彼此不会冲突修改结果. 这需要不断根据反例来修改正则表达式,以及合理地设置执行顺序,是否继续执行条件等.
        normYear();
        normMonth();
        normDay();
        normMonthDay();
        normBaseRelated();
        normCurrentRelated();
        normHour();
        normMinute();
        normSecond();
        normAll();
        modifyTimeBase();

        _tp_origin.tUnit = _tp.tUnit.clone();

        String [] time_grid = new String[6];
        time_grid = normalizer.timeBase().split("-");

        int tunitpointer = 5;
        while (tunitpointer >= 0 && _tp.tUnit[tunitpointer] < 0) {
            tunitpointer--;
        }

        for (int i=0;i<tunitpointer;i++) {
            if (_tp.tUnit[i] < 0) {
                _tp.tUnit[i] = Integer.parseInt(time_grid[i]);
            }
        }

        // 设置年份
        String[] _result_tmp=new String[6];
        _result_tmp[0] = String.valueOf(_tp.tUnit[0]);

        // 将两位数年份转为4位数.
        // TODO: 此处有些不妥.比如16年转化为1916年.实际上2016年可能性更大
        if (_tp.tUnit[0] >= 10 &&_tp.tUnit[0] < 100) {
            _result_tmp[0]="19"+String.valueOf(_tp.tUnit[0]);
        }
        if (_tp.tUnit[0] > 0 &&_tp.tUnit[0] < 10) {
            _result_tmp[0]="200"+String.valueOf(_tp.tUnit[0]);
        }

        // 设置月,日,时,分,秒
        for (int i = 1; i < 6; i++) {
            _result_tmp[i] = String.valueOf(_tp.tUnit[i]);
        }

        // 设置最终结果时间
        Calendar cale = Calendar.getInstance();
        cale.clear();
        if (Integer.parseInt(_result_tmp[0]) != -1) {
            timeNorm += _result_tmp[0] + "年";
            cale.set(Calendar.YEAR, Integer.valueOf(_result_tmp[0]));
            if (Integer.parseInt(_result_tmp[1]) != -1) {
                timeNorm += _result_tmp[1] + "月";
                cale.set(Calendar.MONTH, Integer.valueOf(_result_tmp[1]) - 1);
                if (Integer.parseInt(_result_tmp[2]) != -1) {
                    timeNorm += _result_tmp[2] + "日";
                    cale.set(Calendar.DAY_OF_MONTH, Integer.valueOf(_result_tmp[2]));
                    if (Integer.parseInt(_result_tmp[3]) != -1) {
                        timeNorm += _result_tmp[3] + "时";
                        cale.set(Calendar.HOUR_OF_DAY, Integer.valueOf(_result_tmp[3]));
                        if (Integer.parseInt(_result_tmp[4]) != -1) {
                            timeNorm += _result_tmp[4] + "分";
                            cale.set(Calendar.MINUTE, Integer.valueOf(_result_tmp[4]));
                            if (Integer.parseInt(_result_tmp[5]) != -1) {
                                timeNorm += _result_tmp[5] + "秒";
                                cale.set(Calendar.SECOND, Integer.valueOf(_result_tmp[5]));
                            }
                        }
                    }
                }
            }
        }
        time = cale.getTime();
    }

    /**
     * 输出时间识别的输入与输出
     * <p>
     * @return 识别的输入与输出结果
     */
    @Override
    public String toString(){
        return timeExpression + " ---> " + timeNorm;
    }

    /**
     * 如果用户选项是倾向于未来时间，检查checkTimeIndex所指的时间是否是过去的时间，如果是的话，将大一级的时间设为当前时间的+1。
     * 如在晚上说“早上8点看书”，则识别为明天早上;
     * 12月31日说“3号买菜”，则识别为明年1月的3号。
     * @param checkTimeIndex _tp.tunit时间数组的下标
     */
    private void preferFuture(int checkTimeIndex){
        // 检查被检查的时间级别之前，是否没有更高级的已经确定的时间，如果有，则不进行处理.
        for (int i = 0; i < checkTimeIndex; i++) {
            if (_tp.tUnit[i] != -1) {
                return;
            }
        }
        // 根据上下文补充时间
        checkContextTime(checkTimeIndex);

        // 根据上下文补充时间后再次检查被检查的时间级别之前，是否没有更高级的已经确定的时间，如果有，则不进行倾向处理.
        for(int i = 0; i < checkTimeIndex; i++){
            if (_tp.tUnit[i] != -1) {
                return;
            }
        }

        // 确认用户选项
        if(!normalizer.isPreferFuture()){
            return;
        }

        // 获取当前时间，如果识别到的时间小于当前时间，则将其上的所有级别时间设置为当前时间，并且其上一级的时间步长+1*/
        Calendar c = Calendar.getInstance();
        if (this.normalizer.timeBase() != null) {
            String[] ini = this.normalizer.timeBase().split("-");
            c.set(Integer.valueOf(ini[0]).intValue(), Integer.valueOf(ini[1]).intValue()-1,
                    Integer.valueOf(ini[2]).intValue(), Integer.valueOf(ini[3]).intValue(),
                    Integer.valueOf(ini[4]).intValue(), Integer.valueOf(ini[5]).intValue());
        }

        int curTime = c.get(TUNIT_MAP.get(checkTimeIndex));
        if (curTime < _tp.tUnit[checkTimeIndex]) {
            return;
        }

        //准备增加的时间单位是被检查的时间的上一级，将上一级时间+1
        int addTimeUnit = TUNIT_MAP.get(checkTimeIndex-1);
        c.add(addTimeUnit, 1);

        for (int i = 0; i < checkTimeIndex; i++) {
            _tp.tUnit[i] = c.get(TUNIT_MAP.get(i));
            if(TUNIT_MAP.get(i) == Calendar.MONTH) {
                ++_tp.tUnit[i];
            }
        }
    }

    /**
     * 如果用户选项是倾向于未来时间，检查所指的day_of_week是否是过去的时间，如果是的话，设为下周。
     * 如在周五说：周一开会，识别为下周一开会
     * <p>
     * @param weekday 识别出是周几（范围1-7）
     */
    private void preferFutureWeek(int weekday, Calendar c){
        // 确认用户选项
        if(!normalizer.isPreferFuture()){
            return;
        }

        // 检查被检查的时间级别之前，是否没有更高级的已经确定的时间，如果有，则不进行倾向处理.
        int checkTimeIndex = 2;
        for(int i = 0; i < checkTimeIndex; i++){
            if(_tp.tUnit[i] != -1)  return;
        }

        // 获取当前是在周几，如果识别到的时间小于当前时间，则识别时间为下一周
        Calendar curC = Calendar.getInstance();
        if (this.normalizer.timeBase() != null) {
            String[] ini = this.normalizer.timeBase().split("-");
            curC.set(Integer.valueOf(ini[0]).intValue(), Integer.valueOf(ini[1]).intValue(),
                    Integer.valueOf(ini[2]).intValue(), Integer.valueOf(ini[3]).intValue(),
                    Integer.valueOf(ini[4]).intValue(), Integer.valueOf(ini[5]).intValue());
        }
        int curWeekday = curC.get(Calendar.DAY_OF_WEEK);
        if(weekday == 1){
            weekday = 7;
        }
        if(curWeekday < weekday){
            return;
        }

        //准备增加的时间单位是被检查的时间的上一级，将上一级时间+1
        c.add(Calendar.WEEK_OF_YEAR, 1);
    }

    /**
     * 根据上下文时间补充时间信息
     * <p>
     * @param checkTimeIndex 0-5 表示 年-秒
     */
    private void checkContextTime(int checkTimeIndex) {
        for(int i = 0; i < checkTimeIndex ; i++) {
            if(_tp.tUnit[i] == -1 && _tp_origin.tUnit[i] != -1) {
                _tp.tUnit[i] = _tp_origin.tUnit[i];
            }
        }

        // 在处理小时这个级别时，如果上文时间是下午的且下文没有主动声明小时级别以上的时间，则也把下文时间设为下午
        if (isFirstTimeSolveContext == true &&
                checkTimeIndex == 3 &&
                _tp_origin.tUnit[checkTimeIndex] >= 12 &&
                _tp.tUnit[checkTimeIndex] < 12 ) {
            _tp.tUnit[checkTimeIndex] += 12;
        }
        isFirstTimeSolveContext = false;
    }

    public Date getTime() {
        return time;
    }

    public Boolean getIsAllDayTime() {
        return isAllDayTime;
    }

    public void setIsAllDayTime(Boolean isAllDayTime) {
        this.isAllDayTime = isAllDayTime;
    }

}
