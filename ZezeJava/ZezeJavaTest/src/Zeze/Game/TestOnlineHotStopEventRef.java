package Zeze.Game;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Collections.BeanFactory;
import Zeze.Config;
import Zeze.Hot.HotModule;
import Zeze.Util.Action1;
import Zeze.Util.ConcurrentHashSet;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND-G1-8 回归：Online 登记到 HotModule.stopEvents 与 BeanFactory.watchers 的回调
 * 必须复用固定的方法引用实例（ConcurrentHashSet 的键就是元素自身，lambda 按实例判等）：
 * 同一 Online 重复登记幂等、登记的引用可被 remove 真正移除；stop() 的 unregisterWatch
 * 必须移除掉 start() 注册的那个引用。
 * 修复前每次调用都求值 this::onHotModuleStop / this::tryRecordHotModule 产生新实例：
 * stopEvents 随 setLocalBean/setUserData/getOrAddLocalBean 调用次数无界增长，
 * stop() 用新实例 unregisterWatch 永远移除失败（stop/start 循环时 watch 叠加）。
 * 自包含（不依赖外部进程与数据库，不启动网络），标 @Fast。
 */
@Fast
public class TestOnlineHotStopEventRef {

	// 与其他 @Fast 测试错开 serverId：并行时 Application 本地缓存按 serverId 一份。
	private static final AtomicInteger NextServerId = new AtomicInteger(7150);

	private static Application newApp(String name) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("online_stop_event_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application(name, conf);
	}

	// 本测试不访问表数据，不走 start()：fake ProviderApp 仅建立 zeze.redirect，
	// Online 构造里的 RegisterProtocols/RegisterZezeTables 都是纯内存注册。
	private static Online newOnline(Application zeze) {
		var app = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		new ProviderApp(zeze);
		return new Online(app);
	}

	// 公开构造的 moduleClass（demo.Module1.ModuleModule1）由双亲委托链从 classpath 装载，
	private static HotModule newHotModule(Path tempDir) throws Exception {
		// HotModule 构造器即 loadClass(namespace+".Module"+last(namespace))，findClass 真实读
		// jar 条目：dummy jar 必须包含可装载的 demo/Module1/ModuleModule1.class（运行时编译生成）。
		var javaFile = tempDir.resolve("ModuleModule1.java");
		java.nio.file.Files.writeString(javaFile, "package demo.Module1; public class ModuleModule1 {}\n");
		var compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
		try (var fm = compiler.getStandardFileManager(null, null, null)) {
			var units = fm.getJavaFileObjects(javaFile);
			compiler.getTask(null, fm, null, java.util.List.of("-d", tempDir.toString()), null, units).call();
		}
		var classFile = tempDir.resolve("demo").resolve("Module1").resolve("ModuleModule1.class");
		var jarPath = tempDir.resolve("dummy.jar");
		try (var jarOut = new java.util.jar.JarOutputStream(java.nio.file.Files.newOutputStream(jarPath))) {
			jarOut.putNextEntry(new java.util.jar.JarEntry("demo/Module1/ModuleModule1.class"));
			jarOut.write(java.nio.file.Files.readAllBytes(classFile));
		}
		return new HotModule(null, "demo.Module1", jarPath.toFile());
	}

	// HotModule 延迟打开的 JarFile 不关闭会占住 @TempDir（Windows 句柄），测试结束显式关闭。
	private static void closeJarOf(HotModule hotModule) throws Exception {
		var m = HotModule.class.getDeclaredMethod("getJarFile");
		m.setAccessible(true);
		((java.util.jar.JarFile)m.invoke(hotModule)).close();
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashSet<HotModule> hotModulesHaveLocalOf(Online online) throws Exception {
		var field = Online.class.getDeclaredField("hotModulesHaveLocal");
		field.setAccessible(true);
		return (ConcurrentHashSet<HotModule>)field.get(online);
	}

	@SuppressWarnings("unchecked")
	private static Action1<HotModule> onHotModuleStopRefOf(Online online) throws Exception {
		var field = Online.class.getDeclaredField("onHotModuleStopRef");
		field.setAccessible(true);
		return (Action1<HotModule>)field.get(online);
	}

	@Test
	public void testStopEventAddIdempotent(@TempDir Path tempDir) throws Exception {
		var hotModule = newHotModule(tempDir);
		var zeze = newApp("TestOnlineHotStopEventRef1");
		var online = newOnline(zeze);
		var haveLocal = hotModulesHaveLocalOf(online);

		var add = Online.class.getDeclaredMethod("addHotModuleStopEvent", HotModule.class, ConcurrentHashSet.class);
		add.setAccessible(true);
		for (int i = 0; i < 3; i++)
			add.invoke(online, hotModule, haveLocal);

		// 同一 Online 对同一 HotModule 重复登记：恰好一条（修复前每次 add 新 lambda，size==3）
		Assertions.assertEquals(1, hotModule.stopEvents.size());
		Assertions.assertTrue(haveLocal.contains(hotModule));

		// 登记的引用必须是稳定实例：同一实例 remove 生效（模块停止/清理流程依赖此语义）
		var ref = onHotModuleStopRefOf(online);
		Assertions.assertNotNull(hotModule.stopEvents.remove(ref)); // ConcurrentHashSet.remove 返回元素而非 boolean
		Assertions.assertTrue(hotModule.stopEvents.isEmpty());
		closeJarOf(hotModule);
	}

	@Test
	public void testStopEventPerOnlineInstance(@TempDir Path tempDir) throws Exception {
		// 两个独立 Online（不同 Application）共享同一 HotModule：各登记一条，互不合并
		var hotModule = newHotModule(tempDir);
		var online1 = newOnline(newApp("TestOnlineHotStopEventRef2a"));
		var online2 = newOnline(newApp("TestOnlineHotStopEventRef2b"));

		var add = Online.class.getDeclaredMethod("addHotModuleStopEvent", HotModule.class, ConcurrentHashSet.class);
		add.setAccessible(true);
		add.invoke(online1, hotModule, hotModulesHaveLocalOf(online1));
		add.invoke(online2, hotModule, hotModulesHaveLocalOf(online2));

		Assertions.assertEquals(2, hotModule.stopEvents.size());

		// 移除 online1 登记的引用后剩余的必须恰好是 online2 的（不是新实例，也不是 online1 的）
		var ref1 = onHotModuleStopRefOf(online1);
		var ref2 = onHotModuleStopRefOf(online2);
		Assertions.assertNotNull(hotModule.stopEvents.remove(ref1));
		Assertions.assertEquals(1, hotModule.stopEvents.size());
		Assertions.assertSame(ref2, hotModule.stopEvents.iterator().next());
		closeJarOf(hotModule);
	}

	@Test
	public void testBeanFactoryWatchRegisterUnregisterSymmetric() throws Exception {
		var online = newOnline(newApp("TestOnlineHotStopEventRef3"));

		var field = BeanFactory.class.getDeclaredField("globalToLocalWatchers");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		var watchers = (ConcurrentHashSet<Consumer<Class<?>>>)field.get(Online.beanFactory);
		int before = watchers.size();

		online.registerBeanFactoryWatch();
		// 重复注册（stop 后重新 start 的形态）不得叠加：修复前每次 this::tryRecordHotModule 都是新实例
		online.registerBeanFactoryWatch();
		Assertions.assertEquals(before + 1, watchers.size());

		// stop 侧注销必须移除注册的那个实例：修复前 unregisterWatch(新lambda) 永远失败
		online.unregisterBeanFactoryWatch();
		Assertions.assertEquals(before, watchers.size());
	}
}
