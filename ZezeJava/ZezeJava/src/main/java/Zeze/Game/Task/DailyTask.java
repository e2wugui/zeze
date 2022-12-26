package Zeze.Game.Task;

import java.util.ArrayList;
import java.util.List;
import javax.json.JsonObject;
import Zeze.Builtin.Game.TaskBase.BDailyTask;
import Zeze.Builtin.Game.TaskBase.BTask;
import Zeze.Game.TaskBase;
import Zeze.Util.Random;

/**
 * 内置任务类型：日常任务
 * <p>
 * 功能：
 * 可内置多个小任务，每天更新时随机（或按照一定规则）刷4个任务。
 * 单个任务被视作一个Phase。
 *
 * 每个Phase逻辑上没有先后级，但是在做的过程中是有的。通常是玩家到达每日任务的边上自动触发那个每日任务为currentPhase
 * <p>
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
		bean = new BDailyTask();
		bean.setEverydayTaskCount(json.getInt("everydayTaskCount"));
		bean.setFlushTime(json.getInt("flushTime"));
	}

	@Override
	protected boolean isAbleToStartTask() {
		return true;
	}

	@Override
	protected void onCompleteTask() {
	}

	/**
	 * Runtime方法：每日刷新任务
	 * 刷新四个Phase，每个Phase随机一个任务。
	 */
	public void flushTask() {
		// 这里可以设置个定时器，然后每天刷新。
		// 或者让客户端每天凌晨刷新的时候发rpc也行。
		getBean().setTaskState(Module.Processing); // 刷新状态

		var everydayTaskCount = bean.getEverydayTaskCount();
		var totalPhaseSize = getBean().getTaskPhases().size();

		List<Long> randomPhasesIds = new ArrayList<>();

		var random = Random.getInstance();
		for (int i = 0; i < everydayTaskCount; ++i) {
			var id = random.nextLong(totalPhaseSize);
			while (randomPhasesIds.contains(id)) {
				id = random.nextLong(totalPhaseSize);
			}
			randomPhasesIds.add(id);
		}

		for (var id : randomPhasesIds) {
			bean.getTodayTaskPhaseIds().add(id);
		}
	}
}
