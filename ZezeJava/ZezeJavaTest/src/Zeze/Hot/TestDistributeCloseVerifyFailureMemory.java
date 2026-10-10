package Zeze.Hot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Config;
import Zeze.Net.Binary;

/**
 * 发布上传的关闭验证失败是终态：重复CloseFile不得把失败伪装成成功。
 * <p>
 * closeAndVerify先files.remove再校验，md5不符返回false后条目已消失——修复前重复
 * CloseFile走"未打开即true"分支返回成功，损坏文件留在分发目录且客户端重试拿到
 * 假成功；commitDistribute只创建ready，没有失败终态可供检查。修复：
 * ①校验不符或close异常登记失败终态（key=canonical文件名），重试返回原失败结果，
 * 重新open即新上传代际、清除记忆重新验证；②commitDistribute存在失败记忆时拒绝
 * 创建ready；③closeAll（会话边界setPrepare/setIdle）清空记忆。
 * "从未打开"的close保持true（断线重试/服务端重启后的确认语义不变）。
 * 自包含：NoDatabase轻量Application+@TempDir，不依赖外部进程。
 */
@Fast
public class TestDistributeCloseVerifyFailureMemory {
	@TempDir
	static Path tempDir;

	private static Application app;
	private static AppBase appBase;

	@BeforeAll
	public static void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setNoDatabase(true);
		conf.setDefaultTableConf(new Config.TableConf());
		app = new Application("TestDistributeCloseVerifyMemory", conf);
		appBase = new AppBase() {
			@Override
			public Application getZeze() {
				return app;
			}
		};
	}

	private static DistributeManager newDistributeManager(String name) throws Exception {
		var distributeDir = tempDir.resolve(name).resolve("distributes");
		Files.createDirectories(distributeDir);
		var workingDir = tempDir.resolve(name).resolve("working");
		Files.createDirectories(workingDir.resolve("interfaces"));
		Files.createDirectories(workingDir.resolve("modules"));
		var hotManager = new HotManager(appBase, workingDir.toString(), distributeDir.toString());
		return new DistributeManager(hotManager);
	}

	private static Binary md5Of(byte[] data) throws Exception {
		return new Binary(MessageDigest.getInstance("MD5").digest(data));
	}

	@Test
	public void testRetryCloseAfterMd5MismatchReturnsOriginalFailure() throws Exception {
		var dm = newDistributeManager("retry-mismatch");
		dm.open("mymod.jar").append(0, new Binary("CORRUPT-PAYLOAD".getBytes(StandardCharsets.UTF_8)));
		var wrongMd5 = md5Of("different".getBytes(StandardCharsets.UTF_8));
		Assertions.assertFalse(dm.closeAndVerify("mymod.jar", wrongMd5), "首次校验不符必须false");
		Assertions.assertFalse(dm.closeAndVerify("mymod.jar", wrongMd5),
				"重复CloseFile必须返回原失败结果（修复前：条目已移除、未打开即true）");
		// 从未打开的文件保持true（断线重试/服务端重启后的确认语义不变）
		Assertions.assertTrue(dm.closeAndVerify("never-opened.jar", wrongMd5));
		// 失败未消除时提交必须被拒，ready不得创建
		Assertions.assertThrows(IOException.class, dm::commitDistribute,
				"存在关闭验证失败的文件时不得创建ready");
		Assertions.assertFalse(Files.exists(Path.of(dm.getHotManager().getDistributeDir(), "ready")));
	}

	@Test
	public void testReopenNewGenerationRecoversAndCommitAllowed() throws Exception {
		var dm = newDistributeManager("reopen-recover");
		var content = "GOOD-PAYLOAD".getBytes(StandardCharsets.UTF_8);
		dm.open("mymod.jar").append(0, new Binary(content));
		var wrongMd5 = md5Of("other".getBytes(StandardCharsets.UTF_8));
		Assertions.assertFalse(dm.closeAndVerify("mymod.jar", wrongMd5));
		Assertions.assertFalse(dm.closeAndVerify("mymod.jar", wrongMd5));

		// 重新open=新上传代际：FileBin按现有内容重建md5，等值续传后以正确md5关闭成功。
		dm.open("mymod.jar").append(content.length, new Binary(new byte[0]));
		Assertions.assertTrue(dm.closeAndVerify("mymod.jar", md5Of(content)), "重传后重新验证必须成功");
		Assertions.assertTrue(dm.closeAndVerify("mymod.jar", md5Of(content)), "成功后的重复close保持幂等成功");

		dm.commitDistribute(); // 失败记忆已清除，提交不再被拒
		Assertions.assertTrue(Files.exists(Path.of(dm.getHotManager().getDistributeDir(), "ready")));
	}

	@Test
	public void testCloseAllClearsFailureMemoryAtSessionBoundary() throws Exception {
		var dm = newDistributeManager("closeall-boundary");
		dm.open("mymod.jar").append(0, new Binary("X".getBytes(StandardCharsets.UTF_8)));
		Assertions.assertFalse(dm.closeAndVerify("mymod.jar", md5Of("Y".getBytes(StandardCharsets.UTF_8))));
		dm.closeAll(); // 会话边界（setPrepare/setIdle）清空失败记忆
		Assertions.assertTrue(dm.closeAndVerify("mymod.jar", md5Of("Y".getBytes(StandardCharsets.UTF_8))),
				"会话边界后回到\"未打开即true\"语义，新会话重传重新验证");
	}
}
