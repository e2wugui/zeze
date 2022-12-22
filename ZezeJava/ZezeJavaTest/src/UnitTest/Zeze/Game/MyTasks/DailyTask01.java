package UnitTest.Zeze.Game.MyTasks;

import java.util.Map;
import javax.json.JsonObject;
import TaskTest.TaskExt.BDailyTask01;
import Zeze.Builtin.Game.TaskBase.BTask;
import Zeze.Game.TaskBase;

public class DailyTask01 extends TaskBase<BDailyTask01> {

	public DailyTask01(Module module) {
		super(module);
	}

	private BDailyTask01 dailyTaskData;

	@Override
	public String getType() {
		return "Daily";
	}

	@Override
	protected void loadBeanExtended(BTask bean) {
		dailyTaskData = (BDailyTask01)bean.getExtendedData().getBean();
	}

	@Override
	protected void loadJsonExtended(JsonObject json) {

	}

	@Override
	protected void loadMapExtended(Map<String, String> map) {
		var taskNum = map.get("TaskNum");
		if (null != taskNum) {
			dailyTaskData.setTaskNum(Integer.parseInt(taskNum));
		}
	}
}
