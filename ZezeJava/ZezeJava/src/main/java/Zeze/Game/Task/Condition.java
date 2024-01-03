package Zeze.Game.Task;

import Zeze.Serialize.Serializable;

public interface Condition extends Serializable {
	String getName();
	boolean accept(ConditionEvent event);
	boolean isDone();

	// 任务完成的时候会触发这个，某些条件需要实现这个方法，再次确认idDone。
	// 比如包裹内的物品数量作为条件时，需要实现，因为完成时，物品数量可能发生了变动。
	default boolean finish() {
		return true;
	}
}
