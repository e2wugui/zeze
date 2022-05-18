package Zeze.Transaction;

import Zeze.Util.LongConcurrentHashMap;

public final class TableKey implements java.lang.Comparable<TableKey> {
	// 用来做名字转换，不检查Table.Id唯一性。
	public static final LongConcurrentHashMap<String> Tables = new LongConcurrentHashMap<>();

	private final int Id;
	public int getId() {
		return Id;
	}
	private final Object Key;
	public Object getKey() {
		return Key;
	}

	public TableKey(int id, Object key) {
		Id = id;
		Key = key;
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(TableKey other) {
		int c = Integer.compare(this.Id, other.Id);
		if (c != 0) {
			return c;
		}
		@SuppressWarnings("rawtypes")
		Comparable This = (Comparable)getKey();
		return This.compareTo(other.getKey());
	}

	@Override
	public String toString() {
		return String.format("tkey(%s:%s)", Tables.get(Id), Key);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + Integer.hashCode(Id);
		result = prime * result + getKey().hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof TableKey) {
			TableKey another = (TableKey) obj;
			return Id == another.Id && getKey().equals(another.getKey());
		}
		return false;
	}
}
