package Zeze.Transaction;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import Zeze.Application;
import Zeze.Config;
import Zeze.Util.FuncLong;

/**
 * History 开启时 gid 取号失败的干净失败语义：取号（Id128 发号服务的阻塞等待）必须发生在
 * 日志应用（commit.run）之前——失败时事务在数据未应用前干净回滚（Closed），历史与数据
 * 同生共死。修复前取号在应用之后：Immediately 模式补刷数据后吞掉异常报假成功，
 * tHistory 永久缺失且 gid 未消费（键空间连空洞都没有，回放端无从感知）。
 * 先以关闭 History 的实例写入基线，再用相同持久化库启动启用 History 的实例。
 * ServiceManager=disable 使取号确定失败；运行期修改配置不能绕过已装配的能力。
 */
@Fast
public class TestHistoryGidFailCleanClosed {
	private static final long KEY = 1L;
	private static final long KEY2 = 2L;

	private static final int SERVER_ID = FastServerIds.TEST_HISTORY_GID_FAIL_CLEAN;

	@TempDir
	Path tempDir;

	private @Nullable Application app;
	private demo.Module1.@Nullable tflush table;

	private void startApp(boolean history) throws Exception {
		var config = new Config();
		config.setServiceManager("disable"); // history 关闭时无 SM 依赖，可正常启动
		config.setTakeoverMode("off");
		config.setCheckpointMode(CheckpointMode.Immediately);
		config.setServerId(SERVER_ID);
		config.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.RocksDb);
		dbConf.setDatabaseUrl(tempDir.resolve("dbhome").toString());
		config.getDatabaseConfMap().put("", dbConf);

		if (history)
			config.setHistory("UnitTest.HistoryGidAllocFail");
		app = new Application("TestHistoryGidFailClean", config);
		table = new demo.Module1.tflush();
		app.addTable("", table);
		app.start();
	}

	private long runInTransaction(FuncLong action) {
		return requireNonNull(app).newProcedure(action, "TestHistoryGidFailClean").call();
	}

	private long insert(long key, long value) {
		var b = new demo.Module1.BValue();
		b.setLong2(value);
		return runInTransaction(() -> {
				requireNonNull(table).insert(key, b);
				return 0L;
			});
	}

	/** 事务内读值；记录不存在返回 -1。 */
	private long readValue(long key) {
		final var out = new long[]{-1};
		var rc = runInTransaction(() -> {
			var v = requireNonNull(table).get(key);
			if (v != null)
				out[0] = v.getLong2();
			return 0L;
		});
		Assertions.assertEquals(Procedure.Success, rc, "读事务不取号，必须成功");
		return out[0];
	}

	@Test
	public void testGidFailFailsCleanBeforeApply() throws Exception {
		startApp(false);
		try {
			// 基线：history 关闭时写事务正常提交
			Assertions.assertEquals(Procedure.Success, insert(KEY, 100), "history 关闭时基线写必须成功");
			Assertions.assertEquals(100, readValue(KEY));

			// 下次启动装配 History，取号通道必失败：业务数据应用前干净回滚。
			requireNonNull(app).stop();
			startApp(true);
			Assertions.assertEquals(Procedure.Closed, insert(KEY2, 200),
					"取号失败必须干净失败（修复前：Immediately 补刷数据后吞异常报假成功 Success）");
			Assertions.assertEquals(-1, readValue(KEY2), "取号失败的事务数据不得应用（未应用即回滚）");
			Assertions.assertEquals(100, readValue(KEY), "此前已提交的数据不受影响");

			// 运行期改配置不能卸载已装配的 History，也不能绕过失败。
			app.getConfig().setHistory("");
			Assertions.assertEquals(Procedure.Closed, insert(KEY2, 200));
			requireNonNull(app).stop();
			startApp(false);
			Assertions.assertEquals(Procedure.Success, insert(KEY2, 200), "history 关闭后写事务恢复成功");
			Assertions.assertEquals(200, readValue(KEY2));
		} finally {
			requireNonNull(app).stop();
		}
	}
}
