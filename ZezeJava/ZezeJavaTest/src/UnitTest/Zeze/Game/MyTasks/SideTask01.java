package UnitTest.Zeze.Game.MyTasks;

import java.util.Map;
import javax.json.JsonObject;
import TaskTest.TaskExt.BSideTask01;
import Zeze.Builtin.Game.TaskBase.BTask;
import Zeze.Game.TaskBase;

public class SideTask01 extends TaskBase<BSideTask01> {
	public SideTask01(Module module) {
		super(module);
	}

	@Override
	public String getType() {
		return "Side";
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
