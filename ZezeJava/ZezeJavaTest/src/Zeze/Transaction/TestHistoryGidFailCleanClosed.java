package Zeze.Transaction;

import java.nio.file.Path;

import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Procedure;
import Zeze.Util.FuncLong;

/**
 * History 开启时 gid 取号失败的干净失败语义：取号（Id128 发号服务的阻塞等待）必须发生在
 * 日志应用（commit.run）之前——失败时事务在数据未应用前干净回滚（Closed），历史与数据
 * 同生共死。修复前取号在应用之后：Immediately 模式补刷数据后吞掉异常报假成功，
 * tHistory 永久缺失且 gid 未消费（键空间连空洞都没有，回放端无从感知）。
 * 注入形态：运行期翻转 config.setHistory（Infinite.Simulate 同款开关）——开启后
 * ServiceManager=disable 的组合使 getUsableTid128CacheFuture 必失败；关闭后恢复。
 */
@Fast
public class TestHistoryGidFailCleanClosed {
	private static final long KEY = 1L;
	private static final long KEY2 = 2L;

	private static final int SERVER_ID = FastServerIds.TEST_HISTORY_GID_FAIL_CLEAN;

	@TempDir
	Path tempDir;

	private Application app;
	private demo.Module1.tflush table;

	private void startApp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable"); // history 关闭时无 SM 依赖，可正常启动
		config.setCheckpointMode(CheckpointMode.Immediately);
		config.setServerId(SERVER_ID);
		config.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.RocksDb);
		dbConf.setDatabaseUrl(tempDir.resolve("dbhome").toString());
		config.getDatabaseConfMap().put("", dbConf);

		app = new Application("TestHistoryGidFailClean", config);
		table = new demo.Module1.tflush();
		app.addTable("", table);
		app.start();
	}

	private long runInTransaction(FuncLong action) {
		return app.newProcedure(action, "TestHistoryGidFailClean").call();
	}

	private long insert(long key, long value) {
		var b = new demo.Module1.BValue();
		b.setLong2(value);
		return runInTransaction(() -> {
				table.insert(key, b);
				return 0L;
			});
	}

	/** 事务内读值；记录不存在返回 -1。 */
	private long readValue(long key) {
		final var out = new long[]{-1};
		var rc = runInTransaction(() -> {
			var v = table.get(key);
			if (v != null)
				out[0] = v.getLong2();
			return 0L;
		});
		Assertions.assertEquals(Procedure.Success, rc, "读事务不取号，必须成功");
		return out[0];
	}

	@Test
	public void testGidFailFailsCleanBeforeApply() throws Exception {
		startApp();
		try {
			// 基线：history 关闭时写事务正常提交
			Assertions.assertEquals(Procedure.Success, insert(KEY, 100), "history 关闭时基线写必须成功");
			Assertions.assertEquals(100, readValue(KEY));

			// 开启 history 且取号通道必失败：写事务必须干净失败（Closed），数据不得应用
			app.getConfig().setHistory("UnitTest.HistoryGidAllocFail");
			Assertions.assertEquals(Procedure.Closed, insert(KEY2, 200),
					"取号失败必须干净失败（修复前：Immediately 补刷数据后吞异常报假成功 Success）");
			Assertions.assertEquals(-1, readValue(KEY2), "取号失败的事务数据不得应用（未应用即回滚）");
			Assertions.assertEquals(100, readValue(KEY), "此前已提交的数据不受影响");

			// 关闭 history 恢复：写事务恢复成功
			app.getConfig().setHistory("");
			Assertions.assertEquals(Procedure.Success, insert(KEY2, 200), "history 关闭后写事务恢复成功");
			Assertions.assertEquals(200, readValue(KEY2));
		} finally {
			app.stop();
		}
	}
}
