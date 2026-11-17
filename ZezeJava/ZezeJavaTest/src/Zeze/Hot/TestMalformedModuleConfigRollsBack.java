package Zeze.Hot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Config;
import Zeze.Transaction.Checkpoint;
import Zeze.Transaction.CheckpointMode;
import Zeze.Util.InMemoryJavaCompiler;
import harness.Fast;
import harness.ForkedJvm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
@ForkedJvm
public class TestMalformedModuleConfigRollsBack {
	@TempDir
	Path directory;

	@Test
	void truncatedConfigRejectsPackageBeforeIrreversibleCommit() throws Exception {
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		config.setDefaultTableConf(new Config.TableConf());
		var db = new Config.DatabaseConf();
		db.setDatabaseUrl("configRollback-" + java.util.UUID.randomUUID());
		config.getDatabaseConfMap().put("", db);
		var app = new Application("configRollback", config);
		// NoDatabase 不创建 Checkpoint；注入未启动的 Table 模式实例，不产生缓存目录或线程。
		var checkpoint = Application.class.getDeclaredField("checkpoint");
		checkpoint.setAccessible(true);
		checkpoint.set(app, new Checkpoint(app, CheckpointMode.Table, 0));
		new ProviderApp(app); // 发布工具同款最小 Provider，用于触发配置消费路径。
		var base = new AppBase() {
			@Override public Application getZeze() { return app; }
		};
		app.initialize(base);
		var working = Files.createDirectories(directory.resolve("work"));
		Files.createDirectories(working.resolve("interfaces"));
		Files.createDirectories(working.resolve("modules"));
		var staging = Files.createDirectories(directory.resolve("staging"));
		var manager = new HotManager(base, working.toString(), staging.toString());
		app.setHotManager(manager);
		var compiler = new InMemoryJavaCompiler();
		compiler.useOptions("-cp", System.getProperty("java.class.path"));
		var classes = compiler.compileAllToByteCode(Map.of(
				"configRollback.Entry.ModuleEntry", """
						package configRollback.Entry;
						public class ModuleEntry implements Zeze.IModule, Zeze.Hot.HotService {
						    public static final int ModuleId = 18910;
						    public String getFullName() { return "configRollback.Entry"; }
						    public String getName() { return "Entry"; }
						    public int getId() { return ModuleId; }
						    public void start() { }
						    public void stop() { }
						    public void upgrade(Zeze.Hot.HotService old) { }
						}
						""",
				"configRollback.Schemas", """
						package configRollback;
						public class Schemas extends Zeze.Schemas { }
						"""));
		writeJar(staging.resolve("configRollback.Entry.interface.jar"), Map.of());
		writeJar(staging.resolve("configRollback.Entry.jar"), Map.of(
				"configRollback/Entry/ModuleEntry.class", classes.get("configRollback.Entry.ModuleEntry"),
				HotModule.eModuleConfigName, new byte[0])); // 有条目但没有终止标签，decode 必失败。
		writeJar(staging.resolve("__hot_schemas__configRollback.jar"), Map.of(
				"configRollback/Schemas.class", classes.get("configRollback.Schemas")));
		try {
			assertNull(manager.installReadies(false));
			assertNull(manager.findHotModule("configRollback.Entry.ModuleEntry"));
			assertTrue(Files.exists(staging.resolve("configRollback.Entry.jar")));
			assertTrue(Files.exists(staging.resolve("configRollback.Entry.interface.jar")));
			assertFalse(Files.exists(working.resolve("modules/configRollback.Entry.jar")));
		} finally {
			var jarsField = HotManager.class.getDeclaredField("jars");
			jarsField.setAccessible(true);
			for (var jar : ((Map<?, ?>)jarsField.get(manager)).values())
				((JarFile)jar).close();
			manager.stop();
			app.stop();
		}
	}

	private static void writeJar(Path path, Map<String, byte[]> entries) throws Exception {
		try (var jar = new JarOutputStream(Files.newOutputStream(path))) {
			for (var entry : entries.entrySet()) {
				jar.putNextEntry(new ZipEntry(entry.getKey()));
				jar.write(entry.getValue());
			}
		}
	}
}
