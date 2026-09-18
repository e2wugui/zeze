package Zeze.Transaction;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import Zeze.Application;
import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.Config;
import Zeze.Util.FuncLong;

/**
 * FND8-18 回归：提交路径"修改已应用（commit.run完成）但未登记进任何落库通道"的窗口。
 * Table模式：collectChanges（History开启时的tid128 UDP等待）或needFlushNow的
 * checkpoint.flush 抛出时，原代码直接向上传播，mergedSet不进relativeRecordSetMap，
 * perform的halt兜底checkpointRun只遍历map——已应用数据连同_merge_并入的存量脏集
 * 一并丢失（Immediately模式则连理论通道都没有）。
 * 修复：Table路径catch中先把mergedSet注册进map（锁仍由本线程持有，注册并发安全）
 * 再重抛，交给后台checkpoint重试；Immediately路径趁记录锁未释放做一次受控补刷，
 * 补刷成功则fatal记原异常后吞掉继续。
 * 测试分别覆盖：收集失败注册（01）、needFlushNow落库失败注册（02）、连续失败下
 * 存量脏集随mergedSet得救（03，修复前首战失败产生孤儿脏集、二战合并后仍未注册，
 * 两笔数据全丢）、Immediately补刷成功吞异常继续（04，修复前finalCommit重抛直接
 * halt(543543)进程，测试JVM当场死亡）。
 */
@Fast
public class TestFnd818CommitPathSalvage {
	// 独立serverId+url：@Fast类并行时避免本地RocksCache目录（zeze_cache_<serverId>）互撞。
	private static final int SERVER_ID = 12818;

	@TempDir
	Path tempDir;

	private Application app;
	private tFnd818Table table;

	private void startApp(CheckpointMode mode, String dbSubName, Consumer<Config> tweak) throws Exception {
		F818Bean.encodeFailOnce.set(0);
		var config = new Config();
		config.setServiceManager("disable");
		config.setCheckpointMode(mode);
		config.setCheckpointPeriod(3_600_000); // 关闭后台checkpoint线程的干扰，flush时机全部由测试控制
		config.setServerId(SERVER_ID);
		config.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.RocksDb);
		dbConf.setDatabaseUrl(tempDir.resolve(dbSubName).toString());
		config.getDatabaseConfMap().put("", dbConf);
		if (tweak != null)
			tweak.accept(config);
		app = new Application("TestFnd818CommitPathSalvage", config);
		table = new tFnd818Table();
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
			var b = new F818Bean();
			b.value = value;
			table.put(key, b);
			return 0L;
		}, "TestFnd818CommitPathSalvage.put").call();
		Assertions.assertEquals(Procedure.Success, result, "put 事务必须成功");
	}

	/** 后台库中的值；不存在返回 null。 */
	private Long dbValue(long key) {
		var v = table.selectFromDatabase(key);
		return v != null ? v.value : null;
	}

	/**
	 * 直接驱动 RelativeRecordSet.tryUpdateAndCheckpoint 的 Table 提交通道：
	 * 手工构造与 perform 到达 finalCommit 时等价的事务状态（accessedRecords 携带 PutLog
	 * 且置 dirty），commit 运行等价 Savepoint.commit + Record1.commit 的应用效果。
	 * failCollect：让 collectChanges 抛异常（模拟 History tid128 UDP 超时）。
	 * 这样异常在测试线程可控地重抛出来（经 perform 则直接 halt 进程，无法在测试内断言）。
	 */
	private Exception runManualCommit(long key, long value, boolean failCollect) throws Exception {
		var trans = Transaction.create(app.getLocks());
		try {
			trans.begin();
			var b = new F818Bean();
			b.value = value;
			table.put(key, b);
			for (var ar : trans.getAccessedRecords().values())
				ar.dirty = true; // perform的lockAndCheck在这里把有修改日志的记录置脏

			var proc = new Procedure(app, (FuncLong)() -> 0L, "Fnd818.ManualCommit", null);
			Runnable commit = () -> {
				for (var ar : trans.getAccessedRecords().values()) {
					var log = trans.getLog(ar.objectId());
					if (log instanceof RecordAccessed.PutLog putLog)
						putLog.commit(); // Savepoint.commit 的应用效果：ar.committedPutLog = putLog
					var record = ar.atomicTupleRecord.record;
					record.setNotFresh();
					if (ar.dirty)
						record.commit(ar); // finalCommit 的应用效果：setSoftValue + 置脏
				}
			};
			Callable<BLogChanges.Data> collect = () -> {
				if (failCollect)
					throw new RuntimeException("FND8-18 simulated history tid128 udp timeout");
				return null;
			};
			RelativeRecordSet.tryUpdateAndCheckpoint(trans, proc, commit, null, collect);
			return null;
		} finally {
			Transaction.destroy();
		}
	}

	/** Table普通路径：collectChanges失败时mergedSet必须先注册进map再重抛，后台checkpoint可重试落库。 */
	@Test
	public void test01TablePathCollectFailRegistersRrs() throws Exception {
		try {
			startApp(CheckpointMode.Table, "t01", null);
			putValue(1L, 10L);
			app.getCheckpoint().runOnce(); // 落库并让记录换上全新的孤立rrs（不进map）
			Assertions.assertEquals(Long.valueOf(10L), dbValue(1L));
			Assertions.assertTrue(app.getCheckpoint().relativeRecordSetMap.isEmpty());

			var ex = Assertions.assertThrows(Exception.class, () -> runManualCommit(1L, 20L, true),
					"收集失败必须重抛（perform仍走halt，语义不变）");
			Assertions.assertTrue(ex.getMessage().contains("tid128"), "重抛的必须是原始异常");
			Assertions.assertFalse(app.getCheckpoint().relativeRecordSetMap.isEmpty(),
					"修复点：重抛前mergedSet必须注册进relativeRecordSetMap（修复前map为空，已应用数据无任何落库通道）");

			app.getCheckpoint().runOnce(); // 模拟perform halt分支的checkpointRun()/后台重试
			Assertions.assertEquals(Long.valueOf(20L), dbValue(1L), "已应用数据必须经注册的rrs重试落库");
			Assertions.assertTrue(app.getCheckpoint().relativeRecordSetMap.isEmpty(), "flush后map清空");
		} finally {
			F818Bean.encodeFailOnce.set(0);
			if (app != null)
				stopApp();
		}
	}

	/** Table needFlushNow路径：checkpoint.flush失败时同样先注册再重抛。 */
	@Test
	public void test02TablePathFlushNowFailRegistersRrs() throws Exception {
		try {
			startApp(CheckpointMode.Table, "t02", c -> c.getDefaultTableConf().setCheckpointWhenCommit(true));
			putValue(1L, 10L); // checkpointWhenCommit：提交点同步落库，记录不进map
			Assertions.assertEquals(Long.valueOf(10L), dbValue(1L));

			F818Bean.encodeFailOnce.set(1); // 第一次encode（首刷）抛，第二次（补刷/重试）放行
			var ex = Assertions.assertThrows(Exception.class, () -> runManualCommit(1L, 20L, false),
					"flush失败必须重抛（perform仍走halt，语义不变）");
			Assertions.assertNotNull(ex.getCause(), "flushInternal包装重抛，cause为底层异常");
			Assertions.assertTrue(ex.getCause().getMessage().contains("simulated flush io error"),
					"重抛的必须是原始异常");
			Assertions.assertFalse(app.getCheckpoint().relativeRecordSetMap.isEmpty(),
					"修复点：needFlushNow的flush失败同样先注册mergedSet再重抛");

			app.getCheckpoint().runOnce();
			Assertions.assertEquals(Long.valueOf(20L), dbValue(1L), "flush失败保dirty重试落库（既有语义的注册延伸）");
			Assertions.assertTrue(app.getCheckpoint().relativeRecordSetMap.isEmpty());
			Assertions.assertEquals(2, F818Bean.encodeFailOnce.get(), "encode只失败一次（毒化自限）");
		} finally {
			F818Bean.encodeFailOnce.set(0);
			if (app != null)
				stopApp();
		}
	}

	/**
	 * 连续失败（孪生放大）：第一笔失败若未注册会留下孤儿脏集（不在map、记录已脏），
	 * 第二笔事务把它merge进新mergedSet——修复前两笔都无落库通道全丢；
	 * 修复后每次失败都注册，最终一笔重试落库即可救回全部存量（同键取最新值）。
	 */
	@Test
	public void test03ChainedFailSalvagesExistingDirtySet() throws Exception {
		try {
			startApp(CheckpointMode.Table, "t03", null);
			putValue(1L, 10L);
			app.getCheckpoint().runOnce();
			Assertions.assertEquals(Long.valueOf(10L), dbValue(1L));

			Assertions.assertThrows(Exception.class, () -> runManualCommit(1L, 20L, true)); // 第一笔失败
			Assertions.assertThrows(Exception.class, () -> runManualCommit(1L, 30L, true)); // 第二笔失败
			Assertions.assertFalse(app.getCheckpoint().relativeRecordSetMap.isEmpty(),
					"每笔失败的mergedSet都必须已注册（含第一笔留下的存量脏集）");

			app.getCheckpoint().runOnce();
			Assertions.assertEquals(Long.valueOf(30L), dbValue(1L),
					"修复前：两笔孤儿脏集全丢，库中仍是10；修复后：存量随mergedSet得救，最终值30");
		} finally {
			F818Bean.encodeFailOnce.set(0);
			if (app != null)
				stopApp();
		}
	}

	/**
	 * Immediately路径：落库失败时受控补刷成功则吞掉原异常继续（数据已落库）。
	 * 修复前：finalCommit重抛→perform直接halt(543543)，测试JVM死亡；修复后返回Success。
	 */
	@Test
	public void test04ImmediatelySalvageFlushSavesData() throws Exception {
		try {
			startApp(CheckpointMode.Immediately, "t04", null);
			F818Bean.encodeFailOnce.set(1); // 首刷encode抛，补刷放行
			putValue(1L, 20L); // 内部断言Success：补刷成功后异常被吞，perform正常返回
			Assertions.assertEquals(Long.valueOf(20L), dbValue(1L),
					"受控补刷必须把已应用数据落库（修复前：halt丢数据且进程死亡）");
		} finally {
			F818Bean.encodeFailOnce.set(0);
			if (app != null)
				stopApp();
		}
	}

	// ---------------------------------------------------------------
	// 测试用表和值类型
	// ---------------------------------------------------------------

	/** 值类型：encodeFailOnce==1时下一次encode抛异常（自限一次），模拟flush过程中的瞬时数据库IO错误。 */
	public static final class F818Bean extends Bean {
		public static final AtomicInteger encodeFailOnce = new AtomicInteger();

		public long value;

		@Override
		public void encode(Zeze.Serialize.ByteBuffer bb) {
			if (encodeFailOnce.compareAndSet(1, 2))
				throw new RuntimeException("FND8-18 simulated flush io error");
			bb.WriteLong(value);
		}

		@Override
		public void decode(Zeze.Serialize.IByteBuffer bb) {
			value = bb.ReadLong();
		}

		@Override
		public Bean copy() {
			var c = new F818Bean();
			c.value = value;
			return c;
		}
	}

	/** 最小 TableX 实现（Long key → F818Bean），非内存表（RocksDb存储）。 */
	public static final class tFnd818Table extends TableX<Long, F818Bean> {
		public tFnd818Table() {
			super(990818, "UnitTest_TestFnd818CommitPathSalvage_tFnd818");
		}

		@Override
		public Class<Long> getKeyClass() {
			return Long.class;
		}

		@Override
		public Class<F818Bean> getValueClass() {
			return F818Bean.class;
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
		public F818Bean newValue() {
			return new F818Bean();
		}
	}
}
