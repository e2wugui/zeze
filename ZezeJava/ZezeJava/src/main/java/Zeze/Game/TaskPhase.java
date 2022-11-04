package Zeze.Game;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Game.Task.BTaskCondition;
import Zeze.Builtin.Game.Task.BTaskPhase;
import Zeze.Collections.BeanFactory;
import Zeze.Collections.DAG;
import Zeze.Transaction.Bean;

public class TaskPhase {
	private final static BeanFactory beanFactory = new BeanFactory();
	private final Task task; // Phase所属的Task
	private final String phaseId; // Phase的Id（自动生成，任务内唯一）
	private String phaseDescription; // Phase的名称
	private BTaskPhase bean;
	private DAG<BTaskCondition> conditionDAG;
	private final ConcurrentHashMap<String, Condition> conditions = new ConcurrentHashMap<>(); // 任务阶段的各个条件
	public static long getSpecialTypeIdFromBean(Bean bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}
	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public TaskPhase(Task task, String phaseId, String phaseName) {
		this.task = task;
		this.phaseId = phaseId;
		this.phaseDescription = "";
	}

	public Task getTask() {
		return task;
	}

	public String getPhaseId() {
		return phaseId;
	}

	public String getPhaseDescription() {
		return phaseDescription;
	}

	public BTaskPhase getBean() {
		return bean;
	}

	public void setPhaseDescription(String phaseDescription) {
		this.phaseDescription = phaseDescription;
	}

	public void setBean(BTaskPhase bean) {
		this.bean = bean;
	}

	public boolean accept(ConditionEvent event) {
		return false;
	}
}
