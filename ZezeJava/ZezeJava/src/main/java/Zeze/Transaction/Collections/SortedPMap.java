package Zeze.Transaction.Collections;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.pcollections.Empty;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class SortedPMap<K extends Comparable<K>, V> extends Collection implements NavigableMap<K, V>, Iterable<Map.Entry<K, V>> {
	@NotNull org.pcollections.PSortedMap<K, V> map = Empty.sortedMap();

	public final @NotNull org.pcollections.PSortedMap<K, V> getMap() {
		if (isManaged()) {
			var txn = Transaction.getCurrentVerifyRead(this);
			if (txn == null)
				return map;
			//noinspection DataFlowIssue
			Log log = txn.getLog(parent().objectId() + variableId());
			if (log == null)
				return map;
			@SuppressWarnings("unchecked")
			var mapLog = (LogSortedMap1<K, V>)log;
			return mapLog.getValue();
		}
		return map;
	}

	@Override
	public abstract void encode(@NotNull ByteBuffer bb);

	@Override
	public abstract void decode(@NotNull IByteBuffer bb);

	@Override
	public @NotNull Iterator<Entry<K, V>> iterator() {
		return null;
	}

	@Override
	public Entry<K, V> lowerEntry(K key) {
		return getMap().lowerEntry(key);
	}

	@Override
	public K lowerKey(K key) {
		return getMap().lowerKey(key);
	}

	@Override
	public Entry<K, V> floorEntry(K key) {
		return getMap().floorEntry(key);
	}

	@Override
	public K floorKey(K key) {
		return getMap().floorKey(key);
	}

	@Override
	public Entry<K, V> ceilingEntry(K key) {
		return getMap().ceilingEntry(key);
	}

	@Override
	public K ceilingKey(K key) {
		return getMap().ceilingKey(key);
	}

	@Override
	public Entry<K, V> higherEntry(K key) {
		return getMap().higherEntry(key);
	}

	@Override
	public K higherKey(K key) {
		return getMap().higherKey(key);
	}

	@Override
	public Entry<K, V> firstEntry() {
		return getMap().firstEntry();
	}

	@Override
	public Entry<K, V> lastEntry() {
		return getMap().lastEntry();
	}

	@Override
	public Entry<K, V> pollFirstEntry() {
		var tree = new TreeMap<>();
		return null;
	}

	@Override
	public Entry<K, V> pollLastEntry() {
		return null;
	}

	@Override
	public NavigableMap<K, V> descendingMap() {
		return null;
	}

	@Override
	public NavigableSet<K> navigableKeySet() {
		return null;
	}

	@Override
	public NavigableSet<K> descendingKeySet() {
		return null;
	}

	@Override
	public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
		return null;
	}

	@Override
	public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
		return null;
	}

	@Override
	public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
		return null;
	}

	@Override
	public Comparator<? super K> comparator() {
		return getMap().comparator();
	}

	@Override
	public SortedMap<K, V> subMap(K fromKey, K toKey) {
		return null;
	}

	@Override
	public SortedMap<K, V> headMap(K toKey) {
		return null;
	}

	@Override
	public SortedMap<K, V> tailMap(K fromKey) {
		return null;
	}

	@Override
	public K firstKey() {
		return getMap().firstKey();
	}

	@Override
	public K lastKey() {
		return getMap().lastKey();
	}

	@Override
	public int size() {
		return getMap().size();
	}

	@Override
	public boolean isEmpty() {
		return getMap().isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return getMap().containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return getMap().containsValue(value);
	}

	@Override
	public V get(Object key) {
		return getMap().get(key);
	}

	@Override
	public abstract @Nullable V put(K key, V value);

	@Override
	public abstract V remove(Object key);

	@Override
	public abstract void putAll(@NotNull Map<? extends K, ? extends V> m);

	@Override
	public abstract void clear();

	@Override
	public Set<K> keySet() {
		return Set.of();
	}

	@Override
	public java.util.Collection<V> values() {
		return List.of();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return Set.of();
	}
}
