package Zeze.Game;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Game.Task.BTaskCondition;
import Zeze.Builtin.Game.Task.BTaskPhase;
import Zeze.Collections.BeanFactory;
import Zeze.Collections.DAG;
import Zeze.Transaction.Bean;

public class TaskPhase {
	private final static BeanFactory beanFactory = new BeanFactory();
	private final Task task; // Phase所属的Task
	private final long phaseId; // Phase的Id（自动生成，任务内唯一）
	private BTaskPhase bean;
	private final ArrayList<ConditionEvent> conditions = new ArrayList<>(); // 任务的各个事件
	public static long getSpecialTypeIdFromBean(Bean bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}
	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public TaskPhase(Task task, long phaseId) {
		this.task = task;
		this.phaseId = phaseId;
	}

	public Task getTask() {
		return task;
	}
	public long getPhaseId() {
		return phaseId;
	}
	public BTaskPhase getBean() {
		return bean;
	}
	public void setBean(BTaskPhase bean) {
		this.bean = bean;
	}
	public boolean accept(ConditionEvent event) {
		return false;
	}
}
