package Zeze.Game;

import Zeze.Builtin.Game.TaskModule.Abandon;
import Zeze.Builtin.Game.TaskModule.Accept;
import Zeze.Builtin.Game.TaskModule.BCondition;
import Zeze.Builtin.Game.TaskModule.Finish;
import Zeze.Collections.BeanFactory;
import Zeze.Game.Task.Condition;
import Zeze.Game.Task.ConditionEvent;
import Zeze.Game.Task.TaskGraphics;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;

public class TaskModule extends AbstractTaskModule {
	// 玩家操作
	@Override
	protected long ProcessAbandonRequest(Abandon r) throws Exception {
		return 0;
	}

	@Override
	protected long ProcessAcceptRequest(Accept r) throws Exception {
		return 0;
	}

	@Override
	protected long ProcessFinishRequest(Finish r) throws Exception {
		return 0;
	}

	// 服务器内部接口
	public void accept(long roleId, int taskId) {

	}

	public void abandon(long roleId, int taskId) {

	}

	public void dispatch(long roleId, ConditionEvent event) {

	}

	private TaskGraphics taskGraphics;

	public TaskGraphics getTaskGraphics() {
		return taskGraphics;
	}

	public Condition buildCondition(BCondition bean) {
		return null;
	}

	public static long getSpecialTypeIdFromBean(Bean bean) {
		return bean.typeId();
	}

	private static final BeanFactory beanFactory = new BeanFactory();

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public static Data createDataFromSpecialTypeId(long typeId) {
		return beanFactory.createDataFromSpecialTypeId(typeId);
	}
}
