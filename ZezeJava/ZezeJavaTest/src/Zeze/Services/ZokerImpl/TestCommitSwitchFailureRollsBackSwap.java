package Zeze.Services.ZokerImpl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import Zeze.IModule;
import Zeze.Services.Zoker;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 隔离换装的 switchCurrent 失败回滚链：腾位（旧版本目录改名进暂存删除名）+安装
 * （distributes rename 到版本名）成功后指针切换 IO 失败时，修复前仅记日志返回
 * eCommitFail——同版本重部署（current 文本本就指向该版本名）形态下指针目标的
 * <b>内容</b>已被静默换新（回执失败而部署实际生效），旧内容仅存暂存名待下轮 prune
 * 入口清扫灭失。修复后换装路径补回滚：新内容退回 distributes（重试重走完整换装，
 * 不产生跳装假成功）、旧内容复位版本名（指针文本未变，复位即现役内容恢复）。
 * 失败注入：current 固有位置放目录占位——AtomicFileWriter 的原子 rename 对目录
 * 目标必失败（两主流平台同语义），等价真实失败面（ENOSPC/AV 钉住）的可控形态。
 * 纯新增安装路径（无腾位）固化既有语义：current 未动不回滚，重试跳装再切收敛。
 */
@Fast
public class TestCommitSwitchFailureRollsBackSwap {
	private static final long COMMIT_FAIL = IModule.errorCode(Zoker.ModuleId, Zoker.eCommitFail);
	/** 暂存删除名前缀（与 DistributeManager 的常量同字面；测试内联使红态可先于实现编译）。 */
	private static final String STAGE_PREFIX = ".zoker-deleting.";

	/** distributes/&lt;svc&gt;/ 放文件+裸名集合清单（行含服务名首段，与 ZokerAgent 同构）。 */
	private static void upload(File distributeDir, String serviceName, String fileName, String content)
			throws Exception {
		var svc = distributeDir.toPath().resolve(serviceName);
		Files.createDirectories(svc);
		Files.writeString(svc.resolve(fileName), content);
		Files.writeString(svc.resolve(DistributeManager.DISTRIBUTE_MANIFEST_NAME),
				serviceName + "/" + fileName + "\n");
	}

	private static List<String> stageDirs(Path svcDir) throws Exception {
		try (var list = Files.list(svcDir)) {
			return list.filter(Files::isDirectory).map(p -> p.getFileName().toString())
					.filter(n -> n.startsWith(STAGE_PREFIX)).toList();
		}
	}

	/** current 固有位置换成目录占位：指针原子换版必失败。 */
	private static void blockCurrentPointer(Path svcDir) throws Exception {
		Files.deleteIfExists(svcDir.resolve(DistributeManager.CURRENT_NAME));
		Files.createDirectories(svcDir.resolve(DistributeManager.CURRENT_NAME));
	}

	/** 核心红点：换装+切换失败——旧内容必须复位版本名（修复前静默换新）、新内容退回
	 * distributes（修复前滞留版本名位置）、暂存名回收（修复前残留待下轮清扫灭失）；
	 * 解除占位后重试完整换装收敛（新内容实际成版）。 */
	@Test
	public void testSwitchFailureRestoresOldVersionAndRestagesNewContent(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir);
		var dm = new DistributeManager(distributeDir, servicesDir.toFile());
		var svcDir = servicesDir.resolve("svc");
		var v1 = svcDir.resolve("v1");

		upload(distributeDir, "svc", "app.jar", "old-body");
		assertEquals(0, dm.commit("svc", "v1"), "前置：首版提交成功");

		upload(distributeDir, "svc", "app.jar", "new-body-of-different-size");
		blockCurrentPointer(svcDir);
		assertEquals(COMMIT_FAIL, dm.commit("svc", "v1"), "切换失败必须可见失败");

		assertEquals("old-body", Files.readString(v1.resolve("app.jar")),
				"回滚后版本名位置恢复旧内容（修复前静默换新，同版本形态下现役被换）");
		assertEquals("new-body-of-different-size",
				Files.readString(distributeDir.toPath().resolve("svc").resolve("app.jar")),
				"新内容退回 distributes（修复前滞留版本名位置，旧内容仅存暂存名）");
		assertTrue(Files.readString(v1.resolve(DistributeManager.DISTRIBUTE_MANIFEST_NAME)).contains("app.jar"),
				"复位的旧内容自带清单（重试换装判据的输入）");
		assertEquals(List.of(), stageDirs(svcDir), "腾位暂存名被回滚消费（修复前残留待下轮清扫灭失）");

		// 解除指针占位后重试：完整换装收敛（非跳装假成功——退回 distributes 的新内容重新比对）
		Files.deleteIfExists(svcDir.resolve(DistributeManager.CURRENT_NAME));
		assertEquals(0, dm.commit("svc", "v1"), "重试收敛");
		assertEquals("new-body-of-different-size", Files.readString(v1.resolve("app.jar")),
				"重试后新内容实际成版（若复位形态被跳装收养则仍是旧字节）");
		assertEquals("v1", Files.readString(svcDir.resolve(DistributeManager.CURRENT_NAME)));
		assertFalse(Files.exists(distributeDir.toPath().resolve("svc")), "重试消费退回的新内容");
		assertEquals(List.of(), stageDirs(svcDir), "成功轮无暂存残留");
	}

	/** 边界固化：纯新增安装（无腾位）的切换失败不回滚——current 未动、新内容已装好，
	 * 重试跳装再切收敛（既有语义，回滚仅限换装路径）。 */
	@Test
	public void testFreshInstallSwitchFailureKeepsInstalledForRetry(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir);
		var dm = new DistributeManager(distributeDir, servicesDir.toFile());
		var svcDir = servicesDir.resolve("svc");
		var v1 = svcDir.resolve("v1");

		blockCurrentPointer(svcDir);
		upload(distributeDir, "svc", "app.jar", "fresh-body");
		assertEquals(COMMIT_FAIL, dm.commit("svc", "v1"), "切换失败必须可见失败");

		assertEquals("fresh-body", Files.readString(v1.resolve("app.jar")), "新内容保持已装（无腾位无需回滚）");
		assertFalse(Files.exists(distributeDir.toPath().resolve("svc")), "安装已消费暂存区");
		assertEquals(List.of(), stageDirs(svcDir), "纯新增路径无腾位（也无暂存名）");

		Files.deleteIfExists(svcDir.resolve(DistributeManager.CURRENT_NAME));
		assertEquals(0, dm.commit("svc", "v1"), "重试跳装再切收敛");
		assertEquals("fresh-body", Files.readString(v1.resolve("app.jar")));
		assertEquals("v1", Files.readString(svcDir.resolve(DistributeManager.CURRENT_NAME)));
	}
}
