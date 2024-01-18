package Zeze.Game;

import java.util.List;
import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Game.TaskModule.Abandon;
import Zeze.Builtin.Game.TaskModule.Accept;
import Zeze.Builtin.Game.TaskModule.BRoleTasks;
import Zeze.Builtin.Game.TaskModule.BTask;
import Zeze.Builtin.Game.TaskModule.BTaskConfig;
import Zeze.Builtin.Game.TaskModule.BTaskDescription;
import Zeze.Builtin.Game.TaskModule.Finish;
import Zeze.Builtin.Game.TaskModule.GetRoleTasks;
import Zeze.Collections.BeanFactory;
import Zeze.Collections.LinkedMap;
import Zeze.Game.Task.CheckTaskAcceptable;
import Zeze.Game.Task.ConditionEvent;
import Zeze.Game.Task.IBag;
import Zeze.Game.Task.RewardConfig;
import Zeze.Game.Task.TaskGraphics;
import Zeze.Game.Task.TaskImpl;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;
import Zeze.Transaction.EmptyBean;

public class TaskModule extends AbstractTaskModule {
	private final Online online;
	private final LinkedMap.Module linkedMapModule;
	private final IBag bag;
	private int maxAcceptedTaskCount = 50;

	public void setMaxAcceptedTaskCount(int maxAcceptedTaskCount) {
		this.maxAcceptedTaskCount = maxAcceptedTaskCount;
	}

	public int getMaxAcceptedTaskCount() {
		return maxAcceptedTaskCount;
	}

	public TaskModule(Online online, IBag bag, LinkedMap.Module linkedMapModule) {
		this.online = online;
		this.linkedMapModule =linkedMapModule;
		this.bag = bag;
	}

	public Online getOnline() {
		return online;
	}

	public IBag getBag() {
		return bag;
	}

	// 玩家操作
	@Override
	protected long ProcessAbandonRequest(Abandon r) throws Exception {
		var session = ProviderUserSession.get(r);
		var roleId = session.getRoleIdNotNull();
		var roleTasks = getRoleTasks(roleId);
		TaskImpl.abandonTask(this, roleId, roleTasks, r.Argument.getTaskId());
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessAcceptRequest(Accept r) throws Exception {
		var session = ProviderUserSession.get(r);
		var roleId = session.getRoleIdNotNull();
		return TaskImpl.acceptTask(this, roleId, r);
	}

	@Override
	protected long ProcessFinishRequest(Finish r) throws Exception {
		var session = ProviderUserSession.get(r);
		var roleId = session.getRoleIdNotNull();
		return TaskImpl.finishTask(this, roleId, r);
	}

	@Override
	protected long ProcessGetRoleTasksRequest(GetRoleTasks r) throws Exception {
		var session = ProviderUserSession.get(r);
		var roleId = session.getRoleIdNotNull();
		var roleTasks = getRoleTasks(roleId);
		for (var bTask : roleTasks.getTasks().values()) {
			var des = new BTaskDescription();
			TaskImpl.toDescription(this, roleId, bTask, des);
			r.Result.getTasks().put(bTask.getTaskId(), des);
		}
		r.SendResult();
		return 0;
	}

	// 服务器内部接口
	public void dispatch(long roleId, ConditionEvent event) throws Exception {
		TaskImpl.dispatch(this, roleId, event);
	}

	public void getNpcAcceptTasks(long roleId, int npcId,
								  List<BTaskConfig.Data> acceptableTasks,
								  List<BTaskConfig.Data> hasTasks) throws Exception {
		TaskImpl.getNpcAcceptTasks(this, roleId, npcId, acceptableTasks, hasTasks);
	}

	public List<BTask> getNpcFinishTasks(long roleId, int npcId) throws Exception {
		return TaskImpl.getNpcFinishTasks(this, roleId, npcId);
	}

	private TaskGraphics taskGraphics;
	private final RewardConfig rewardConfig = new RewardConfig();
	private CheckTaskAcceptable checkTaskAcceptable;

	public void setCheckTaskAcceptable(CheckTaskAcceptable checkTaskAcceptable) {
		this.checkTaskAcceptable = checkTaskAcceptable;
	}

	public CheckTaskAcceptable getCheckTaskAcceptable() {
		return checkTaskAcceptable;
	}

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

	public boolean checkTaskAcceptCondition(BTaskConfig.Data task, long roleId) {
		if (null != checkTaskAcceptable)
			checkTaskAcceptable.check(task, roleId);
		return true;
	}

	public LinkedMap<EmptyBean> getRoleCompletedTasks(long roleId) {
		return linkedMapModule.open("Zeze.Game.Task.Completed." + roleId, EmptyBean.class);
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
