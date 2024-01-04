package Zeze.Game.Task;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jetbrains.annotations.NotNull;

public class ConditionBag implements Condition {
	/**
	 * 在包裹中添加或者删除物品某种物品的时候触发事件。
	 */
	public static class Event extends ConditionEvent {
		private final int itemId;

		public Event(int itemId) {
			super("");
			this.itemId = itemId;
		}

		public int getItemId() {
			return itemId;
		}
	}

	private int count;
	private int expected;
	private int itemId; // 不考虑接受多个物品。这也可实现，不过即使实现也不建议用。

	public ConditionBag() {

	}

	public ConditionBag(int itemId, int expected) {
		this.itemId = itemId;
		this.expected = expected;
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public boolean accept(ConditionEvent event) {
		if (event instanceof Event e) {
			if (itemId == e.getItemId()) {
				count = bagCount(itemId);
				// 注意，当存在多个ConditionBag需要拾取相同的物品时，不支持广播时，只会有一个条件被满足。
				// 如果支持广播（现不考虑），多个条件都会被满足，但是交任务时，先扣减成功的可以完成，
				// 另外的任务条件可能会变成不满足。建议使用ConditionPickItem来作为通常拾取物品条件。
				// ConditionBag只用来实现特殊的物品拾取，避免上面的问题。
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isDone() {
		return count >= expected;
	}

	private int bagCount(int itemId) {
		return 0; // todo 访问包裹进行物品数量统计。
	}

	private boolean bagRemove(int itemId, int count) {
		return false; // todo 删除包裹物品。
	}

	@Override
	public boolean finish() {
		if (bagRemove(itemId, expected))
			return true;
		// 当发生完成任务的时候扣除物品失败，意味着物品变成了不够的，更新一下本地的计数。
		// 最外面完成任务的时候会检查finish结果，失败的时候，会重新更新任务的状态。
		count = bagCount(itemId);
		return false;
	}

	@Override
	public String getDescription() {
		return "pick " + itemId + " " + count + "/" + expected;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteInt(count);
		bb.WriteInt(expected);
		bb.WriteInt(itemId);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		count = bb.ReadInt();
		expected = bb.ReadInt();
		itemId = bb.ReadInt();
	}
}
