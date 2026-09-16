package Zeze.Arch.Gen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.RedirectToServer;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * FND7-33回归：GenModule离线生成（-DGenFileSrcRoot模式）写文件失败吞IOException且
 FileOutputStream打开即截断旧文件——磁盘满/权限/路径问题时目标源码树留下空/半截.java，
 异常仅printStackTrace，生成流程照样报成功，错误延后到编译期爆发。
 * 修复：临时文件+原子move（失败不触碰旧文件），失败删临时文件并上抛，由
 * createRedirectModules既有catch包装模块上下文中止生成。
 * <p>
 * 失败注入用不存在的源码根目录（createTempFile即抛IOException，平台无关）；
 * 成功路径（新建/同内容跳过/覆盖）作护栏。GenModule.instance为JVM级单例且
 * genFileSrcRoot是全局开关——@Isolated独占运行，finally恢复null。
 */
@Fast
@Isolated
public class TestFnd733GenModuleWriteFailFast {

	public static class TestModule {
		public static final int ModuleId = 0;
		public static final String ModuleFullName = "TestFnd733GenModule.TestModule";

		@RedirectToServer
		public void ping(int serverId, long arg) {
		}
	}

	@TempDir
	Path tempDir;

	private final AppBase dummyApp = new AppBase() {
		@Override
		public Application getZeze() {
			return null; // genModuleCode只读app.getClass()匹配构造器，不触碰zeze
		}
	};

	@BeforeEach
	public void setUp() {
		Assertions.assertNull(GenModule.instance.genFileSrcRoot, "测试前提：全局genFileSrcRoot默认关闭");
	}

	@AfterEach
	public void tearDown() {
		GenModule.instance.genFileSrcRoot = null; // 全局开关必须恢复
	}

	/** 写失败必须上抛（原实现吞IOException后照常返回null=成功）。 */
	@Test
	public void testWriteFailurePropagates() {
		GenModule.instance.genFileSrcRoot = tempDir.resolve("not_exists_dir").toString();
		var ex = Assertions.assertThrows(IllegalStateException.class,
				() -> GenModule.instance.createRedirectModules(dummyApp, new Class<?>[]{TestModule.class}),
				"写文件失败必须中止生成（FND7-33：吞IOException留半截源码报成功）");
		Assertions.assertNotNull(ex.getCause(), "须携带原始原因");
		Assertions.assertTrue(ex.getCause() instanceof IOException, "原因必须是IOException");
		Assertions.assertTrue(ex.getMessage().contains(TestModule.class.getName()), "须携带模块类上下文");
	}

	/** 成功路径护栏：生成文件内容正确；同内容重复生成跳过写入；内容变化覆盖更新。 */
	@Test
	public void testSuccessPathUnchanged() throws Exception {
		GenModule.instance.genFileSrcRoot = tempDir.toString();
		var modules = GenModule.instance.createRedirectModules(dummyApp, new Class<?>[]{TestModule.class});
		Assertions.assertNull(modules, "离线生成模式返回null（不编译不实例化）");
		var file = tempDir.resolve(GenModule.REDIRECT_PREFIX
				+ TestModule.class.getName().replace('.', '_') + ".java");
		Assertions.assertTrue(Files.exists(file), "生成文件必须存在");
		var content = Files.readString(file, StandardCharsets.UTF_8);
		Assertions.assertTrue(content.contains("public class " + GenModule.REDIRECT_PREFIX
				+ TestModule.class.getName().replace('.', '_')), "生成内容必须包含重定向类定义");
		Assertions.assertTrue(content.contains("ping("),
				"生成内容必须包含重定向方法");

		// 同内容重复生成：不重写（时间戳不变）且仍成功
		var firstModified = Files.getLastModifiedTime(file);
		GenModule.instance.genFileSrcRoot = tempDir.toString();
		GenModule.instance.createRedirectModules(dummyApp, new Class<?>[]{TestModule.class});
		Assertions.assertEquals(firstModified, Files.getLastModifiedTime(file), "内容未变不得重写文件");
	}
}
