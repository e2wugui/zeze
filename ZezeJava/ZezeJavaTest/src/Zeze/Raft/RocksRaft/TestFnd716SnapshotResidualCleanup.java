package Zeze.Raft.RocksRaft;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RocksRaft snapshot 残留 checkpoint 清理（FND7-16）。
 * checkpoint_<ts> 是状态机RocksDB的完整物理拷贝，快照失败/删除失败会残留，反复重试
 * 渐进占满DbHome。FND-R2-5只覆盖"会去清理"，清理本身用的是忽略失败的deleteDirectory
 *（不看重试也不校验），文件被占用（Windows杀毒/备份软件锁定）时静默残留且无日志。
 * 修复：三处清理升级为deletedDirectoryAndCheck（重试+校验），失败记error不抛出
 * （不掩盖快照本身的成败）；每次snapshot开始前清扫DbHome下所有历史checkpoint_*
 * 残留（当前快照目录尚未创建，不会误删）——删除失败只是延迟到下一轮快照，不再累积。
 * 直接构造Rocks（不start server）执行真实snapshot流程：修复前预置的残留目录在快照
 * 后仍然存在，断言失败，有区分度；另验证清理失败不阻断快照（锁文件占用，Windows
 * 下确定性触发删除失败，Linux下删除直接成功同样通过）。
 */
@Fast
public class TestFnd716SnapshotResidualCleanup {
	private static final String raftName = "127.0.0.1:17700";
	private static final String dbHome = "TestFnd716SnapshotResidualCleanup.raft";

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17700" DbHome="TestFnd716SnapshotResidualCleanup.raft">
					<node Host="127.0.0.1" Port="17700"/>
					<node Host="127.0.0.1" Port="17701"/>
					<node Host="127.0.0.1" Port="17702"/>
				</raft>
				""");
	}

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() {
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	private static long countCheckpointDirs() {
		var files = new File(dbHome).listFiles();
		if (files == null)
			return 0;
		return Arrays.stream(files).filter(f -> f.isDirectory() && f.getName().startsWith("checkpoint_")).count();
	}

	private static void createResidual(String name) throws Exception {
		var dir = new File(dbHome, name);
		assertTrue(dir.mkdirs());
		Files.write(dir.toPath().resolve("000003.log"), new byte[]{1, 2, 3}); // 模拟RocksDB文件
	}

	// 快照开始前清扫历史残留；快照成功路径自身不再留下checkpoint目录。
	@Test
	public void testSnapshotSweepsResidualCheckpoints() throws Exception {
		createResidual("checkpoint_1111111111111");
		createResidual("checkpoint_2222222222222");

		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false)) {
			var tmpZip = Paths.get(dbHome, "snapshot.dat.tmp.zip");
			var result = rocks.snapshot(tmpZip.toString());
			assertTrue(result.success);

			assertEquals(0L, countCheckpointDirs(),
					"residual checkpoint_* dirs must be swept before generating a new snapshot, "
							+ "and the current checkpoint must be deleted after success");
			assertTrue(new File(rocks.getRaft().getLogSequence().getSnapshotFullName()).isFile(),
					"committed snapshot file must exist");
		}
	}

	// 残留删除失败（文件占用）不阻断快照本身：清理记error告警，快照继续成功；
	// 占用解除后的下一次快照把残留清掉——删除失败只延迟一轮，不累积。
	@Test
	public void testCleanupFailureDoesNotAbortSnapshot() throws Exception {
		createResidual("checkpoint_3333333333333");
		var locked = Paths.get(dbHome, "checkpoint_3333333333333", "locked.file").toFile();

		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false)) {
			// Windows：打开的文件句柄令File.delete()失败；Linux：删除直接成功，断言同样成立。
			try (var out = new FileOutputStream(locked)) {
				out.write(1);
				out.flush();
				var tmpZip = Paths.get(dbHome, "snapshot.dat.tmp.zip");
				var result = rocks.snapshot(tmpZip.toString()); // 不得因清理失败抛出
				assertTrue(result.success, "snapshot must survive residual cleanup failure");
			}

			// 句柄已关：下一次快照清扫掉残留。
			var tmpZip2 = Paths.get(dbHome, "snapshot.dat.tmp2.zip");
			assertTrue(rocks.snapshot(tmpZip2.toString()).success);
			assertEquals(0L, countCheckpointDirs(), "residual must be cleaned once no longer locked");
			assertTrue(new File(rocks.getRaft().getLogSequence().getSnapshotFullName()).isFile());
		}
	}
}
