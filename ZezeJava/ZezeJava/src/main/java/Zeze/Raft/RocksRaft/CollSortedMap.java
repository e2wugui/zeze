package Zeze.Raft.RocksRaft;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;

/**
 * 与 {@link CollMap} 完全对应，但底层使用 {@link org.pcollections.PSortedMap}，
 * 因此 key 必须实现 {@link Comparable}。所有读写 API 行为与 CollMap 一致，
 * 区别仅在于迭代顺序按键升序。
 */
public abstract class CollSortedMap<K extends Comparable<K>, V> extends Collection
		implements Iterable<Map.Entry<K, V>> {
	public org.pcollections.PSortedMap<K, V> map = org.pcollections.Empty.sortedMap();

	public final V get(K key) {
		return getMap().get(key);
	}

	public abstract void add(K key, V value);

	public abstract void put(K key, V value);

	public abstract void remove(K key);

	public abstract void clear();

	public Set<Map.Entry<K, V>> entrySet() {
		return getMap().entrySet();
	}

	protected final org.pcollections.PSortedMap<K, V> getMap() {
		if (isManaged()) {
			var t = Transaction.getCurrent();
			if (t == null)
				return map;
			var log = t.getLog(parent().objectId() + variableId());
			if (log == null)
				return map;
			@SuppressWarnings("unchecked")
			var mapLog = (LogSortedMap1<K, V>)log;
			return mapLog.getValue();
		}
		return map;
	}

	public final boolean containsValue(Object v) {
		//noinspection SuspiciousMethodCalls
		return getMap().containsValue(v);
	}

	public final boolean containsKey(Object key) {
		//noinspection SuspiciousMethodCalls
		return getMap().containsKey(key);
	}

	public java.util.Collection<V> values() {
		return getMap().values();
	}

	public Set<K> keys() {
		return getMap().keySet();
	}

	public final int size() {
		return getMap().size();
	}

	@Override
	public Iterator<Map.Entry<K, V>> iterator() {
		return getMap().entrySet().iterator();
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, getMap());
		return sb.toString();
	}
}
