package Zeze.Transaction;

import Zeze.Util.LongConcurrentHashMap;

public final class TableKey implements Comparable<TableKey> {
	// 用来做名字转换，不检查Table.Id唯一性。
	public static final LongConcurrentHashMap<String> tables = new LongConcurrentHashMap<>();

	private final int id;
	private final Object key;

	public TableKey(int id, Object key) {
		this.id = id;
		this.key = key;
	}

	public int getId() {
		return id;
	}

	public Object getKey() {
		return key;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + id;
		result = prime * result + key.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof TableKey) {
			TableKey another = (TableKey)obj;
			return id == another.id && key.equals(another.key);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(TableKey other) {
		int c = Integer.compare(id, other.id);
		return c != 0 ? c : ((Comparable<Object>)key).compareTo(other.key);
	}

	@Override
	public String toString() {
		return String.format("tkey(%s:%s)", tables.get(id), key);
	}
}
