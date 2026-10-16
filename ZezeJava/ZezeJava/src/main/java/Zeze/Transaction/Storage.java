package Zeze.Transaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 表的存储封装：把 TableX 绑定到后台 Database.Table（关系表或 KV 表），承担打开与关闭。 */
public final class Storage<K extends Comparable<K>, V extends Bean> {
	private static final Logger logger = LogManager.getLogger(Storage.class);
	private final Table table;
	private final Database.Table databaseTable;

	public Storage(TableX<K, V> table, Database database, String tableName) {
		this.table = table;
		if (table.isRelationalMapping() && database instanceof DatabaseRelationalMapping mapping) {
			databaseTable = mapping.openRelationalTable(tableName);
			return; // done
		}
		databaseTable = database.openTable(tableName, table.getId());
	}

	public Table getTable() {
		return table;
	}

	public Database.Table getDatabaseTable() {
		return databaseTable;
	}

	public void close() {
		try {
			databaseTable.close();
		} catch (Throwable e) { // logger.error
			logger.error("Database.Table.close exception:", e);
		}
	}
}
