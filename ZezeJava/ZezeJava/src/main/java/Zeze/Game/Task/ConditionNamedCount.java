package Zeze.Game.Task;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jetbrains.annotations.NotNull;

/**
 * 通用计数型条件，拥有一个计数值和期望值，给他不同的名字就能表示所有这种计数方式的条件。
 */
public class ConditionNamedCount implements Condition {
	private String name;
	private int count;
	private int expected;

	public ConditionNamedCount() {

	}

	public ConditionNamedCount(String name, int expected) {
		this.name = name;
		this.expected = expected;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean accept(ConditionEvent event) {
		if (name.equals(event.getName())) {
			count += 1;
			return true;
		}
		return false;
	}

	@Override
	public boolean isDone() {
		return count >= expected;
	}

	@Override
	public String getDescription() {
		return name + " " + count + "/" + expected;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteString(name);
		bb.WriteInt(count);
		bb.WriteInt(expected);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		name = bb.ReadString();
		count = bb.ReadInt();
		expected = bb.ReadInt();
	}
}
