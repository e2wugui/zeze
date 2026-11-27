package Zeze.Transaction.Collections;

import Zeze.Transaction.Collections.LogList1.OpLog;

import Zeze.Transaction.Bean;
import Zeze.Util.IdentityHashSet;
import org.jspecify.annotations.Nullable;
import org.pcollections.PVector;

/** PList2 的事务增量状态：记录有序结构操作及已以完整值新增的 Bean 身份。 */
final class LogList2Delta<V extends Bean> extends LogListDelta<V> {

	private @Nullable IdentityHashSet<V> addSet;

	LogList2Delta(PList<V> source, PVector<V> value) {
		super(source, value);
	}

	@Override
	public boolean add(V item) {
		if (!super.add(item))
			return false;
		if (addSet == null)
			addSet = new IdentityHashSet<>();
		addSet.add(item);
		return true;
	}

	@Override
	protected void noteAppend(int index, V item) {
		super.noteAppend(index, item);
		if (addSet == null)
			addSet = new IdentityHashSet<>();
		addSet.add(item);
	}

	@Override
	public void clear() {
		super.clear();
		if (addSet != null)
			addSet.clear();
	}

	@Override
	public void add(int index, V item) {
		super.add(index, item);
		if (addSet == null)
			addSet = new IdentityHashSet<>();
		addSet.add(item);
	}

	@Override
	public V set(int index, V item) {
		V old = super.set(index, item);
		if (addSet == null)
			addSet = new IdentityHashSet<>();
		addSet.remove(old);
		addSet.add(item);
		return old;
	}

	@Override
	public V remove(int index) {
		V old = super.remove(index);
		if (addSet != null)
			addSet.remove(old);
		return old;
	}

	private void merge(LogList2Delta<V> from) {
		if (!from.opLogs.isEmpty()) {
			if (from.opLogs.getFirst().op == OpLog.OP_CLEAR) {
				opLogs.clear();
				addSet = null;
			}
			opLogs.addAll(from.opLogs);
			if (from.addSet != null) {
				if (addSet == null)
					addSet = from.addSet;
				else
					addSet.addAll(from.addSet);
			}
		}
	}

	@Override
	protected void mergeFrom(LogCollection<?> child) {
		//noinspection unchecked
		var c = (LogList2Delta<V>)child;
		setValue(c.getValue());
		merge(c);
	}

	@Nullable IdentityHashSet<V> addedIdentities() {
		return addSet;
	}

	@Override
	public LogList2Delta<V> beginSavepoint() {
		return new LogList2Delta<>(source, getValue());
	}
}
