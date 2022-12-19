package UnitTest.Zeze.Game.MyTasks;

import java.util.Map;
import TaskTest.TaskExt.BDailyTask01;
import Zeze.Builtin.Game.TaskBase.BTask;
import Zeze.Game.TaskBase;

public class DailyTask01 extends TaskBase<BDailyTask01> {
	static int DAILY_TASK_TYPE = 1;

	private BDailyTask01 dailyTaskData;

	@Override
	public int getType() {
		return DAILY_TASK_TYPE;
	}

	public DailyTask01(Module module) {
		super(module, DailyTask01.class);
	}

	@Override
	protected void loadBeanExtended(BTask bean) {
		dailyTaskData = (BDailyTask01)bean.getExtendedData().getBean();
	}

	@Override
	protected void loadMapExtended(Map<String, String> map) {
		var taskNum = map.get("TaskNum");
		if (null != taskNum) {
			dailyTaskData.setTaskNum(Integer.parseInt(taskNum));
		}
	}
}
