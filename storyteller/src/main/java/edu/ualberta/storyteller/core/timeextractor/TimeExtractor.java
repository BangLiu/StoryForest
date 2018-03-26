package edu.ualberta.storyteller.core.timeextractor;

import edu.ualberta.storyteller.core.util.StringUtils;
import edu.ualberta.storyteller.core.util.TimeUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 时间表达式识别的主要工作类
 * 此类读入匹配时间的正则表达式的文件,得到字符串片段后交由TimeUnit类识别
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1220
 */
public class TimeExtractor implements Serializable {

    /**
     * 基准时间
     */
	private String timeBase;

    /**
     * 老基准时间
     */
	private String oldTimeBase;

    /**
     * 用于匹配输入字符串中时间表达式的pattern.
     * 它从模型文件中导入. 是最关键的变量! 直接影响时间识别器的性能.
     */
	private static Pattern patterns = null;

    /**
     * 待解析字符串
     */
	private String target;

    /**
     * 检测到的时间列表
     */
	private TimeUnit[] timeUnits = new TimeUnit[0];

    /**
     * 是否开启未来倾向功能
     */
	private boolean isPreferFuture = true;

	/**
	 * 构造器
     * <p>
	 * @param path TimeExp.m文件路径
	 */
	public TimeExtractor(String path) {
		if (patterns == null) {
			try {
				patterns = readModel(path);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.print("Read model error!");
			}
		}
	}

	/**
	 * 构造器
     * <p>
	 * @param path TimeExp.m文件路径
     * @param isPreferFuture 是否开启未来倾向功能
	 */
	public TimeExtractor(String path, boolean isPreferFuture) {
		this.isPreferFuture = isPreferFuture;
		if (patterns == null) {
			try {
				patterns = readModel(path);
				System.out.println(patterns.pattern());
			} catch (Exception e) {
				e.printStackTrace();
				System.err.print("Read model error!");
			}
		}
	}
	
	/**
	 * TimeNormalizer的构造方法，根据提供的待分析字符串和timeBase进行时间表达式提取
	 * 在构造方法中已完成对待分析字符串的表达式提取工作
     * <p>
	 * @param target 待分析字符串
	 * @param timeBase 给定的timeBase
	 * @return 返回值
	 */
	public TimeUnit[] parse(String target, String timeBase) {
		this.target = target;
		this.timeBase = timeBase;
		this.oldTimeBase = timeBase;
		// 字符串预处理
		preHandling();
		timeUnits = extractTime(this.target, timeBase);
		return timeUnits;
	}

	/**
	 * 同上的TimeNormalizer的构造方法，timeBase取默认的系统当前时间
	 * <p>
	 * @param target 待分析字符串
	 * @return 时间单元数组
	 */
	public TimeUnit[] parse(String target) {
		this.target = target;
        // TODO Calendar.getInstance().getTime()换成new Date
		this.timeBase = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().getTime());
		this.oldTimeBase = timeBase;
		preHandling();// 字符串预处理
		timeUnits = extractTime(this.target, timeBase);
		return timeUnits;
	}

	/**
	 * 待匹配字符串的清理空白符和语气助词以及大写数字转化的预处理
	 */
	private void preHandling() {
        // 清理空白符
		target = StringUtils.delKeyword(target, "\\s+");
        // 清理语气助词
		target = StringUtils.delKeyword(target, "[的]+");
        // 大写数字转化
		target = StringUtils.numberTranslator(target);
		// TODO: 处理大小写标点符号
	}

	/**
	 * 有基准时间输入的时间表达式识别
	 * <p>
	 * 这是时间表达式识别的主方法， 通过已经构建的正则表达式对字符串进行识别，并按照预先定义的基准时间进行规范化
	 * 将所有别识别并进行规范化的时间表达式进行返回， 时间表达式通过TimeUnit类进行定义
     * <p>
	 * @param tar 输入文本字符串
	 * @param timebase 输入基准时间
	 * @return TimeUnit[] 时间表达式类型数组
	 */
	private TimeUnit[] extractTime(String tar, String timebase) {
		Matcher match;
		int startLine = -1;
		int endLine = -1;

        // 用于存储从输入字符串中识别到的所有时间表达语句片段
		String[] temp = new String[99];
        // 计数器，记录当前识别到哪一个字符串了
		int rPointer = 0;
		TimeUnit[] timeResult;

		match = patterns.matcher(tar);
		while (match.find()) {
			startLine = match.start();
            // 假如下一个识别到的时间字段和上一个是相连的 @author kexm
			if (endLine == startLine) {
				rPointer--;
                // 则把下一个识别到的时间字段加到上一个时间字段去
				temp[rPointer] = temp[rPointer] + match.group();
			} else {
                // 记录当前识别到的时间字段
				temp[rPointer] = match.group();
			}
			endLine = match.end();
			rPointer++;
		}
		timeResult = new TimeUnit[rPointer];

        // 时间上下文:前一个识别出来的时间会是下一个时间的上下文，用于处理"周六3点到5点"这样的多个时间的识别，第二个5点应识别到是周六的.
        // NOTICE: 因此多句识别可能会受影响.前一句的时间不一定是后一句的上下文.
		TimePoint contextTp = new TimePoint();
		for (int j = 0; j < rPointer; j++) {
            // 一一识别匹配到的各个时间语句片段
			timeResult[j] = new TimeUnit(temp[j], this, contextTp);
			contextTp = timeResult[j]._tp;
		}

        // 过滤无法识别的字段
        // TODO: 过滤非法的时间. 例如1997-34-23, 2001-2-30, ...
		timeResult = filterTimeUnit(timeResult);

		return timeResult;
	}
	
	/**
	 * 过滤timeUnit中无用的识别词。无用识别词识别出的时间是1970.01.01 00:00:00(fastTime=-28800000)
     * <p>
	 * @param timeUnit 时间单元列表
	 * @return 过滤的时间列表
	 */
	public static TimeUnit[] filterTimeUnit(TimeUnit[] timeUnit){
		if(timeUnit == null || timeUnit.length < 1){
			return timeUnit;
		}
		List<TimeUnit> list = new ArrayList<>();
        for(TimeUnit t : timeUnit){
        	if(t.getTime().getTime() != -28800000){
        		list.add(t);
        	}
        }
        TimeUnit[] newT = new TimeUnit[list.size()];
        newT = list.toArray(newT);
        return newT;
	}
	
	private Pattern readModel(String file) throws Exception {
		ObjectInputStream in = new ObjectInputStream(
				new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))));
		return readModel(in);
	}

	private Pattern readModel(ObjectInputStream in) throws Exception {
		Pattern p = (Pattern) in.readObject();
		return Pattern.compile(p.pattern());
	}

	public static void writeModel(Object p, String path) throws Exception{
		ObjectOutputStream out = new ObjectOutputStream(
				new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(path))));
		out.writeObject(p);
		out.close();
	}

    /**
     * 重置timeBase为oldTimeBase
     */
    public void resetTimeBase() {
        timeBase = oldTimeBase;
    }

    public String timeBase() {
        return timeBase;
    }

    public void timeBase(String s) {
        timeBase = s;
    }

    public String oldTimeBase() {
        return oldTimeBase;
    }

    public boolean isPreferFuture() {
        return isPreferFuture;
    }

    public void preferFuture(boolean isPreferFuture) {
        this.isPreferFuture = isPreferFuture;
    }

    public TimeUnit[] getTimeUnit() {
        return timeUnits;
    }

    /**
     * main
     * @param args main parameters
     */
    public static void main(String args[]) throws Exception {
		String path = TimeExtractor.class.getResource("").getPath();
        System.out.println(path);
		String classPath = path.substring(0, path.indexOf("storyteller/"));

        // 读取中文时间正则表达式
        // TODO: 检测现有的表达式有无错误
        // TODO: 增加更多的时间正则表达式: 节日,例如双十一，情人节等; 专有名词，例如XX时代等, 其他
        // TODO: 这里能匹配到的时间字段并不是所有的情况都会被TimeUnit类处理.后面可以逐渐增加相应的处理函数.
        String ChineseTimeExp = StringUtils.concatenateLines(classPath + "storyteller/conf/ChineseTimeRE");
        System.out.println(ChineseTimeExp);

        Pattern p = Pattern.compile(ChineseTimeExp);
        writeModel(p, classPath+"storyteller/conf/ChineseTimeRE.zip");
		System.out.println(classPath + "storyteller/conf/ChineseTimeRE.zip");
		TimeExtractor normalizer = new TimeExtractor(classPath + "storyteller/conf/ChineseTimeRE.zip");

        // 测试
		normalizer.parse("19/9/2001是我生日");
		TimeUnit[] unit = normalizer.getTimeUnit();
		System.out.println(TimeUtils.formatDateDefault(unit[0].getTime()));
	}

}
