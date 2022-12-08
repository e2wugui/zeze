package UnitTest.Zeze.Game.MyTasks;

import TaskTest.TaskExt.BHiddenTask01;
import Zeze.Game.TaskBase;

public class HiddenTask01 extends TaskBase<BHiddenTask01> {
	public static class Opt extends TaskBase.Opt {

	}
	public HiddenTask01(Module module, Opt opt) {
		super(module, opt);
	}
	@Override
	protected void loadExtendedData() {

	}
}
