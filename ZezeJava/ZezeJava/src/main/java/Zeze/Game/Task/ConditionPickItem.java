package Zeze.Game.Task;

import java.util.ArrayList;
import java.util.List;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jetbrains.annotations.NotNull;

/**
 * 拾取计数型条件，可以配置可接受的物品Id。物品Id之间是或者的关系。如，拾取猪心或熊心 0/10
 */
public class ConditionPickItem implements Condition {
	public static class Event extends ConditionEvent {
		private final int itemId;
		private final int count;

		public Event(int itemId, int count) {
			super("");
			this.itemId = itemId;
			this.count = count;
		}

		public int getItemId() {
			return itemId;
		}

		public int getCount() {
			return count;
		}
	}

	private int count;
	private int expected;
	private List<Integer> items; // 本来是期望的物品Id集合，但为了描述顺序使用List，一般是少量的，所以问题不大。

	public ConditionPickItem() {

	}

	public ConditionPickItem(int expected, List<Integer> items) {
		this.expected = expected;
		this.items = items;
	}

	@Override
	public String getName() {
		return ""; // instanceof 模式匹配，名字为空。see ConditionEvent
	}

	@Override
	public boolean accept(ConditionEvent event) {
		if (event instanceof Event e) {
			if (items.contains(e.getItemId())) {
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
		sb.append("pick ");
		var first = true;
		for (var m : items) {
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
		bb.WriteInt(items.size());
		for (var m : items)
			bb.WriteInt(m);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		count = bb.ReadInt();
		expected = bb.ReadInt();
		items = new ArrayList<>();
		for (var c = bb.ReadInt(); c > 0; --c)
			items.add(bb.ReadInt());
	}
}
