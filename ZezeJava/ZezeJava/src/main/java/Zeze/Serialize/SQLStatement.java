package Zeze.Serialize;

import java.util.ArrayList;
import Zeze.Net.Binary;
import org.jetbrains.annotations.NotNull;

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

	public void appendBoolean(@NotNull String columnName, boolean value) {
		if (sql.length() > 0)
			sql.append(", ");
		sql.append(columnName).append(value ? "=true" : "=false");
	}

	public void appendByte(@NotNull String columnName, byte value) {
		if (sql.length() > 0)
			sql.append(", ");
		sql.append(columnName).append('=').append(value);
	}

	public void appendShort(@NotNull String columnName, short value) {
		if (sql.length() > 0)
			sql.append(", ");
		sql.append(columnName).append('=').append(value);
	}

	public void appendInt(@NotNull String columnName, int value) {
		if (sql.length() > 0)
			sql.append(", ");
		sql.append(columnName).append('=').append(value);
	}

	public void appendLong(@NotNull String columnName, long value) {
		if (sql.length() > 0)
			sql.append(", ");
		sql.append(columnName).append('=').append(value);
	}

	public void appendFloat(@NotNull String columnName, float value) {
		if (sql.length() > 0)
			sql.append(", ");
		sql.append(columnName).append('=').append(value);
	}

	public void appendDouble(@NotNull String columnName, double value) {
		if (sql.length() > 0)
			sql.append(", ");
		sql.append(columnName).append('=').append(value);
	}

	public void appendString(@NotNull String columnName, @NotNull String value) {
		if (sql.length() > 0)
			sql.append(", ");
		sql.append(columnName).append("=?");
		params.add(value);
	}

	public void appendBinary(@NotNull String columnName, @NotNull Binary value) {
		if (sql.length() > 0)
			sql.append(", ");
		sql.append(columnName).append("=?");
		params.add(value);
	}
}
