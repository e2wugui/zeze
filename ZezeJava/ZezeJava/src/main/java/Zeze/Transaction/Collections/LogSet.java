package Zeze.Transaction.Collections;

import java.util.Collection;

import Zeze.Collections.Persistents;

/** Persistent set transaction state, without wire responsibilities. */
public abstract class LogSet<V> extends LogCollection {
	private org.pcollections.PSet<V> value;

	protected LogSet(PSet<V> source, org.pcollections.PSet<V> value) {
		super(source);
		this.value = value;
	}

	public final org.pcollections.PSet<V> getValue() {
		return value;
	}

	final void setValue(org.pcollections.PSet<V> value) {
		this.value = value;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void mergeFrom(LogCollection child) {
		value = ((LogSet<V>)child).value;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final void commit() {
		((PSet<V>)source).set = value;
	}

	public boolean add(V item) {
		var next = value.plus(item);
		if (next == value)
			return false;
		value = next;
		return true;
	}

	public boolean addAll(Collection<? extends V> items) {
		// MapPSet.plusAll always wraps: single-item plus preserves the no-change identity check.
		var next = value;
		for (V item : items)
			next = next.plus(item);
		if (next == value)
			return false;
		value = next;
		return true;
	}

	public boolean remove(V item) {
		var next = value.minus(item);
		if (next == value)
			return false;
		value = next;
		return true;
	}

	public boolean removeAll(Collection<? extends V> items) {
		var next = value;
		for (V item : items)
			next = next.minus(item);
		if (next == value)
			return false;
		value = next;
		return true;
	}

	public void clear() {
		value = Persistents.set();
	}
}
