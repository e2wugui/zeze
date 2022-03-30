package Zeze.Raft.RocksRaft;

public final class TableKey implements Comparable<TableKey> {
	public String Name;
	public Object Key;

	public TableKey() {
	}

	public TableKey(String name, Object key) {
		Name = name;
		Key = key;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + Name.hashCode();
		result = prime * result + Key.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj instanceof TableKey) {
			var another = (TableKey)obj;
			return Name.equals(another.Name) && Key.equals(another.Key);
		}
		return false;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public int compareTo(TableKey other) {
		int c = Name.compareTo(other.Name);
		if (c != 0)
			return c;
		return ((Comparable)Key).compareTo(other.Key);
	}

	@Override
	public String toString() {
		return String.format("(%s,%s)", Name, Key);
	}
}
