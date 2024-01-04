package Zeze.Game.Task;

import java.util.ArrayList;
import java.util.List;
import Zeze.Builtin.Game.TaskModule.BCondition;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jetbrains.annotations.NotNull;

/**
 * 把多个条件组合起来形成或的关系。
 */
public class ConditionCompositeOr implements Condition {
	private final List<Condition> composite;

	public ConditionCompositeOr() {
		this.composite = new ArrayList<>();
	}

	public ConditionCompositeOr(List<Condition> conditions) {
		this.composite = conditions;
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public boolean accept(ConditionEvent event) {
		for (var c : composite) {
			if (c.accept(event))
				return true;
		}
		return false;
	}

	@Override
	public boolean isDone() {
		for (var c : composite) {
			if (c.isDone())
				return true;
		}
		return false;
	}

	@Override
	public String getDescription() {
		var sb = new StringBuilder();
		for (var c : composite) {
			sb.append(c.getDescription()).append("|");
		}
		return sb.toString();
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteInt(composite.size());
		for (var c : composite) {
			var bean = new BCondition.Data();
			bean.setClassName(c.getClass().getName());
			var bbc = ByteBuffer.Allocate();
			c.encode(bbc);
			bean.setParameter(new Binary(bbc));
			bean.encode(bb);
		}
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		for (var count = bb.ReadInt(); count > 0; --count) {
			var bean = new BCondition.Data();
			bean.decode(bb);
			try {
				composite.add(Condition.construct(bean));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
