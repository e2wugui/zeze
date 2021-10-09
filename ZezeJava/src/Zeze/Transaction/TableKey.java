package Zeze.Transaction;

import Zeze.*;

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

	public int compareTo(TableKey other) {
		int c = (new Integer(this.getTableId())).compareTo(other.getTableId());
		if (c != 0) {
			return c;
		}
		return Comparer<java.lang.Comparable>.Default.Compare((java.lang.Comparable)getKey(), (java.lang.Comparable)other.getKey());
	}

	@Override
	public String toString() {
		return String.format("tkey{%1$s:%2$s,%3$s", getTableId(), Table.GetTable(getTableId()).getName(), getKey()}});
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