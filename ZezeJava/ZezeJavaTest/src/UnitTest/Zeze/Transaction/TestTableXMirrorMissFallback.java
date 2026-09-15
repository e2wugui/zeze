package UnitTest.Zeze.Transaction;

import java.lang.reflect.Method;
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
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Record;
import Zeze.Transaction.Record1;
import Zeze.Transaction.TableX;
import Zeze.Util.FuncLong;

/**
 * FND6-01：本地 Rocks 镜像不变式（镜像 ⊇ 干净缓存值）被打破（rocksCachePut 写失败被吞、
 * 镜像库损坏）后，softValue 被 GC 的干净记录（Share/Modify + clean）唯一恢复源只剩后台库。
 * 原缺陷：load 快路径与 Record1.loadValue 镜像 miss 不回退 storage，直接返回 null——
 * 存量记录被读成"不存在"，业务据此插入新值提交会覆盖丢数据。
 * 修复：镜像 miss 且非内存表时穿透读后台库一次，回填 softValue 并自愈镜像。
 * 测试以「删除镜像条目 + 反射清空 softValue」模拟该状态（GC 不可强制，等价于软引用被回收）。
 */
@Fast
public class TestTableXMirrorMissFallback {
	private static final long KEY = 1L;

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

	private long readValue() {
		final long[] out = {-1};
		var result = app.newProcedure((FuncLong)() -> {
			var v = table.get(KEY);
			if (v != null)
				out[0] = v.value;
			return 0L;
		}, "TestTableXMirrorMissFallback.read").call();
		Assertions.assertEquals(Procedure.Success, result, "读取事务必须成功");
		return out[0];
	}

	@Test
	public void testMirrorMissFallsBackToStorage() throws Exception {
		startApp();
		try {
			var value = new MirrorBean();
			value.value = 100;
			Assertions.assertEquals(Procedure.Success, app.newProcedure((FuncLong)() -> {
				table.insert(KEY, value);
				return 0L;
			}, "TestTableXMirrorMissFallback.put").call());
			Assertions.assertEquals(100, readValue(), "基线：正常装载后可读");

			// 前置成立：镜像有条目、softValue 在。
			Assertions.assertTrue(table.getLocalRocksCacheTable().containsKey(table, KEY), "镜像应已写入");

			// 模拟不变式被打破 + softValue 被 GC：
			// 1) 删除镜像条目（等价于当初 rocksCachePut 写失败未落镜像）。
			try (var txn = app.getLocalRocksCacheDb().beginTransaction()) {
				table.getLocalRocksCacheTable().remove(txn, table.encodeKey(KEY));
				txn.commit();
			}
			Assertions.assertFalse(table.getLocalRocksCacheTable().containsKey(table, KEY), "镜像条目应已删除");
			// 2) 清空 softValue（Record.setSoftValue 包私有，反射模拟软引用回收）。
			Record1<Long, MirrorBean> r = table.getCache().getOrAdd(KEY, () -> new Record1<>(table, KEY, null));
			Method setSoftValue = Record.class.getDeclaredMethod("setSoftValue", Bean.class);
			setSoftValue.setAccessible(true);
			setSoftValue.invoke(r, (Object)null);

			// 记录为 Share + clean + softValue=null + 镜像 miss：修复前此处读到 null（-1）。
			Assertions.assertEquals(100, readValue(), "镜像 miss 必须回退后台库，存量记录不得读成不存在");

			// 自愈：穿透读后镜像应已回填。
			Assertions.assertTrue(table.getLocalRocksCacheTable().containsKey(table, KEY), "镜像应被自愈回填");
		} finally {
			stopApp();
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
