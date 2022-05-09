package Zeze.Transaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Config;
import Zeze.Schemas;
import Zeze.Serialize.ByteBuffer;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

public class ResetDB {

	public void CheckAndRemoveTable(Schemas other, Application app) throws RocksDBException {
		if (!app.getConfig().autoResetTable())
			return;

		if (null == other)
			return;

		String databaseName = app.getConfig().getDefaultTableConf().getDatabaseName();
		Database defaultDb = app.GetDatabase(databaseName);
		for (var db : app.getDatabases().values()) {
			db.Open(app);
		}

		List<String> removeList = new LinkedList<>();
		app.getSchemas().Compile();
		var keyOfSchemas = ByteBuffer.Allocate(24);
		keyOfSchemas.WriteString("zeze.Schemas." + app.getConfig().getServerId());
		var dataVersion = defaultDb.getDirectOperates().GetDataWithVersion(keyOfSchemas);
		long version = 0;
		if (dataVersion != null && dataVersion.Data != null) {
			var SchemasPrevious = new Schemas();
			try {
				SchemasPrevious.Decode(dataVersion.Data);
				SchemasPrevious.Compile();
			} catch (Throwable ex) {
				SchemasPrevious = null;
			}

			CheckCompatible(SchemasPrevious, app, removeList);
			version = dataVersion.Version;
		}

		switch (defaultDb.GetConf().getDatabaseType()) {
			case MySql:
				ResetMySql(app.getConfig(), databaseName, removeList);
				ByteBuffer newData = ByteBuffer.Allocate(1024);
				app.getSchemas().Encode(newData);
				break;
			case RocksDb:
				defaultDb.Close();
				ResetRocksDB(app, defaultDb.GetConf(), removeList);
				break;
			default:
				return;
		}
		var newData = ByteBuffer.Allocate(1024);
		app.getSchemas().Encode(newData);
		defaultDb.getDirectOperates().SaveDataWithSameVersion(keyOfSchemas, newData, version);
	}

	public void CheckCompatible(Schemas other, Application app, List<String> removeList) {
		if (null == other) {
			return;
		}
		String dbName = app.getConfig().getDefaultTableConf().getDatabaseName();
		Database defaultDb = app.GetDatabase(dbName);

		var context = new Schemas.Context();
		{
			context.setCurrent(app.getSchemas());
			context.setPrevious(other);
			context.setConfig(app.getConfig());
		}

		HashMap<String, Integer> removeModules = new HashMap<>();
		for (var table : app.getSchemas().Tables.values()) {
			Schemas.Table otherTable = other.Tables.get(table.Name);
			if (null != otherTable) {
				if (!table.IsCompatible(otherTable, context)) {
					var rmTable = defaultDb.GetTable(otherTable.Name);
					if (rmTable != null) {
						String[] strs = otherTable.Name.split("_", 3);
						String moduleName = String.format("_" + strs[1] + "_");
						removeModules.putIfAbsent(moduleName, 1);
					}
				}
			}
		}

		for (var table : app.getSchemas().Tables.values()) {
			String[] strs = table.Name.split("_", 3);
			String key = String.format("_" + strs[1] + "_");
			if (removeModules.get(key) != null) {
				removeList.add(table.Name);
			}
		}

		try {
			context.Update();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public void ResetRocksDB(Application app, Config.DatabaseConf dbConf, List<String> removeList) throws RocksDBException {
		RocksDB.loadLibrary();

		final ColumnFamilyOptions CfOptions = new ColumnFamilyOptions();
		final ReadOptions ReadOptions = new ReadOptions();
		final DBOptions dbOptions = new DBOptions().setCreateIfMissing(true);
		var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
		// 用于存放key对应的表操作使用的类
		ConcurrentHashMap<String, ColumnFamilyHandle> cfhMap = new ConcurrentHashMap<>();

		String path = dbConf.getDatabaseUrl().isEmpty() ? "db" : dbConf.getDatabaseUrl();
		org.rocksdb.Options options = new Options();
		for (var cf : RocksDB.listColumnFamilies(options, path)) {
			columnFamilies.add(new ColumnFamilyDescriptor(cf, CfOptions));
		}
		if (columnFamilies.isEmpty()) {
			columnFamilies.add(new ColumnFamilyDescriptor("default".getBytes(), CfOptions));
		}
		var outHandles = new ArrayList<ColumnFamilyHandle>();

		RocksDB db = RocksDB.open(dbOptions, path, columnFamilies, outHandles);

		if (columnFamilies.size() > 0) {
			for (int i = 0; i < columnFamilies.size(); i++) {
				ColumnFamilyHandle cfh = outHandles.get(i);
				String tableName = new String(columnFamilies.get(i).getName());
				if (!cfhMap.containsKey(tableName)) {
					cfhMap.put(tableName, cfh);
				}
			}
		}

		// 删除表
		for (var rmTable : removeList) {
			ColumnFamilyHandle rmCfh = cfhMap.get(rmTable);
			if (rmCfh == null)
				continue;

			RocksIterator iter = db.newIterator(rmCfh, ReadOptions);

			for (iter.seekToFirst(); iter.isValid(); iter.next()) {
				db.delete(rmCfh, iter.key());
			}
			db.dropColumnFamily(rmCfh);
		}

		db.close();
	}

	public void ResetMySql(Config config, String databaseName, List<String> removeList) {
		for (var conf : config.getDatabaseConfMap().values()) {
			if (conf.getName().equals(databaseName)) {
				Connection conn = null;
				Statement stmt = null;
				try {
					Class.forName(conf.getDbcpConf().DriverClassName);
					conn = DriverManager.getConnection(conf.getDatabaseUrl());
					stmt = conn.createStatement();
					for (var rmTable : removeList) {
						String sql = String.format("DROP TABLE %s", rmTable);
						stmt.execute(sql);
					}
					stmt.close();
					conn.close();
				} catch(SQLException se){
					// 处理 JDBC 错误
					se.printStackTrace();
				}catch(Exception e){
					// 处理 Class.forName 错误
					e.printStackTrace();
				}finally{
					// 关闭资源
					try{
						if(stmt != null) {
							stmt.close();
						}
					}catch(SQLException ignored){
					}// 什么都不做
					try{
						if(conn != null) {
							conn.close();
						}
					}catch(SQLException se){
						se.printStackTrace();
					}
				}
			}
		}
	}
}
