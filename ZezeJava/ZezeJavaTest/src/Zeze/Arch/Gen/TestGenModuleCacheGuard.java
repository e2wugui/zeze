package Zeze.Arch.Gen;

import static java.util.Objects.requireNonNull;

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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * GenModule.genClassMap 缓存守卫回归：
 * 1) 同一模块 Class 重复调用时，父类身份校验通过，必须复用缓存中的生成类；
 * 2) 同名但来自不同 ClassLoader 的模块类命中缓存时，genClass.getSuperclass() != moduleClass
 *    必须经 checkRedirectSuperclass 显式 fail-fast，禁止静默复用旧生成类。
 */
@Fast
@Isolated // GenModule.instance（genClassMap/compiler）是JVM级单例，独占运行
public class TestGenModuleCacheGuard {

	/** 横向委托装载器：将 modcache.* 转发给当前测试模块子装载器，其余走双亲委派。 */
	private static final class DelegatingModuleLoader extends ClassLoader {
		volatile @Nullable ClassLoader currentModuleLoader;

		DelegatingModuleLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (name.startsWith("modcache.") && currentModuleLoader != null)
				return requireNonNull(currentModuleLoader).loadClass(name);
			return super.loadClass(name, resolve);
		}
	}

	/** 独立子装载器：把模块类字节码 defineClass 到独立装载器，使同名类具备不同 Class 身份。 */
	private static final class IsolatedModuleLoader extends ClassLoader {
		private final String className;
		private final byte[] bytes;

		IsolatedModuleLoader(ClassLoader parent, String className, byte[] bytes) {
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
			package modcache;
			public class {simple} implements Zeze.IModule {
			    public static final int ModuleId = 8801;
			    public static final String ModuleFullName = "modcache.{simple}";
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

	private static @Nullable Application app;
	private static @Nullable AppBase dummyApp;
	private static @Nullable DelegatingModuleLoader delegatingLoader;
	private static @Nullable Object savedCompilerOptions;
	private static @Nullable ClassLoader savedCompilerLoader;

	@BeforeAll
	public static void setUp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		app = new Application("modcache", config);
		new ProviderApp(app); // 哑构造：设置app.redirect（RedirectBase），fake服务不启动
		dummyApp = new AppBase() {
			@Override
			public @Nullable Application getZeze() {
				return app;
			}
		};
		delegatingLoader = new DelegatingModuleLoader(TestGenModuleCacheGuard.class.getClassLoader());

		var compiler = GenModule.instance.getCompiler();
		savedCompilerOptions = readCompilerField(compiler, "options");
		savedCompilerLoader = compiler.getClassloader();
		compiler.useParentClassLoader(delegatingLoader);
	}

	@AfterAll
	public static void tearDown() throws Exception {
		var compiler = GenModule.instance.getCompiler();
		writeCompilerField(compiler, "options", savedCompilerOptions);
		writeCompilerField(compiler, "classLoader", savedCompilerLoader);
	}

	@Test
	public void testStaleCacheFailsFast() throws Exception {
		var v1 = compileAndLoadModule("CacheGuardModuleUp", tempDir.resolve("cp1"));
		var m1 = GenModule.instance.createRedirectModules(requireNonNull(dummyApp), new Class<?>[]{v1});
		Assertions.assertNotNull(m1);
		Assertions.assertSame(v1, m1[0].getClass().getSuperclass(), "首装生成类必须extends传入模块类");

		var v2 = compileAndLoadModule("CacheGuardModuleUp", tempDir.resolve("cp2"));
		Assertions.assertNotSame(v1, v2, "测试前提：同名类不同装载器，Class身份不同");
		Assertions.assertThrows(IllegalStateException.class,
				() -> GenModule.instance.createRedirectModules(requireNonNull(dummyApp), new Class<?>[]{v2}),
				"缓存命中陈旧父类必须显式失败，不得静默复用旧生成类");
	}

	@Test
	public void testCacheHitReused() throws Exception {
		var v = compileAndLoadModule("CacheGuardModuleHit", tempDir.resolve("cp"));
		var m1 = GenModule.instance.createRedirectModules(requireNonNull(dummyApp), new Class<?>[]{v});
		var m2 = GenModule.instance.createRedirectModules(dummyApp, new Class<?>[]{v});
		Assertions.assertNotNull(m1);
		Assertions.assertNotNull(m2);
		Assertions.assertSame(m1[0].getClass(), m2[0].getClass(),
				"同Class命中缓存且父类身份校验通过，必须复用生成类");
		Assertions.assertSame(v, m2[0].getClass().getSuperclass());
	}

	private Class<?> compileAndLoadModule(String simpleName, Path cpDir) throws Exception {
		var className = "modcache." + simpleName;
		var src = MODULE_SRC.replace("{simple}", simpleName);
		var bytes = new InMemoryJavaCompiler().compileAllToByteCode(Map.of(className, src)).get(className);
		var classFile = cpDir.resolve("modcache").resolve(simpleName + ".class");
		Files.createDirectories(classFile.getParent());
		Files.write(classFile, bytes);
		requireNonNull(delegatingLoader).currentModuleLoader = new IsolatedModuleLoader(
				TestGenModuleCacheGuard.class.getClassLoader(), className, bytes);
		GenModule.instance.getCompiler().useOptions("-cp",
				cpDir + File.pathSeparator + System.getProperty("java.class.path"));
		return requireNonNull(delegatingLoader.currentModuleLoader).loadClass(className);
	}

	private static Object readCompilerField(InMemoryJavaCompiler compiler, String name) throws Exception {
		var field = InMemoryJavaCompiler.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(compiler);
	}

	private static void writeCompilerField(InMemoryJavaCompiler compiler, String name, @Nullable Object value) throws Exception {
		var field = InMemoryJavaCompiler.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(compiler, value);
	}
}
