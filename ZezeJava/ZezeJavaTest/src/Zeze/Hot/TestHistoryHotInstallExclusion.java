package Zeze.Hot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Util.InMemoryJavaCompiler;
import harness.Fast;
import harness.FastServerIds;
import harness.ForkedJvm;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
@ForkedJvm
public class TestHistoryHotInstallExclusion {
	private static final String NAMESPACE = "hothistory.Worker";
	private static final String MODULE_CLASS = NAMESPACE + ".ModuleWorker";
	private static final String MODULE_SOURCE = """
			package hothistory.Worker;
			public class ModuleWorker implements Zeze.IModule, Zeze.Hot.HotService {
			    public boolean running;
			    public int stopBeforeCalls;
			    @Override public String getFullName() { return "hothistory.Worker"; }
			    @Override public String getName() { return "Worker"; }
			    @Override public int getId() { return 18804; }
			    @Override public void start() { running = true; }
			    @Override public void stopBefore() { ++stopBeforeCalls; running = false; }
			    @Override public void stop() { running = false; }
			    @Override public void upgrade(Zeze.Hot.HotService old) { }
			}
			""";

	@Test
	public void historyInAnotherApplicationKeepsOldModuleRunning() throws Exception {
		var historyApp = newApp(true, FastServerIds.TEST_HISTORY_HOT_INSTALL_EXCLUSION);
		var hotApp = newApp(false, FastServerIds.TEST_HISTORY_HOT_INSTALL_EXCLUSION + 1);
		try {
			historyApp.start();
			var appBase = new AppBase() {
				@Override
				public Application getZeze() {
					return hotApp;
				}
			};
			hotApp.initialize(appBase);
			hotApp.start();
			var working = Path.of("working");
			var modules = Files.createDirectories(working.resolve("modules"));
			var distribute = Files.createDirectories(Path.of("distribute"));
			prepareModuleJars(modules, distribute);
			var manager = new HotManager(appBase, working.toString(), distribute.toString());
			hotApp.setHotManager(manager);
			try (var oldModule = requireNonNull(manager.findHotModule(NAMESPACE))) {
				var type = requireNonNull(oldModule.getModuleClass());
				var service = (IModule)type.getConstructor().newInstance();
				oldModule.setService(service);
				oldModule.start();
				try {
					assertRejected(manager, oldModule, service, type);
					historyApp.stop();
					assertRejected(manager, oldModule, service, type);
				} finally {
					oldModule.stop();
				}
			} finally {
				manager.stop();
			}
		} finally {
			try {
				hotApp.stop();
			} finally {
				historyApp.stop();
			}
		}
	}

	private static void assertRejected(HotManager manager, HotModule oldModule, IModule service, Class<?> type)
			throws Exception {
		assertNull(manager.installReadies(false));
		assertSame(oldModule, manager.findHotModule(NAMESPACE));
		assertTrue(type.getField("running").getBoolean(service));
		assertEquals(0, type.getField("stopBeforeCalls").getInt(service));
	}

	private static Application newApp(boolean history, int serverId) throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setTakeoverMode("off");
		config.setServerId(serverId);
		config.setDefaultTableConf(new Config.TableConf());
		var db = new Config.DatabaseConf();
		db.setDatabaseUrl(FastServerIds.URL_TEST_HISTORY_HOT_INSTALL_EXCLUSION);
		config.getDatabaseConfMap().put("", db);
		if (history)
			config.setHistory("HistoryHotInstallExclusion");
		return new Application("historyHotInstallExclusion" + serverId, config);
	}

	private static void prepareModuleJars(Path modules, Path distribute) throws Exception {
		var compiler = new InMemoryJavaCompiler();
		compiler.useOptions("-cp", System.getProperty("java.class.path"));
		var bytes = requireNonNull(compiler.compileAllToByteCode(Map.of(MODULE_CLASS, MODULE_SOURCE)).get(MODULE_CLASS));
		var oldJar = modules.resolve(NAMESPACE + ".jar");
		try (var jar = new JarOutputStream(Files.newOutputStream(oldJar))) {
			jar.putNextEntry(new ZipEntry(MODULE_CLASS.replace('.', '/') + ".class"));
			jar.write(bytes);
		}
		Files.copy(oldJar, distribute.resolve(NAMESPACE + ".jar"));
		try (var jar = new JarOutputStream(Files.newOutputStream(distribute.resolve(NAMESPACE + ".interface.jar")))) {
			jar.finish();
		}
	}
}
