package Zeze.Services.ZokerImpl;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND24 zoker-01 守卫：目录世代身份（DistributeManager.dirGeneration）原语语义直驱。
 * open 的世代锚点复检全部承力于这个原语的四条不变量，违反任一条即复检失效（假接受=写改
 * 现役版本，或假拒绝=合法续传被拒）：
 * <ol>
 * <li>存在目录的身份非空（空串专用于"目录不存在"——锚点空=open 无条件拒绝的输入）；</li>
 * <li>子文件增删不变（不变则同目录上传期间的并发写会把合法世代误判为换代——这正是
 *     lastModifiedTime 不可用作身份的原因）；</li>
 * <li>rename 搬走后原路径身份变空（commit 把 distributes/&lt;svc&gt; 搬进 services 的直接证据）；</li>
 * <li>同路径重建得到可判别的新身份（世代判别的核心：fileKey=dev+ino，Linux 上被 rename 走的
 *     旧目录仍持有原 inode，重建必得新值；Windows 依赖 creationTime，同 tick 碰撞以有界重试
 *     兜底）。退化平台（"?"恒等）在本守卫下必须红——世代防护失效不可静默。</li>
 * </ol>
 * 私有静态方法反射直驱（结构性判别先例：TestE02CommitLockCaseFolding 反射读 commitLocks）。
 * 全链"commit 完整起止于 FileBin 构造窗内"的确定性红绿需故障注入缝（README FND25 首项），
 * 本守卫锁的是修复所依赖的平台行为链——不依赖时序，全平台确定性。
 */
@Fast
public class TestDirGenerationIdentity {

	private static String dirGeneration(Path dir) throws Exception {
		Method method = DistributeManager.class.getDeclaredMethod("dirGeneration", Path.class);
		method.setAccessible(true);
		return (String)method.invoke(null, dir);
	}

	/** 不变量1+2：存在目录身份非空且对子文件增删稳定（并发上传常态不得误判换代）。 */
	@Test
	public void testIdentityStableAcrossChildChanges(@TempDir Path tempDir) throws Exception {
		var svc = Files.createDirectories(tempDir.resolve("svc"));
		var identity = dirGeneration(svc);
		assertFalse(identity.isEmpty(), "存在目录的世代身份非空（空串=不存在专用）");

		Files.writeString(svc.resolve("a.jar"), "partial-upload");
		assertEquals(identity, dirGeneration(svc), "子文件新增不得改变目录世代身份");
		Files.writeString(svc.resolve("b.jar"), "another");
		assertEquals(identity, dirGeneration(svc), "子文件再增不得改变目录世代身份");
		Files.delete(svc.resolve("a.jar"));
		assertEquals(identity, dirGeneration(svc), "子文件删除不得改变目录世代身份");
	}

	/** 不变量3+4：rename 搬走原路径变空；同路径重建得到可判别的新身份。 */
	@Test
	public void testRecreatedDirectoryGetsDistinctIdentity(@TempDir Path tempDir) throws Exception {
		var svcPath = tempDir.resolve("svc");
		var svc = Files.createDirectories(svcPath);
		var oldIdentity = dirGeneration(svc);

		// commit 的 rename 语义：整个服务目录被搬走，原路径不复存在
		Files.move(svc, svcPath.resolveSibling("v1"));
		assertEquals("", dirGeneration(svcPath), "被 rename 搬走后原路径身份=空（换代最直接证据）");
		assertEquals(oldIdentity, dirGeneration(svcPath.resolveSibling("v1")),
				"目录实体身份随目录走（rename 不换代，W10/跳装场景锚==今接受的正确性基础）");

		// 同路径重建：新世代必须可判别。Linux 首轮必过（旧 inode 仍被 v1 持有，重建必得新 inode）；
		// Windows 依赖 creationTime，同 tick 碰撞以有界重试兜底（重试间旧目录身份恒定，不引入抖动）。
		Files.createDirectories(svcPath);
		for (int attempt = 0; dirGeneration(svcPath).equals(oldIdentity); attempt++) {
			assertTrue(attempt < 200, "同路径重建目录必须得到与旧世代可判别的身份"
					+ "（old=" + oldIdentity + "，持续碰撞或平台退化\"?\"=世代防护失效，红）");
			Files.delete(svcPath); // 重建目录为空，直接删除重试
			//noinspection BusyWait
			Thread.sleep(5);
			Files.createDirectories(svcPath);
		}
	}

	/** 不变量1（不存在面）：从未存在的路径身份=空串。 */
	@Test
	public void testMissingDirectoryIsEmpty(@TempDir Path tempDir) throws Exception {
		assertEquals("", dirGeneration(tempDir.resolve("never-exist")),
				"不存在目录身份=空串（open 的空锚点无条件拒绝分支的输入面）");
	}
}
