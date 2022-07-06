package Zeze.Transaction;

import Zeze.Util.LongConcurrentHashMap;

public final class TableKey implements Comparable<TableKey> {
	// 用来做名字转换，不检查Table.Id唯一性。
	public static final LongConcurrentHashMap<String> Tables = new LongConcurrentHashMap<>();

	private final int Id;
	private final Object Key;

	public TableKey(int id, Object key) {
		Id = id;
		Key = key;
	}

	public int getId() {
		return Id;
	}

	public Object getKey() {
		return Key;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + Id;
		result = prime * result + Key.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof TableKey) {
			TableKey another = (TableKey)obj;
			return Id == another.Id && Key.equals(another.Key);
		}
		return false;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public int compareTo(TableKey other) {
		int c = Integer.compare(Id, other.Id);
		return c != 0 ? c : ((Comparable)Key).compareTo(other.Key);
	}

	@Override
	public String toString() {
		return String.format("tkey(%s:%s)", Tables.get(Id), Key);
	}
}
