package Zeze.History;

import harness.Extra;
import java.nio.file.Path;
import Zeze.Application;
import Zeze.Builtin.HistoryModule.tHistory;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DatabaseMemory;
import Zeze.Transaction.Table;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Fast
@Extra
public class TestApplyDatabaseAliasIsolation {
	private static Application newApp(Path businessPath, Path applyPath) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setNoDatabase(true);
		var businessConf = new Config.DatabaseConf();
		businessConf.setDatabaseUrl(businessPath.toString());
		conf.getDatabaseConfMap().put("", businessConf);
		var applyConf = new Config.DatabaseConf();
		applyConf.setName("applyAlias");
		applyConf.setDatabaseUrl(applyPath.toString());
		conf.getDatabaseConfMap().put("applyAlias", applyConf);
		return new Application("TestApplyDatabaseAliasIsolation", conf);
	}

	private static tHistory registerBusinessTable(Application app) throws Exception {
		var table = new tHistory();
		app.addTable("", table);
		var field = Table.class.getDeclaredField("database");
		field.setAccessible(true);
		field.set(table, app.getDatabase(""));
		return table;
	}

	@Test
	public void rejectsDistinctConfigNamesForSamePhysicalDatabase(@TempDir Path tempDir) throws Exception {
		var app = newApp(tempDir, tempDir); // same real Memory bucket despite a different config name
		try {
			var table = registerBusinessTable(app);
			Assertions.assertNotSame(app.getDatabase(""), app.getDatabase("applyAlias"));
			// 前提证明（写读互通）：Memory 后端按 URL 共享静态命名空间，不同配置名同 URL=同一物理桶。
			// 旧断言钉 TableMemory 实例相等；表身份移到 TableData 世代（06e7980a7/72c41d9fc：
			// openTable 每次新句柄、MemTrans 按世代校验）后实例必不相同，物理共享改由行为证明。
			var probeKey = ByteBuffer.Wrap(new byte[] {1});
			var applyTable = (DatabaseMemory.TableMemory)app.getDatabase("applyAlias")
					.openTable(table.getName(), table.getId());
			var businessTable = (DatabaseMemory.TableMemory)app.getDatabase("")
					.openTable(table.getName(), table.getId());
			try (var txn = app.getDatabase("applyAlias").beginTransaction()) {
				applyTable.replace(txn, probeKey, ByteBuffer.Wrap(new byte[] {7}));
				txn.commit();
			}
			var found = businessTable.find(probeKey);
			Assertions.assertNotNull(found,
					"different configuration names must resolve to the same physical table");
			Assertions.assertEquals(7, found.Bytes[found.ReadIndex]);

			Assertions.assertThrows(RuntimeException.class, () -> new ApplyDatabaseZeze(app, "applyAlias"),
					"replay must reject a physical database alias before it can overwrite business tables");
		} finally {
			app.stop();
		}
	}

	@Test
	public void rejectsAliasedDatabaseWhenBusinessTableIsRegisteredLater(@TempDir Path tempDir) throws Exception {
		var app = newApp(tempDir, tempDir);
		try {
			var applied = new ApplyDatabaseZeze(app, "applyAlias");
			var table = registerBusinessTable(app);
			Assertions.assertThrows(RuntimeException.class, () -> applied.open(table.getName()),
					"late business table registration must not bypass physical database isolation");
		} finally {
			app.stop();
		}
	}

	@Test
	public void permitsDifferentPhysicalDatabase(@TempDir Path tempDir) throws Exception {
		var app = newApp(tempDir.resolve("business"), tempDir.resolve("applied"));
		try {
			var table = registerBusinessTable(app);
			var applied = Assertions.assertDoesNotThrow(() -> new ApplyDatabaseZeze(app, "applyAlias"));
			Assertions.assertDoesNotThrow(() -> applied.open(table.getName()));
		} finally {
			app.stop();
		}
	}
}
