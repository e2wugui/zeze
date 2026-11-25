package Zeze.Collections;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.BMyBean;
import Zeze.Config;
import Zeze.Hot.HotBeanFactory;
import Zeze.Hot.HotManager;
import Zeze.Hot.HotModule;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.TaskSpec;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 验证 Queue.remove() 随事务提交逐出 Module.queues 时，同步注销 HotManager.hotBeanFactories
 * 与 HotModule.stopEvents 中的强引用，避免随临时队列销毁累积泄漏；回滚时保持原登记不变。
 */
@Fast
public class TestQueueRemoveUnregistersHotRefs {

	private static final AtomicInteger NEXT_SERVER_ID =
			new AtomicInteger(FastServerIds.TEST_QUEUE_REMOVE_UNREGISTERS_HOT_REFS);

	private static final class Env extends AppBase implements AutoCloseable {
		final Application app;
		final HotManager hotManager;

		Env(Path dir) throws Exception {
			var conf = new Config();
			conf.setServiceManager("disable");
			conf.setServerId(NEXT_SERVER_ID.getAndIncrement());
			conf.setTakeoverMode("off");
			conf.setDefaultTableConf(new Config.TableConf());
			var dbConf = new Config.DatabaseConf();
			dbConf.setDatabaseUrl("queue_remove_hot_refs_" + conf.getServerId());
			conf.getDatabaseConfMap().putIfAbsent("", dbConf);
			app = new Application("TestQueueRemoveUnregistersHotRefs@" + conf.getServerId(), conf);
			var workingDir = dir.resolve("hot-" + conf.getServerId());
			Files.createDirectories(workingDir.resolve("interfaces"));
			Files.createDirectories(workingDir.resolve("modules"));
			Files.createDirectories(workingDir.resolve("distributes"));
			app.initialize(this);
			hotManager = new HotManager(this, workingDir.toString(), workingDir.resolve("distributes").toString());
			app.setHotManager(hotManager);
			app.start();
		}

		@Override
		public Application getZeze() {
			return app;
		}

		@SuppressWarnings("unchecked")
		ConcurrentHashSet<HotBeanFactory> hotBeanFactories() throws Exception {
			var f = HotManager.class.getDeclaredField("hotBeanFactories");
			f.setAccessible(true);
			return (ConcurrentHashSet<HotBeanFactory>)f.get(hotManager);
		}

		@Override
		public void close() throws Exception {
			hotManager.stop();
			app.stop();
		}
	}

	private static HotModule compileHotQueueModule(HotManager hotManager, Path dir) throws Exception {
		var srcDir = dir.resolve("src");
		var outDir = dir.resolve("classes");
		Files.createDirectories(srcDir);
		Files.createDirectories(outDir);
		var modSrc = srcDir.resolve("ModuleHotQ.java");
		var valSrc = srcDir.resolve("BHotQValue.java");
		Files.writeString(modSrc, "package hotq.HotQ; public class ModuleHotQ {}\n");
		Files.writeString(valSrc, """
				package hotq.HotQ;
				public class BHotQValue extends Zeze.Transaction.EmptyBean {
					public static final long TYPEID = 0x484F545156414C01L;
					@Override
					public long typeId() { return TYPEID; }
					@Override
					public BHotQValue copy() { return new BHotQValue(); }
				}
				""");
		var compiler = requireNonNull(ToolProvider.getSystemJavaCompiler());
		var cp = System.getProperty("java.class.path");
		try (var fm = compiler.getStandardFileManager(null, null, null)) {
			var units = fm.getJavaFileObjects(modSrc, valSrc);
			var ok = compiler.getTask(null, fm, null, List.of("-cp", cp, "-d", outDir.toString()), null, units).call();
			assertTrue(Boolean.TRUE.equals(ok), "热更测试类编译必须成功");
		}
		var jarPath = dir.resolve("hotq.HotQ.jar");
		try (var jarOut = new JarOutputStream(Files.newOutputStream(jarPath))) {
			for (var rel : List.of("hotq/HotQ/ModuleHotQ.class", "hotq/HotQ/BHotQValue.class")) {
				jarOut.putNextEntry(new JarEntry(rel));
				jarOut.write(Files.readAllBytes(outDir.resolve(rel)));
				jarOut.closeEntry();
			}
		}
		return new HotModule(hotManager, "hotq.HotQ", jarPath.toFile());
	}

	@Test
	public void testRemoveEvictsQueueFromHotBeanFactories(@TempDir Path tempDir) throws Exception {
		try (var env = new Env(tempDir)) {
			var queueModule = env.app.getQueueModule();
			var factories = env.hotBeanFactories();
			var baseline = factories.size();

			for (int i = 0; i < 20; i++) {
				var name = "transient-queue-" + i;
				var rc = TaskSpec.ofProcedure(env.app.newProcedure(() -> {
					var q = queueModule.open(name, BMyBean.class);
					var b = new BMyBean();
					b.setI(1);
					q.add(b);
					return Procedure.Success;
				}, "openAndAdd")).call();
				assertEquals(Procedure.Success, rc);
				assertEquals(baseline + 1, factories.size(), "活跃队列应登记在 hotBeanFactories 中");

				rc = TaskSpec.ofProcedure(env.app.newProcedure(() -> {
					queueModule.open(name, BMyBean.class).remove();
					return Procedure.Success;
				}, "removeQueue")).call();
				assertEquals(Procedure.Success, rc);
				assertEquals(baseline, factories.size(), "Queue.remove 提交后必须从 hotBeanFactories 注销");
			}
		}
	}

	@Test
	public void testRemoveUnregistersHotModuleStopEvent(@TempDir Path tempDir) throws Exception {
		try (var env = new Env(tempDir); var hotModule = compileHotQueueModule(env.hotManager, tempDir.resolve("hotmod"))) {
			@SuppressWarnings("unchecked")
			var hotBeanClass = (Class<Bean>)hotModule.loadClass("hotq.HotQ.BHotQValue");
			var queueModule = env.app.getQueueModule();
			var factories = env.hotBeanFactories();

			var rc = TaskSpec.ofProcedure(env.app.newProcedure(() -> {
				var q = queueModule.open("hot-value-queue", hotBeanClass);
				q.add(hotBeanClass.getDeclaredConstructor().newInstance());
				return Procedure.Success;
			}, "openHotValueQueue")).call();
			assertEquals(Procedure.Success, rc);
			assertEquals(1, hotModule.stopEvents.size(), "热更 Bean 队列应向 HotModule.stopEvents 登记回调");

			rc = TaskSpec.ofProcedure(env.app.newProcedure(() -> {
				queueModule.open("hot-value-queue", hotBeanClass).remove();
				return Procedure.Success;
			}, "removeHotValueQueue")).call();
			assertEquals(Procedure.Success, rc);
			assertTrue(hotModule.stopEvents.isEmpty(), "Queue.remove 提交后必须注销 HotModule.stopEvents 回调");
			assertFalse(factories.keySet().stream().anyMatch(f -> f instanceof Queue<?> q && "hot-value-queue".equals(q.getName())),
					"Queue.remove 提交后不得残留于 hotBeanFactories");
		}
	}

	@Test
	public void testRolledBackRemoveKeepsHotRegistration(@TempDir Path tempDir) throws Exception {
		try (var env = new Env(tempDir)) {
			var queueModule = env.app.getQueueModule();
			var factories = env.hotBeanFactories();

			var rc = TaskSpec.ofProcedure(env.app.newProcedure(() -> {
				var q = queueModule.open("rollback-queue", BMyBean.class);
				q.add(new BMyBean());
				return Procedure.Success;
			}, "seedRollbackQueue")).call();
			assertEquals(Procedure.Success, rc);
			var queue = queueModule.open("rollback-queue", BMyBean.class);
			assertTrue(factories.contains(queue));

			rc = TaskSpec.ofProcedure(env.app.newProcedure(() -> {
				queue.remove();
				return Procedure.LogicError; // 事务回滚，whileCommit 不执行
			}, "rollbackRemoveQueue")).call();
			assertEquals(Procedure.LogicError, rc);
			assertTrue(factories.contains(queue), "remove 回滚时不得注销 hotBeanFactories 登记");
		}
	}
}
