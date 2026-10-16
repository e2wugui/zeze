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

	// NaN/Infinity没有跨库可往返的SQL字面量：MySQL/SqlServer的FLOAT/DOUBLE列本身存不了这两种值；
	// 写NULL读回(JDBC ResultSet.getXxx对NULL返回0)会把NaN静默篡改成0.0，且key列=NULL破坏行身份。
	// 因此在生成语句处即抛明确异常，fail-fast并指向具体列，而不是产出非法SQL在数据库端反复失败。
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
