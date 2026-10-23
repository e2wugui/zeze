package Zeze.Transaction.Collections;

import java.util.ArrayList;

import java.util.Collection;

import Zeze.Collections.Persistents;

/** PList1 的变更日志：按序记录 OpLog 操作序列（增删改清），供编码及跟随者重放。 */
final class LogListNoDelta<V> extends LogList<V> {

	LogListNoDelta(PList<V> source, org.pcollections.PVector<V> value) {
		super(source, value);
	}

	public boolean add(V item) {
		//noinspection ConstantValue
		if (item == null)
			throw new IllegalArgumentException("null item");
		var list = getValue();
		setValue(list.plus(item));

		return true;
	}

	public boolean addAll(Collection<? extends V> items) {
		var list = getValue().plusAll(items);
		if (list == getValue())
			return false;
		setValue(list);
		return true;
	}

	public boolean removeAll(Collection<? extends V> c) {
		// JDK契约：删除c中每个元素在列表里的全部出现（逐元素remove(v)只删首个出现，
		// [x,x].removeAll(Set.of(x))会留下[x]，违反契约）。从高索引往低删：
		// 生成的OP_REMOVE序列在顺序重放时低位索引不受高位删除影响，opLog词表不变。
		// 命中检测走线性equals：生成bean覆写equals但不覆写hashCode（值等哈希不等），
		// HashSet对bean元素静默漏命中。
		var hit = new ArrayList<>(c);
		var list = getValue();
		var result = false;
		for (var i = list.size() - 1; i >= 0; i--) {
			if (hit.contains(list.get(i))) {
				remove(i);
				result = true; // 只要有一个删除成功，就认为成功：removeAll的定义是发生了改变就返回true。
			}
		}
		return result;
	}

	public boolean remove(V item) {
		int index = getValue().indexOf(item);
		if (index < 0)
			return false;
		remove(index);
		return true;
	}

	public void clear() {
		setValue(Persistents.vector());

	}

	public void add(int index, V item) {
		setValue(getValue().plus(index, item));

	}

	public V set(int index, V item) {
		var list = getValue();
		V old = list.get(index);
		setValue(list.with(index, item));

		return old;
	}

	public V remove(int index) {
		var list = getValue();
		V old = list.get(index);
		setValue(list.minus(index));

		return old;
	}

	@Override
	public boolean hasDelta() {
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public LogListNoDelta<V> beginSavepoint() {
		return new LogListNoDelta<>((PList<V>)source, getValue());
	}
}
