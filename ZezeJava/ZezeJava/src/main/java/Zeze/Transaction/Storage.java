package Zeze.Transaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/** 表的存储封装：把 TableX 绑定到后台 Database.Table（关系表或 KV 表），承担打开与关闭。 */
public final class Storage<K extends Comparable<K>, V extends Bean> {
	private static final @NotNull Logger logger = LogManager.getLogger(Storage.class);
	private final @NotNull Table table;
	private final @NotNull Database.Table databaseTable;

	public Storage(@NotNull TableX<K, V> table, @NotNull Database database, @NotNull String tableName) {
		this.table = table;
		if (table.isRelationalMapping() && database instanceof DatabaseRelationalMapping mapping) {
			databaseTable = mapping.openRelationalTable(tableName);
			return; // done
		}
		databaseTable = database.openTable(tableName, table.getId());
	}

	public @NotNull Table getTable() {
		return table;
	}

	public @NotNull Database.Table getDatabaseTable() {
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
