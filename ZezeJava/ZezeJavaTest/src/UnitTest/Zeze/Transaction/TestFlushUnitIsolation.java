package UnitTest.Zeze.Transaction;

import java.nio.file.Path;
import java.util.TreeMap;
import java.util.function.Consumer;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Bean;
import Zeze.Transaction.CheckpointFlushMode;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TableKey;
import Zeze.Transaction.TableX;
import Zeze.Util.FuncLong;

/**
 * 单元失败隔离三连（FND2-T2-2 / T2-3 / T2-5）：
 * 1. flushWhenCheckpoint 单个 rrs/FlushSet flush 失败（毒化记录编码抛异常）不能中断整轮，
 *    其余集合照常落库，失败单元留在 relativeRecordSetMap（flush成功才会remove）下轮重试；
 * 2. TableX.cacheCopy 拷贝段复用 Record1.copyValue：lockey 锁忙时回退最后落库镜像，
 *    不得拷贝进行中（已提交未flush）的内存值；
 * 3. TableCache.cleanNow 超容量 while+sleep 循环有单次执行时界（CacheCleanPeriod×3），
 *    最老块持续清不掉时必须按时界返回，留给下一轮调度。
 * 测试用本地 RocksDb（临时目录），自包含，@Fast。
 */
@Fast
public class TestFlushUnitIsolation {
	// serverId 决定本地 RocksCache 目录名（zeze_cache_<serverId>），取独立值避免与其他测试冲突。
	private static final int SERVER_ID = 7312;

	@TempDir
	Path tempDir;

	private Application app;
	private tPoisonFlush table;

	private void startApp(String dbSubName, Consumer<Config> tweak) throws Exception {
		PoisonBean.poison = false;
		var config = new Config();
		config.setServiceManager("disable");
		config.setCheckpointMode(CheckpointMode.Table);
		config.setCheckpointPeriod(3_600_000); // 消除后台checkpoint线程的干扰（start时首轮flush时map为空）
		config.setServerId(SERVER_ID);
		config.setDefaultTableConf(new Config.TableConf()); // 裸 Config 不会补默认值；内置模块注册表时需要
		var dbConf = new Config.DatabaseConf(); // name="" 即默认数据库，与 DefaultTableConf.databaseName("") 匹配
		dbConf.setDatabaseType(Config.DbType.RocksDb);
		dbConf.setDatabaseUrl(tempDir.resolve(dbSubName).toString());
		config.getDatabaseConfMap().put("", dbConf);
		if (tweak != null)
			tweak.accept(config);

		app = new Application("TestFlushUnitIsolation", config);
		table = new tPoisonFlush();
		app.addTable("", table);
		app.start();
	}

	private void stopApp() throws Exception {
		app.stop();
		app = null;
		table = null;
	}

	private void putValue(long key, long value) {
		var result = app.newProcedure((FuncLong)() -> {
			var b = new PoisonBean();
			b.value = value;
			table.put(key, b);
			return 0L;
		}, "TestFlushUnitIsolation.put").call();
		Assertions.assertEquals(Procedure.Success, result, "put 事务必须成功");
	}

	/** 后台库中的值；不存在返回 null。 */
	private Long dbValue(long key) {
		var v = table.selectFromDatabase(key);
		return v != null ? v.value : null;
	}

	// ---------------------------------------------------------------
	// FND2-T2-2: 毒化 rrs/FlushSet 的失败隔离（四种 CheckpointFlushMode 全覆盖）
	// ---------------------------------------------------------------

	@Test
	public void testPoisonedUnitDoesNotBreakCheckpointRound() throws Exception {
		for (var mode : CheckpointFlushMode.values())
			flushIsolationScenario(mode);
	}

	private void flushIsolationScenario(CheckpointFlushMode mode) throws Exception {
		try {
			startApp("flush-" + mode.name(), c -> {
				c.setCheckpointFlushMode(mode);
				// Merge模式每次FlushSet只装1个rrs：隔离粒度与per-rrs一致，断言不依赖哈希迭代顺序。
				c.setCheckpointModeTableFlushSetCount(1);
			});
			// 三个独立事务 → 三个独立rrs：两个好记录 + 一个毒记录。
			putValue(1, 11);
			putValue(2, 22);
			putValue(99, 99);

			PoisonBean.poison = true;
			// 未修复：毒化单元的异常传播出 flushWhenCheckpoint（SingleThread直接中断迭代饿死其余集合，
			// parallelStream模式取消未启动任务），runOnce 本身抛出。
			Assertions.assertDoesNotThrow(() -> app.getCheckpoint().runOnce(),
					mode + ": 毒化单元不能让整轮 checkpoint 失败");

			// 其余照刷：所有成功单元都必须落库（与迭代顺序无关）。
			Assertions.assertEquals(Long.valueOf(11), dbValue(1), mode + ": 好记录必须照常落库");
			Assertions.assertEquals(Long.valueOf(22), dbValue(2), mode + ": 好记录必须照常落库");
			// 毒记录未落库（flush失败回滚），但也没有被丢弃。
			Assertions.assertNull(dbValue(99), mode + ": 毒记录在毒化期间不得落库");

			// 解毒后下轮重试成功：验证失败单元保留在 relativeRecordSetMap 的重试语义。
			PoisonBean.poison = false;
			app.getCheckpoint().runOnce();
			Assertions.assertEquals(Long.valueOf(99), dbValue(99), mode + ": 解毒后失败单元必须被重试落库");
		} finally {
			PoisonBean.poison = false;
			if (app != null)
				stopApp();
		}
	}

	// ---------------------------------------------------------------
	// FND2-T2-3: cacheCopy 复用 copyValue（lockey tryRead + 镜像回退）
	// ---------------------------------------------------------------

	@Test
	public void testWalkMemoryFallsBackToMirrorWhenLockeyBusy() throws Exception {
		try {
			startApp("walkmemory", null);
			putValue(1, 10);
			app.getCheckpoint().runOnce(); // 落库+写镜像：镜像里是10

			putValue(1, 20); // 内存中已提交新值20（dirty，等checkpoint），镜像仍是10

			// 其他线程持有该记录的lockey写锁（模拟进行中的事务）：
			// lockey.tryEnterReadLock(0) 与写锁互斥，walkMemory 必须回退镜像而不是拷内存值。
			// ReentrantReadWriteLock 必须由持锁线程解锁（异线程unlock抛IllegalMonitorStateException，
			// 锁会滞留在死线程上），用单个辅助线程 + 闭锁控制持锁窗口。
			var lockey = app.getLocks().get(new TableKey(table.getId(), 1L));
			var lockEntered = new java.util.concurrent.CountDownLatch(1);
			var lockRelease = new java.util.concurrent.CountDownLatch(1);
			var helper = new Thread(() -> {
				lockey.enterWriteLock();
				lockEntered.countDown();
				try {
					lockRelease.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					lockey.exitWriteLock();
				}
			});
			helper.start();
			lockEntered.await();
			try {
				var seen = walkMemoryValues();
				Assertions.assertEquals(Long.valueOf(10L), seen.get(1L),
						"锁忙时必须回退最后落库镜像（旧但完整），不得拷贝已提交未flush的内存值");
			} finally {
				lockRelease.countDown();
				helper.join();
			}

			// 无锁竞争时正常路径不受影响：看到最新已提交内存值。
			var seen2 = walkMemoryValues();
			Assertions.assertEquals(Long.valueOf(20L), seen2.get(1L), "无锁竞争时必须看到最新已提交内存值");
		} finally {
			if (app != null)
				stopApp();
		}
	}

	private TreeMap<Long, Long> walkMemoryValues() throws Exception {
		var result = new TreeMap<Long, Long>();
		table.walkMemory((k, v) -> {
			result.put(k, v.value);
			return true;
		});
		return result;
	}

	private static void runOnHelperThread(Runnable action) throws InterruptedException {
		var t = new Thread(action);
		t.start();
		t.join();
	}

	// ---------------------------------------------------------------
	// FND2-T2-5: cleanNow 超容量循环的单次执行时界
	// ---------------------------------------------------------------

	@Test
	@Timeout(60)
	public void testCleanNowTimeBoundedWhenExceedCapacity() throws Exception {
		try {
			startApp("cleannow", c -> {
				var tableConf = c.getDefaultTableConf();
				tableConf.setCacheCapacity(1); // realCacheCapacity = floor(1 * 1.0) = 1
				tableConf.setCacheFactor(1.0f);
				tableConf.setCacheCleanPeriod(500); // 时界 = 500 × 3 = 1500ms
				tableConf.setCacheCleanPeriodWhenExceedCapacity(100);
				tableConf.setCacheNewLruHotPeriod(100); // 快速产生非hot的最老节点，超容量循环可达
			});

			// 全部记录毒化：脏且flush必败（编码异常被单元隔离吞掉）→ 最老lru节点永远清不掉。
			// 毒化按记录判定（见PoisonBean）：全部写入毒值99。
			PoisonBean.poison = true;
			for (long k = 1; k <= 5; k++)
				putValue(k, PoisonBean.POISON_VALUE);
			Thread.sleep(500); // 等newLruHot定时器把5条记录所在的节点变成非hot的最老节点

			var t0 = System.nanoTime();
			table.getCache().cleanNow(); // 未修复：while+sleep无限循环（isStart恒真），占死调度线程
			var elapsedMs = (System.nanoTime() - t0) / 1_000_000;
			Assertions.assertTrue(elapsedMs < 15_000,
					"cleanNow 单次执行必须有时界（实际 " + elapsedMs + " ms）");
			Assertions.assertEquals(5L, table.getCache().size(), "毒化期间脏记录清不掉，但也不能丢");

			// 解毒：flush成功后，下一轮 cleanNow 必须能收敛回收至容量以内。
			PoisonBean.poison = false;
			app.getCheckpoint().runOnce();
			table.getCache().cleanNow();
			Assertions.assertTrue(table.getCache().size() <= 1,
					"解毒后必须能回收至容量以内（实际 " + table.getCache().size() + "）");
		} finally {
			PoisonBean.poison = false;
			if (app != null)
				stopApp();
		}
	}

	// ---------------------------------------------------------------
	// 测试用表和值类型
	// ---------------------------------------------------------------

	/** 值类型：poison=true 且 value 为毒值(99)时 encode 抛异常，模拟单条坏记录（编码异常/约束冲突/
	 * 连接抖动中持续失败的记录）。毒化必须按记录：全局毒化会令好记录同轮一样失败，测不出"其余照刷"。 */
	public static final class PoisonBean extends Bean {
		public static volatile boolean poison = false;
		public static final long POISON_VALUE = 99;

		public long value;

		@Override
		public void encode(Zeze.Serialize.ByteBuffer bb) {
			if (poison && value == POISON_VALUE)
				throw new RuntimeException("poison encode");
			bb.WriteLong(value);
		}

		@Override
		public void decode(Zeze.Serialize.IByteBuffer bb) {
			value = bb.ReadLong();
		}

		@Override
		public Bean copy() {
			var c = new PoisonBean();
			c.value = value;
			return c;
		}
	}

	/** 最小 TableX 实现（Long key → PoisonBean），非内存表（RocksDb存储）。 */
	public static final class tPoisonFlush extends TableX<Long, PoisonBean> {
		public tPoisonFlush() {
			super(990711, "UnitTest_TestFlushUnitIsolation_tPoisonFlush");
		}

		@Override
		public Class<Long> getKeyClass() {
			return Long.class;
		}

		@Override
		public Class<PoisonBean> getValueClass() {
			return PoisonBean.class;
		}

		@Override
		public Long decodeKey(Zeze.Serialize.ByteBuffer bb) {
			return bb.ReadLong();
		}

		@Override
		public Zeze.Serialize.ByteBuffer encodeKey(Long key) {
			var bb = Zeze.Serialize.ByteBuffer.Allocate(8);
			bb.WriteLong(key);
			return bb;
		}

		@Override
		public Long decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
			return rs.getLong("__key");
		}

		@Override
		public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Long key) {
			st.appendLong("__key", key);
		}

		@Override
		public PoisonBean newValue() {
			return new PoisonBean();
		}
	}
}
