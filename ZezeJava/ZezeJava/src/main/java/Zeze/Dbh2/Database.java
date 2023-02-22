package Zeze.Dbh2;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理表
 */
public class Database {
	private ConcurrentHashMap<String, Table> tables = new ConcurrentHashMap<>();

	public boolean createTable(String tableName) {
		return null == tables.putIfAbsent(tableName, new Table(tableName));
	}
}
