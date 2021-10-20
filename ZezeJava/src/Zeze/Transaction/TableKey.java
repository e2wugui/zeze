package Zeze.Transaction;

public final class TableKey implements java.lang.Comparable<TableKey> {
	private int TableId;
	public int getTableId() {
		return TableId;
	}
	private Object Key;
	public Object getKey() {
		return Key;
	}

	public TableKey(int tableId, Object key) {
		TableId = tableId;
		Key = key;
	}

	@SuppressWarnings("unchecked")
	public int compareTo(TableKey other) {
		int c = Integer.compare(this.getTableId(), other.getTableId());
		if (c != 0) {
			return c;
		}
		@SuppressWarnings("rawtypes")
		Comparable This = (Comparable)getKey();
		return This.compareTo(other.getKey());
	}

	@Override
	public String toString() {
		return Zeze.Util.Str.format("tkey({}:{},{})",
				getTableId(), Table.GetTable(getTableId()).getName(), getKey());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + getTableId();
		result = prime * result + getKey().hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		boolean tempVar = obj instanceof TableKey;
		TableKey another = tempVar ? (TableKey)obj : null;
		if (tempVar) {
			return getTableId() == another.getTableId() && getKey().equals(another.getKey());
		}
		return false;
	}
}