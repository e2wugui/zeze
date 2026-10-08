package Zeze.Services.ZokerImpl;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.IModule;
import Zeze.Services.Zoker;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND24 zoker-07 守卫：commitLocks 键折叠必须剥尾部点/空格（foldVersionName）。
 * Windows(Win32) 路径规范化大小写不敏感且剥尾点/空格："svc"/"Svc"/"svc." 指向同一物理容器
 * ——仅 toLowerCase 折叠下三个名字三把锁，commit 三步（install→switch→prune）完全交错，
 * keepVersions=1 时 A 的 prune 可删 B 已 install 未 switch 的版本（current 悬空，返回 0
 * 服务永不可启动）。修复=锁键复用 foldVersionName（剥尾点/空格+小写）。
 * 结构性红（确定性，先例 TestCommitLockCaseFolding 反射读 commitLocks）：变体名 commit 后
 * 必须共享同一锁条目——修复前为 3 个（"svc"/"svc."/"svc.. "）。commit 无分发内容即失败
 * （eCommitFail）不影响锁条目建立，直构空目录即可，无需真实分发。
 */
@Fast
public class TestCommitLockTrailingDotFolding {

	private static final long COMMIT_FAIL = IModule.errorCode(Zoker.ModuleId, Zoker.eCommitFail);

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, Object> commitLocksOf(DistributeManager dm) throws Exception {
		Field field = DistributeManager.class.getDeclaredField("commitLocks");
		field.setAccessible(true);
		return (ConcurrentHashMap<String, Object>)field.get(dm);
	}

	/**
	 * 核心红点：大小写+尾点/尾空格变体必须折叠到同一锁条目（本案病灶=锁键）。
	 * 修复前红点：commitLocks 含 "svc"、"svc."、"svc.. " 三个条目（三把锁，互斥失效）。
	 */
	@Test
	public void testTrailingDotAndSpaceVariantsShareOneLockEntry(@TempDir Path tempDir) throws Exception {
		var dm = new DistributeManager(Files.createDirectories(tempDir.resolve("distributes")).toFile(),
				Files.createDirectories(tempDir.resolve("services")).toFile());

		assertEquals(COMMIT_FAIL, dm.commit("svc", "v1"), "无分发内容，失败在锁内（锁条目已建立）");
		assertEquals(COMMIT_FAIL, dm.commit("svc.", "v1"), "尾点变体（isSafePathSegment 通过）");
		assertEquals(COMMIT_FAIL, dm.commit("Svc.. ", "v1"), "大小写+尾点+尾空格变体");

		var locks = commitLocksOf(dm);
		assertEquals(1, locks.size(), "变体名必须折叠为同一把锁: " + locks.keySet());
		assertTrue(locks.containsKey("svc"), "折叠键=剥尾点/空格+小写（foldVersionName 语义）: " + locks.keySet());
	}
}
