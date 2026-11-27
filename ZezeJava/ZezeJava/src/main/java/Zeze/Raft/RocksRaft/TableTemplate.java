package Zeze.Raft.RocksRaft;

import org.jspecify.annotations.Nullable;

import java.util.function.BiPredicate;

/**
 * 表模板：按名字与 key/value 类型注册，按 templateId 打开（缓存复用）对应的 Table。
 */
public final class TableTemplate<K, V extends Bean> {
	private final String name;
	private final Rocks rocks;
	private final Class<K> keyClass;
	private final Class<V> valueClass;
	private @Nullable BiPredicate<K, Record<K>> lruTryRemoveCallback;

	public String getName() {
		return name;
	}

	public Rocks getRocks() {
		return rocks;
	}

	public void setLruTryRemoveCallback(@Nullable BiPredicate<K, Record<K>> callback) {
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

	public Table<K, V> openTable(@Nullable BiPredicate<K, Record<K>> callback) {
		return openTable(0, callback);
	}

	@SuppressWarnings("unchecked")
	public Table<K, V> openTable(int templateId, @Nullable BiPredicate<K, Record<K>> callback) {
		var t = (Table<K, V>)rocks.getTables().computeIfAbsent(name + "#" + templateId,
				__ -> new Table<>(rocks, name, templateId, keyClass, valueClass, callback));
		// 命中已存在表时仍更新回调，以最后一次调用为准。
		if (callback != null)
			t.setLruTryRemoveCallback(callback);
		return t;
	}
}
