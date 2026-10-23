package Zeze.Transaction.Collections;

import java.util.ArrayList;

import Zeze.Transaction.Collections.LogList1.OpLog;

import java.util.Collection;

/** Transaction state with ordered structural notes, shared by List1 and List2. */
abstract class LogListDelta<V> extends LogList<V> {
	@Override
	public final boolean hasDelta() {
		return true;
	}

	// A field write normally produces one operation; bulk writes reserve their complete sequence below.
	protected final ArrayList<OpLog<V>> opLogs = new ArrayList<>(1);

	LogListDelta(PList<V> source, org.pcollections.PVector<V> value) {
		super(source, value);
	}

	public ArrayList<OpLog<V>> getOpLogs() {
		return opLogs;
	}

	@Override
	public boolean add(V item) {
		int index = getValue().size();
		super.add(item);
		opLogs.add(new OpLog<>(OpLog.OP_ADD, index, item));
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends V> items) {
		int index = getValue().size();
		if (!super.addAll(items))
			return false;
		opLogs.ensureCapacity(opLogs.size() + items.size());
		for (V item : items)
			opLogs.add(new OpLog<>(OpLog.OP_ADD, index++, item));
		return true;
	}

	@Override
	public void clear() {
		super.clear();
		opLogs.clear();
		opLogs.add(new OpLog<>(OpLog.OP_CLEAR, 0, null));
	}

	@Override
	public void add(int index, V item) {
		super.add(index, item);
		opLogs.add(new OpLog<>(OpLog.OP_ADD, index, item));
	}

	@Override
	public V set(int index, V item) {
		V old = super.set(index, item);
		opLogs.add(new OpLog<>(OpLog.OP_MODIFY, index, item));
		return old;
	}

	@Override
	public V remove(int index) {
		V old = super.remove(index);
		opLogs.add(new OpLog<>(OpLog.OP_REMOVE, index, null));
		return old;
	}

	private void merge(LogListDelta<V> from) {
		if (!from.opLogs.isEmpty()) {
			if (from.opLogs.getFirst().op == OpLog.OP_CLEAR)
				opLogs.clear();
			opLogs.addAll(from.opLogs);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void mergeFrom(LogCollection child) {
		super.mergeFrom(child);
		merge((LogListDelta<V>)child);
	}

}
