package Zeze.Raft.RocksRaft;

import java.util.function.BiPredicate;

public final class TableTemplate<K, V extends Bean> {
	private final String name;
	private final Rocks rocks;
	private final Class<K> keyClass;
	private final Class<V> valueClass;
	private BiPredicate<K, Record<K>> lruTryRemoveCallback;

	public String getName() {
		return name;
	}

	public Rocks getRocks() {
		return rocks;
	}

	public void setLruTryRemoveCallback(BiPredicate<K, Record<K>> callback) {
		lruTryRemoveCallback = callback;
	}

	public TableTemplate(Rocks r, String name, Class<K> keyClass, Class<V> valueClass) {
		rocks = r;
		this.name = name;
		this.keyClass = keyClass;
		this.valueClass = valueClass;
	}

	public Table<K, V> openTable() {
		return openTable(0);
	}

	@SuppressWarnings("unchecked")
	public Table<K, V> openTable(int templateId) {
		return (Table<K, V>)rocks.getTables().computeIfAbsent(name + "#" + templateId,
				__ -> new Table<>(rocks, name, templateId, keyClass, valueClass, lruTryRemoveCallback));
	}

	public Table<K, V> openTable(BiPredicate<K, Record<K>> callback) {
		return openTable(0, callback);
	}

	@SuppressWarnings("unchecked")
	public Table<K, V> openTable(int templateId, BiPredicate<K, Record<K>> callback) {
		return (Table<K, V>)rocks.getTables().computeIfAbsent(name + "#" + templateId,
				__ -> new Table<>(rocks, name, templateId, keyClass, valueClass, callback));
	}
}
