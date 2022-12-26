package UnitTest.Zeze.Game.MyTasks;

import javax.json.JsonObject;
import TaskTest.TaskExt.BMyTask01;
import Zeze.Builtin.Game.TaskBase.BTask;
import Zeze.Game.TaskBase;

public class MyTask01 extends TaskBase<BMyTask01> {
	BMyTask01 bean;
	public MyTask01(Module module) {
		super(module);
	}

	@Override
	public String getType() {
		return "MyTask01";
	}

	@Override
	public BMyTask01 getExtendedBean() {
		return bean;
	}

	@Override
	protected void loadJsonExtended(JsonObject json) {

	}

	@Override
	protected void loadBeanExtended(BTask bean) {
		this.bean = (BMyTask01)bean.getExtendedData().getBean();
	}

	@Override
	protected boolean isAbleToStartTask() {
		return false;
	}

	@Override
	protected void onCompleteTask() {

	}
}
