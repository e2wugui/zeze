package Zeze.Hot;

import harness.FastServerIds;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Procedure;
import Zeze.Util.InMemoryJavaCompiler;
import harness.Fast;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND18 hot-03 红绿钉板：安装成功后删 ready 失败不得反转为失败码。
 * <p>
 * 修复前：deleteIfExists 与安装本体同 try，安装已成功（模块已 start、事务已 commit）后
 * 删除抛 IOException 被同一 catch(Throwable) 归类 rc=Procedure.Exception 并走失败清理
 * renameDistributes——!atomicAll 下 setIdle 会把实际成功的安装误报给控制台。
 * 修复后：成功分支 deleteIfExists 单列 try-catch，失败仅记 ERROR，rc 保持 0；
 * ready 残留交给下轮定时器空包路径清理。
 * <p>
 * 钉板构造：完整合法包（模块 jar 对 + module.config + schemas jar + ready）走
 * tryDistribute(false, fromTimer=true) 全流程；删除失败用 Windows 无 share-delete 的
 * 只读句柄锁住 ready 制造（readAllLines 的共享读不受影响）。断言返回值 0（红侧=-1）
 * 且模块真实安装成功（modules 在册 + startLast 旗标经 jar 类装载器反射置位）。
 */
@Fast
public class TestFnd18Hot03TryDistributeSuccessDeleteFail {
	private static final String NS = "fnd18hot3.G1";
	private static final String CLASS = "fnd18hot3.G1.ModuleG1";
	private static final String SCHEMAS_CLASS = "fnd18hot3.Schemas";

	private static final String SRC = """
			package fnd18hot3.G1;
			public class ModuleG1 implements Zeze.IModule, Zeze.Hot.HotService {
			    public static volatile boolean STARTED_LAST = false;
			    public static final int ModuleId = 18803;
			    @Override public String getFullName() { return "fnd18hot3.G1"; }
			    @Override public String getName() { return "G1"; }
			    @Override public int getId() { return ModuleId; }
			    @Override public void start() throws Exception { }
			    @Override public void startLast() throws Exception { STARTED_LAST = true; }
			    @Override public void stop() throws Exception { }
			    @Override public void upgrade(Zeze.Hot.HotService old) throws Exception { }
			}
			""";

	private static final String SCHEMAS_SRC = """
			package fnd18hot3;
			public class Schemas extends Zeze.Schemas {
			    public Schemas() { }
			}
			""";

	@TempDir
	Path tempDir;

	private static AppBase dummyApp;
	private static Application app;

	@BeforeAll
	public static void setUp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setServerId(FastServerIds.TEST_FND18_HOT03_TRY_DISTRIBUTE_SUCCESS_DELETE_FAIL);
		config.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("fnd18hot3_memory");
		config.getDatabaseConfMap().put("", dbConf);
		app = new Application("fnd18hot3", config);
		dummyApp = new AppBase() {
			@Override
			public Application getZeze() {
				return app;
			}
		};
		app.initialize(dummyApp); // install 全流程经 zeze.getAppBase() 取 app，构造期为 null
		app.start();
	}

	@AfterAll
	public static void tearDown() throws Exception {
		if (app.getStartState() != Application.StartState.eStopped)
			app.stop();
	}

	@Test
	public void testSuccessDeleteFailNotMisclassified() throws Exception {
		var workingDir = Files.createDirectories(tempDir.resolve("w"));
		Files.createDirectories(workingDir.resolve("modules"));
		Files.createDirectories(workingDir.resolve("interfaces"));
		var distributeDir = Files.createDirectories(tempDir.resolve("dist"));
		var manager = new HotManager(dummyApp, workingDir.toString(), distributeDir.toString());
		app.setHotManager(manager); // install 不可回滚区的 BeanFactory.resetHot 读 app.getHotManager()

		var modulesField = HotManager.class.getDeclaredField("modules");
		modulesField.setAccessible(true);
		@SuppressWarnings("unchecked")
		var modules = (Map<String, HotModule>)modulesField.get(manager);

		// Windows 经典 io 只读句柄（RandomAccessFile 不带 FILE_SHARE_DELETE，与 NIO FileChannel
		// 不同——后者实测挡不住删除）：readAllLines 共享读不受影响、DeleteFile 因句柄无
		// share-delete 返回 ACCESS_DENIED；dos:readonly 也不可用（Files.delete 会清属性重试）。
		var ready = distributeDir.resolve("ready");
		Files.writeString(ready, "");
		var readyLock = new java.io.RandomAccessFile(ready.toFile(), "r");
		try {
			var compiler = new InMemoryJavaCompiler();
			compiler.useOptions("-cp", System.getProperty("java.class.path"));
			var moduleBytes = compiler.compileAllToByteCode(Map.of(CLASS, SRC)).get(CLASS);
			var schemasBytes = compiler.compileAllToByteCode(Map.of(SCHEMAS_CLASS, SCHEMAS_SRC)).get(SCHEMAS_CLASS);
			Assertions.assertNotNull(moduleBytes, "模块类必须编译成功");
			Assertions.assertNotNull(schemasBytes, "Schemas 类必须编译成功");
			writeJar(distributeDir.resolve(NS + ".interface.jar"), Map.of());
			writeJar(distributeDir.resolve(NS + ".jar"), Map.of(
					"fnd18hot3/G1/ModuleG1.class", moduleBytes,
					"META-INF/module.config", new byte[0]));
			writeJar(distributeDir.resolve("__hot_schemas__fnd18hot3.jar"), Map.of(
					"fnd18hot3/Schemas.class", schemasBytes));

			var rc = manager.tryDistribute(false, true);
			Assertions.assertEquals(0L, rc,
					"安装成功但删ready失败不得反转为失败码（hot-03：!atomicAll下setIdle会把rc误报给控制台）");

			Assertions.assertTrue(modules.containsKey(NS), "模块必须真实安装成功在册");
			var module = modules.get(NS);
			var flag = module.getModuleClass().getField("STARTED_LAST");
			Assertions.assertTrue(flag.getBoolean(null), "正常模块的startLast必须已执行（证明安装真实成功）");
		} finally {
			readyLock.close(); // 释放句柄，ready 残留交给 @TempDir 清理
			// 防御收尾：释放一切 jar 句柄（Windows 下 @TempDir 清理）。
			var jarsField = HotManager.class.getDeclaredField("jars");
			jarsField.setAccessible(true);
			@SuppressWarnings("unchecked")
			var jars = (Map<java.io.File, java.util.jar.JarFile>)jarsField.get(manager);
			for (var jar : jars.values()) {
				try {
					jar.close();
				} catch (Exception ignored) {
				}
			}
			jars.clear();
			for (var m : modules.values().toArray(new HotModule[0])) {
				modules.remove(m.getName());
				try {
					m.close();
				} catch (Exception ignored) {
				}
			}
		}
	}

	private static void writeJar(Path path, Map<String, byte[]> entries) throws Exception {
		try (var jos = new JarOutputStream(Files.newOutputStream(path))) {
			for (var e : entries.entrySet()) {
				jos.putNextEntry(new ZipEntry(e.getKey()));
				jos.write(e.getValue());
			}
		}
	}
}
