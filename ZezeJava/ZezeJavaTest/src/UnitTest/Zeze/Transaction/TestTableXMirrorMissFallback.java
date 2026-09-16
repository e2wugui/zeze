package UnitTest.Zeze.Transaction;

import java.lang.reflect.Field;
import java.nio.file.Path;

import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.DatabaseRocksDb;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TableX;
import Zeze.Util.FuncLong;

/**
 * FND6-01根因修复验证：本地Rocks镜像写失败不得被吞——记录不允许以clean状态失去/背离镜像备份。
 * 镜像不变式（clean+Share记录 ⇒ 镜像条目存在且==storage真相）是softValue被GC后快路径正确性的
 * 唯一依托：缺失方向把存量记录读成"不存在"（续写覆盖丢数据）；陈旧方向在租约间隙（驱逐/GCM
 * reduce释放后其他进程改storage，本进程驱逐remove与重装载put相继被吞）后返回旧值或复活已删记录。
 * 修复：rocksCachePut/rocksCacheRemove失败抛RuntimeException，装载路径由load既有异常出口作废
 * 记录（cache.remove）并上报——Procedure.call内层catch回滚后返回Procedure.Exception（单次失败，
 * 不自动redo）；TableCache.remove（驱逐与异常清理共用）局部吞+日志。
 *
 * 故障注入：反射替换Application.LocalRocksCacheDb为beginTransaction()抛异常的子类（纯Java异常，
 * 零native风险——不close活库：全活应用下close镜像库的native路径不可靠，且Immediately提交路径
 * 对空记录集也无条件开镜像事务，任何成功提交都会踩到已关闭句柄）。
 * 热可用性不在此断言：Immediately模式任何成功提交（含只读）都经Checkpoint.flush无条件开镜像事务，
 * 镜像故障下成功提交本就halt（finalCommit不可恢复点的既有设计，非本修复范围）——本测试只验
 * 装载路径的"失败可见+记录作废"。
 * Immediately模式保证提交返回即clean+已落storage，__ClearTableCacheUnsafe__安全地制造冷装载条件
 * （数据在storage，仅弃缓存壳）。
 */
@Fast
public class TestTableXMirrorMissFallback {
	// 冷装载·存在记录：storage读成功后rocksCachePut失败——put路径。
	private static final long KEY_EXISTENT = 3L;
	// 冷装载·不存在记录：storage miss后rocksCacheRemove失败——remove路径。
	private static final long KEY_ABSENT = 2L;

	// serverId 决定本地 RocksCache 目录名（zeze_cache_<serverId>），取独立值避免与其他测试冲突。
	private static final int SERVER_ID = 7313;

	@TempDir
	Path tempDir;

	private Application app;
	private tMirror table;

	private void startApp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setCheckpointMode(CheckpointMode.Immediately); // 提交返回即落后台库：记录干净、storage 有值
		config.setServerId(SERVER_ID);
		config.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.RocksDb);
		dbConf.setDatabaseUrl(tempDir.resolve("dbhome").toString());
		config.getDatabaseConfMap().put("", dbConf);

		app = new Application("TestTableXMirrorMissFallback", config);
		table = new tMirror();
		app.addTable("", table);
		app.start();
	}

	private void stopApp() throws Exception {
		app.stop();
		app = null;
		table = null;
	}

	private long insert(long key, long value) {
		return app.newProcedure((FuncLong)() -> {
			var v = new MirrorBean();
			v.value = value;
			table.insert(key, v);
			return 0L;
		}, "TestTableXMirrorMissFallback.put").call();
	}

	private long get(long key, final long[] out) {
		return app.newProcedure((FuncLong)() -> {
			var v = table.get(key);
			if (v != null)
				out[0] = v.value;
			return 0L;
		}, "TestTableXMirrorMissFallback.read").call();
	}

	@Test
	public void testMirrorWriteFailureFailsLoadAndInvalidates() throws Exception {
		startApp();
		// 故障注入：替换镜像库为beginTransaction()抛异常的子类（真实打开一个临时库以通过构造器，
		// 但所有事务入口直接抛出——不触任何native路径）。
		var realMirror = app.getLocalRocksCacheDb();
		var brokenMirror = new BrokenMirrorDb(app, tempDir.resolve("brokenMirror"));
		var field = Application.class.getDeclaredField("LocalRocksCacheDb");
		field.setAccessible(true);
		try {
			// 基线：镜像健康期插入（Immediately：提交返回即clean、storage与镜像均已写）。
			Assertions.assertEquals(Procedure.Success, insert(KEY_EXISTENT, 300));

			// 制造冷装载条件：清缓存（Immediately下记录已clean落库，__ClearTableCacheUnsafe__仅弃缓存壳）。
			table.__ClearTableCacheUnsafe__();
			Assertions.assertEquals(0, table.getCacheSize());

			try {
				field.set(app, brokenMirror);
				// 冷装载·存在记录：storage读成功 → rocksCachePut的beginTransaction失败 → 事务失败
				// + 记录作废出缓存（作废即"不得以clean态失去镜像备份"的落地：记录进不了快路径）。
				Assertions.assertEquals(Procedure.Exception, get(KEY_EXISTENT, new long[1]),
						"rocksCachePut失败必须使装载事务失败（不得吞）");
				Assertions.assertEquals(0, table.getCacheSize(), "失败装载的记录必须被作废出缓存");

				// 冷装载·不存在记录：storage miss → rocksCacheRemove的beginTransaction失败 → 事务失败 + 记录作废。
				Assertions.assertEquals(Procedure.Exception, get(KEY_ABSENT, new long[1]),
						"rocksCacheRemove失败必须使装载事务失败（不得吞）");
				Assertions.assertEquals(0, table.getCacheSize(), "失败装载的记录必须被作废出缓存");
			} finally {
				field.set(app, realMirror); // 恢复真库，stopApp走正常关闭路径
			}
		} finally {
			brokenMirror.close(); // 释放临时库句柄（须在TempDir清理前）
			stopApp();
		}
	}

	/** 镜像故障注入：所有事务入口直接抛出（等价磁盘满的"写全失败"形态，纯Java异常零native风险）。 */
	public static final class BrokenMirrorDb extends DatabaseRocksDb {
		public BrokenMirrorDb(@NotNull Application app, @NotNull Path dir) {
			super(app, brokenConf(dir), true);
		}

		private static @NotNull Config.DatabaseConf brokenConf(@NotNull Path dir) {
			var conf = new Config.DatabaseConf();
			conf.setDatabaseType(Config.DbType.RocksDb);
			conf.setDatabaseUrl(dir.toString());
			return conf;
		}

		@Override
		public @NotNull Transaction beginTransaction() {
			throw new RuntimeException("mirror broken (test injection)");
		}
	}

	// ---------------------------------------------------------------
	// 测试用表和值类型
	// ---------------------------------------------------------------

	public static final class MirrorBean extends Bean {
		public long value;

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteLong(value);
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
			value = bb.ReadLong();
		}

		@Override
		public @NotNull Bean copy() {
			var c = new MirrorBean();
			c.value = value;
			return c;
		}
	}

	/** 最小 TableX 实现（Long key → MirrorBean），非内存表（RocksDb存储）。 */
	public static final class tMirror extends TableX<Long, MirrorBean> {
		public tMirror() {
			super(731301, "UnitTest_TestTableXMirrorMissFallback_tMirror");
		}

		@Override
		public Class<Long> getKeyClass() {
			return Long.class;
		}

		@Override
		public Class<MirrorBean> getValueClass() {
			return MirrorBean.class;
		}

		@Override
		public Long decodeKey(@NotNull ByteBuffer bb) {
			return bb.ReadLong();
		}

		@Override
		public @NotNull ByteBuffer encodeKey(Long key) {
			var bb = ByteBuffer.Allocate(8);
			bb.WriteLong(key);
			return bb;
		}

		@Override
		public Long decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
			return rs.getLong("__key");
		}

		@Override
		public void encodeKeySQLStatement(@NotNull Zeze.Serialize.SQLStatement st, Long key) {
			st.appendLong("__key", key);
		}

		@Override
		public MirrorBean newValue() {
			return new MirrorBean();
		}
	}
}
