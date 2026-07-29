package Zeze.Raft.RocksRaft;

/**
 * RocksRaft 下 SortedMap 的 Log 基类。
 * 与 LogMap 的唯一区别是 value 类型为 {@link org.pcollections.PSortedMap}。
 * 由于 pcollections 的 PSortedMap 要求 K 实现 Comparable，
 * 这里也把泛型约束 K extends Comparable&lt;K&gt; 加上。
 */
public abstract class LogSortedMap<K extends Comparable<K>, V> extends LogBean {
	private org.pcollections.PSortedMap<K, V> value;

	public LogSortedMap(int typeId) {
		super(typeId);
	}

	public final org.pcollections.PSortedMap<K, V> getValue() {
		return value;
	}

	public final void setValue(org.pcollections.PSortedMap<K, V> value) {
		this.value = value;
	}

	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}
}
