package UnitTest.Zeze.Transaction;

import java.nio.file.Path;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Procedure;
import Zeze.Util.FuncLong;

/**
 * CheckpointMode.Immediately 的持久化承诺：事务提交返回时数据已同步写入后台库。
 * Application.start 每次都会整体删除本地 RocksCache（zeze_cache_*），
 * 因此重启后能读到的数据只能来自后台库——这正是原缺陷（encode0/flush 因 record.dirty
 * 恒 false 被短路，只写内存）的判定标准。
 * 覆盖 增加(insert)、修改(in-place)、删除(remove) 三种提交路径（Record1.flush 的 replace/remove 分支）。
 */
@Fast
public class TestCheckpointImmediately {
	private static final long KEY = 1L;

	// serverId 决定本地 RocksCache 目录名（zeze_cache_<serverId>），取独立值避免与其他测试冲突。
	private static final int SERVER_ID = 7123;

	@TempDir
	Path tempDir;

	private Application app;
	private demo.Module1.tflush table;

	private void startApp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setCheckpointMode(CheckpointMode.Immediately);
		config.setServerId(SERVER_ID);
		config.setDefaultTableConf(new Config.TableConf()); // 裸 Config 不会补默认值；内置模块注册表时需要
		var dbConf = new Config.DatabaseConf(); // name="" 即默认数据库，与 DefaultTableConf.databaseName("") 匹配
		dbConf.setDatabaseType(Config.DbType.RocksDb);
		dbConf.setDatabaseUrl(tempDir.resolve("dbhome").toString());
		config.getDatabaseConfMap().put("", dbConf);

		app = new Application("TestCheckpointImmediately", config);
		table = new demo.Module1.tflush();
		app.addTable("", table);
		app.start();
	}

	private void stopApp() throws Exception {
		app.stop();
		app = null;
		table = null;
	}

	private long runInTransaction(FuncLong action) {
		return app.newProcedure(action, "TestCheckpointImmediately").call();
	}

	/** 事务内读值；记录不存在返回 -1。 */
	private long readValue() {
		final long[] out = {-1};
		var result = runInTransaction(() -> {
			var v = table.get(KEY);
			if (v != null)
				out[0] = v.getLong2();
			return 0L;
		});
		Assertions.assertEquals(Procedure.Success, result, "读取事务必须成功");
		return out[0];
	}

	@Test
	public void testPersistAcrossRestart() throws Exception {
		// phase 1: insert 提交后重启
		startApp();
		var value = new demo.Module1.BValue();
		value.setLong2(100);
		Assertions.assertEquals(Procedure.Success, runInTransaction(() -> {
			table.insert(KEY, value);
			return 0L;
		}));
		Assertions.assertEquals(100, readValue());
		stopApp();

		// phase 2: 重启后数据仍在（只能来自后台库），in-place 修改后重启
		startApp();
		Assertions.assertEquals(100, readValue(), "重启后 insert 的数据必须仍在（原缺陷：静默丢失）");
		Assertions.assertEquals(Procedure.Success, runInTransaction(() -> {
			table.getOrAdd(KEY).setLong2(200);
			return 0L;
		}));
		stopApp();

		// phase 3: 重启后读到修改值，删除后重启
		startApp();
		Assertions.assertEquals(200, readValue(), "重启后 in-place 修改必须生效");
		Assertions.assertEquals(Procedure.Success, runInTransaction(() -> {
			table.remove(KEY);
			return 0L;
		}));
		stopApp();

		// phase 4: 重启后记录不存在
		startApp();
		Assertions.assertEquals(-1, readValue(), "重启后删除必须生效");
		stopApp();
	}
}
