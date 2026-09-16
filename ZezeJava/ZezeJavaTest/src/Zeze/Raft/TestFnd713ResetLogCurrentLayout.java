package Zeze.Raft;

import java.io.File;
import java.nio.file.Files;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Raft Test.resetLog 按现行存储布局清理节点日志数据（FND7-13）。
 * Raft日志/状态早已从<DbHome>/logs、<DbHome>/rafts独立目录迁入<DbHome>/db共享库的
 * 列族（<raftName>.logs/<raftName>.rafts，see LogSequence构造），resetLog仍删旧目录：
 * 三处删除目标不存在，deletedDirectoryAndCheck静默通过，真正的db目录未动——节点带着
 * 旧日志/旧term/旧unique存根重启，"InstallSnapshot Clean One Node Data"故障注入不再
 * 强制触发InstallSnapshot（日志完好走普通AppendEntries补齐），测试场景静默失效。
 * 修复：resetLogData按列族精确删除.logs/.rafts（保留unique存根列族），并删snapshot.dat。
 * 直接合成现行布局的DbHome现场验证清理契约；修复前本测试无法编译（helper不存在，
 * 旧代码只删不存在的旧目录），修复后断言通过。
 */
@Fast
public class TestFnd713ResetLogCurrentLayout {
	private static final String dbHome = "TestFnd713ResetLogCurrentLayout.raft";
	// 列族名用raft.getName()（节点名原样，含冒号），DbHome才是冒号转下划线（Raft构造器）。
	private static final String raftName = "127.0.0.1:17710";

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() {
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	@Test
	public void testResetLogDropsLogColumnFamiliesKeepsUnique() throws Exception {
		// 合成现行布局现场：db库内含.logs/.rafts/unique列族（各带数据），外置snapshot.dat。
		Files.createDirectories(new File(dbHome).toPath());
		try (var db = new RocksDatabase(new File(dbHome, "db").getPath())) {
			db.getOrAddTable(raftName + ".logs").put(new byte[]{1}, new byte[]{1, 1});
			db.getOrAddTable(raftName + ".rafts").put(new byte[]{2}, new byte[]{2, 2});
			db.getOrAddTable(raftName + ".unique.2026.9.16").put(new byte[]{3}, new byte[]{3, 3});
		}
		var snapshotDat = new File(dbHome, "snapshot.dat");
		Files.write(snapshotDat.toPath(), new byte[]{9});

		// 执行resetLog的清理逻辑（startRaft(resetLog)在raft停止后调用它）。
		Zeze.Raft.Test.TestRaft.resetLogData(dbHome, raftName);

		assertFalse(snapshotDat.exists(), "committed snapshot file must be deleted");
		try (var db = new RocksDatabase(new File(dbHome, "db").getPath())) {
			var tables = db.getTableMap();
			assertFalse(tables.containsKey(raftName + ".logs"),
					"logs column family must be dropped so the node restarts with an empty log (forces InstallSnapshot)");
			assertFalse(tables.containsKey(raftName + ".rafts"),
					"rafts column family must be dropped (term/voteFor reset)");
			// unique存根列族保留（重复请求检测），数据完好。
			var unique = tables.get(raftName + ".unique.2026.9.16");
			assertTrue(unique != null, "unique request stub column family must be kept");
			assertArrayEquals(new byte[]{3, 3}, unique.get(new byte[]{3}));
			// 状态机目录不受影响（由InstallSnapshot恢复）。
			assertTrue(new File(dbHome, "statemachine").isDirectory() || !new File(dbHome, "statemachine").exists());
		}
	}
}
