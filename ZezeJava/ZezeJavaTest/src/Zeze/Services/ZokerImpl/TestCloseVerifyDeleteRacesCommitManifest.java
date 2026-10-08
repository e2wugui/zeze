package Zeze.Services.ZokerImpl;

import harness.Extra;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Services.Zoker;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND35 zoker-01 回归：closeAndVerify 的 md5 失配清场删除与同服务 commit 的互斥。
 * 修复前：摘账→close→校验→删除全程不持 commitLocks、不查 committingPrefixes——摘账后
 * FileBin 脱离 closeUnder 回收面，而清单校验（isFile）与 renameTo 之间无复查，一次常规
 * 清场删除落进屏障窗口：删除晚于 rename 形态=md5 未收口的字节随版本成版回执 0（混合
 * 内容假成功，本测试的确定性红）；删除早于 rename 形态=清单已放行文件被抽走，版本缺
 * 文件回执 0。修复后：校验+删除段与 commit 同 commitLocks（折叠服务段）串行——删除必
 * 先于 commit 的清单校验完成（isFile 见缺文件→eCommitFail），或 commit 先行清账
 * （closeUnder 删在途产物→同样 eCommitFail），两形态都不得假成功。
 * 交错注入点=测试 seam（摘账+句柄关闭后、清场删除前）：红路径（无互斥）commit 在删除
 * 前全程完成；绿路径（持锁）commit 阻塞在 commitLocks 上，注入点的有界 join 超时放行
 * 删除即完成交错推演（与 TestRunPidDeleteRacesConcurrentStart 同构）。
 */
@Fast
@Extra
public class TestCloseVerifyDeleteRacesCommitManifest {
	private static final long MD5_MISMATCH = IModule.errorCode(Zoker.ModuleId, Zoker.eMd5Mismatch);

	private static byte[] md5Of(byte[] data) throws Exception {
		var md5 = MessageDigest.getInstance("MD5");
		md5.update(data);
		return md5.digest();
	}

	/**
	 * 确定性交错：清单列入在途文件 f2，其 CloseFile 携带失配摘要（并发同名分发会话/
	 * 单客户端 close 超时-重试形成的新旧会话重叠形态）。注入点内发起并发 commit——
	 * 修复前 commit 在删除落盘前完整通过清单校验并 rename，未收口字节成版回执 0；
	 * 修复后 commit 排队到删除之后，清单见缺文件 eCommitFail，无版本成版。
	 */
	@Test
	public void testMd5MismatchDeleteCannotSneakPastCommitBarrier(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services").toFile();
		var dm = new DistributeManager(distributeDir, servicesDir);

		// 真实流量形态（ServiceName=空串，服务段取自 FileName 首段）：f1 已 md5 收口
		var good = "good-app".getBytes();
		var bin1 = dm.open("", "svc/app.jar", null);
		bin1.append(0, new Binary(good));
		assertEquals(0, dm.closeAndVerify("", "svc/app.jar", new Binary(md5Of(good)), null));
		// f2 在途且暂存区内容与客户端摘要必失配（断点续传残留≠本地前缀的常态故障形态）
		var bin2 = dm.open("", "svc/lib/x.jar", null);
		bin2.append(0, new Binary("corrupted-partial".getBytes()));
		var wrongMd5 = md5Of("client-complete-different".getBytes());
		// 部署方清单：两文件齐全（f2 列入——屏障按清单声明校验集合）
		Files.writeString(distributeDir.toPath().resolve("svc")
						.resolve(DistributeManager.distributeManifestName("v1")),
				"svc/app.jar\nsvc/lib/x.jar\n");

		var commitRc = new AtomicLong();
		var commitFailure = new AtomicReference<Throwable>();
		var committer = new Thread(() -> {
			try {
				commitRc.set(dm.commit("svc", "v1"));
			} catch (Throwable t) {
				commitFailure.set(t);
			}
		});
		// seam：f2 摘账+句柄关闭后、清场删除前暂停——红路径 commit 在此窗口全程完成；
		// 绿路径 commit 阻塞在 commitLocks 上，join 有界超时放行删除。
		dm.setCloseVerifyBeforeDeleteHookForTest(() -> {
			committer.start();
			try {
				committer.join(5_000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});

		var closeRc = dm.closeAndVerify("", "svc/lib/x.jar", new Binary(wrongMd5), null);
		committer.join(60_000);
		assertEquals(false, committer.isAlive(), "commit 线程应已结束");
		assertNull(commitFailure.get(), "commit 不得异常终止");
		assertEquals(MD5_MISMATCH, closeRc, "失配 close 的三态返回不受修复影响");
		assertNotEquals(0L, commitRc.get(),
				"清单已声明的文件在 CloseFile 失配未收口时 commit 不得回执成功"
						+ "（修复前：未收口字节随 rename 成版回执 0）");
		var version = Path.of(servicesDir.getPath(), "svc", "v1");
		assertFalse(Files.exists(version), "屏障闭合：缺文件/未收口内容不得成版切 current");
		// 失配产物按部署语义弃置：重传从 0 开始（状态机闭合不受串行化影响）
		assertFalse(Files.exists(distributeDir.toPath().resolve("svc").resolve("lib").resolve("x.jar")),
				"失配文件在 commit 失败后仍须被清场删除");
		var reopened = dm.open("", "svc/lib/x.jar", null);
		try {
			assertEquals(0, reopened.getLength(), "坏文件删除后 OpenFile 从 0 续传");
		} finally {
			reopened.close();
		}
	}
}
