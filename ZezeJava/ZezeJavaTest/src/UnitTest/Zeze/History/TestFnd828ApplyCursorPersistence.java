package UnitTest.Zeze.History;

import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.Config;
import Zeze.History.ApplyDatabaseZeze;
import Zeze.History.ApplyHelper;
import Zeze.History.IApplyDatabase;
import Zeze.History.IApplyRecordTxn;
import Zeze.History.IApplyTable;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DatabaseMemory;
import Zeze.Util.Id128;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * FND8-28 回归：ApplyHelper回放游标（exclusiveStartKey）纯内存，配持久化后端
 * ApplyDatabaseZeze时重启归零——apply从tHistory表头整段重放到已有状态上
 * （Edit类日志非幂等，PList2的OP_ADD按索引插入产生重复元素，回放副本被污染）。
 * 修复：IApplyDatabase增loadCursor/saveCursor（default覆盖内存后端），ApplyHelper
 * 构造时恢复游标、每条记录在记录级事务内与数据同原子单元保存；ApplyDatabaseZeze经
 * __cursor__伪表与记录同一dbTxn提交。
 * 用计数包装器证明"重启后只应用新记录"：修复前游标null，10/11被整段重放（3条事务）；
 * 修复后从持久游标续传（1条事务）。
 */
@Fast
public class TestFnd828ApplyCursorPersistence {
	// 独立serverId+url：@Fast类并行时避免DatabaseMemory静态Map按url分桶互撞；
	// 两个用例各自独立serverId（tHistory存储按url在JVM内持续存在，用例间不得共享）。
	private static final int SERVER_ID = 12828;
	private static final int SERVER_ID_MEMORY = 12829;
	private static final String APPLIED_DB_NAME = "a2_applied";
	private static final String tHistoryName = "Zeze_Builtin_HistoryModule_tHistory";
	private static final int tHistoryId = 370198048;

	/** 计数包装器：每个beginRecordTxn=一条tHistory记录被应用，证明重放/续传的应用面。 */
	private static final class CountingApplyDb implements IApplyDatabase {
		final ApplyDatabaseZeze inner;
		final AtomicInteger recordTxns = new AtomicInteger();

		CountingApplyDb(Application app) {
			inner = new ApplyDatabaseZeze(app, APPLIED_DB_NAME);
		}

		@Override
		public @NotNull IApplyTable open(@NotNull String tableName) {
			return inner.open(tableName);
		}

		@Override
		public @NotNull IApplyRecordTxn beginRecordTxn() {
			recordTxns.incrementAndGet();
			return inner.beginRecordTxn();
		}

		@Override
		public @Nullable Id128 loadCursor() {
			return inner.loadCursor();
		}

		@Override
		public void saveCursor(@NotNull Id128 key, @NotNull IApplyRecordTxn txn) throws Exception {
			inner.saveCursor(key, txn); // txn即inner的RecordTxn：__cursor__随记录同一dbTxn提交
		}
	}

	private static Application newApp(int serverId) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("a2_fnd828_hist_" + serverId);
		conf.getDatabaseConfMap().put("", dbConf);
		var appliedConf = new Config.DatabaseConf();
		appliedConf.setName(APPLIED_DB_NAME);
		appliedConf.setDatabaseUrl("a2_fnd828_applied_" + serverId);
		conf.getDatabaseConfMap().put(APPLIED_DB_NAME, appliedConf);
		return new Application("TestFnd828ApplyCursorPersistence_" + serverId, conf);
	}

	// 直写tHistory底层存储（对齐TestApplyHelperCursorHole）：空changes——本测试聚焦
	// 游标持久化本身，不构造业务表变更。
	private static void insertHistory(Application app, Id128 key, long timestamp) throws Exception {
		var db = (DatabaseMemory)app.getDatabase("");
		var storageTable = db.openTable(tHistoryName, tHistoryId);
		var value = new BLogChanges();
		value.setGlobalSerialId(key.clone());
		value.setTimestamp(timestamp);
		var valueBb = ByteBuffer.Allocate();
		value.encode(valueBb);
		var keyBb = ByteBuffer.Allocate();
		key.encode(keyBb);
		var txn = db.beginTransaction();
		try {
			storageTable.replace(txn, keyBb, valueBb);
			txn.commit();
		} finally {
			txn.close();
		}
	}

	@Test
	public void testCursorSurvivesRestartAndSkipsReplay() throws Exception {
		var app = newApp(SERVER_ID);
		try {
			app.start();
			var old = System.currentTimeMillis() - 60_000; // 全部满足endTime=now-20s

			// 首轮：应用10、11，游标随记录事务持久化到__cursor__。
			var db1 = new CountingApplyDb(app);
			var helper1 = new ApplyHelper(app, app.getHistoryModule().getHistoryTable(), db1, 20_000, 600_000);
			assertNull(helper1.getExclusiveStartKey(), "空库首启游标为null");
			insertHistory(app, new Id128(0, 10), old);
			insertHistory(app, new Id128(0, 11), old);
			helper1.apply(100);
			assertEquals(new Id128(0, 11), helper1.getExclusiveStartKey());
			assertEquals(2, db1.recordTxns.get(), "两条记录各开一个记录级事务");

			// 模拟重启：新ApplyDatabaseZeze（同一持久库）+新ApplyHelper。
			var db2 = new CountingApplyDb(app);
			var helper2 = new ApplyHelper(app, app.getHistoryModule().getHistoryTable(), db2, 20_000, 600_000);
			assertEquals(new Id128(0, 11), helper2.getExclusiveStartKey(),
					"游标必须从持久化后端恢复（修复前为null，重启后从表头整段重放）");

			// 续传：新记录12落库后apply，只应用12——10/11不得被重放。
			insertHistory(app, new Id128(0, 12), old);
			helper2.apply(100);
			assertEquals(new Id128(0, 12), helper2.getExclusiveStartKey());
			assertEquals(1, db2.recordTxns.get(),
					"恢复游标后只应用新记录12（修复前重放10/11/12共3条，Edit类日志被二次应用即污染）");

			// 再次"重启"：12也已持久化。
			var db3 = new CountingApplyDb(app);
			var helper3 = new ApplyHelper(app, app.getHistoryModule().getHistoryTable(), db3, 20_000, 600_000);
			assertEquals(new Id128(0, 12), helper3.getExclusiveStartKey());
		} finally {
			app.stop();
		}
	}

	/** 内存后端default语义：loadCursor为null、saveCursor丢弃，行为与修复前完全一致。 */
	@Test
	public void testMemoryBackendDefaultsUnchanged() throws Exception {
		var app = newApp(SERVER_ID_MEMORY);
		try {
			app.start();
			var helper = new ApplyHelper(app, app.getHistoryModule().getHistoryTable(),
					new Zeze.History.ApplyDatabaseMemory(), 20_000, 600_000);
			assertNull(helper.getExclusiveStartKey(), "内存后端游标为null（重启即空，天然一致）");
			var old = System.currentTimeMillis() - 60_000;
			insertHistory(app, new Id128(0, 10), old);
			helper.apply(100);
			Assertions.assertEquals(new Id128(0, 10), helper.getExclusiveStartKey(), "内存后端apply行为不变");
		} finally {
			app.stop();
		}
	}
}
