package Zeze.Game;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Builtin.Game.Bag.BItem;
import Zeze.Collections.BeanFactory;
import Zeze.Config;
import Zeze.Hot.HotBeanFactory;
import Zeze.Hot.HotManager;
import Zeze.Hot.HotModule;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.FewModifySortedMap;
import Zeze.Util.LongHashMap;
import harness.Fast;
import harness.FastServerIds;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * 验证 Game.Bag.Module 接入 HotBeanFactory 后的完整热更生命周期：
 * 1. 对 HotManager、Bag.beanFactory watch、hotRegisteredModules 与 HotModule.stopEvents 的注册/注销严格对称且无泄漏；
 * 2. 热更道具 Bean 经 bag.add / Bag.Module.register 写入后，触发模块停止 -> hasFreshStopModuleDynamicOnce
 *    -> clearTableCache -> BeanFactory.resetHot -> processWithNewClasses，重新读取可反序列化为新 ClassLoader 下的 Bean，
 *    且支持连续多代热更；
 * 3. 冷启动反序列化（经 BeanFactory.findClass 触发 watch）同样记录 HotModule；
 * 4. 未启用 HotManager 时零注册、零开销且完全兼容既有调用方式。
 */
@Fast
@Isolated("HotManager installs the process-wide BeanFactory application and resets static Bag.beanFactory")
public class TestBagHotBeanFactory {

	private static final AtomicInteger NEXT_SERVER_ID =
			new AtomicInteger(FastServerIds.TEST_BAG_HOT_BEAN_FACTORY);

	private static final String HOT_NAMESPACE = "hotbag.HotBag";
	private static final String HOT_ITEM_CLASS_NAME = "hotbag.HotBag.BHotBagItem";
	private static final long HOT_ITEM_TYPE_ID = Bean.hash64(HOT_ITEM_CLASS_NAME);

	private @Nullable Application savedBeanFactoryApp;

	@BeforeEach
	public void saveStaticState() throws Exception {
		savedBeanFactoryApp = getBeanFactoryApplication();
	}

	@AfterEach
	public void restoreStaticState() throws Exception {
		removeTestHotClassFromBeanFactory();
		setBeanFactoryApplication(savedBeanFactoryApp);
	}

	private static final class Env extends AppBase implements AutoCloseable {
		final Application app;
		final @Nullable HotManager hotManager;
		final int serverId;
		final String dbUrl;

		Env(Path dir, boolean withHotManager) throws Exception {
			this(dir, withHotManager, NEXT_SERVER_ID.getAndIncrement(), null);
		}

		Env(Path dir, boolean withHotManager, int serverId, @Nullable String sharedDbUrl) throws Exception {
			this.serverId = serverId;
			this.dbUrl = sharedDbUrl != null ? sharedDbUrl : "bag_hot_bean_factory_" + serverId;
			var conf = new Config();
			conf.setServiceManager("disable");
			conf.setServerId(serverId);
			conf.setTakeoverMode("off");
			conf.setDefaultTableConf(new Config.TableConf());
			var dbConf = new Config.DatabaseConf();
			dbConf.setDatabaseUrl(dbUrl);
			conf.getDatabaseConfMap().putIfAbsent("", dbConf);
			app = new Application("TestBagHotBeanFactory@" + serverId, conf);
			app.initialize(this);
			if (withHotManager) {
				var workingDir = dir.resolve("hot-" + serverId);
				Files.createDirectories(workingDir.resolve("interfaces"));
				Files.createDirectories(workingDir.resolve("modules"));
				Files.createDirectories(workingDir.resolve("distributes"));
				hotManager = new HotManager(this, workingDir.toString(), workingDir.resolve("distributes").toString());
				app.setHotManager(hotManager);
			} else {
				hotManager = null;
			}
		}

		@Override
		public Application getZeze() {
			return app;
		}

		HotManager requireHotManager() {
			return requireNonNull(hotManager);
		}

		@SuppressWarnings("unchecked")
		ConcurrentHashSet<HotBeanFactory> hotBeanFactories() throws Exception {
			var f = HotManager.class.getDeclaredField("hotBeanFactories");
			f.setAccessible(true);
			return (ConcurrentHashSet<HotBeanFactory>)f.get(requireHotManager());
		}

		@SuppressWarnings("unchecked")
		FewModifySortedMap<String, HotModule> hotModules() throws Exception {
			var f = HotManager.class.getDeclaredField("modules");
			f.setAccessible(true);
			return (FewModifySortedMap<String, HotModule>)f.get(requireHotManager());
		}

		@Override
		public void close() throws Exception {
			if (hotManager != null)
				hotManager.stop();
			app.stop();
		}
	}

	private static @Nullable Application getBeanFactoryApplication() throws Exception {
		var f = BeanFactory.class.getDeclaredField("zeze");
		f.setAccessible(true);
		return (Application)f.get(null);
	}

	private static void setBeanFactoryApplication(@Nullable Application zeze) throws Exception {
		var f = BeanFactory.class.getDeclaredField("zeze");
		f.setAccessible(true);
		f.set(null, zeze);
	}

	private static BeanFactory bagBeanFactory() throws Exception {
		var f = Bag.class.getDeclaredField("beanFactory");
		f.setAccessible(true);
		return (BeanFactory)f.get(null);
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashSet<Consumer<Class<?>>> bagBeanFactoryWatchers() throws Exception {
		var f = BeanFactory.class.getDeclaredField("globalToLocalWatchers");
		f.setAccessible(true);
		return (ConcurrentHashSet<Consumer<Class<?>>>)f.get(bagBeanFactory());
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashSet<Bag.Module> bagHotRegisteredModules() throws Exception {
		var f = Bag.class.getDeclaredField("hotRegisteredModules");
		f.setAccessible(true);
		return (ConcurrentHashSet<Bag.Module>)f.get(null);
	}

	private static void removeTestHotClassFromBeanFactory() throws Exception {
		var bf = bagBeanFactory();
		var writingField = BeanFactory.class.getDeclaredField("writingBeanFactory");
		writingField.setAccessible(true);
		var writing = (LongHashMap<?>)writingField.get(bf);
		writing.remove(HOT_ITEM_TYPE_ID);

		var readingField = BeanFactory.class.getDeclaredField("readingBeanFactory");
		readingField.setAccessible(true);
		readingField.set(bf, null);

		var registeredField = BeanFactory.class.getDeclaredField("registeredClasses");
		registeredField.setAccessible(true);
		@SuppressWarnings("unchecked")
		var registered = (ConcurrentHashMap<Class<?>, ?>)registeredField.get(bf);
		registered.keySet().removeIf(cls -> HOT_ITEM_CLASS_NAME.equals(cls.getName()));

		var allMapField = BeanFactory.class.getDeclaredField("allClassNameMap");
		allMapField.setAccessible(true);
		var allMap = (LongHashMap<?>)allMapField.get(null);
		allMap.remove(HOT_ITEM_TYPE_ID);
	}

	private static void invokeStopInternal(HotModule hotModule) throws Exception {
		var m = HotModule.class.getDeclaredMethod("stopInternal");
		m.setAccessible(true);
		m.invoke(hotModule);
	}

	private static Path compileHotBagModuleJar(Path dir, int version) throws Exception {
		var srcDir = dir.resolve("src-v" + version);
		var outDir = dir.resolve("classes-v" + version);
		Files.createDirectories(srcDir);
		Files.createDirectories(outDir);
		var modSrc = srcDir.resolve("ModuleHotBag.java");
		var itemSrc = srcDir.resolve("BHotBagItem.java");
		Files.writeString(modSrc, "package " + HOT_NAMESPACE + "; public class ModuleHotBag {}\n");
		Files.writeString(itemSrc, """
				package %s;
				import Zeze.Serialize.ByteBuffer;
				import Zeze.Serialize.IByteBuffer;
				import Zeze.Transaction.Bean;
				public class BHotBagItem extends Bean {
					public static final long TYPEID = %dL;
					private int level;
					public int getLevel() { return level; }
					public void setLevel(int v) { level = v; }
					public int getVersion() { return %d; }
					@Override
					public long typeId() { return TYPEID; }
					@Override
					public void encode(ByteBuffer bb) {
						bb.WriteInt(level);
					}
					@Override
					public void decode(IByteBuffer bb) {
						level = bb.ReadInt();
					}
					@Override
					public BHotBagItem copy() {
						var c = new BHotBagItem();
						c.level = this.level;
						return c;
					}
				}
				""".formatted(HOT_NAMESPACE, HOT_ITEM_TYPE_ID, version));
		var compiler = requireNonNull(ToolProvider.getSystemJavaCompiler());
		var cp = System.getProperty("java.class.path");
		try (var fm = compiler.getStandardFileManager(null, null, null)) {
			var units = fm.getJavaFileObjects(modSrc, itemSrc);
			var ok = compiler.getTask(null, fm, null, List.of("-cp", cp, "-d", outDir.toString()), null, units).call();
			assertTrue(Boolean.TRUE.equals(ok), "热更背包道具测试类编译必须成功");
		}
		var jarPath = dir.resolve("hotbag.HotBag-v" + version + ".jar");
		try (var jarOut = new JarOutputStream(Files.newOutputStream(jarPath))) {
			for (var rel : List.of("hotbag/HotBag/ModuleHotBag.class", "hotbag/HotBag/BHotBagItem.class")) {
				jarOut.putNextEntry(new JarEntry(rel));
				jarOut.write(Files.readAllBytes(outDir.resolve(rel)));
				jarOut.closeEntry();
			}
		}
		return jarPath;
	}

	private static Bean newHotItemBean(Class<?> cls, int level) throws Exception {
		var bean = (Bean)cls.getDeclaredConstructor().newInstance();
		cls.getMethod("setLevel", int.class).invoke(bean, level);
		return bean;
	}

	private static int getHotItemLevel(Bean bean) throws Exception {
		return (int)bean.getClass().getMethod("getLevel").invoke(bean);
	}

	private static int getHotItemVersion(Bean bean) throws Exception {
		return (int)bean.getClass().getMethod("getVersion").invoke(bean);
	}

	@Test
	public void testHotRegistrationUnregistrationSymmetric(@TempDir Path tempDir) throws Exception {
		var watchers = bagBeanFactoryWatchers();
		var activeModules = bagHotRegisteredModules();
		int watchersBefore = watchers.size();
		int activeBefore = activeModules.size();

		// 1) 构造前已装配 HotManager：构造即自动注册，重复 start 幂等，stop/start 循环不叠加，stop 彻底清理 stopEvents
		var jarPath = compileHotBagModuleJar(tempDir.resolve("sym"), 1);
		try (var env = new Env(tempDir, true);
			 var hotModule = new HotModule(env.requireHotManager(), HOT_NAMESPACE, jarPath.toFile())) {
			var factories = env.hotBeanFactories();
			int factoriesBefore = factories.size();

			var module = new Bag.Module(env.app);
			assertTrue(factories.contains(module), "构造时若 HotManager 已存在，应自动注册 HotBeanFactory");
			assertEquals(factoriesBefore + 1, factories.size());
			assertEquals(watchersBefore + 1, watchers.size());
			assertEquals(activeBefore + 1, activeModules.size());

			// 重复 start / start(app) 幂等，不得叠加任何集合
			module.start();
			module.start(env.app);
			assertEquals(factoriesBefore + 1, factories.size());
			assertEquals(watchersBefore + 1, watchers.size());
			assertEquals(activeBefore + 1, activeModules.size());

			// 注册热更道具 Bean 后，HotModule.stopEvents 登记恰好 1 条（重复注册幂等）
			var hotClass = hotModule.loadClass(HOT_ITEM_CLASS_NAME);
			Bag.Module.register(newHotItemBean(hotClass, 1));
			Bag.Module.register(newHotItemBean(hotClass, 2));
			assertEquals(1, hotModule.stopEvents.size(), "同一 Bag.Module 对同一 HotModule 的 stopEvents 登记应幂等");

			// stop() 必须同步清理 HotManager、beanFactory watch、hotRegisteredModules 与 HotModule.stopEvents
			module.stop();
			assertFalse(factories.contains(module), "stop() 后应从 HotManager 注销");
			assertEquals(watchersBefore, watchers.size(), "stop() 后应从 BeanFactory 注销 watch");
			assertEquals(activeBefore, activeModules.size(), "stop() 后应从活跃集合注销");
			assertTrue(hotModule.stopEvents.isEmpty(), "stop() 后应清理已订阅的 HotModule.stopEvents");

			// stop() 后再 start() 再经 UnRegisterZezeTables() 注销（模拟 demo/测试不调 stop 只调 UnRegisterZezeTables）
			module.start();
			Bag.Module.register(newHotItemBean(hotClass, 3));
			assertEquals(1, hotModule.stopEvents.size());
			module.UnRegisterZezeTables(env.app);
			assertFalse(factories.contains(module), "UnRegisterZezeTables() 应注销 HotManager");
			assertEquals(watchersBefore, watchers.size(), "UnRegisterZezeTables() 应注销 BeanFactory watch");
			assertEquals(activeBefore, activeModules.size());
			assertTrue(hotModule.stopEvents.isEmpty(), "UnRegisterZezeTables() 应清理 HotModule.stopEvents");
		}

		// 2) 先 new Bag.Module（此时无 HotManager），再 setHotManager + start()，停机时先 setHotManager(null) 再 stop() 仍能对称注销
		try (var env = new Env(tempDir.resolve("late"), false)) {
			var module = new Bag.Module(env.app);
			assertEquals(watchersBefore, watchers.size(), "无 HotManager 时构造不得注册 watch");
			assertEquals(activeBefore, activeModules.size());

			var workingDir = tempDir.resolve("late-hot");
			Files.createDirectories(workingDir.resolve("interfaces"));
			Files.createDirectories(workingDir.resolve("modules"));
			Files.createDirectories(workingDir.resolve("distributes"));
			var lateHotManager = new HotManager(env, workingDir.toString(), workingDir.resolve("distributes").toString());
			try {
				env.app.setHotManager(lateHotManager);
				module.start();
				var f = HotManager.class.getDeclaredField("hotBeanFactories");
				f.setAccessible(true);
				@SuppressWarnings("unchecked")
				var factories = (ConcurrentHashSet<HotBeanFactory>)f.get(lateHotManager);
				assertTrue(factories.contains(module), "后置装配 HotManager 后调用 start() 应完成注册");
				assertEquals(watchersBefore + 1, watchers.size());

				// 即使外部先清空 zeze.hotManager，stop() 仍须能从已记住的 registeredHotManager 注销
				env.app.setHotManager(null);
				module.stop();
				assertFalse(factories.contains(module), "即使 zeze.hotManager 已被置空，stop() 仍须从原 HotManager 注销");
				assertEquals(watchersBefore, watchers.size());
				assertEquals(activeBefore, activeModules.size());
			} finally {
				lateHotManager.stop();
			}
		}
	}

	@Test
	public void testHotReloadItemBeanLifecycleEndToEnd(@TempDir Path tempDir) throws Exception {
		var jarV1 = compileHotBagModuleJar(tempDir, 1);
		var jarV2 = compileHotBagModuleJar(tempDir, 2);
		var jarV3 = compileHotBagModuleJar(tempDir, 3);

		try (var env = new Env(tempDir, true);
			 var modV1 = new HotModule(env.requireHotManager(), HOT_NAMESPACE, jarV1.toFile());
			 var modV2 = new HotModule(env.requireHotManager(), HOT_NAMESPACE, jarV2.toFile());
			 var modV3 = new HotModule(env.requireHotManager(), HOT_NAMESPACE, jarV3.toFile())) {

			env.hotModules().put(HOT_NAMESPACE, modV1);
			var bagModule = new Bag.Module(env.app);
			bagModule.funcItemPileMax = itemId -> 99;
			env.app.start();
			try {
				// 阶段 1：v1 热更模块往背包放入自定义道具 Bean
				var clsV1 = modV1.loadClass(HOT_ITEM_CLASS_NAME);
				assertSame(modV1, clsV1.getClassLoader());
				var rc = env.app.newProcedure(() -> {
					var bag = bagModule.open(1001L, "main");
					bag.setCapacity(10);
					var item = new BItem(5001, 1);
					item.getItem().setBean(newHotItemBean(clsV1, 42));
					assertEquals(0, bag.add(0, item));
					return Procedure.Success;
				}, "addHotItemV1").call();
				assertEquals(Procedure.Success, rc);
				assertEquals(1, modV1.stopEvents.size(), "bag.add 写入热更道具后应订阅 v1 HotModule.stopEvents");

				// 模拟 HotManager.install: checkpointRun -> 替换模块 -> exist.stopInternal -> HotBeanFactory 升级序列
				env.app.checkpointRun();
				env.hotModules().put(HOT_NAMESPACE, modV2);
				invokeStopInternal(modV1);

				assertTrue(modV1.stopEvents.isEmpty());
				assertTrue(bagModule.hasFreshStopModuleDynamicOnce(), "v1 停止后 hasFreshStopModuleDynamicOnce 必须返回 true");
				assertFalse(bagModule.hasFreshStopModuleDynamicOnce(), "hasFreshStopModuleDynamicOnce 须一次性复位");

				bagModule.clearTableCache();
				var beanFactories = new HashMap<BeanFactory, List<Class<?>>>();
				beanFactories.put(bagModule.beanFactory(), new ArrayList<>());
				try (var jfV2 = new JarFile(jarV2.toFile())) {
					BeanFactory.resetHot(beanFactories, List.of(jfV2), env.requireHotManager().getHotRedirect());
				}
				bagModule.processWithNewClasses(requireNonNull(beanFactories.get(bagModule.beanFactory())));
				assertEquals(1, modV2.stopEvents.size(), "processWithNewClasses 须为新版本 v2 续订 stopEvents");

				// 阶段 2：v2 读取同一格子道具，必须反序列化为 v2 ClassLoader 实例且字段数据完好
				var clsV2 = modV2.loadClass(HOT_ITEM_CLASS_NAME);
				assertSame(modV2, clsV2.getClassLoader());
				assertNotSame(clsV1, clsV2);
				rc = env.app.newProcedure(() -> {
					var bag = requireNonNull(bagModule.openOrNull(1001L, "main"));
					var item = requireNonNull(bag.getBean().getItems().get(0));
					var bean = item.getItem().getBean();
					assertSame(modV2, bean.getClass().getClassLoader(), "热更后读取道具必须使用 v2 ClassLoader，避免 ClassCastException");
					assertTrue(clsV2.isInstance(bean), "读取出的道具 Bean 必须可转为 v2 类型");
					assertEquals(42, getHotItemLevel(bean), "热更重加载后持久化字段值必须保持一致");
					assertEquals(2, getHotItemVersion(bean), "新实例方法逻辑必须来自 v2");
					return Procedure.Success;
				}, "readHotItemV2").call();
				assertEquals(Procedure.Success, rc);

				// 阶段 3：连续第二次热更 v2 -> v3，验证 processWithNewClasses 闭合多代热更链路
				env.app.checkpointRun();
				env.hotModules().put(HOT_NAMESPACE, modV3);
				invokeStopInternal(modV2);
				assertTrue(bagModule.hasFreshStopModuleDynamicOnce(), "v2 停止后应再次触发 hasFreshStopModuleDynamicOnce");

				bagModule.clearTableCache();
				var beanFactoriesV3 = new HashMap<BeanFactory, List<Class<?>>>();
				beanFactoriesV3.put(bagModule.beanFactory(), new ArrayList<>());
				try (var jfV3 = new JarFile(jarV3.toFile())) {
					BeanFactory.resetHot(beanFactoriesV3, List.of(jfV3), env.requireHotManager().getHotRedirect());
				}
				bagModule.processWithNewClasses(requireNonNull(beanFactoriesV3.get(bagModule.beanFactory())));
				assertEquals(1, modV3.stopEvents.size(), "processWithNewClasses 须为 v3 续订 stopEvents");

				var clsV3 = modV3.loadClass(HOT_ITEM_CLASS_NAME);
				rc = env.app.newProcedure(() -> {
					var bag = requireNonNull(bagModule.openOrNull(1001L, "main"));
					var bean = requireNonNull(bag.getBean().getItems().get(0)).getItem().getBean();
					assertSame(modV3, bean.getClass().getClassLoader(), "二次热更后读取道具必须使用 v3 ClassLoader");
					assertTrue(clsV3.isInstance(bean));
					assertEquals(42, getHotItemLevel(bean));
					assertEquals(3, getHotItemVersion(bean));
					return Procedure.Success;
				}, "readHotItemV3").call();
				assertEquals(Procedure.Success, rc);
			} finally {
				bagModule.stop();
				bagModule.UnRegister();
			}
		}
	}

	@Test
	public void testWatchPathRecordsHotModuleOnColdDeserialize(@TempDir Path tempDir) throws Exception {
		var jarV1 = compileHotBagModuleJar(tempDir, 1);
		int sharedServerId = NEXT_SERVER_ID.getAndIncrement();
		String sharedDbUrl = "bag_hot_cold_deserialize_" + sharedServerId;

		// 进程 1：写入热更道具并落库后停机
		try (var env1 = new Env(tempDir.resolve("p1"), true, sharedServerId, sharedDbUrl);
			 var mod1 = new HotModule(env1.requireHotManager(), HOT_NAMESPACE, jarV1.toFile())) {
			env1.hotModules().put(HOT_NAMESPACE, mod1);
			var bag1 = new Bag.Module(env1.app);
			env1.app.start();
			try {
				var cls1 = mod1.loadClass(HOT_ITEM_CLASS_NAME);
				var rc = env1.app.newProcedure(() -> {
					var bag = bag1.open(2002L, "persist");
					bag.setCapacity(10);
					var item = new BItem(6001, 1);
					item.getItem().setBean(newHotItemBean(cls1, 77));
					assertEquals(0, bag.add(0, item));
					return Procedure.Success;
				}, "seedColdDeserialize").call();
				assertEquals(Procedure.Success, rc);
				env1.app.checkpointRun();
			} finally {
				bag1.stop();
				bag1.UnRegister();
			}
		}

		// 清理静态 Bag.beanFactory，模拟全新进程冷启动（只通过 reloadClassesFromJar 建立了类名索引）
		removeTestHotClassFromBeanFactory();

		// 进程 2：冷启动后不调用 bag.add/register，直接从数据库反序列化，依赖 beanFactory.registerWatch 自动追踪 HotModule
		try (var env2 = new Env(tempDir.resolve("p2"), true, sharedServerId, sharedDbUrl);
			 var mod2 = new HotModule(env2.requireHotManager(), HOT_NAMESPACE, jarV1.toFile())) {
			env2.hotModules().put(HOT_NAMESPACE, mod2);
			try (var jf = new JarFile(jarV1.toFile())) {
				BeanFactory.reloadClassesFromJar(jf);
			}
			var bag2 = new Bag.Module(env2.app);
			env2.app.start();
			try {
				assertTrue(mod2.stopEvents.isEmpty(), "尚未反序列化前不应预先订阅 stopEvents");
				var rc = env2.app.newProcedure(() -> {
					var bag = requireNonNull(bag2.openOrNull(2002L, "persist"));
					var bean = requireNonNull(bag.getBean().getItems().get(0)).getItem().getBean();
					assertNotNull(bean);
					assertSame(mod2, bean.getClass().getClassLoader());
					assertEquals(77, getHotItemLevel(bean));
					return Procedure.Success;
				}, "coldReadHotItem").call();
				assertEquals(Procedure.Success, rc);

				assertEquals(1, mod2.stopEvents.size(), "冷反序列化经 BeanFactory watch 必须自动订阅 HotModule.stopEvents");
				invokeStopInternal(mod2);
				assertTrue(bag2.hasFreshStopModuleDynamicOnce(), "冷读追踪的 HotModule 停止后应触发 hasFreshStopModuleDynamicOnce");
			} finally {
				bag2.stop();
				bag2.UnRegister();
			}
		}
	}

	@Test
	public void testWithoutHotManagerZeroOverheadAndCompatible(@TempDir Path tempDir) throws Exception {
		var watchers = bagBeanFactoryWatchers();
		var activeModules = bagHotRegisteredModules();
		int watchersBefore = watchers.size();
		int activeBefore = activeModules.size();

		try (var env = new Env(tempDir, false)) {
			var bagModule = new Bag.Module(env.app);
			bagModule.funcItemPileMax = itemId -> 99;
			env.app.start();
			try {
				// 无 ProviderApp 时调用 start() / start(app) 必须安全无 NPE
				bagModule.start();
				bagModule.start(env.app);
				assertEquals(watchersBefore, watchers.size(), "无 HotManager 时不得注册 BeanFactory watch");
				assertEquals(activeBefore, activeModules.size(), "无 HotManager 时不得进入活跃热更集合");

				var rc = env.app.newProcedure(() -> {
					var bag = bagModule.open(3003L, "plain");
					bag.setCapacity(10);
					assertEquals(0, bag.add(100, 15));
					assertEquals(15, requireNonNull(bag.getBean().getItems().get(0)).getNumber());
					return Procedure.Success;
				}, "plainBagAdd").call();
				assertEquals(Procedure.Success, rc);
				assertFalse(bagModule.hasFreshStopModuleDynamicOnce());
			} finally {
				bagModule.stop();
				bagModule.UnRegister();
			}
		}
		assertEquals(watchersBefore, watchers.size());
		assertEquals(activeBefore, activeModules.size());
	}

	@Test
	public void testMultiHotManagerOwnershipIsolation(@TempDir Path tempDir) throws Exception {
		var jarPath = compileHotBagModuleJar(tempDir, 1);
		try (var env1 = new Env(tempDir.resolve("m1"), true);
			 var env2 = new Env(tempDir.resolve("m2"), true);
			 var mod1 = new HotModule(env1.requireHotManager(), HOT_NAMESPACE, jarPath.toFile())) {
			var bag1 = new Bag.Module(env1.app);
			var bag2 = new Bag.Module(env2.app);
			try {
				var cls1 = mod1.loadClass(HOT_ITEM_CLASS_NAME);
				Bag.Module.register(newHotItemBean(cls1, 9));

				// 仅归属 env1.hotManager 的 bag1 订阅 mod1.stopEvents，不得串扰订阅到 env2 的 bag2
				assertEquals(1, mod1.stopEvents.size(), "仅归属同一 HotManager 的 Bag.Module 应订阅该 HotModule.stopEvents");
				invokeStopInternal(mod1);
				assertTrue(bag1.hasFreshStopModuleDynamicOnce(), "归属 HotManager 的 bag1 应收到热模块停止标记");
				assertFalse(bag2.hasFreshStopModuleDynamicOnce(), "另一 HotManager 的 bag2 不得被串扰触发");
			} finally {
				bag2.stop();
				bag2.UnRegister();
				bag1.stop();
				bag1.UnRegister();
			}
		}
	}
}
