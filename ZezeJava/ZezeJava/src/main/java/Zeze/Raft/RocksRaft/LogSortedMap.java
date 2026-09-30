package Zeze.Raft.RocksRaft;

import java.util.Comparator;
import java.util.Objects;

/**
 * RocksRaft 下 SortedMap 的 Log 基类。
 * 与 LogMap 的唯一区别是 value 类型为 {@link org.pcollections.PSortedMap}。
 * 由于 pcollections 的 PSortedMap 要求 K 实现 Comparable，
 * 这里也把泛型约束 K extends Comparable&lt;K&gt; 加上。
 */
public abstract class LogSortedMap<K extends Comparable<K>, V> extends LogBean {
	private org.pcollections.PSortedMap<K, V> value;
	private Comparator<? super K> comparator;

	public LogSortedMap(int typeId) {
		this(typeId, null);
	}

	protected LogSortedMap(int typeId, Comparator<? super K> comparator) {
		super(typeId);
		this.comparator = comparator;
	}

	public final org.pcollections.PSortedMap<K, V> getValue() {
		return value;
	}

	public final void setValue(org.pcollections.PSortedMap<K, V> value) {
		var newComparator = value != null ? value.comparator() : null;
		this.value = value;
		if (!Objects.equals(comparator, newComparator)) {
			comparator = newComparator;
			onComparatorChanged(newComparator);
		}
	}

	/** 增量的键等价关系须跟随源有序Map；未显式配置的decode日志使用自然顺序。 */
	protected void onComparatorChanged(Comparator<? super K> comparator) {
	}

	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}
}
