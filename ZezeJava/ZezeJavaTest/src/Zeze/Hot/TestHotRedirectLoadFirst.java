package Zeze.Hot;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.ProviderApp;
import Zeze.Config;
import Zeze.Util.InMemoryJavaCompiler;
import harness.Fast;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * Hot模块redirect子类装载优先（S1）+ 冷抢载守卫（S4）回归。
 * <p>
 * 热模块的Redirect_子类应直接从模块jar装载（Distribute.pack产出，免运行时编译）：
 * 实例的getClassLoader必须是装载它的HotModule；jar未打包子类时兜底内存编译，
 * 换代后必须重新生成且新代码生效。冷classpath同名类不得抢先——构造器fail-fast。
 * <p>
 * 生产等价物：workingDir/modules下的jar经HotManager构造器装载注册（loadExistModules），
 * 编译装载器parent为manager.getHotRedirect()（=initialize的接线），-cp含模块class目录。
 */
@Fast
@Isolated // GenModule.instance（compiler/genClassMap）与BeanFactory.zeze是JVM级静态，独占运行
public class TestHotRedirectLoadFirst {
	private static final String NS = "a1hot.Ahot1";
	private static final String MODULE_CLASS = "a1hot.Ahot1.ModuleAhot1";
	private static final String REDIRECT_CLASS = "Redirect_a1hot_Ahot1_ModuleAhot1";

	private static final String MODULE_SRC = """
			package a1hot.Ahot1;
			public class ModuleAhot1 implements Zeze.IModule {
			    public static final int ModuleId = 18701;
			    public static final String ModuleFullName = "a1hot.Ahot1";
			    public static String marker;
			    public ModuleAhot1(Zeze.AppBase app) {
			    }
			    @Override
			    public String getFullName() {
			        return ModuleFullName;
			    }
			    @Override
			    public String getName() {
			        return "Ahot1";
			    }
			    @Override
			    public int getId() {
			        return ModuleId;
			    }
			    @Zeze.Arch.RedirectToServer
			    @Zeze.Util.TransactionLevelAnnotation(Level = Zeze.Transaction.TransactionLevel.None)
			    public void ping(int serverId) {
			        marker = "{marker}";
			    }
			}
			""";

	// 打包子类只须extends模块类+可构造：装载优先路径不校验方法拦截内容，只校验身份。
	private static final String PACKAGED_REDIRECT_SRC = """
			public class Redirect_a1hot_Ahot1_ModuleAhot1 extends a1hot.Ahot1.ModuleAhot1 {
			    public Redirect_a1hot_Ahot1_ModuleAhot1(Zeze.AppBase _app_) {
			        super(_app_);
			    }
			}
			""";

	@TempDir
	Path tempDir;

	// HotModule惰性持有jar句柄，Windows下不close则@TempDir清理失败。
	private final ArrayList<HotManager> managers = new ArrayList<>();

	private static Application app;
	private static AppBase dummyApp;
	private static Object savedCompilerOptions;
	private static ClassLoader savedCompilerLoader;
	private static Object savedBeanFactoryApp;

	@BeforeAll
	public static void setUp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		app = new Application("a1hot", config);
		new ProviderApp(app); // 哑构造：设置app.redirect（生成子类构造器引用）
		dummyApp = new AppBase() {
			@Override
			public @Nullable Application getZeze() {
				return app;
			}
		};

		var compiler = GenModule.instance.getCompiler();
		savedCompilerOptions = readCompilerField(compiler, "options");
		savedCompilerLoader = compiler.getClassloader();
		// HotManager构造会写BeanFactory静态，测试后恢复
		var field = Zeze.Collections.BeanFactory.class.getDeclaredField("zeze");
		field.setAccessible(true);
		savedBeanFactoryApp = field.get(null);
	}

	@AfterAll
	public static void tearDown() throws Exception {
		var compiler = GenModule.instance.getCompiler();
		writeCompilerField(compiler, "options", savedCompilerOptions);
		writeCompilerField(compiler, "classLoader", savedCompilerLoader);
		var field = Zeze.Collections.BeanFactory.class.getDeclaredField("zeze");
		field.setAccessible(true);
		field.set(null, savedBeanFactoryApp);
	}

	@AfterEach
	public void closeModules() throws Exception {
		for (var manager : managers) {
			var hotModule = manager.findHotModule(NS);
			if (null != hotModule)
				hotModule.close();
		}
		managers.clear();
	}

	/** 模块jar里打包了Redirect_子类：直接装载、免编译，实例classLoader就是HotModule。 */
	@Test
	public void testLoadFirstFromModuleJar() throws Exception {
		var cpDir = tempDir.resolve("cp");
		var moduleBytes = compileModule("v1", cpDir);
		var redirectBytes = compilePackagedRedirect(cpDir);
		var workingDir = tempDir.resolve("work1");
		writeModuleJar(workingDir, Map.of(
				"a1hot/Ahot1/ModuleAhot1.class", moduleBytes,
				REDIRECT_CLASS + ".class", redirectBytes));

		var manager = newHotManager(workingDir);
		var hotModule = manager.findHotModule(NS);
		Assertions.assertNotNull(hotModule, "HotManager构造后应注册模块");
		var moduleClass = hotModule.getModuleClass();
		Assertions.assertSame(hotModule, moduleClass.getClassLoader(), "模块类必须由HotModule装载");

		var modules = GenModule.instance.createRedirectModules(dummyApp, new Class<?>[]{moduleClass});
		Assertions.assertNotNull(modules);
		var service = modules[0];
		Assertions.assertEquals(REDIRECT_CLASS, service.getClass().getName());
		Assertions.assertSame(hotModule, service.getClass().getClassLoader(),
				"装载优先：子类来自模块jar（编译兜底会落在编译器装载器上）");
		Assertions.assertSame(moduleClass, service.getClass().getSuperclass(), "同jar构建产物身份自洽");

		var serverId = (int)app.getConfig().getServerId();
		service.getClass().getMethod("ping", int.class).invoke(service, serverId);
		Assertions.assertEquals("v1", moduleClass.getField("marker").get(null), "服务实例必须可执行");
	}

	/** 模块jar未打包子类：兜底内存编译；换代后必须重新生成，新代码生效、旧类不再复用。 */
	@Test
	public void testFallbackCompileOnUpgrade() throws Exception {
		var cpDir1 = tempDir.resolve("cp1");
		var v1Bytes = compileModule("v1", cpDir1);
		var workingDir = tempDir.resolve("work2");
		writeModuleJar(workingDir, Map.of("a1hot/Ahot1/ModuleAhot1.class", v1Bytes));
		var manager = newHotManager(workingDir);
		var v1 = manager.findHotModule(NS).getModuleClass();

		var cpDir2 = tempDir.resolve("cp2");
		var v2Bytes = compileModule("v2", cpDir2);
		var workingDir2 = tempDir.resolve("work3");
		writeModuleJar(workingDir2, Map.of("a1hot/Ahot1/ModuleAhot1.class", v2Bytes));
		var manager2 = newHotManager(workingDir2);
		var hotModule2 = manager2.findHotModule(NS);
		var v2 = hotModule2.getModuleClass();
		Assertions.assertNotSame(v1, v2, "测试前提：同名类不同装载器，Class身份不同");

		var compiler = GenModule.instance.getCompiler();
		compiler.useParentClassLoader(manager2.getHotRedirect()); // = initialize的编译装载器接线
		compiler.useOptions("-cp", cpDir2 + File.pathSeparator + System.getProperty("java.class.path"));

		var modules = GenModule.instance.createRedirectModules(dummyApp, new Class<?>[]{v2});
		Assertions.assertNotNull(modules);
		var service = modules[0];
		Assertions.assertEquals(REDIRECT_CLASS, service.getClass().getName());
		Assertions.assertSame(hotModule2, service.getClass().getClassLoader(),
				"兜底编译产物必须define进HotModule（每代隔离），不落共享编译器装载器");
		Assertions.assertSame(v2, service.getClass().getSuperclass(),
				"兜底编译的生成类必须extends当前代模块类");

		var serverId = (int)app.getConfig().getServerId();
		service.getClass().getMethod("ping", int.class).invoke(service, serverId);
		Assertions.assertEquals("v2", v2.getField("marker").get(null),
				"回环本地执行（choiceServer为null走super）必须执行新版本实现");
		Assertions.assertNull(v1.getField("marker").get(null), "旧版本实现不得再被执行");
	}

	/** 冷classpath上存在模块类时（双亲委派可抢先）：HotModule构造必须fail-fast。 */
	@Test
	public void testColdShadowGuard() throws Exception {
		var workingDir = tempDir.resolve("work4");
		var modulesDir = Files.createDirectories(workingDir.resolve("modules"));
		Files.createDirectories(workingDir.resolve("interfaces"));
		// jar不含模块类条目：loadClass沿双亲委派命中test classpath上的a1hot.Guard.ModuleGuard
		try (var jos = new JarOutputStream(Files.newOutputStream(modulesDir.resolve("a1hot.Guard.jar")))) {
			jos.putNextEntry(new ZipEntry("a1hot/Guard/Ignore.class"));
			jos.write(new byte[]{0});
		}
		var distributeDir = Files.createDirectories(tempDir.resolve("dist4"));
		Assertions.assertThrows(IllegalStateException.class,
				() -> new HotManager(dummyApp, workingDir.toString(), distributeDir.toString()),
				"冷抢载必须构造期显式失败，不允许静默失效");
	}

	private HotManager newHotManager(Path workingDir) throws Exception {
		Files.createDirectories(workingDir.resolve("modules"));
		Files.createDirectories(workingDir.resolve("interfaces"));
		var distributeDir = Files.createDirectories(tempDir.resolve("dist" + System.nanoTime()));
		var manager = new HotManager(dummyApp, workingDir.toString(), distributeDir.toString());
		managers.add(manager);
		return manager;
	}

	private void writeModuleJar(Path workingDir, Map<String, byte[]> entries) throws Exception {
		var modulesDir = Files.createDirectories(workingDir.resolve("modules"));
		Files.createDirectories(workingDir.resolve("interfaces"));
		try (var jos = new JarOutputStream(Files.newOutputStream(modulesDir.resolve(NS + ".jar")))) {
			for (var e : entries.entrySet()) {
				jos.putNextEntry(new ZipEntry(e.getKey()));
				jos.write(e.getValue());
			}
		}
	}

	/** 编译模块类源码并把class落cpDir（生产等价物：buildCp含模块jar），返回字节码。 */
	private static byte[] compileModule(String marker, Path cpDir) throws Exception {
		var src = MODULE_SRC.replace("{marker}", marker);
		var compiler = new InMemoryJavaCompiler();
		compiler.useOptions("-cp", System.getProperty("java.class.path"));
		var bytes = compiler.compileAllToByteCode(Map.of(MODULE_CLASS, src)).get(MODULE_CLASS);
		var classFile = cpDir.resolve("a1hot/Ahot1/ModuleAhot1.class");
		Files.createDirectories(classFile.getParent());
		Files.write(classFile, bytes);
		return bytes;
	}

	private static byte[] compilePackagedRedirect(Path moduleCpDir) throws Exception {
		var compiler = new InMemoryJavaCompiler();
		compiler.useOptions("-cp", moduleCpDir + File.pathSeparator + System.getProperty("java.class.path"));
		return compiler.compileAllToByteCode(Map.of(REDIRECT_CLASS, PACKAGED_REDIRECT_SRC)).get(REDIRECT_CLASS);
	}

	private static Object readCompilerField(InMemoryJavaCompiler compiler, String name) throws Exception {
		var field = InMemoryJavaCompiler.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(compiler);
	}

	private static void writeCompilerField(InMemoryJavaCompiler compiler, String name, Object value) throws Exception {
		var field = InMemoryJavaCompiler.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(compiler, value);
	}
}
