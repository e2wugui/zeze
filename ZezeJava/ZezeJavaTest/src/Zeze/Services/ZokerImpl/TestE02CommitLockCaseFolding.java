package Zeze.Services.ZokerImpl;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
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
 * FND21 GE-C02：GE-C04 串行化锁键的大小写逃逸（FND20 GE-C04 修复的逃逸）。
 * commitLocks 的键原为裸 serviceName：Windows(NTFS) 大小写不敏感解析下 "svc"/"Svc" 指向
 * 同一物理容器却各持一把锁，互斥失效——FND20 GE-C04 已实证的"prune 删除并发方已 install
 * 未 switch 的版本目录 → current 悬空"竞态（TestFnd20E04 锤式加压基线两次复现）经大小写
 * 变体复活。修复：锁键大小写折叠（toLowerCase(Locale.ROOT)，与 GE-C01 同一判据）。
 * <ul>
 * <li>结构性红（确定性）：反射读私有 commitLocks，"svc"/"Svc" 两次 commit 后必须共享同一
 * 个锁条目（修复前为 2 个）——本案病灶即键本身，直指病灶是最低噪声的红；</li>
 * <li>行为红（概率性，镜像 TestFnd20E04 的锤式形态）：两线程以大小写变体并发 commit
 * （keepVersions=1），不变式"每次返回 0 的 commit 之后 current 必解析到存在的版本目录"
 * 修复后由互斥确定性成立；修复前竞态窗口毫秒级、命中为概率性（红跑可能通过——窗口窄，
 * 记录在案，同 E04 先例）。</li>
 * </ul>
 * 直构 DistributeManager（包内构造器）；staging（distributes/svc 与 distributes/Svc 在
 * Windows 上同一物理目录）由测试线程竞争写/消费属既有良性竞态（失败码/上传异常跳过该轮）。
 */
@Fast
public class TestE02CommitLockCaseFolding {
	private static final int ROUNDS = 300;
	private static final String[] OLD_VERSIONS = {
			"v01", "v02", "v03", "v04", "v05", "v06", "v07", "v08", "v09", "v10",
			"v11", "v12", "v13", "v14", "v15", "v16", "v17", "v18", "v19", "v20",
	};

	private static void upload(Path distributeDir, String serviceName, String content) throws IOException {
		var svc = distributeDir.resolve(serviceName);
		Files.createDirectories(svc);
		Files.writeString(svc.resolve("app.jar"), content);
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, Object> commitLocksOf(DistributeManager dm) throws Exception {
		Field field = DistributeManager.class.getDeclaredField("commitLocks");
		field.setAccessible(true);
		return (ConcurrentHashMap<String, Object>)field.get(dm);
	}

	/** 结构性判别：大小写变体必须折叠到同一锁条目（本案病灶=锁键）。
	 * 修复前红点：commitLocks 含 "svc" 与 "Svc" 两个条目（两把锁，互斥失效）。 */
	@Test
	public void testCaseVariantsShareOneLockEntry(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		Files.createDirectories(servicesDir);
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

		upload(distributeDir, "svc", "lower");
		assertEquals(0, dm.commit("svc", "v1"));
		upload(distributeDir, "Svc", "upper");
		assertEquals(0, dm.commit("Svc", "v2"));

		var locks = commitLocksOf(dm);
		assertEquals(1, locks.size(), "大小写变体必须折叠到同一锁条目: " + locks.keySet());

		// 不同服务名不受折叠影响：仍是独立锁条目（并行度保留）
		upload(distributeDir, "other", "other-body");
		assertEquals(0, dm.commit("other", "v1"));
		assertEquals(2, locks.size(), "真不同的服务名仍各持锁: " + locks.keySet());
	}

	/** 行为判别（概率性红）：大小写变体并发 commit（keep=1）下 current 永不悬空。
	 * 镜像 TestFnd20E04 的锤式加压形态，仅把 t-b 的 serviceName 换成大小写变体——
	 * 修复前两线程各持不同锁（裸键），FND20 GE-C04 的 prune 竞态在同一物理容器上复活。 */
	@Test
	public void testConcurrentCaseVariantCommitNeverDanglesCurrent(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		Files.createDirectories(servicesDir);
		var svc = servicesDir.resolve("svc");

		// 预摆旧版本（mtime 拉老，加宽 prune 执行窗口）
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
		var dangling = new AtomicBoolean(false);
		var hammer = new Thread(() -> {
			for (var i = 0; i < ROUNDS; i++) {
				// t-a/t-b 仅 serviceName 大小写不同：Windows 上同一物理容器
				var svcName = Thread.currentThread().getName().equals("t-a") ? "svc" : "Svc";
				var version = Thread.currentThread().getName().equals("t-a") ? "v1" : "v2";
				try {
					upload(distributeDir, svcName, "body-" + Thread.currentThread().getName() + "-" + i);
				} catch (IOException e) {
					continue; // staging 被并发 commit 消费（良性竞态），跳过本轮
				}
				if (dm.commit(svcName, version) == 0) {
					successes.incrementAndGet();
					// GE-C04 固定不变式：commit 成功 ⇔ current 指向存在的版本目录
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
		var currentDir = DistributeManager.currentVersionDir(svc.toFile());
		assertNotNull(currentDir, "终态 current 不得悬空");
		assertTrue(currentDir.getName().equals("v1") || currentDir.getName().equals("v2"),
				"终态现役应为加压版本: " + currentDir);
		// 锁条目收敛于折叠后的单键（变体不再各留条目）——确定性红点（修复前为裸键双条目）
		assertEquals(1, commitLocksOf(dm).size(), "变体不得各留锁条目: " + commitLocksOf(dm).keySet());
	}
}
