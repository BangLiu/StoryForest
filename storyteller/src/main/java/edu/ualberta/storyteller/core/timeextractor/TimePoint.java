package edu.ualberta.storyteller.core.timeextractor;

/**
 * 时间表达式单元规范化对应的内部类,
 * 对应时间表达式规范化的每个字段，
 * 六个字段分别是：年-月-日-时-分-秒，
 * 每个字段初始化为-1
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1220
 */
public class TimePoint {

	public int[] tUnit = {-1, -1, -1, -1, -1, -1};

}
