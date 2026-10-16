package Zeze.Transaction.Collections;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;

import Zeze.Collections.Persistents;

/** PSet1 的变更日志：以 added/removed 两个增量集合记录事务内对持久化 Set 的修改。 */
public class LogSet1<V> extends LogSet<V> {
	protected final Meta1<V> meta;
	private final Set<V> added = new HashSet<>();
	private final Set<V> removed = new HashSet<>();

	public LogSet1(@Nullable Bean belong, int varId, @Nullable Bean self, org.pcollections.PSet<V> value,
				   Meta1<V> meta) {
		super(belong, varId, self, value);
		this.meta = meta;
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public String getTypeName() {
		return meta.name;
	}

	public final Set<V> getAdded() {
		return added;
	}

	public final Set<V> getRemoved() {
		return removed;
	}

	public final boolean add(V item) {
		var newSet = getValue().plus(item);
		if (newSet != getValue()) {
			removed.remove(item);
			added.add(item);
			setValue(newSet);
			return true;
		}
		return false;
	}

	public final boolean addAll(Collection<? extends V> c) {
		var old = getValue();
		// MapPSet.plusAll恒新建包装（same-instance判定失效），逐项plus：单项无变化返回同一
		// 实例，可正确判定"全已存在"——无变化时不记账不置脏。
		var newSet = old;
		for (V v : c)
			newSet = newSet.plus(v);
		if (newSet == old)
			return false;
		// 只记真实新增：已存在的元素混入added会作为幻影增量进入Changes通知链，
		// 按增量驱动的下游监听器会产生错误动作（plus(v)含v时返回同一实例，等价判定）。
		for (V v : c) {
			if (!old.contains(v)) {
				removed.remove(v);
				added.add(v);
			}
		}
		setValue(newSet);
		return true;
	}

	public final boolean remove(V item) {
		var newSet = getValue().minus(item);
		if (newSet != getValue()) {
			added.remove(item);
			removed.add(item);
			setValue(newSet);
			return true;
		}
		return false;
	}

	public final boolean removeAll(Collection<? extends V> c) {
		var old = getValue();
		// 同addAll：MapPSet.minusAll恒新建包装，逐项minus判定真实变化。
		var newSet = old;
		for (V v : c)
			newSet = newSet.minus(v);
		if (newSet == old)
			return false;
		// 只记真实移除：不在集合中的元素混入removed同样是幻影增量（minus等价判定）。
		for (V v : c) {
			if (old.contains(v)) {
				added.remove(v);
				removed.add(v);
			}
		}
		setValue(newSet);
		return true;
	}

	public final void clear() {
		var exist = getValue();
		added.removeAll(exist);
		removed.addAll(exist);
		setValue(Persistents.set());
	}

	@Override
	public void encode(ByteBuffer bb) {
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
	public void decode(IByteBuffer bb) {
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
	public void endSavepoint(Savepoint currentSp) {
		Log log = currentSp.getLog(getLogKey());
		if (log != null) {
			@SuppressWarnings("unchecked")
			var currentLog = (LogSet1<V>)log;
			currentLog.setValue(getValue());
			currentLog.merge(this);
		} else
			currentSp.putLog(this);
	}

	private void merge(LogSet1<V> from) {
		for (V v : from.added) {
			removed.remove(v);
			added.add(v);
		}
		for (V v : from.removed) {
			added.remove(v);
			removed.add(v);
		}
	}

	@Override
	public Log beginSavepoint() {
		return new LogSet1<>(getBelong(), getVariableId(), getThis(), getValue(), meta);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" added:");
		ByteBuffer.BuildSortedString(sb, added);
		sb.append(" removed:");
		ByteBuffer.BuildSortedString(sb, removed);
		return sb.toString();
	}
}
