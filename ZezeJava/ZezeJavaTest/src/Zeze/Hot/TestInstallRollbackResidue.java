package Zeze.Hot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Config;
import Zeze.Util.InMemoryJavaCompiler;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND16 hot-01 红绿钉板：HotManager._install 的 modules.put 新装条目必须登记回滚清理。
 * <p>
 * 修复前：put 后失败（setService 之前）走 txn.rollback，MainRollbackAction.recoverModules
 * 只恢复旧模块（新装条目 exists==null 无恢复也无清理），残留 service==null 的 HotModule——
 * 下次重装该 namespace 在锁外 stopBefore() 必 NPE，热更通道砖死到进程重启（红）。
 * 修复后：put 即登记 whileRollback(modules.remove+close)，回滚逆序最先执行
 * （close 释放懒开 jar 句柄后，jar rename 回退才在 Windows 上可行）。
 * <p>
 * 钉板直接反射调 _install + 手动 rollback：_install 内的登记动作就是修复本体，
 * MainRollbackAction（旧模块恢复）与本修复正交由 install 全路径承担。
 */
@Fast
public class TestInstallRollbackResidue {
	private static final String NS = "fnd16hot1.A1";
	private static final String MODULE_CLASS = "fnd16hot1.A1.ModuleA1";

	private static final String MODULE_SRC = """
			package fnd16hot1.A1;
			public class ModuleA1 implements Zeze.IModule {
			    public static final int ModuleId = 18791;
			    public ModuleA1(Zeze.AppBase app) {
			    }
			    @Override
			    public String getFullName() {
			        return "fnd16hot1.A1";
			    }
			    @Override
			    public String getName() {
			        return "A1";
			    }
			    @Override
			    public int getId() {
			        return ModuleId;
			    }
			}
			""";

	@TempDir
	Path tempDir;

	private static AppBase dummyApp;

	@BeforeAll
	public static void setUp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		var app = new Application("fnd16hot1", config);
		dummyApp = new AppBase() {
			@Override
			public Application getZeze() {
				return app;
			}
		};
	}

	@Test
	public void testRollbackRemovesInstalledModuleEntry() throws Exception {
		var workingDir = tempDir.resolve("w");
		Files.createDirectories(workingDir.resolve("modules"));
		Files.createDirectories(workingDir.resolve("interfaces"));
		var distributeDir = Files.createDirectories(tempDir.resolve("dist"));
		var manager = new HotManager(dummyApp, workingDir.toString(), distributeDir.toString());

		var modulesField = HotManager.class.getDeclaredField("modules");
		modulesField.setAccessible(true);
		@SuppressWarnings("unchecked")
		var modules = (Map<String, HotModule>)modulesField.get(manager);
		try {
			// distributes 下放 NS 的 jar 对：module jar（编译模块类）+ interface jar（空 zip）。
			var compiler = new InMemoryJavaCompiler();
			compiler.useOptions("-cp", System.getProperty("java.class.path"));
			var moduleBytes = compiler.compileAllToByteCode(Map.of(MODULE_CLASS, MODULE_SRC)).get(MODULE_CLASS);
			Assertions.assertNotNull(moduleBytes, "模块类必须编译成功");
			writeJar(distributeDir.resolve(NS + ".interface.jar"), Map.of()); // putJar 需合法 zip
			writeJar(distributeDir.resolve(NS + ".jar"), Map.of("fnd16hot1/A1/ModuleA1.class", moduleBytes));

			var txn = new HotTransaction("test-fnd16-hot01");
			var install = HotManager.class.getDeclaredMethod("_install", String.class, HotTransaction.class);
			install.setAccessible(true);
			install.invoke(manager, NS, txn);
			Assertions.assertTrue(modules.containsKey(NS), "_install成功后新装条目应在册");
			Assertions.assertNull(modules.get(NS).getService(), "put时点service尚未装配（半成品，即残留危害形态）");

			// 失败回滚（install 失败路径的等价动作序列）：必须移除新装条目——
			// 残留会使下次重装 exists.add(残留) 后锁外 stopBefore 对 null service 必 NPE。
			txn.rollback();
			Assertions.assertFalse(modules.containsKey(NS),
					"回滚必须移除新装条目（hot-01：残留半成品使重装stopBefore必NPE，通道砖死）");
		} finally {
			// 防御收尾：万一断言失败残留条目，也要释放 jar 句柄（Windows 下 @TempDir 清理）。
			var hotModule = modules.remove(NS);
			if (hotModule != null)
				hotModule.close();
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
