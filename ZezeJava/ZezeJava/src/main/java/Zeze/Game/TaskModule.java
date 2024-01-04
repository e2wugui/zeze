package Zeze.Game;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Game.TaskModule.Abandon;
import Zeze.Builtin.Game.TaskModule.Accept;
import Zeze.Builtin.Game.TaskModule.BRoleTasks;
import Zeze.Builtin.Game.TaskModule.Finish;
import Zeze.Builtin.Game.TaskModule.GetRoleTasks;
import Zeze.Collections.BeanFactory;
import Zeze.Game.Task.ConditionEvent;
import Zeze.Game.Task.RewardConfig;
import Zeze.Game.Task.TaskGraphics;
import Zeze.Game.Task.TaskImpl;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;

public class TaskModule extends AbstractTaskModule {
	private final Online online;

	public TaskModule(Online online) {
		this.online = online;
	}

	public Online getOnline() {
		return online;
	}

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

	@Override
	protected long ProcessGetRoleTasksRequest(GetRoleTasks r) throws Exception {
		return 0;
	}

	// 服务器内部接口
	public void accept(long roleId, int taskId) {

	}

	public void abandon(long roleId, int taskId) {

	}

	public void dispatch(long roleId, ConditionEvent event) throws Exception {
		TaskImpl.dispatch(this, roleId, event);
	}

	private TaskGraphics taskGraphics;
	private final RewardConfig rewardConfig = new RewardConfig();

	public TaskGraphics getTaskGraphics() {
		return taskGraphics;
	}

	public void setTaskGraphics(TaskGraphics taskGraphics) {
		this.taskGraphics = taskGraphics;
	}

	public RewardConfig getRewardConfig() {
		return rewardConfig;
	}

	public BRoleTasks getRoleTasks(long roleId) {
		return _tRoleTasks.getOrAdd(roleId);
	}

	public static long getSpecialTypeIdFromBean(Bean bean) {
		return bean.typeId();
	}

	public static long getSpecialTypeIdFromBean(Data bean) {
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
