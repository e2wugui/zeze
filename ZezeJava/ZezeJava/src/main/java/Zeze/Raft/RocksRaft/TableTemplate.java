package Zeze.Raft.RocksRaft;

import java.util.function.BiPredicate;

public final class TableTemplate<K, V extends Bean> {
	private final String Name;
	private final Rocks Rocks;
	private final Class<K> keyClass;
	private final Class<V> valueClass;
	private BiPredicate<K, Record<K>> lruRemoveCallback;

	public String getName() {
		return Name;
	}

	public Rocks getRocks() {
		return Rocks;
	}

	public void setLruRemoveCallback(BiPredicate<K, Record<K>> callback) {
		lruRemoveCallback = callback;
	}

	public TableTemplate(Rocks r, String name, Class<K> keyClass, Class<V> valueClass) {
		Rocks = r;
		Name = name;
		this.keyClass = keyClass;
		this.valueClass = valueClass;
	}

	public Table<K, V> OpenTable() {
		return OpenTable(0);
	}

	@SuppressWarnings("unchecked")
	public Table<K, V> OpenTable(int templateId) {
		return (Table<K, V>)Rocks.getTables().computeIfAbsent(Name + "#" + templateId,
				__ -> new Table<>(Rocks, Name, templateId, keyClass, valueClass, lruRemoveCallback));
	}

	public Table<K, V> OpenTable(BiPredicate<K, Record<K>> callback) {
		return OpenTable(0, callback);
	}

	@SuppressWarnings("unchecked")
	public Table<K, V> OpenTable(int templateId, BiPredicate<K, Record<K>> callback) {
		return (Table<K, V>)Rocks.getTables().computeIfAbsent(Name + "#" + templateId,
				__ -> new Table<>(Rocks, Name, templateId, keyClass, valueClass, callback));
	}
}
