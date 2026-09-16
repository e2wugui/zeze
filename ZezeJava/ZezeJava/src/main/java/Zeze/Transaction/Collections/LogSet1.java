package Zeze.Transaction.Collections;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;
import org.jetbrains.annotations.NotNull;
import org.pcollections.Empty;

public class LogSet1<V> extends LogSet<V> {
	protected final @NotNull Meta1<V> meta;
	private final Set<V> added = new HashSet<>();
	private final Set<V> removed = new HashSet<>();

	public LogSet1(Bean belong, int varId, Bean self, @NotNull org.pcollections.PSet<V> value,
				   @NotNull Meta1<V> meta) {
		super(belong, varId, self, value);
		this.meta = meta;
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public @NotNull String getTypeName() {
		return meta.name;
	}

	public final @NotNull Set<V> getAdded() {
		return added;
	}

	public final @NotNull Set<V> getRemoved() {
		return removed;
	}

	public final boolean add(@NotNull V item) {
		var newSet = getValue().plus(item);
		if (newSet != getValue()) {
			removed.remove(item);
			added.add(item);
			setValue(newSet);
			return true;
		}
		return false;
	}

	public final boolean addAll(@NotNull Collection<? extends V> c) {
		var old = getValue();
		// MapPSet.plusAll恒新建包装（same-instance判定失效），逐项plus：单项无变化返回同一
		// 实例，可正确判定"全已存在"（FND7-08）——无变化时不记账不置脏。
		var newSet = old;
		for (V v : c)
			newSet = newSet.plus(v);
		if (newSet == old)
			return false;
		for (V v : c) {
			removed.remove(v);
			added.add(v);
		}
		setValue(newSet);
		return true;
	}

	public final boolean remove(@NotNull V item) {
		var newSet = getValue().minus(item);
		if (newSet != getValue()) {
			added.remove(item);
			removed.add(item);
			setValue(newSet);
			return true;
		}
		return false;
	}

	public final boolean removeAll(@NotNull Collection<? extends V> c) {
		var old = getValue();
		// 同addAll：MapPSet.minusAll恒新建包装，逐项minus判定真实变化（FND7-08）。
		var newSet = old;
		for (V v : c)
			newSet = newSet.minus(v);
		if (newSet == old)
			return false;
		for (V v : c) {
			added.remove(v);
			removed.add(v);
		}
		setValue(newSet);
		return true;
	}

	public final void clear() {
		//for (V v : getValue())
		//	remove(v);
		var exist = getValue();
		added.removeAll(exist);
		removed.addAll(exist);
		setValue(Empty.set());
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		var encoder = meta.valueEncoder;

		bb.WriteUInt(added.size());
		for (V v : added) {
			//noinspection DataFlowIssue
			encoder.accept(bb, v);
		}

		bb.WriteUInt(removed.size());
		for (V v : removed) {
			//noinspection DataFlowIssue
			encoder.accept(bb, v);
		}
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		var decoder = meta.valueDecoder;

		added.clear();
		for (int i = bb.ReadUInt(); i > 0; i--) {
			//noinspection DataFlowIssue
			added.add(decoder.apply(bb));
		}

		removed.clear();
		for (int i = bb.ReadUInt(); i > 0; i--) {
			//noinspection DataFlowIssue
			removed.add(decoder.apply(bb));
		}
	}

	@Override
	public void endSavepoint(@NotNull Savepoint currentSp) {
		Log log = currentSp.getLog(getLogKey());
		if (log != null) {
			@SuppressWarnings("unchecked")
			var currentLog = (LogSet1<V>)log;
			currentLog.setValue(getValue());
			currentLog.merge(this);
		} else
			currentSp.putLog(this);
	}

	private void merge(@NotNull LogSet1<V> from) {
		// add,remove 需要确认有没有顺序问题
		// this: add 1,3 remove 2,4 nest: add 2 remove 1
		for (V v : from.added) {
			removed.remove(v); // replace 1,2,3 remove 4
			added.add(v);
		}
		for (V v : from.removed) {
			added.remove(v); // replace 2,3 remove 1,4
			removed.add(v);
		}
	}

	@Override
	public @NotNull Log beginSavepoint() {
		return new LogSet1<>(getBelong(), getVariableId(), getThis(), getValue(), meta);
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		sb.append(" added:");
		ByteBuffer.BuildSortedString(sb, added);
		sb.append(" removed:");
		ByteBuffer.BuildSortedString(sb, removed);
		return sb.toString();
	}
}
