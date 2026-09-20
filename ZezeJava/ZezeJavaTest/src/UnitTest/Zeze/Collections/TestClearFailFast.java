package UnitTest.Zeze.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Collections.BoolList;
import Zeze.Component.DbWeb;
import Zeze.Config;
import Zeze.Transaction.Procedure;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CO1-F1/CP1-F3 回归（P2）：clearAll（BoolList）与clearTable（DbWeb）原先完全忽略删除
 * 事务的call()返回码——Procedure失败只返码不抛（吞Throwable），失败批被游标跳过后
 * clearAll/clearTable正常返回，数据静默残留。
 * 修复：非0返回码即抛RuntimeException（携带批次数），调用方感知后整体重跑（幂等）即收敛。
 * 失败注入：停机（checkpoint置空）后所有procedure确定性返回Procedure.Closed——
 * "remove first"/删除批返回非0，修复前被静默吞掉、clearAll正常返回。
 */
@Fast
public class TestClearFailFast {

	// 762段：避开200/400/500/700/730/750/760/761段。
	private static final int ServerId = 762;

	private Application app;
	private BoolList.Module boolListModule;
	private DbWeb dbWeb;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(ServerId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("clear_failfast_test_" + ServerId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		app = new Application("TestClearFailFast", conf);
		boolListModule = new BoolList.Module(app);
		dbWeb = new DbWeb();
		dbWeb.Initialize(new AppBase() {
			@Override
			public Application getZeze() {
				return app;
			}
		});
		app.start();
	}

	@AfterEach
	public void tearDown() {
		try {
			app.stop(); // 测试内已stop则幂等再停一次
		} catch (Exception ignored) {
		}
	}

	// CO1-F1：删除事务失败必须抛出（携带语义），不得静默跳过失败批。
	@Test
	public void testBoolListClearAllThrowsOnFailedProcedure() throws Exception {
		var list = boolListModule.open("clearfail_" + System.nanoTime());
		assertEquals(Procedure.Success, TaskSpec.ofProcedure(app.newProcedure(() -> {
			for (int i = 0; i < 100; i++)
				list.set(i);
			return Procedure.Success;
		}, "TestClearFailFast.set")).call());

		var savedCheckpoint = nullCheckpoint(); // 注入：procedure确定性返回Procedure.Closed（非0）
		try {
			var ex = assertThrows(RuntimeException.class, list::clearAll,
					"删除事务失败时clearAll必须抛出（修复前静默吞掉返回码，数据残留且调用方无感知）");
			assertTrue(ex.getMessage().contains("clearAll"),
					"异常消息必须带语义定位，实际: " + ex.getMessage());
		} finally {
			restoreCheckpoint(savedCheckpoint);
		}
	}

	// CP1-F3：clearTable删除批失败必须中止报错，不得流式输出done后静默残留。
	@Test
	public void testDbWebClearTableThrowsOnFailedProcedure() throws Exception {
		var list = boolListModule.open("clearfail2_" + System.nanoTime());
		assertEquals(Procedure.Success, TaskSpec.ofProcedure(app.newProcedure(() -> {
			for (int i = 0; i < 100; i++)
				list.set(i);
			return Procedure.Success;
		}, "TestClearFailFast.setup2")).call());

		// _tBoolList是模块protected表，按表名从app取（类型即生成表）
		var table = (Zeze.Transaction.TableX<?, ?>)app.getTable("Zeze_Builtin_Collections_BoolList_tBoolList");
		assertTrue(table != null, "前置：tBoolList表已注册");

		var savedCheckpoint = nullCheckpoint(); // 注入：删除批procedure确定性返回Procedure.Closed（非0）
		try {
			var ex = assertThrows(RuntimeException.class,
					() -> dbWeb.clearTable(table, null),
					"删除批失败时clearTable必须抛出（修复前被跳过，流式响应仍输出ClearTable done!）");
			assertTrue(ex.getMessage().contains("clearTable"),
					"异常消息必须带语义定位，实际: " + ex.getMessage());
		} finally {
			restoreCheckpoint(savedCheckpoint);
		}
	}

	/**
	 * 失败注入：反射置空checkpoint（app保持started，newProcedure正常），procedure.call()
	 * 在perform入口即返回Procedure.Closed（Transaction.perform:checkpoint==null分支）——
	 * 直接命中"删除事务返回非0"的被测路径（停机注入会在newProcedure处抛异常，达不到此处）。
	 * 返回原值供恢复。
	 */
	private Object nullCheckpoint() throws Exception {
		var field = Application.class.getDeclaredField("checkpoint");
		field.setAccessible(true);
		var saved = field.get(app);
		field.set(app, null);
		return saved;
	}

	private void restoreCheckpoint(Object saved) throws Exception {
		var field = Application.class.getDeclaredField("checkpoint");
		field.setAccessible(true);
		field.set(app, saved);
	}
}
