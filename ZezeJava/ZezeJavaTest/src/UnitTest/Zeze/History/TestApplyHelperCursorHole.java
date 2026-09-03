package UnitTest.Zeze.History;

import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.Config;
import Zeze.History.ApplyDatabaseMemory;
import Zeze.History.ApplyHelper;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DatabaseMemory;
import Zeze.Util.Id128;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND-G2-7：ApplyHelper独占游标越过"提交早但落库晚"的记录，迟到记录被永久跳过。
 * 构造性验证：手工向tHistory存储按乱序时间插入记录（模拟多进程共享发号名、
 * checkpoint停滞时BLogChanges迟到落库），断言游标停在键空洞前不越过；
 * 空洞填充后继续推进；只有超过holeGraceMs的老化空洞才被越过（崩溃/发号作废产生的永久空洞）。
 */
@Fast
public class TestApplyHelperCursorHole {
	// Application并发需要不同serverId+不同DatabaseUrl：DatabaseMemory的表存储是JVM级
	// 静态Map按url分桶，@Fast类并行时共用会互相污染。从400起避开Takeover等测试的号段。
	private static final AtomicInteger NextServerId = new AtomicInteger(400);
	private static final String tHistoryName = "Zeze_Builtin_HistoryModule_tHistory";
	private static final int tHistoryId = 370198048;

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		int serverId = NextServerId.getAndIncrement();
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("history_apply_test_" + serverId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf); // Memory库，独立url=独立存储
		return new Application("TestApplyHelperCursorHole", conf);
	}

	// 直接向底层存储写一条历史记录：模拟Checkpoint flush的落库效果（绕开TableX缓存），
	// 可以自由控制key的落库顺序与时间戳，构造"提交早但落库晚"的乱序。
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
	public void testCursorNotCrossHole() throws Exception {
		var app = newApp();
		try {
			app.start();
			var helper = new ApplyHelper(app, app.getHistoryModule().getHistoryTable(),
					new ApplyDatabaseMemory(), 20_000, 600_000);

			var old = System.currentTimeMillis() - 60_000; // 全部满足 endTime=now-20s
			// 表中有10、12：11是空洞——正是finding的触发形态（S1未落库，S2已落库）。
			insertHistory(app, new Id128(0, 10), old);
			insertHistory(app, new Id128(0, 12), old);

			helper.apply(100);
			Assertions.assertEquals(new Id128(0, 10), helper.getExclusiveStartKey(),
					"游标必须停在空洞前，不得越过可能迟到的记录");

			// 迟到记录11落库（checkpoint恢复），下一轮继续推进，12不再被跳过。
			insertHistory(app, new Id128(0, 11), old);
			helper.apply(100);
			Assertions.assertEquals(new Id128(0, 12), helper.getExclusiveStartKey(),
					"空洞填充后游标应继续推进");
		} finally {
			app.stop();
		}
	}

	@Test
	public void testCrossAgedHole() throws Exception {
		var app = newApp();
		try {
			app.start();
			// holeGraceMs=100：空洞老化100ms后按永久空洞（进程崩溃/发号作废）越过。
			var helper = new ApplyHelper(app, app.getHistoryModule().getHistoryTable(),
					new ApplyDatabaseMemory(), 20_000, 100);

			var old = System.currentTimeMillis() - 60_000;
			insertHistory(app, new Id128(0, 20), old);
			insertHistory(app, new Id128(0, 22), old);

			helper.apply(100);
			Assertions.assertEquals(new Id128(0, 20), helper.getExclusiveStartKey(),
					"新空洞必须先阻塞游标等待老化");

			Thread.sleep(150); // 超过holeGraceMs
			helper.apply(100);
			Assertions.assertEquals(new Id128(0, 22), helper.getExclusiveStartKey(),
					"老化空洞应被越过，永久空洞不能让消费永久停滞");
		} finally {
			app.stop();
		}
	}

	@Test
	public void testFreshStartIgnoresUnknownPrefix() throws Exception {
		var app = newApp();
		try {
			app.start();
			var helper = new ApplyHelper(app, app.getHistoryModule().getHistoryTable(),
					new ApplyDatabaseMemory(), 20_000, 600_000);

			// 从头消费（游标null）：首key不是1，表前缀无法判断空洞，不得卡死。
			var old = System.currentTimeMillis() - 60_000;
			insertHistory(app, new Id128(0, 100), old);
			insertHistory(app, new Id128(0, 101), old);

			helper.apply(100);
			Assertions.assertEquals(new Id128(0, 101), helper.getExclusiveStartKey(),
					"游标为null时不检查前缀空洞，应正常消费");
		} finally {
			app.stop();
		}
	}

	@Test
	public void testTimeBarrier() throws Exception {
		var app = newApp();
		try {
			app.start();
			var helper = new ApplyHelper(app, app.getHistoryModule().getHistoryTable(),
					new ApplyDatabaseMemory(), 20_000, 600_000);

			// 连续key中混入时间未到的记录：游标停在它前面（原有endTime行为不回归）。
			insertHistory(app, new Id128(0, 30), System.currentTimeMillis() - 60_000);
			insertHistory(app, new Id128(0, 31), System.currentTimeMillis());

			helper.apply(100);
			Assertions.assertEquals(new Id128(0, 30), helper.getExclusiveStartKey(),
					"时间未到的记录必须阻塞游标");
		} finally {
			app.stop();
		}
	}
}
