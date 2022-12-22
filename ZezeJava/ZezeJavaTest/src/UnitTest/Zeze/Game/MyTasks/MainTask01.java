package UnitTest.Zeze.Game.MyTasks;

import java.util.Map;
import javax.json.JsonObject;
import TaskTest.TaskExt.BMainTask01;
import Zeze.Builtin.Game.TaskBase.BTask;
import Zeze.Game.TaskBase;

public class MainTask01 extends TaskBase<BMainTask01> {
	public MainTask01(Module module) {
		super(module);
	}

	@Override
	public String getType() {
		return "Main";
	}

	@Override
	protected void loadBeanExtended(BTask bean) {

	}

	@Override
	protected void loadJsonExtended(JsonObject json) {

	}

	@Override
	protected void loadMapExtended(Map<String, String> map) {

	}
}
