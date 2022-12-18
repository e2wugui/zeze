package UnitTest.Zeze.Game.MyTasks;

import java.util.Map;
import TaskTest.TaskExt.BDailyTask01;
import Zeze.Game.TaskBase;

public class DailyTask01 extends TaskBase<BDailyTask01> {

	@Override
	public int getType() {
		return 0;
	}

	@Override
	public void parse(Map<String, String> values) {

	}

	public DailyTask01(Module module) {
		super(module, DailyTask01.class);
	}

	@Override
	protected void loadBeanExtended() {

	}

	@Override
	protected void loadMapExtended() {

	}
}
