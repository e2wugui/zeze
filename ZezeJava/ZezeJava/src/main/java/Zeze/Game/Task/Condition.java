package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskModule.BCondition;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

public interface Condition extends Serializable {
	String getName();
	boolean accept(ConditionEvent event);
	boolean isDone();

	// 任务完成的时候会调用这个方法，某些条件需要实现这个方法，再次确认isDone。
	// 比如包裹内的物品数量作为条件时，需要实现它。因为任务完成时，物品数量可能发生了变动。
	default boolean finish() {
		return true;
	}

	// 描述，用于客户端显示。
	String getDescription();

	static Condition construct(BCondition bean) throws Exception {
		var c = construct(bean.getClassName());
		c.decode(ByteBuffer.Wrap(bean.getParameter()));
		return c;
	}

	static Condition construct(String className) throws Exception {
		return (Condition)Class.forName(className).getConstructor().newInstance();
	}
}
