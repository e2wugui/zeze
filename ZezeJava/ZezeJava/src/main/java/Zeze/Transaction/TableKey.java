package Zeze.Transaction;

import Zeze.Util.LongConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TableKey implements Comparable<TableKey> {
	// 用来做名字转换，不检查Table.Id唯一性。
	public static final LongConcurrentHashMap<String> tables = new LongConcurrentHashMap<>();

	private final int id;
	private final @NotNull Object key;

	public TableKey(int id, @NotNull Object key) {
		this.id = id;
		this.key = key;
	}

	public int getId() {
		return id;
	}

	public @NotNull Object getKey() {
		return key;
	}

	@Override
	public int hashCode() {
		return id * 31 + key.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj)
			return true;

		if (obj instanceof TableKey) {
			TableKey another = (TableKey)obj;
			return id == another.id && key.equals(another.key);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(@NotNull TableKey other) {
		int c = Integer.compare(id, other.id);
		return c != 0 ? c : ((Comparable<Object>)key).compareTo(other.key);
	}

	@Override
	public @NotNull String toString() {
		return String.format("tkey(%s:%s)", tables.get(id), key);
	}
}
