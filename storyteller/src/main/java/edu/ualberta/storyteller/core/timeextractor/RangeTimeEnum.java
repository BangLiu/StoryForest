package edu.ualberta.storyteller.core.timeextractor;

/**
 * 此类定义范围时间的默认时间点
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1220
 */
public enum RangeTimeEnum {

	/**
	 * 不同范围时间的默认时间
	 */
	// 凌晨
	day_break(3),
	// 早
	early_morning(8),
	// 上午
	morning(10),
	// 中午、午间
	noon(12),
	// 下午、午后
	afternoon(15),
	// 晚上、傍晚
	night(18),
	// 晚、晚间
	lateNight(20),
	// 深夜
	midNight(23);

	/**
	 * 用于设置和获取不同范围时间的值
	 */
	private int hourTime = 0;

	/**
	 * 设置范围时间
     * <p>
	 * @param hourTime 范围时间
	 */
	RangeTimeEnum(int hourTime) {
		this.setHourTime(hourTime);
	}

	/**
	 * 设置范围时间
     * <p>
	 * @param hourTime 范围时间
	 */
	public void setHourTime(int hourTime) {
		this.hourTime = hourTime;
	}

	/**
	 * 获取范围时间
     * <p>
	 * @return 范围时间
	 */
	public int getHourTime() {
		return hourTime;
	}
	
}
