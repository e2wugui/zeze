package Zeze.Arch.Gen;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Config;
import Zeze.Util.InMemoryJavaCompiler;
import harness.Fast;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * FND8-80回归：genClassMap按生成类名缓存，缓存命中必须强校验
 * genClass.getSuperclass()==moduleClass，否则同名不同装载器的模块类（热更换代形态）
 * 会按名复用旧生成类（extends旧模块类），新代码静默不生效。
 * <p>
 * 热更换代场景已由RedirectClassSink装载优先结构性消除（产物装载/define进各HotModule，
 * 回归见TestHotRedirectLoadFirst）；本类守卫冷路径：身份校验命中陈旧父类即fail-fast，
 * 同Class重复调用命中缓存必须复用。
 * <p>
 * 复现模拟生产结构：模块类字节码defineClass到独立子装载器（=HotModule v1/v2），
 * 编译装载器parent为HotRedirect式横向委托装载器，javac用-cp指向模块class临时目录。
 */
@Fast
@Isolated // GenModule.instance（genClassMap/compiler）是JVM级单例，独占运行
public class TestFnd880GenModuleCacheInvalidOnUpgrade {

	/** 模拟HotRedirect：模块包名横向转发给"当前HotModule装载器"，其余按标准路线走双亲。 */
	private static final class HotLikeRedirectLoader extends ClassLoader {
		volatile ClassLoader currentModuleLoader;

		HotLikeRedirectLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (name.startsWith("a7fnd880.") && currentModuleLoader != null)
				return currentModuleLoader.loadClass(name);
			return super.loadClass(name, resolve);
		}
	}

	/** 模拟HotModule：把模块类字节码defineClass到独立子装载器（同名类身份即不同）。 */
	private static final class HotLikeModuleLoader extends ClassLoader {
		private final String className;
		private final byte[] bytes;

		HotLikeModuleLoader(ClassLoader parent, String className, byte[] bytes) {
			super(parent);
			this.className = className;
			this.bytes = bytes;
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			if (!name.equals(className))
				throw new ClassNotFoundException(name);
			return defineClass(name, bytes, 0, bytes.length);
		}
	}

	private static final String MODULE_SRC = """
			package a7fnd880;
			public class {simple} implements Zeze.IModule {
			    public static final int ModuleId = 8801;
			    public static final String ModuleFullName = "a7fnd880.{simple}";
			    public {simple}(Zeze.AppBase app) {
			    }
			    @Override
			    public String getFullName() {
			        return ModuleFullName;
			    }
			    @Override
			    public String getName() {
			        return "{simple}";
			    }
			    @Override
			    public int getId() {
			        return ModuleId;
			    }
			    @Zeze.Arch.RedirectToServer
			    @Zeze.Util.TransactionLevelAnnotation(Level = Zeze.Transaction.TransactionLevel.None)
			    public void ping(int serverId) {
			    }
			}
			""";

	@TempDir
	Path tempDir;

	private static Application app;
	private static AppBase dummyApp;
	private static HotLikeRedirectLoader hotLike;
	private static Object savedCompilerOptions;
	private static ClassLoader savedCompilerLoader;

	@BeforeAll
	public static void setUp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		app = new Application("a7fnd880", config);
		new ProviderApp(app); // 哑构造：设置app.redirect（RedirectBase），fake服务不启动
		dummyApp = new AppBase() {
			@Override
			public @Nullable Application getZeze() {
				return app;
			}
		};
		hotLike = new HotLikeRedirectLoader(TestFnd880GenModuleCacheInvalidOnUpgrade.class.getClassLoader());

		// 模拟HotManager.initialize一次性设置编译装载器parent；全局状态测试后恢复
		var compiler = GenModule.instance.getCompiler();
		savedCompilerOptions = readCompilerField(compiler, "options");
		savedCompilerLoader = compiler.getClassloader();
		compiler.useParentClassLoader(hotLike);
	}

	@AfterAll
	public static void tearDown() throws Exception {
		var compiler = GenModule.instance.getCompiler();
		writeCompilerField(compiler, "options", savedCompilerOptions);
		writeCompilerField(compiler, "classLoader", savedCompilerLoader);
	}

	/**
	 * 冷缓存陈旧命中（同名模块类换了装载器，热更换代形态）：不变式破坏必须fail-fast。
	 * 热更换代的正常路径（重生成+新代码生效）由sink结构性保证，见TestHotRedirectLoadFirst。
	 */
	@Test
	public void testStaleColdCacheFailsFast() throws Exception {
		var v1 = compileAndLoadModule("HotModuleUp", tempDir.resolve("cp1"));
		var m1 = GenModule.instance.createRedirectModules(dummyApp, new Class<?>[]{v1});
		Assertions.assertNotNull(m1);
		Assertions.assertSame(v1, m1[0].getClass().getSuperclass(), "首装生成类必须extends传入模块类");

		var v2 = compileAndLoadModule("HotModuleUp", tempDir.resolve("cp2"));
		Assertions.assertNotSame(v1, v2, "测试前提：同名类不同装载器，Class身份不同");
		Assertions.assertThrows(IllegalStateException.class,
				() -> GenModule.instance.createRedirectModules(dummyApp, new Class<?>[]{v2}),
				"冷缓存命中陈旧父类必须显式失败，不得静默复用旧生成类");
	}

	/** 冷缓存护栏：同一模块Class重复调用，身份校验通过，生成类必须复用（不误伤冷路径）。 */
	@Test
	public void testColdCacheHitReused() throws Exception {
		var v = compileAndLoadModule("HotModuleHit", tempDir.resolve("cp"));
		var m1 = GenModule.instance.createRedirectModules(dummyApp, new Class<?>[]{v});
		var m2 = GenModule.instance.createRedirectModules(dummyApp, new Class<?>[]{v});
		Assertions.assertNotNull(m1);
		Assertions.assertNotNull(m2);
		Assertions.assertSame(m1[0].getClass(), m2[0].getClass(),
				"同Class命中缓存且父类身份校验通过，必须复用生成类");
		Assertions.assertSame(v, m2[0].getClass().getSuperclass());
	}

	/**
	 * 编译模块类源码并装载：字节码写入cpDir供javac解析符号（生产等价物：buildCp含hot jar），
	 * defineClass到新子装载器（生产等价物：新HotModule），并把编译装载器的"当前模块装载器"切换过去。
	 */
	private Class<?> compileAndLoadModule(String simpleName, Path cpDir) throws Exception {
		var className = "a7fnd880." + simpleName;
		var src = MODULE_SRC.replace("{simple}", simpleName);
		var bytes = new InMemoryJavaCompiler().compileAllToByteCode(Map.of(className, src)).get(className);
		var classFile = cpDir.resolve("a7fnd880").resolve(simpleName + ".class");
		Files.createDirectories(classFile.getParent());
		Files.write(classFile, bytes);
		hotLike.currentModuleLoader = new HotLikeModuleLoader(
				TestFnd880GenModuleCacheInvalidOnUpgrade.class.getClassLoader(), className, bytes);
		GenModule.instance.getCompiler().useOptions("-cp",
				cpDir + File.pathSeparator + System.getProperty("java.class.path"));
		return hotLike.currentModuleLoader.loadClass(className);
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
