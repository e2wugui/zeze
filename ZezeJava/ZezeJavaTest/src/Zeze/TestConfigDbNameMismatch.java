package Zeze;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import Zeze.Config;

/**
 * FND15 svc-02 回归：clearOpenDatabaseFlag对TableConf.DatabaseName引用不存在的DatabaseConf
 * 带名fail-fast（对齐Application.getDatabase判例），而不是裸NPE（运维工具失败不可诊断）。
 */
@Fast
public class TestConfigDbNameMismatch {

	@Test
	public final void testMismatchFailsFastWithDbName() throws Exception {
		var config = new Config();
		var tableConf = new Config.TableConf();
		// TableConf.databaseName为XML解析直赋字段（无setter），测试经反射注入
		var nameField = Config.TableConf.class.getDeclaredField("databaseName");
		nameField.setAccessible(true);
		nameField.set(tableConf, "no-such-database-conf");
		config.setDefaultTableConf(tableConf);
		// 不登记同名DatabaseConf：引用失配
		var ex = Assertions.assertThrows(IllegalStateException.class, config::clearOpenDatabaseFlag,
				"失配必须抛IllegalStateException（修复前是裸NullPointerException）");
		Assertions.assertTrue(ex.getMessage() != null && ex.getMessage().contains("no-such-database-conf"),
				() -> "异常必须带失配的DatabaseName便于诊断，实际: " + ex.getMessage());
	}

	// 守护：引用一致且非MySql（默认Memory）时正常返回不抛。
	@Test
	public final void testMatchingConfPassesThrough() throws Exception {
		var config = new Config();
		var tableConf = new Config.TableConf();
		var nameField = Config.TableConf.class.getDeclaredField("databaseName");
		nameField.setAccessible(true);
		nameField.set(tableConf, "");
		config.setDefaultTableConf(tableConf);
		config.getDatabaseConfMap().putIfAbsent("", new Config.DatabaseConf());
		Assertions.assertDoesNotThrow(config::clearOpenDatabaseFlag);
	}
}
