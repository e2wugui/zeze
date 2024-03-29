package Zeze.Dbh2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Util.OutLong;
import Zeze.Util.OutObject;
import Zeze.Util.PerfCounter;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BenchClient {
	private static final Logger logger = LogManager.getLogger(BenchClient.class);

	private static Database newDatabase(Dbh2AgentManager dbh2AgentManager, String masterIp, int masterPort) {
		var databaseConf = new Config.DatabaseConf();
		databaseConf.setDatabaseType(Config.DbType.Dbh2);
		databaseConf.setDatabaseUrl("dbh2://" + masterIp + ":" + masterPort + "/dbh2TestDb");
		databaseConf.setName("dbh2");
		return new Database(null, dbh2AgentManager, databaseConf);
	}

	public static void main(String[] args) {
		try {
			Task.tryInitThreadPool();

			var tableNumber = 1;
			var threadNumber = 128;
			var valueSize = 100;
			var masterIp = "127.0.0.1";
			var masterPort = 10999;
			var tableAccess = 1;
			var selector = Runtime.getRuntime().availableProcessors();
			var get = false;

			var serviceManager = Application.createServiceManager(Config.load(), "Dbh2ServiceManager");
			assert serviceManager != null;
			serviceManager.start();
			serviceManager.waitReady();

			for (int i = 0; i < args.length; ++i) {
				switch (args[i]) {
				case "-tableNumber":
					tableNumber = Integer.parseInt(args[++i]);
					break;
				case "-threadNumber":
					threadNumber = Integer.parseInt(args[++i]);
					break;
				case "-valueSize":
					valueSize = Integer.parseInt(args[++i]);
					break;
				case "-masterIp":
					masterIp = args[++i];
					break;
				case "-masterPort":
					masterPort = Integer.parseInt(args[++i]);
					break;
				case "-tableAccess":
					tableAccess = Integer.parseInt(args[++i]);
					break;
				case "-selector":
					selector = Integer.parseInt(args[++i]);
					break;
				case "-get":
					get = true;
					break;
				default:
					throw new RuntimeException("unknown option: " + args[i]);
				}
			}

			Zeze.Net.Selectors.getInstance().add(selector - 1);
			PerfCounter.instance.tryStartScheduledLog();

			var dbh2AgentManager = new Dbh2AgentManager(serviceManager, null);
			dbh2AgentManager.start();
			var database = newDatabase(dbh2AgentManager, masterIp, masterPort);
			var tables = new ArrayList<Zeze.Transaction.Database.AbstractKVTable>();
			for (int i = 0; i < tableNumber; ++i) {
				var tableName = "table" + i;
				tables.add((Database.AbstractKVTable)database.openTable(tableName, Bean.hash32(tableName)));
			}
			for (var table : tables)
				table.waitReady();

			var running = new OutObject<>(true);
			var futures = new ArrayList<Future<?>>();
			var transCounter = new AtomicLong();
			var benchClient = new BenchClient();
			for (int i = 0; i < threadNumber; ++i) {
				futures.add(get
						? startGetTask(running, tables, transCounter)
						: benchClient.startPutTask(running, database, tableAccess, tables, valueSize, transCounter));
			}
			var lastReportTime = new OutLong(System.currentTimeMillis());
			var lastReportCount = new OutLong();
			var reportTimer = Task.scheduleUnsafe(2000, 2000, () -> {
				var now = System.currentTimeMillis();
				var elapse = (now - lastReportTime.value) / 1000.0f;
				lastReportTime.value = now;
				var countNow = transCounter.get();
				var diff = countNow - lastReportCount.value;
				lastReportCount.value = countNow;

				System.out.println("transaction/s: " + diff / elapse);
			});

			var inputReader = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				var line = inputReader.readLine();
				if (line == null || line.equals("exit"))
					break;
			}
			running.value = false;
			for (var future : futures)
				future.get();
			dbh2AgentManager.stop();
			reportTimer.cancel(true);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	static Future<?> startGetTask(OutObject<Boolean> running,
								  ArrayList<Zeze.Transaction.Database.AbstractKVTable> tables,
								  AtomicLong transCounter) {

		return Task.runUnsafe(() -> {
			while (Boolean.TRUE.equals(running.value)) {
				// 限制所有key的范围，防止服务器占用太大硬盘。
				try {
					var key = (Zeze.Util.Random.getInstance().nextLong() + 1) % 1000_00000;
					var keyBb = ByteBuffer.Allocate(9);
					keyBb.WriteLong(key);
					var table = tables.get(Zeze.Util.Random.getInstance().nextInt(tables.size()));
					table.find(keyBb);
					transCounter.incrementAndGet();
				} catch (Throwable ex) {
					logger.error("", ex);
				}
			}
		}, "table get thread");
	}

	public static class TableKey {
		public TableKey(Zeze.Transaction.Database.AbstractKVTable table, long key) {
			this.table = table;
			this.key = key;
		}

		final Zeze.Transaction.Database.AbstractKVTable table;
		final long key;

		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (other instanceof TableKey) {
				var otk = (TableKey)other;
				return otk.table == this.table && otk.key == this.key;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return table.hashCode() ^ Long.hashCode(key);
		}
	}

	private final ConcurrentHashMap<TableKey, TableKey> rrs = new ConcurrentHashMap<>();

	private java.util.HashSet<TableKey> rrs(ArrayList<Zeze.Transaction.Database.AbstractKVTable> tables, int tableAccess) {
		var result = new java.util.HashSet<TableKey>();
		while (tableAccess > 0) {
			var key = Zeze.Util.Random.getInstance().nextLong(100_0000_0000L);
			var table = tables.get(Zeze.Util.Random.getInstance().nextInt(tables.size()));
			var tkey = new TableKey(table, key);
			if (rrs.containsKey(tkey))
				continue;
			tableAccess--;
			rrs.put(tkey, tkey);
			result.add(tkey);
		}
		return result;
	}

	Future<?> startPutTask(OutObject<Boolean> running,
						   Database database,
						   int tableAccess,
						   ArrayList<Zeze.Transaction.Database.AbstractKVTable> tables,
						   int valueSize,
						   AtomicLong transCounter) {
		var value = ByteBuffer.Wrap(Zeze.Util.Random.nextBinary(valueSize));
		return Task.runUnsafe(() -> {
			while (Boolean.TRUE.equals(running.value)) {
				// 限制所有key的范围，防止服务器占用太大硬盘。
				try (var trans = database.beginTransaction()) {
					var rrs = rrs(tables, tableAccess);
					for (var r : rrs) {
						var keyBb = ByteBuffer.Allocate(9);
						keyBb.WriteLong(r.key);
						r.table.replace(trans, keyBb, value);
					}
					trans.commit();
					for (var r : rrs)
						rrs.remove(r);
					transCounter.incrementAndGet();
				} catch (Throwable ex) {
					logger.error("", ex);
				}
			}
		}, "table put thread");
	}
}
