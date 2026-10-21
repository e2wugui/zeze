package Zeze.Raft.RocksRaft;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * 表键：表名 + 业务 key 的复合键，用作事务访问记录集的索引。
 */
public final class TableKey implements Comparable<TableKey> {
	public @Nullable String name;
	public @Nullable Object key;

	public TableKey() {
	}

	public TableKey(String name, Object key) {
		this.name = name;
		this.key = key;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		//noinspection DataFlowIssue
		result = prime * result + name.hashCode();
		//noinspection DataFlowIssue
		result = prime * result + key.hashCode();
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj)
			return true;

		if (obj instanceof TableKey another) {
			return Objects.requireNonNull(name).equals(another.name) && Objects.requireNonNull(key).equals(another.key);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(TableKey other) {
		int c = Objects.requireNonNull(name).compareTo(Objects.requireNonNull(other.name));
		if (c != 0)
			return c;
		return ((Comparable<Object>)Objects.requireNonNull(key)).compareTo(Objects.requireNonNull(other.key));
	}

	@Override
	public String toString() {
		return String.format("(%s,%s)", name, key);
	}
}
