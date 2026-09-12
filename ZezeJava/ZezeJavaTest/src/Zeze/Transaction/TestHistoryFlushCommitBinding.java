package Zeze.Transaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Application;
import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.FuncLong;
import Zeze.Util.Id128;

/**
 * FND3-51：Checkpoint.flush 在数据库事务 commit 之前清空 History 容器
 * （encode0 清 logChanges、flush 清 encoded）。Table 模式下 flush 失败的单元
 * 保留在 relativeRecordSetMap 等下轮重试——数据记录的快照保留在 Record 里
 * （cleanup 在 commit 成功后才执行），幂等重写落库；但 History 的两个容器已空，
 * tHistory 行永久缺失，回放/对账与主库永久分歧。
 *
 * 失败注入：FlakyDatabase 承载测试表与 tHistory（TableConf 改挂），事务 commit
 * 可毒化——失败精确落在丢失窗口（写库之后、commit 之中）；测试表与 tHistory 同库，
 * 与提交顺序无关地确定性复现。
 */
@Fast
public class TestHistoryFlushCommitBinding {
	// serverId 决定本地 RocksCache 目录名（zeze_cache_<serverId>），取独立值避免与其他测试冲突。
	private static final int SERVER_ID = 7351;
	private static final String tHistoryName = "Zeze_Builtin_HistoryModule_tHistory";

	private Application app;
	private FlakyDatabase flaky;
	private tSimple table;

	private void startApp(String dbSubName) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setCheckpointMode(CheckpointMode.Table);
		conf.setCheckpointPeriod(3_600_000); // 消除后台checkpoint线程的干扰
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸 Config 不会补默认值；内置模块注册表时需要
		var dbConf = new Config.DatabaseConf(); // name="" 即默认数据库
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("history_commit_binding_" + dbSubName);
		conf.getDatabaseConfMap().put("", dbConf);

		app = new Application("TestHistoryFlushCommitBinding", conf);

		// flaky 库入表；tHistory 构造期注册在默认库上（TableConf 无 setter），构造后、
		// start 前手工改挂（此时 storage 尚未创建，removeTable 的 close 是 no-op）。
		var flakyConf = new Config.DatabaseConf();
		flakyConf.setDatabaseType(Config.DbType.Memory);
		flakyConf.setDatabaseUrl("history_commit_binding_flaky_" + dbSubName);
		flaky = new FlakyDatabase(flakyConf);
		app.getDatabases().put("flaky", flaky);
		app.removeTable("", app.getHistoryModule().getHistoryTable());
		app.addTable("flaky", app.getHistoryModule().getHistoryTable());

		table = new tSimple();
		app.addTable("flaky", table);
		app.start();
	}

	private void stopApp() throws Exception {
		app.stop();
		app = null;
		flaky = null;
		table = null;
	}

	private void putValue(long key, long value) {
		var result = app.newProcedure((FuncLong)() -> {
			var b = new SimpleBean();
			b.value = value;
			table.put(key, b);
			return 0L;
		}, "TestHistoryFlushCommitBinding.put").call();
		Assertions.assertEquals(Procedure.Success, result, "put 事务必须成功");
	}

	/** 后台库中的值；不存在返回 null。 */
	private Long dbValue(long key) {
		var v = table.selectFromDatabase(key);
		return v != null ? v.value : null;
	}

	/** 手工向 rrs 夹带一条历史记录：绕开 SM 发号（构造期 History 采集需要 tid128），
	 * 直接构造 BLogChanges.Data 挂进事务留下的 rrs，走真实 flush 路径。 */
	private Id128 smuggleHistory(long serialLo) {
		var record = table.getCache().get(1L);
		var rrs = record.getRelativeRecordSet();
		var serial = new Id128(0x51D3L, serialLo);
		var lc = new BLogChanges.Data();
		lc.setGlobalSerialId(serial.clone());
		lc.setTimestamp(System.currentTimeMillis());
		rrs.addLogChanges(lc);
		return serial;
	}

	private HashSet<ByteBuffer> tHistoryKeys() throws Exception {
		var keys = new HashSet<ByteBuffer>();
		var t = (FlakyDatabase.FlakyTable)flaky.openTable(tHistoryName, 0);
		t.walk((key, value) -> {
			keys.add(ByteBuffer.Wrap(key));
			return true;
		});
		return keys;
	}

	private static ByteBuffer encodedKey(Id128 serial) {
		var bb = ByteBuffer.Allocate();
		serial.encode(bb);
		return ByteBuffer.Wrap(bb.Copy());
	}

	@Test
	public void testFlushFailRetryKeepsHistoryRow() throws Exception {
		// 四种 CheckpointFlushMode 全覆盖：Single/MultiThread 走 encodeN+per-rrs flush，
		// 两种 Merge 走 FlushSet 合并路径，最终都汇到同一个 Checkpoint.flush 收口。
		for (var mode : CheckpointFlushMode.values())
			flushFailRetryScenario(mode);
	}

	private void flushFailRetryScenario(CheckpointFlushMode mode) throws Exception {
		try {
			startApp(mode.name());
			putValue(1, 11);

			var serial = smuggleHistory(7);

			// 毒化 commit：flush 走到写库之后、提交时失败，回滚，rrs 留待下轮重试。
			FlakyDatabase.poisonCommit = true;
			Assertions.assertDoesNotThrow(() -> app.getCheckpoint().runOnce(),
					mode + ": 毒化单元必须被单元隔离吞掉，保留重试");

			// 解毒重试：数据记录与 tHistory 行都必须落库。
			FlakyDatabase.poisonCommit = false;
			app.getCheckpoint().runOnce();

			Assertions.assertEquals(Long.valueOf(11), dbValue(1), mode + ": 重试后数据记录必须落库");
			Assertions.assertTrue(tHistoryKeys().contains(encodedKey(serial)),
					mode + ": 重试后 tHistory 必须补上该事务的历史行（FND3-51）");
		} finally {
			FlakyDatabase.poisonCommit = false;
			if (app != null)
				stopApp();
		}
	}

	/** History 容器契约（FND3-51 修复核心）：encode0/writeOnly 只搬运不清空，
	 * commitDone 绑定提交结果；失败重试的形态（重复 encode0+writeOnly）按系列号幂等。 */
	@Test
	public void testHistoryContainerContract() throws Exception {
		var serial = new Id128(0x51D3L, 42);
		var lc = new BLogChanges.Data();
		lc.setGlobalSerialId(serial.clone());
		lc.setTimestamp(System.currentTimeMillis());
		var history = new Zeze.History.History(lc);

		var conf = new Config.DatabaseConf();
		conf.setDatabaseType(Config.DbType.Memory);
		conf.setDatabaseUrl("history_commit_binding_unit");
		var flaky = new FlakyDatabase(conf);
		var table = (FlakyDatabase.FlakyTable)flaky.openTable("unit_tHistory", 1);

		// 失败重试的形态：encode0+writeOnly 执行两遍（第一遍"失败"，容器未清，第二遍重来）。
		for (int i = 0; i < 2; i++) {
			var txn = flaky.beginTransaction();
			try {
				history.encode0();
				history.writeOnly(table, txn);
				if (i == 0)
					txn.rollback(); // 第一轮回滚
				else
					txn.commit();
			} finally {
				txn.close();
			}
		}
		history.commitDone(); // 仅在提交成功后调用

		var keys = new HashSet<ByteBuffer>();
		table.walk((key, value) -> {
			keys.add(ByteBuffer.Wrap(key));
			return true;
		});
		Assertions.assertEquals(1, keys.size(), "重试不得产生重复行");
		Assertions.assertTrue(keys.contains(encodedKey(serial)), "提交成功后历史行必须存在");
	}

	// ---------------------------------------------------------------
	// 测试用表、值类型与可毒化数据库
	// ---------------------------------------------------------------

	public static final class SimpleBean extends Zeze.Transaction.Bean {
		public long value;

		@Override
		public void encode(Zeze.Serialize.ByteBuffer bb) {
			bb.WriteLong(value);
		}

		@Override
		public void decode(Zeze.Serialize.IByteBuffer bb) {
			value = bb.ReadLong();
		}

		@Override
		public Bean copy() {
			var c = new SimpleBean();
			c.value = value;
			return c;
		}
	}

	/** 最小 TableX 实现（Long key → SimpleBean），非内存表。 */
	public static final class tSimple extends TableX<Long, SimpleBean> {
		public tSimple() {
			super(990751, "UnitTest_TestHistoryFlushCommitBinding_tSimple");
		}

		@Override
		public Class<Long> getKeyClass() {
			return Long.class;
		}

		@Override
		public Class<SimpleBean> getValueClass() {
			return SimpleBean.class;
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
		public SimpleBean newValue() {
			return new SimpleBean();
		}
	}

	/** HashMap 存储的最小 Database：事务 commit 可按静态标志毒化，用于精确注入
	 * 提交阶段失败（磁盘满/RocksDB写失败/多库部分提交等的测试替身）。 */
	static final class FlakyDatabase extends Database {
		static volatile boolean poisonCommit;

		private final TreeMap<String, FlakyTable> tables = new TreeMap<>();

		FlakyDatabase(@NotNull Config.DatabaseConf conf) {
			super(null, conf);
			setDirectOperates(new NullOperates());
		}

		@Override
		public synchronized @NotNull Table openTable(@NotNull String name, int id) {
			return tables.computeIfAbsent(name, FlakyTable::new);
		}

		@Override
		public @NotNull Transaction beginTransaction() {
			return new FlakyTrans();
		}

		final class FlakyTrans implements Transaction {
			// value 为 null 表示 remove（对齐 DatabaseMemory.MemTrans 的 removed 语义）。
			private final HashMap<String, HashMap<ByteBuffer, byte @Nullable []>> batch = new HashMap<>();

			@Override
			public void commit() {
				if (poisonCommit)
					throw new RuntimeException("poison commit");
				synchronized (FlakyDatabase.this) {
					for (var e : batch.entrySet()) {
						var table = (FlakyTable)openTable(e.getKey(), 0);
						for (var r : e.getValue().entrySet()) {
							if (r.getValue() == null)
								table.map.remove(r.getKey());
							else
								table.map.put(r.getKey(), r.getValue());
						}
					}
				}
				batch.clear();
			}

			@Override
			public void rollback() {
				batch.clear();
			}

			@Override
			public void close() {
			}

			private void put(@NotNull String name, @NotNull ByteBuffer key, byte @Nullable [] value) {
				batch.computeIfAbsent(name, __ -> new HashMap<>())
						.put(ByteBuffer.Wrap(key.Copy()), value);
			}

			void replace(@NotNull String name, @NotNull ByteBuffer key, byte @NotNull [] value) {
				put(name, key, value.clone());
			}

			void remove(@NotNull String name, @NotNull ByteBuffer key) {
				put(name, key, null);
			}
		}

		final class FlakyTable extends AbstractKVTable {
			public final String name;
			final TreeMap<ByteBuffer, byte[]> map = new TreeMap<>(ByteBuffer::compareTo);

			FlakyTable(@NotNull String name) {
				this.name = name;
			}

			@Override
			public @NotNull Database getDatabase() {
				return FlakyDatabase.this;
			}

			public @NotNull String getName() {
				return name;
			}

			@Override
			public boolean isNew() {
				return true;
			}

			@Override
			public void close() {
			}

			@Override
			public @Nullable ByteBuffer find(@NotNull ByteBuffer key) {
				byte[] value;
				synchronized (FlakyDatabase.this) {
					value = map.get(key);
				}
				return value != null ? ByteBuffer.Wrap(value.clone()) : null;
			}

			@Override
			public void replace(@NotNull Transaction t, @NotNull ByteBuffer key, @NotNull ByteBuffer value) {
				((FlakyTrans)t).replace(name, key, value.Copy());
			}

			@Override
			public void remove(@NotNull Transaction t, @NotNull ByteBuffer key) {
				((FlakyTrans)t).remove(name, key);
			}

			@Override
			public long walk(@NotNull TableWalkHandleRaw callback) throws Exception {
				java.util.List<ByteBuffer> keys;
				java.util.List<byte[]> values;
				synchronized (FlakyDatabase.this) {
					keys = new java.util.ArrayList<>(map.keySet());
					values = new java.util.ArrayList<>(map.values());
				}
				long count = 0;
				for (int i = 0; i < keys.size(); i++) {
					count++;
					if (!callback.handle(keys.get(i).Copy(), values.get(i).clone()))
						break;
				}
				return count;
			}

			@Override
			public long walkKey(@NotNull TableWalkKeyRaw callback) throws Exception {
				java.util.List<ByteBuffer> keys;
				synchronized (FlakyDatabase.this) {
					keys = new java.util.ArrayList<>(map.keySet());
				}
				long count = 0;
				for (var key : keys) {
					count++;
					if (!callback.handle(key.Copy()))
						break;
				}
				return count;
			}

			@Override
			public long walkDesc(@NotNull TableWalkHandleRaw callback) throws Exception {
				throw new UnsupportedOperationException();
			}

			@Override
			public long walkKeyDesc(@NotNull TableWalkKeyRaw callback) throws Exception {
				throw new UnsupportedOperationException();
			}

			@Override
			public @Nullable ByteBuffer walk(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											 @NotNull TableWalkHandleRaw callback) throws Exception {
				throw new UnsupportedOperationException();
			}

			@Override
			public @Nullable ByteBuffer walkKey(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
												@NotNull TableWalkKeyRaw callback) throws Exception {
				throw new UnsupportedOperationException();
			}

			@Override
			public @Nullable ByteBuffer walkDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
												 @NotNull TableWalkHandleRaw callback) throws Exception {
				throw new UnsupportedOperationException();
			}

			@Override
			public @Nullable ByteBuffer walkKeyDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
													@NotNull TableWalkKeyRaw callback) throws Exception {
				throw new UnsupportedOperationException();
			}
		}
	}
}
