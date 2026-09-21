package Zeze.Arch.Gen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.RedirectToServer;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND7-33回归：GenModule离线生成写文件失败吞IOException且FileOutputStream
 * 打开即截断旧文件——磁盘满/权限/路径问题时目标源码树留下空/半截.java，
 * 异常仅printStackTrace，生成流程照样报成功，错误延后到编译期爆发。
 * 修复：临时文件+原子move（失败不触碰旧文件），失败删临时文件并上抛，
 * 由generateRedirectSources既有catch包装模块上下文中止生成。
 * <p>
 * 失败注入用不存在的源码根目录（createTempFile即抛IOException，平台无关）；
 * 成功路径（新建/同内容跳过/覆盖）作护栏。
 */
@Fast
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

	/** 写失败必须上抛（原实现吞IOException后照常返回null=成功）。 */
	@Test
	public void testWriteFailurePropagates() {
		var ex = Assertions.assertThrows(IllegalStateException.class,
					() -> GenModule.instance.generateRedirectSources(
							tempDir.resolve("not_exists_dir").toString(), dummyApp, new Class<?>[]{TestModule.class}, false),
				"写文件失败必须中止生成（FND7-33：吞IOException留半截源码报成功）");
		Assertions.assertNotNull(ex.getCause(), "须携带原始原因");
		Assertions.assertTrue(ex.getCause() instanceof IOException, "原因必须是IOException");
		Assertions.assertTrue(ex.getMessage().contains(TestModule.class.getName()), "须携带模块类上下文");
	}

	/** 成功路径护栏：生成文件内容正确；同内容重复生成跳过写入。 */
	@Test
	public void testSuccessPathUnchanged() throws Exception {
		GenModule.instance.generateRedirectSources(tempDir.toString(), dummyApp, new Class<?>[]{TestModule.class}, false);
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
		GenModule.instance.generateRedirectSources(tempDir.toString(), dummyApp, new Class<?>[]{TestModule.class}, false);
		Assertions.assertEquals(firstModified, Files.getLastModifiedTime(file), "内容未变不得重写文件");
	}
}
