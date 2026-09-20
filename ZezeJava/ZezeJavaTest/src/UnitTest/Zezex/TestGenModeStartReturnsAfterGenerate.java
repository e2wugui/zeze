package UnitTest.Zezex;

import java.nio.file.Files;
import java.nio.file.Path;

import Zeze.Arch.Gen.GenModule;
import Game.App;
import Game.Rank.ModuleRank;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * 生成模式Start协议回归：Game.App在-GenFileSrcRoot下生成Redirect代码后【返回】。
 * 原实现createRedirectModules内System.exit(0)——进程内不可测试（一调即杀JVM）、
 * 隐式控制流、跳过一切清理；重构后终止权归应用层（main见标志不进入wait，进程
 * 自然退出）。Start能执行到断言即"不再exit"的核心证明。
 */
@Fast
@Isolated // genFileSrcRoot是JVM级全局开关，且Game.App单例started置位后不复位，独占运行
public class TestGenModeStartReturnsAfterGenerate {
	@TempDir
	Path tempDir;

	@AfterEach
	public void tearDown() {
		GenModule.instance.genFileSrcRoot = null; // JVM级全局开关必须恢复
	}

	@Test
	public void testStartGeneratesAndReturns() throws Exception {
		App.getInstance().Start(new String[]{"-GenFileSrcRoot", tempDir.toString()});
		var file = tempDir.resolve(GenModule.REDIRECT_PREFIX + ModuleRank.class.getName().replace('.', '_') + ".java");
		Assertions.assertTrue(Files.exists(file), "生成文件必须存在: " + file);
	}
}
