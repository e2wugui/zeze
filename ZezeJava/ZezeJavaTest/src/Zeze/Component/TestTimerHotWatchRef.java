package Zeze.Component;

import harness.FastServerIds;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Collections.BeanFactory;
import Zeze.Config;
import Zeze.Hot.HotManager;
import Zeze.Hot.HotModule;
import Zeze.Util.Action1;
import Zeze.Util.ConcurrentHashSet;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * kimi-audit01 C-01回归：Timer登记到HotModule.stopEvents与BeanFactory.watchers的回调
 * 必须复用固定的方法引用实例（ConcurrentHashSet的键就是元素自身，lambda按实例判等）。
 * 修复前每次求值this::tryRecordHotModule/this::onHotModuleStop都产生新实例：
 * stop()用新实例unregisterWatch永远失败（static beanFactory持有死Timer，stop/start循环
 * watch叠加）；tryRecordHotModule路径的stopEvents.add随调用次数无界增长。
 * 同型覆盖：Arch/Online、DepartmentTree、Queue、LinkedMap的stopEvents.add同修。
 */
@Fast
public class TestTimerHotWatchRef {

	// 与其他 @Fast 测试错开 serverId：并行时 Application 本地缓存按 serverId 一份。
	private static final AtomicInteger NextServerId = new AtomicInteger(FastServerIds.TEST_TIMER_HOT_WATCH_REF);

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("timer_hot_ref_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestTimerHotWatchRef" + conf.getServerId(), conf);
	}

	/** 完整装配到可start的Application（hotManager非null，Timer.start才走registerWatch路径）。 */
	private static Application startedApp(Path workingDir, Path distributeDir) throws Exception {
		Files.createDirectories(workingDir);
		Files.createDirectories(distributeDir);
		var zeze = newApp();
		new Zeze.Arch.ProviderApp(zeze);
		var appBase = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		zeze.initialize(appBase);
		zeze.setHotManager(new HotManager(appBase, workingDir.toString(), distributeDir.toString()));
		zeze.start();
		return zeze;
	}

	// 公开构造的moduleClass（demo.Module1.ModuleModule1）经HotModule的findClass真实读jar装载，
	// getClassLoader()==该HotModule，满足tryRecordHotModule的isHotModule判定。
	private static HotModule newHotModule(Path tempDir) throws Exception {
		var javaFile = tempDir.resolve("ModuleModule1.java");
		Files.writeString(javaFile, "package demo.Module1; public class ModuleModule1 {}\n");
		var compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
		try (var fm = compiler.getStandardFileManager(null, null, null)) {
			var units = fm.getJavaFileObjects(javaFile);
			compiler.getTask(null, fm, null, List.of("-d", tempDir.toString()), null, units).call();
		}
		var classFile = tempDir.resolve("demo").resolve("Module1").resolve("ModuleModule1.class");
		var jarPath = tempDir.resolve("dummy.jar");
		try (var jarOut = new java.util.jar.JarOutputStream(Files.newOutputStream(jarPath))) {
			jarOut.putNextEntry(new java.util.jar.JarEntry("demo/Module1/ModuleModule1.class"));
			jarOut.write(Files.readAllBytes(classFile));
		}
		return new HotModule(null, "demo.Module1", jarPath.toFile());
	}

	// HotModule延迟打开的JarFile不关闭会占住@TempDir（Windows句柄），测试结束显式关闭。
	private static void closeJarOf(HotModule hotModule) throws Exception {
		var m = HotModule.class.getDeclaredMethod("getJarFile");
		m.setAccessible(true);
		((java.util.jar.JarFile)m.invoke(hotModule)).close();
	}

	@SuppressWarnings("unchecked")
	private static Action1<HotModule> onHotModuleStopRefOf(Timer timer) throws Exception {
		var field = Timer.class.getDeclaredField("onHotModuleStopRef");
		field.setAccessible(true);
		return (Action1<HotModule>)field.get(timer);
	}

	/** stopEvents登记幂等：同一Timer对同一HotModule重复登记恰好一条，且登记的是稳定实例。 */
	@Test
	public void testStopEventAddIdempotent(@TempDir Path tempDir) throws Exception {
		var zeze = startedApp(tempDir.resolve("w1"), tempDir.resolve("d1"));
		try {
			var timer = zeze.getTimer();
			Assertions.assertNotNull(timer);
			var hotModule = newHotModule(tempDir);
			var hotClass = hotModule.loadClass("demo.Module1.ModuleModule1");

			for (int i = 0; i < 3; i++)
				timer.processWithNewClasses(List.of(hotClass)); // tryRecordHotModule路径

			// 同一Timer对同一HotModule重复登记：恰好一条（修复前每次add新lambda，size==3）
			Assertions.assertEquals(1, hotModule.stopEvents.size());
			// 登记的必须是稳定实例：同一实例remove生效（模块停止流程依赖此语义）
			var ref = onHotModuleStopRefOf(timer);
			Assertions.assertNotNull(hotModule.stopEvents.remove(ref));
			Assertions.assertTrue(hotModule.stopEvents.isEmpty());
			closeJarOf(hotModule);
		} finally {
			zeze.stop();
		}
	}

	/** beanFactory.watch登记/注销对称：stop必须移除start登记的那个实例，stop/start循环不叠加。 */
	@Test
	public void testBeanFactoryWatchRegisterUnregisterSymmetric(@TempDir Path tempDir) throws Exception {
		var zeze = startedApp(tempDir.resolve("w2"), tempDir.resolve("d2"));
		try {
			var timer = zeze.getTimer();
			Assertions.assertNotNull(timer);

			var beanFactoryField = Timer.class.getDeclaredField("beanFactory");
			beanFactoryField.setAccessible(true);
			var watchersField = BeanFactory.class.getDeclaredField("globalToLocalWatchers");
			watchersField.setAccessible(true);
			@SuppressWarnings("unchecked")
			var watchers = (ConcurrentHashSet<Consumer<Class<?>>>)watchersField.get(beanFactoryField.get(null));

			timer.stop(); // 归零到未启动态（无论zeze.start是否已自动start过）
			int before = watchers.size();

			timer.start();
			Assertions.assertEquals(before + 1, watchers.size(), "start登记恰好一条watch");
			timer.stop();
			Assertions.assertEquals(before, watchers.size(), "stop必须注销start登记的watch实例（修复前永失败）");

			// stop/start循环不得叠加（修复前每轮净增一条，static beanFactory持有死Timer）
			timer.start();
			timer.stop();
			Assertions.assertEquals(before, watchers.size(), "stop/start循环不得叠加watch");
		} finally {
			zeze.stop();
		}
	}
}
