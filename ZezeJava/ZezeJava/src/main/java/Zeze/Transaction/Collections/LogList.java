package Zeze.Transaction.Collections;

import java.util.ArrayList;
import java.util.Collection;

import Zeze.Collections.Persistents;

/** 持久化 List 的事务状态，不承担编解码职责。 */
public class LogList<V> extends LogCollection<PList<V>> {
	private org.pcollections.PVector<V> value;

	protected LogList(PList<V> source, org.pcollections.PVector<V> value) {
		super(source);
		this.value = value;
	}

	@Override
	public boolean hasDelta() {
		return false;
	}

	@Override
	public LogList<V> beginSavepoint() {
		return new LogList<>(source, getValue());
	}

	public final org.pcollections.PVector<V> getValue() {
		return value;
	}

	final void setValue(org.pcollections.PVector<V> value) {
		this.value = value;
	}

	@Override
	protected void mergeFrom(LogCollection<?> child) {
		//noinspection unchecked
		value = ((LogList<V>)child).value;
	}

	@Override
	public final void commit() {
		source.list = value;
	}

	public boolean add(V item) {
		//noinspection ConstantValue
		if (item == null)
			throw new IllegalArgumentException("null item");
		value = value.plus(item);
		return true;
	}

	public boolean addAll(Collection<? extends V> items) {
		var next = value.plusAll(items);
		if (next == value)
			return false;
		value = next;
		return true;
	}

	public boolean remove(V item) {
		int index = value.indexOf(item);
		if (index < 0)
			return false;
		remove(index);
		return true;
	}

	public boolean removeAll(Collection<? extends V> items) {
		// 快照输入并从高索引往低删以保持回放下标稳定；线性 equals 兼容仅覆写 equals 的生成 Bean。
		var hit = new ArrayList<>(items);
		var old = value;
		boolean changed = false;
		for (int i = old.size() - 1; i >= 0; i--) {
			if (hit.contains(old.get(i))) {
				remove(i);
				changed = true;
			}
		}
		return changed;
	}

	public void clear() {
		value = Persistents.vector();
	}

	public void add(int index, V item) {
		value = value.plus(index, item);
	}

	public V set(int index, V item) {
		V old = value.get(index);
		value = value.with(index, item);
		return old;
	}

	public V remove(int index) {
		V old = value.get(index);
		value = value.minus(index);
		return old;
	}
}
