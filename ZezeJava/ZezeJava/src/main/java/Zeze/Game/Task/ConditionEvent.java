package Zeze.Game.Task;

/**
 * 条件事件。
 * 事件名字(name)和条件名字匹配成功，将引起条件状态变化。
 * 通常情况下只需要指定事件名字即可。
 * 在某些情况下，可能需要继承这个类，并带上额外的参数。
 * 条件事件和条件需要匹配。为了达到最大灵活度，分成如下几种：
 * 1. 按名字相等表示匹配。
 * 2. 条件识别专门的事件类型(instanceof)，完成匹配。这种匹配模式的条件和事件名字为空。
 */
public class ConditionEvent {
	private final String name;

	public ConditionEvent(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
