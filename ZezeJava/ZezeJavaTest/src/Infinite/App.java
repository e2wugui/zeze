package Infinite;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SQLStatement;
import Zeze.Transaction.Database;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TableX;
import Zeze.Util.Random;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {
	private static final Logger logger = LogManager.getLogger(App.class);
	final demo.App app = new demo.App();
	final ArrayList<Future<?>> RunningTasks = new ArrayList<>(Simulate.BatchTaskCount);
	private final Config config;
	final CoverHistory coverHistory;

	public App(int serverId) {
		config = Config.load("zeze.xml");
		config.setServerId(serverId);
		config.setFastRedoWhenConflict(false);
		config.setCheckpointPeriod(1000);
		config.getServiceConfMap().remove("Zeze.Onz.Server");
		config.setHistory("ZezeTest");

		var tdef = config.getDefaultTableConf();
		// 提高并发
		tdef.setCacheCleanPeriod(1000);
		// 超出容量时，快速尝试。
		tdef.setCacheCleanPeriodWhenExceedCapacity(100);
		// 减少容量，实际使用记录数要超过一些。让TableCache.Cleanup能并发起来。
		tdef.setCacheCapacity(Simulate.CacheCapacity);
		tdef.setCacheFactor(1.0f);

		var tflush = config.getTableConfMap().get("demo_Module1_tflush");
		// 提高并发
		tflush.setCacheCleanPeriod(1000);
		// 超出容量时，快速尝试。
		tflush.setCacheCleanPeriodWhenExceedCapacity(100);
		// 减少容量，实际使用记录数要超过一些。让TableCache.Cleanup能并发起来。
		tflush.setCacheCapacity(Tasks.tflushInt1Trade.CacheCapacity);
		tflush.setCacheFactor(1.0f);

		config.getServiceConfMap().remove("TestServer");
		coverHistory = new CoverHistory(this.app);

		for (var dbCfg : config.getDatabaseConfMap().values()) {
			var dc = dbCfg.getDruidConf();
			if (dc != null && (dc.maxActive == null || dc.maxActive > 10))
				dc.maxActive = 10;
		}
	}

	public int getServerId() {
		return config.getServerId();
	}

	public void Start() throws Exception {
		app.Start(config);
	}

	public void Stop() throws Exception {
		for (var task : RunningTasks) {
			task.cancel(false);
		}
		app.Stop();
	}

	void Run(Tasks.Task task) throws ExecutionException, InterruptedException {
		task.App = app;
		String name = task.getClass().getName();
		int keyBound = task.getKeyBound();
		for (int i = 0, n = task.getKeyNumber(); i < n; i++) {
			long key;
			do
				key = Random.getInstance().nextInt(keyBound);
			while (!task.Keys.add(key));
			Tasks.getKeyCounter(name, key).increment();
		}
		Tasks.getRunCounter(name).increment();
		RunningTasks.add(task.IsProcedure()
				? Task.runUnsafe(app.Zeze.newProcedure(task, name), DispatchMode.Normal)
				: Task.runUnsafe(task::call, name, DispatchMode.Normal));
		}

	public static <K extends Comparable<K>> void clearDbTable(TableX<K, ?> table) throws Exception {
		var t = System.nanoTime();
		table.__ClearTableCacheUnsafe__();
		//noinspection DataFlowIssue
		var dbTable = table.internalGetStorageForTestOnly("IKnownWhatIAmDoing").getDatabaseTable();
		var txnWrap = new Object() {
			Database.Transaction txn = dbTable.getDatabase().beginTransaction();
			int n;
			int total;
		};

		if (table.isUseRelationalMapping()) {
			var sqlKey = new SQLStatement();
			dbTable.walkDatabaseKey(table, k -> {
				sqlKey.clear();
				table.encodeKeySQLStatement(sqlKey, k);
				var txn = txnWrap.txn;
				dbTable.remove(txn, sqlKey);
				if (++txnWrap.n >= 10000) {
					txn.commit();
					txn.close();
					txnWrap.total += txnWrap.n;
					txnWrap.n = 0;
					txnWrap.txn = dbTable.getDatabase().beginTransaction();
				}
				return true;
			});
		} else {
			var bb = ByteBuffer.Allocate(0);
			((Database.AbstractKVTable)dbTable).walkKey(k -> {
				var txn = txnWrap.txn;
				bb.wraps(k);
				dbTable.remove(txn, bb);
				if (++txnWrap.n >= 10000) {
					txn.commit();
					txn.close();
					txnWrap.total += txnWrap.n;
					txnWrap.n = 0;
					txnWrap.txn = dbTable.getDatabase().beginTransaction();
				}
				return true;
			});
		}
		var txn = txnWrap.txn;
		txn.commit();
		txn.close();
		txnWrap.total += txnWrap.n;
		logger.info("clear table {}: {}, {} ms", table.getName(), txnWrap.total, (System.nanoTime() - t) / 1_000_000);
	}

	public void clearTables() throws Exception {
		clearDbTable(app.demo_Module1.getTable1());
		clearDbTable(app.demo_Module1.getTflush());
		clearDbTable(app.demo_Module1.getTableCoverHistory());
		//noinspection DataFlowIssue
		clearDbTable((TableX<?, ?>)app.getZeze().getTable("Zeze_Builtin_DelayRemove_tJobs"));
		clearDbTable(app.getZeze().getHistoryModule().getHistoryTable()); // 必须在最后清空
	}

	public void WaitAllRunningTasksAndClear() throws ExecutionException, InterruptedException {
		Task.waitAll(RunningTasks);
		RunningTasks.clear();
	}
}
