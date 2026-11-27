package Zeze.Raft.RocksRaft;

import org.jspecify.annotations.Nullable;


/**
 * SortedMap 容器日志基类：在 LogBean 之上携带容器的当前值（{@link org.pcollections.PSortedMap}）。
 */
public abstract class LogSortedMap<K extends Comparable<K>, V> extends LogBean {
	private org.pcollections.@Nullable PSortedMap<K, V> value;
	public LogSortedMap(int typeId) {
		super(typeId);
	}

	public final org.pcollections.@Nullable PSortedMap<K, V> getValue() {
		return value;
	}

	final void setValue(org.pcollections.@Nullable PSortedMap<K, V> value) {
		this.value = value;
	}

	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}
}
