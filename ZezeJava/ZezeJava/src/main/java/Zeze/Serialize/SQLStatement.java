package Zeze.Serialize;

import java.util.ArrayList;
import Zeze.Net.Binary;

public final class SQLStatement {
	public final static class Record {
		public final SQLStatement key = new SQLStatement();
		public final SQLStatement value = new SQLStatement();
	}

	// 数据对象，全public。
	public final StringBuilder sql = new StringBuilder();
	public final ArrayList<Object> params = new ArrayList<>();

	public void appendBoolean(String columnName, boolean value) {
		if (!sql.isEmpty())
			sql.append(", ");
		sql.append(columnName).append(value ? "=true" : "=false");
	}

	public void appendByte(String columnName, byte value) {
		if (!sql.isEmpty())
			sql.append(", ");
		sql.append(columnName).append("=").append(value);
	}

	public void appendShort(String columnName, short value) {
		if (!sql.isEmpty())
			sql.append(", ");
		sql.append(columnName).append("=").append(value);
	}

	public void appendInt(String columnName, int value) {
		if (!sql.isEmpty())
			sql.append(", ");
		sql.append(columnName).append("=").append(value);
	}

	public void appendLong(String columnName, long value) {
		if (!sql.isEmpty())
			sql.append(", ");
		sql.append(columnName).append("=").append(value);
	}

	public void appendFloat(String columnName, float value) {
		if (!sql.isEmpty())
			sql.append(", ");
		sql.append(columnName).append("=").append(value);
	}

	public void appendDouble(String columnName, double value) {
		if (!sql.isEmpty())
			sql.append(", ");
		sql.append(columnName).append("=").append(value);
	}

	public void appendString(String columnName, String value) {
		if (!sql.isEmpty())
			sql.append(", ");
		sql.append(columnName).append("=?");
		params.add(value);
	}

	public void appendBinary(String columnName, Binary value) {
		if (!sql.isEmpty())
			sql.append(", ");
		sql.append(columnName).append("=?");
		params.add(value);
	}
}
