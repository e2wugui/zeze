package Zeze.Game.Task;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * 杀怪计数型条件，可以配置可接受的怪物Id。怪物Id之间是或者的关系。如，杀死兔子或乌龟 0/10
 */
public class ConditionKillMonster implements Condition {
	public static class Event extends ConditionEvent {
		private final int monsterId;
		private final int count;

		public Event(int monsterId, int count) {
			super("");
			this.monsterId = monsterId;
			this.count = count;
		}

		public int getMonsterId() {
			return monsterId;
		}

		public int getCount() {
			return count;
		}
	}

	private int count;
	private int expected;
	private List<Integer> monsters; // 本来是期望的怪物Id集合，但为了描述顺序使用List，一般是少量的，所以问题不大。

	public ConditionKillMonster() {

	}

	public ConditionKillMonster(int expected, List<Integer> monsters) {
		this.expected = expected;
		this.monsters = monsters;
	}

	@Override
	public String getName() {
		return ""; // instanceof 模式匹配，名字为空。see ConditionEvent
	}

	@Override
	public boolean accept(ConditionEvent event) {
		if (event instanceof Event e) {
			if (monsters.contains(e.getMonsterId())) {
				count += e.getCount();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isDone() {
		return count >= expected;
	}

	@Override
	public String getDescription() {
		var sb = new StringBuilder();
		sb.append("kill ");
		var first = true;
		for (var m : monsters) {
			if (!first)
				sb.append(" or ");
			else
				first = false;
			sb.append(m); // todo to name;
		}
		sb.append(" ").append(count).append("/").append(expected);
		return sb.toString();
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteInt(count);
		bb.WriteInt(expected);
		bb.WriteInt(monsters.size());
		for (var m : monsters)
			bb.WriteInt(m);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		count = bb.ReadInt();
		expected = bb.ReadInt();
		monsters = new ArrayList<>();
		for (var c = bb.ReadInt(); c > 0; --c)
			monsters.add(bb.ReadInt());
	}
}
