package Zeze.Services.ZokerImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Services.Zoker;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND20 GE-C04：同服务 commit 串行化（keepVersions=1 正确性）。
 * 机制链：commit 三步（install→switch→prune）间无自洽性且 CommitService 为 Normal 派发可并发，
 * keep=1 时 A 的 prune 可删除并发 B 已 install 未 switch 的版本目录，B 随后 switch 使 current
 * 指向已删除目录（返回 0 但 currentVersionDir 恒 null，无自愈）。修复为 services/&lt;svc&gt;
 * 粒度互斥（对齐 R1-04 锁形态）。
 * 双线程同服务交替提交（v1/v2）加压：固定不变式"每次返回 0 的 commit 之后 current 必解析到
 * 存在的版本目录"（修复后由互斥确定性成立）；修复前该竞态窗口毫秒级，命中为概率性
 * （红跑可能通过——窗口窄，记录在案；绿跑确定性通过）。预摆 20 个旧版本目录加宽 prune
 * 的执行窗口以提高交错概率。
 * 直构 DistributeManager（包内构造器），纯文件逻辑；staging（distributes/svc）由测试线程
 * 竞争写/消费属既有良性竞态（失败码/上传异常跳过该轮），不影响被测不变式。
 */
@Fast
public class TestE04CommitSerializedKeepOne {
	private static final int ROUNDS = 300;
	private static final String[] OLD_VERSIONS = {
			"v01", "v02", "v03", "v04", "v05", "v06", "v07", "v08", "v09", "v10",
			"v11", "v12", "v13", "v14", "v15", "v16", "v17", "v18", "v19", "v20",
	};

	private static void upload(Path distributeDir, String content) throws IOException {
		var svc = distributeDir.resolve("svc");
		Files.createDirectories(svc);
		Files.writeString(svc.resolve("app.jar"), content);
	}

	@Test
	public void testConcurrentCommitKeepOneNeverDanglesCurrent(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		Files.createDirectories(servicesDir);
		var svc = servicesDir.resolve("svc");

		// 预摆旧版本（mtime 拉老，使每次 prune 都有大批删除、执行窗口加宽）
		Files.createDirectories(svc);
		var old = System.currentTimeMillis() - 1_000_000;
		for (var v : OLD_VERSIONS) {
			Files.createDirectories(svc.resolve(v));
			Files.writeString(svc.resolve(v).resolve("app.jar"), "old-" + v);
			assertTrue(svc.resolve(v).toFile().setLastModified(old));
		}
		Files.writeString(svc.resolve(DistributeManager.CURRENT_NAME), OLD_VERSIONS[0]);

		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());
		dm.setKeepVersions(1);

		var successes = new AtomicInteger();
		var dangling = new AtomicBoolean(false); // 立即检查发现的悬空（红信号）
		var hammer = new Thread(() -> {
			for (var i = 0; i < ROUNDS; i++) {
				try {
					upload(distributeDir, "body-" + Thread.currentThread().getName() + "-" + i);
				} catch (IOException e) {
					continue; // staging 被并发 commit 消费（良性竞态），跳过本轮
				}
				var version = Thread.currentThread().getName().equals("t-a") ? "v1" : "v2";
				if (dm.commit("svc", version) == 0) {
					successes.incrementAndGet();
					// GE-C04 固定不变式：commit 成功 ⇔ current 指向存在的版本目录。
					// 修复后由同服务互斥确定性成立；修复前 prune 竞态可在此观察到悬空。
					if (null == DistributeManager.currentVersionDir(svc.toFile()))
						dangling.set(true);
				}
				Thread.yield();
			}
		});
		var a = new Thread(hammer, "t-a");
		var b = new Thread(hammer, "t-b");
		a.start();
		b.start();
		a.join(120_000);
		b.join(120_000);
		assertEquals(false, a.isAlive(), "线程应已结束");
		assertEquals(false, b.isAlive(), "线程应已结束");

		assertTrue(successes.get() > 0, "加压应有成功提交");
		assertEquals(false, dangling.get(), "每次成功 commit 后 current 都必须解析到存在的版本目录");
		// 终态：current 不悬空，且名字 ∈ {v1,v2}；旧版本目录已被 keep=1 的 prune 清空
		var currentDir = DistributeManager.currentVersionDir(svc.toFile());
		assertNotNull(currentDir, "终态 current 不得悬空");
		assertTrue(currentDir.getName().equals("v1") || currentDir.getName().equals("v2"),
				"终态现役应为加压版本: " + currentDir);
		for (var v : OLD_VERSIONS)
			assertEquals(false, Files.exists(svc.resolve(v)), "旧版本应被清理: " + v);
	}
}
