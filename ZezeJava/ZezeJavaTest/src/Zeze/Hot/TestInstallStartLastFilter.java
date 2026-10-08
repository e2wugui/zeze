package Zeze.Hot;

import harness.FastServerIds;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Config;
import Zeze.Util.InMemoryJavaCompiler;
import harness.Fast;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND18 hot-01 红绿钉板：install 对 start() 失败的模块不得再执行 startLast()。
 * <p>
 * 修复前：start 失败模块经 stop+stopInternal 完整停机并移出 modules 后，收尾循环仍对
 * 未过滤的 result 调 startLast()——用户模块典型在 startLast 注册全局定时器/监听器，
 * 死对象上静默成功即幻影资源（进程级永久，唯一取消者 StopBefore 永不再被调）。
 * 修复后：startLast 循环按 startErrors 过滤（对齐同函数 addHotModule 形态）。
 * <p>
 * 钉板走 installReadies 全流程（schemas jar 装载 + 双模块 jar 对 + module.config 预检），
 * F1=启动失败模块（startLast 置静态旗标，跨 jar 类装载器经反射断言）、F2=正常模块
 * （证明过滤不过度）。红侧（stash 主码）：F1 旗标=true；绿侧：F1=false 且 F2=true。
 */
@Fast
public class TestInstallStartLastFilter {
	private static final String NS1 = "fnd18hot1.F1";
	private static final String CLASS1 = "fnd18hot1.F1.ModuleF1";
	private static final String NS2 = "fnd18hot1.F2";
	private static final String CLASS2 = "fnd18hot1.F2.ModuleF2";
	private static final String SCHEMAS_CLASS = "fnd18hot1.Schemas";

	private static final String SRC1 = """
			package fnd18hot1.F1;
			public class ModuleF1 implements Zeze.IModule, Zeze.Hot.HotService {
			    public static volatile boolean STARTED_LAST = false;
			    public static final int ModuleId = 18801;
			    @Override public String getFullName() { return "fnd18hot1.F1"; }
			    @Override public String getName() { return "F1"; }
			    @Override public int getId() { return ModuleId; }
			    @Override public void start() throws Exception { throw new RuntimeException("start-fail-by-fnd18hot1"); }
			    @Override public void startLast() throws Exception { STARTED_LAST = true; }
			    @Override public void stop() throws Exception { }
			    @Override public void upgrade(Zeze.Hot.HotService old) throws Exception { }
			}
			""";

	private static final String SRC2 = """
			package fnd18hot1.F2;
			public class ModuleF2 implements Zeze.IModule, Zeze.Hot.HotService {
			    public static volatile boolean STARTED_LAST = false;
			    public static final int ModuleId = 18802;
			    @Override public String getFullName() { return "fnd18hot1.F2"; }
			    @Override public String getName() { return "F2"; }
			    @Override public int getId() { return ModuleId; }
			    @Override public void start() throws Exception { }
			    @Override public void startLast() throws Exception { STARTED_LAST = true; }
			    @Override public void stop() throws Exception { }
			    @Override public void upgrade(Zeze.Hot.HotService old) throws Exception { }
			}
			""";

	private static final String SCHEMAS_SRC = """
			package fnd18hot1;
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
		config.setServerId(FastServerIds.TEST_HOT_INSTALL_START_LAST_FILTER);
		config.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf(); // Memory 库：schemasCompatible 需要 defaultTable 库存在
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl(FastServerIds.URL_TEST_HOT_INSTALL_RESIDUE);
		config.getDatabaseConfMap().put("", dbConf);
		app = new Application("fnd18hot1", config);
		dummyApp = new AppBase() {
			@Override
			public Application getZeze() {
				return app;
			}
		};
		// install 全流程经 zeze.getAppBase() 取 app：构造期该字段为 null，须 initialize 注入。
		app.initialize(dummyApp);
		app.start();
	}

	@AfterAll
	public static void tearDown() throws Exception {
		if (app.getStartState() != Application.StartState.eStopped)
			app.stop();
	}

	@Test
	public void testStartFailedModuleSkippedByStartLast() throws Exception {
		var workingDir = Files.createDirectories(tempDir.resolve("w"));
		Files.createDirectories(workingDir.resolve("modules"));
		Files.createDirectories(workingDir.resolve("interfaces"));
		var distributeDir = Files.createDirectories(tempDir.resolve("dist"));
		var manager = new HotManager(dummyApp, workingDir.toString(), distributeDir.toString());
		// install 不可回滚区的 BeanFactory.resetHot 读 app.getHotManager()（生产由 App 装配，测试注入）。
		app.setHotManager(manager);

		var modulesField = HotManager.class.getDeclaredField("modules");
		modulesField.setAccessible(true);
		@SuppressWarnings("unchecked")
		var modules = (Map<String, HotModule>)modulesField.get(manager);

		ArrayList<HotModule> result = null;
		try {
			var compiler = new InMemoryJavaCompiler();
			compiler.useOptions("-cp", System.getProperty("java.class.path"));
			var bytes1 = compiler.compileAllToByteCode(Map.of(CLASS1, SRC1)).get(CLASS1);
			var bytes2 = compiler.compileAllToByteCode(Map.of(CLASS2, SRC2)).get(CLASS2);
			var schemasBytes = compiler.compileAllToByteCode(Map.of(SCHEMAS_CLASS, SCHEMAS_SRC)).get(SCHEMAS_CLASS);
			Assertions.assertNotNull(bytes1, "F1 模块类必须编译成功");
			Assertions.assertNotNull(bytes2, "F2 模块类必须编译成功");
			Assertions.assertNotNull(schemasBytes, "Schemas 类必须编译成功");

			writeJar(distributeDir.resolve(NS1 + ".interface.jar"), Map.of());
			writeJar(distributeDir.resolve(NS1 + ".jar"), Map.of(
					"fnd18hot1/F1/ModuleF1.class", bytes1,
					"META-INF/module.config", new byte[0])); // install 预检要求条目存在（本用例无 ProviderApp 不解析）
			writeJar(distributeDir.resolve(NS2 + ".interface.jar"), Map.of());
			writeJar(distributeDir.resolve(NS2 + ".jar"), Map.of(
					"fnd18hot1/F2/ModuleF2.class", bytes2,
					"META-INF/module.config", new byte[0]));
			writeJar(distributeDir.resolve("__hot_schemas__fnd18hot1.jar"), Map.of(
					"fnd18hot1/Schemas.class", schemasBytes));

			result = manager.installReadies(false);
			Assertions.assertEquals(2, result.size(), "两个模块都应完成安装流程");

			var flag1 = startLastFlag(result, NS1);
			var flag2 = startLastFlag(result, NS2);
			Assertions.assertFalse(flag1, "start失败的模块已stop+stopInternal完整停机，不得再执行startLast（hot-01：幻影资源）");
			Assertions.assertTrue(flag2, "正常模块的startLast必须执行（过滤不得过度）");
			Assertions.assertFalse(modules.containsKey(NS1), "失败模块必须已移出modules表");
			Assertions.assertTrue(modules.containsKey(NS2), "正常模块应留在modules表");
		} finally {
			// 防御收尾：释放一切 jar 句柄（Windows 下 @TempDir 清理）。
			// 接口 jar 常开在 HotManager.jars（生产语义=进程生命周期），测试须显式释放。
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
			for (var module : modules.values().toArray(new HotModule[0])) {
				modules.remove(module.getName());
				closeQuietly(module);
			}
			if (result != null)
				for (var module : result)
					closeQuietly(module);
		}
	}

	private static boolean startLastFlag(ArrayList<HotModule> result, String namespace) throws Exception {
		for (var module : result) {
			if (module.getName().equals(namespace)) { // HotModule.getName()=命名空间（modules表键）
				// 旗标在 jar 类装载器加载的 Class 上，须从模块类反射读取（跨装载器静态不共享）。
				Field flag = module.getModuleClass().getField("STARTED_LAST");
				return flag.getBoolean(null);
			}
		}
		throw new AssertionError("未找到模块 " + namespace);
	}

	private static void closeQuietly(HotModule module) {
		try {
			module.close();
		} catch (Exception ignored) {
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
