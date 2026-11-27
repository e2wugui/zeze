package Zeze.Serialize;

import java.util.ArrayList;
import Zeze.Net.Binary;

/** SQL 更新语句装配器：按列拼接 "col=value" 子句并收集参数，供 Bean 的 encodeSQLStatement 输出。 */
public final class SQLStatement {
	private final StringBuilder sql = new StringBuilder();
	private final ArrayList<Object> params = new ArrayList<>();

	public StringBuilder getSql() {
		return sql;
	}

	public ArrayList<Object> getParams() {
		return params;
	}

	public void clear() {
		sql.setLength(0);
		params.clear();
	}

	public void appendBoolean(String columnName, boolean value) {
		if (!sql.isEmpty())
			sql.append(", ");
		sql.append(columnName).append(value ? "=true" : "=false");
	}

	public void appendByte(String columnName, byte value) {
		if (!sql.isEmpty())
			sql.append(", ");
		sql.append(columnName).append('=').append(value);
	}

	public void appendShort(String columnName, short value) {
		if (!sql.isEmpty())
			sql.append(", ");
		sql.append(columnName).append('=').append(value);
	}

	public void appendInt(String columnName, int value) {
		if (!sql.isEmpty())
			sql.append(", ");
		sql.append(columnName).append('=').append(value);
	}

	public void appendLong(String columnName, long value) {
		if (!sql.isEmpty())
			sql.append(", ");
		sql.append(columnName).append('=').append(value);
	}

	public void appendFloat(String columnName, float value) {
		if (!Float.isFinite(value))
			throw notFinite(columnName, value);
		if (!sql.isEmpty())
			sql.append(", ");
		sql.append(columnName).append('=').append(value);
	}

	public void appendDouble(String columnName, double value) {
		if (!Double.isFinite(value))
			throw notFinite(columnName, value);
		if (!sql.isEmpty())
			sql.append(", ");
		sql.append(columnName).append('=').append(value);
	}

	// 关系库 FLOAT/DOUBLE 无法可靠往返 NaN/Infinity，生成语句前提前拒绝。
	private static IllegalStateException notFinite(String columnName, double value) {
		return new IllegalStateException("SQLStatement: column '" + columnName
				+ "' value " + value + " is NaN/Infinity, not representable as SQL literal");
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
