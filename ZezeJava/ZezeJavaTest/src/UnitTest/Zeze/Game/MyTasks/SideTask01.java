package UnitTest.Zeze.Game.MyTasks;

import java.util.Map;
import TaskTest.TaskExt.BSideTask01;
import Zeze.Builtin.Game.TaskBase.BTask;
import Zeze.Game.TaskBase;

public class SideTask01 extends TaskBase<BSideTask01> {
	static int DAILY_TASK_TYPE = 3;
	public SideTask01(Module module) {
		super(module);
	}

	@Override
	public int getType() {
		return DAILY_TASK_TYPE;
	}

	@Override
	protected void loadBeanExtended(BTask bean) {

	}

	@Override
	protected void loadMapExtended(Map<String, String> map) {

	}
}
