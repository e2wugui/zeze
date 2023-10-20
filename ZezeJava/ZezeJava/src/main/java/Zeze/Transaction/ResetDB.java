package Zeze.Transaction;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Schemas;
import Zeze.Serialize.ByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResetDB {
	public static final Logger logger = LogManager.getLogger(ResetDB.class);

	public static void checkAndRemoveTable(Schemas other, Application app) throws Exception {
		if (null == other)
			return;

		app.getSchemas().checkCompatible(other, app);
		// todo 先禁用“自动删除不兼容的表。”以后删除相关代码。
		/*
		if (!app.getConfig().autoResetTable()) {
			app.getSchemas().checkCompatible(other, app);
			return;
		}

		String databaseName = app.getConfig().getDefaultTableConf().getDatabaseName();
		Database defaultDb = app.getDatabase(databaseName);

		List<String> removeList = getRemoveList(other, app);
		if (removeList.isEmpty()) {
			return;
		}
		logger.debug("reset db start!");

		removeTable(defaultDb, removeList);
		*/
	}

	private static List<String> getRemoveList(Schemas other, Application app) {
		String dbName = app.getConfig().getDefaultTableConf().getDatabaseName();
		Database defaultDb = app.getDatabase(dbName);

		var context = new Schemas.Context();
		context.setCurrent(app.getSchemas());
		context.setPrevious(other);
		context.setConfig(app.getConfig());

		HashMap<String, Integer> removeModules = new HashMap<>();
		for (var table : app.getSchemas().tables.values()) {
			Schemas.Table otherTable = other.tables.get(table.name);
			if (null == otherTable) {
				continue;
			}
			if (!table.isCompatible(otherTable, context)) {
				var rmTable = defaultDb.getTable(otherTable.name);
				if (rmTable == null) {
					continue;
				}
				Storage<?, ?> storage = rmTable.getStorage();
				if (storage == null) {
					continue;
				}
				// 空表删除也很快，不需要特别忽略？
				/*
				Database.Table databaseTable = storage.getDatabaseTable();
				AtomicBoolean empty = new AtomicBoolean(true);
				databaseTable.walk((key, value) -> {
					empty.set(false);
					return false;
				});
				if (empty.get()) {
					continue;
				}
				*/
				String[] strs = otherTable.name.split("_", 3);
				String moduleName = "_" + strs[1] + "_";
				removeModules.putIfAbsent(moduleName, 1);
			}
		}
		List<String> removeList = new LinkedList<>();
		for (var table : app.getSchemas().tables.values()) {
			String[] strs = table.name.split("_", 3);
			String key = "_" + strs[1] + "_";
			if (removeModules.get(key) != null) {
				removeList.add(table.name);
				logger.debug("add remove table list : {}.", table.name);
			}
		}
		return removeList;
	}

	private static void removeTable(Database db, List<String> removeList) throws Exception {
		for (var rmTable : removeList) {
			Table table = db.getTable(rmTable);
			if (table == null) {
				continue;
			}
			try (Database.Transaction transaction = db.beginTransaction()) {
				Storage<?, ?> storage = table.getStorage();
				if (storage == null) {
					continue;
				}
				if (storage.getDatabaseTable() instanceof Database.AbstractKVTable) {
					var databaseTable = (Database.AbstractKVTable)storage.getDatabaseTable();
					AtomicInteger count = new AtomicInteger();
					databaseTable.walk((key, value) -> {
						databaseTable.remove(transaction, ByteBuffer.Wrap(key));
						count.incrementAndGet();
						return true;
					});
					logger.warn("remove table :{} count:{}", rmTable, count.get());
				} else {
					logger.warn("remove table :{} NOT A KV TABLE.", rmTable);
				}
				transaction.commit();
			}
		}
	}
}
