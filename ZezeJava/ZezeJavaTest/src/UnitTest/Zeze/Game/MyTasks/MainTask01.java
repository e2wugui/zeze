package UnitTest.Zeze.Game.MyTasks;

import java.util.Map;
import TaskTest.TaskExt.BMainTask01;
import Zeze.Builtin.Game.TaskBase.BTask;
import Zeze.Game.TaskBase;

public class MainTask01 extends TaskBase<BMainTask01> {
	static int MAIN_TASK_TYPE = 2;
	public MainTask01(Module module) {
		super(module);
	}

	@Override
	public int getType() {
		return MAIN_TASK_TYPE;
	}

	@Override
	protected void loadBeanExtended(BTask bean) {

	}

	@Override
	protected void loadMapExtended(Map<String, String> map) {

	}
}
