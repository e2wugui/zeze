package Zeze.Transaction;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Schemas;
import Zeze.Serialize.ByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResetDB {
	public static final Logger logger = LogManager.getLogger(ResetDB.class);

	public static void checkAndRemoveTable(Schemas other, Application app) throws Throwable {
		if (null == other)
			return;

		if (!app.getConfig().autoResetTable()){
			app.getSchemas().CheckCompatible(other,app);
			return;
		}

		String databaseName = app.getConfig().getDefaultTableConf().getDatabaseName();
		Database defaultDb = app.GetDatabase(databaseName);

		List<String> removeList = getRemoveList(other, app);
		if (removeList.isEmpty()) {
			return;
		}
		logger.debug("reset db start!");

		removeTable(defaultDb, removeList);
	}

	private static List<String> getRemoveList(Schemas other, Application app) {
		String dbName = app.getConfig().getDefaultTableConf().getDatabaseName();
		Database defaultDb = app.GetDatabase(dbName);

		var context = new Schemas.Context();
		context.setCurrent(app.getSchemas());
		context.setPrevious(other);
		context.setConfig(app.getConfig());

		HashMap<String, Integer> removeModules = new HashMap<>();
		for (var table : app.getSchemas().Tables.values()) {
			Schemas.Table otherTable = other.Tables.get(table.Name);
			if (null == otherTable) {
				continue;
			}
			if (!table.IsCompatible(otherTable, context)) {
				var rmTable = defaultDb.getTable(otherTable.Name);
				if (rmTable == null) {
					continue;
				}
				Database.Table databaseTable = rmTable.getStorage().getDatabaseTable();
				AtomicBoolean empty = new AtomicBoolean(true);
				databaseTable.walk((key, value) -> {
					empty.set(false);
					return false;
				});
				if (empty.get()) {
					continue;
				}
				String[] strs = otherTable.Name.split("_", 3);
				String moduleName = "_" + strs[1] + "_";
				removeModules.putIfAbsent(moduleName, 1);
			}
		}
		List<String> removeList = new LinkedList<>();
		for (var table : app.getSchemas().Tables.values()) {
			String[] strs = table.Name.split("_", 3);
			String key = "_" + strs[1] + "_";
			if (removeModules.get(key) != null) {
				removeList.add(table.Name);
				logger.debug("add remove table list : {}.", table.Name);
			}
		}
		return removeList;
	}

	private static void removeTable(Database db, List<String> removeList) {
		for (var rmTable : removeList) {
			Table table = db.getTable(rmTable);
			if (table == null) {
				continue;
			}
			Database.Transaction transaction = db.beginTransaction();
			Database.Table databaseTable = table.getStorage().getDatabaseTable();
			AtomicInteger count = new AtomicInteger();
			databaseTable.walk((key, value) -> {
				databaseTable.remove(transaction, ByteBuffer.Wrap(key));
				count.incrementAndGet();
				return true;
			});
			logger.warn("remove table :{} count:{}", rmTable, count.get());
			transaction.commit();
		}
	}
}
