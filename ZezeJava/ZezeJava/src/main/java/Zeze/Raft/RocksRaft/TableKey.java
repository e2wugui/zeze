package Zeze.Raft.RocksRaft;

public final class TableKey implements Comparable<TableKey> {
	public String name;
	public Object key;

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
		result = prime * result + name.hashCode();
		result = prime * result + key.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj instanceof TableKey) {
			var another = (TableKey)obj;
			return name.equals(another.name) && key.equals(another.key);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(TableKey other) {
		int c = name.compareTo(other.name);
		if (c != 0)
			return c;
		return ((Comparable<Object>)key).compareTo(other.key);
	}

	@Override
	public String toString() {
		return String.format("(%s,%s)", name, key);
	}
}
