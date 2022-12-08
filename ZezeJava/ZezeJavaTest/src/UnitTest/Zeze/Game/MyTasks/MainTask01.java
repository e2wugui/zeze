package UnitTest.Zeze.Game.MyTasks;

import TaskTest.TaskExt.BMainTask01;
import Zeze.Game.TaskBase;

public class MainTask01 extends TaskBase<BMainTask01> {
	public static class Opt extends TaskBase.Opt {

	}
	public MainTask01(Module module, Opt opt) {
		super(module, opt);
	}

	@Override
	protected void loadExtendedData() {
	}
}
