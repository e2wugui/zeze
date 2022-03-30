package Zeze.Transaction;

public final class TableKey implements java.lang.Comparable<TableKey> {
	private final String Name;
	public String getName() {
		return Name;
	}
	private final Object Key;
	public Object getKey() {
		return Key;
	}

	public TableKey(String name, Object key) {
		Name = name;
		Key = key;
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(TableKey other) {
		int c = this.Name.compareTo(other.Name);
		if (c != 0) {
			return c;
		}
		@SuppressWarnings("rawtypes")
		Comparable This = (Comparable)getKey();
		return This.compareTo(other.getKey());
	}

	@Override
	public String toString() {
		return String.format("tkey(%s:%s)", Name, Key);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + Name.hashCode();
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
			return Name.equals(another.Name) && getKey().equals(another.getKey());
		}
		return false;
	}
}
