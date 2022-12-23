package Zeze.Game.Task;

import javax.json.JsonObject;
import Zeze.Builtin.Game.TaskBase.BDailyTask;
import Zeze.Builtin.Game.TaskBase.BTask;
import Zeze.Game.TaskBase;

/**
 * 内置任务类型：日常任务
 *
 * 功能：
 * 可内置多个小任务，每天更新时随机（或按照一定规则）刷4个任务。
 *
 * 后续扩展：考虑数据记录等，比如记录哪个每日任务完成了几次等等。
 */
public class DailyTask extends TaskBase<BDailyTask> {
	// @formatter:off
	private BDailyTask bean;
	public DailyTask(Module module) { super(module); }
	@Override
	public String getType() { return "DailyTask"; }
	@Override
	public BDailyTask getExtendedBean() { return bean; }
	@Override
	protected void loadBeanExtended(BTask bean) { this.bean = (BDailyTask)bean.getExtendedData().getBean(); }
	// @formatter:on
	@Override
	protected void loadJsonExtended(JsonObject json) {
		bean.setEverydayTaskCount(json.getInt("everydayTaskCount"));
		bean.setFlushTime(json.getInt("flushTime"));
	}

	/**
	 * Runtime方法：每日刷新任务
	 * 刷新四个Phase，每个Phase随机一个任务。
	 */
	public void flushTask() {
		var everydayTaskCount = bean.getEverydayTaskCount();

	}
}
