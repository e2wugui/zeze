package Zeze.Raft;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LogSequence.endReceiveInstallSnapshot 与本地 snapshot() 的互斥（FND3-21 竞态半）。
 * 本地快照的重阶段（RocksDatabase.backup 写 backupDir、zip）在 raft 锁外；旧实现
 * endReceiveInstallSnapshot 拿锁后不检查 snapshotting 即执行破坏性重置，loadSnapshot
 * 会与进行中的快照并发删除/重建/恢复同一 backupDir（Windows 上删除失败抛
 * IllegalStateException；Linux 上解压与备份写竞争），且失败后节点留在"日志已重置、
 * 状态机未恢复"的不可自愈状态。
 * 修复：snapshotting 进行中时放弃本次接收并应答 ResultCodeSnapshottingConflict
 * （leader 中断安装、下个心跳自动重试）；snapshotting 的检查/设置与重置全程在同一把
 * raft 锁内串行，无"检查后翻转"缝隙。
 * 本测试直接置 snapshotting=true（等价于本地快照正处于锁外重阶段），确定性驱动
 * done=true 的 endReceiveInstallSnapshot：旧代码会继续执行重置（lastIndex 被改写为
 * LastIncludedLog.index、孤儿 .installing 文件被 rename 成 snapshot.dat），断言失败，
 * 有区分度。
 */
@Fast
public class TestEndReceiveInstallSnapshotSnapshottingConflict {
	private static final String raftName = "127.0.0.1:17650";
	private static final String dbHome = "TestEndReceiveInstallSnapshotSnapshottingConflict.raft";

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17650" DbHome="TestEndReceiveInstallSnapshotSnapshottingConflict.raft">
					<node Host="127.0.0.1" Port="17650"/>
					<node Host="127.0.0.1" Port="17651"/>
					<node Host="127.0.0.1" Port="17652"/>
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

	@Test
	public void testSnapshottingConflictAbortsReset() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false)) {
			var raft = rocks.getRaft();
			var logSequence = raft.getLogSequence();

			// 模拟 processInstallSnapshot done 分支之后的进入状态：
			// Argument.term 与当前 term 一致，done=true 的最后一个 trunk。
			var r = new InstallSnapshot();
			r.Argument.setTerm(logSequence.getTerm()); // 初始 0
			r.Argument.setLeaderId("127.0.0.1:17651");
			r.Argument.setLastIncludedIndex(5);
			r.Argument.setLastIncludedTerm(0);
			r.Argument.setDone(true);
			r.Argument.setLastIncludedLog(new Binary(new RaftLog(0, 5, new HeartbeatLog()).encode()));

			// done 分支已移除 receiveSnapshotting 条目并关闭文件之后留下的孤儿 .installing 文件。
			var installingPath = Paths.get(dbHome, LogSequence.snapshotFileName + ".installing.5");
			Files.write(installingPath, new byte[]{1, 2, 3});

			// 等价于"本地快照正处于锁外重阶段"：snapshotting 的检查/设置都在 raft 锁内，
			// 与 endReceiveInstallSnapshot 的重置全程串行。
			raft.lock();
			try {
				logSequence.setSnapshotting(true);
			} finally {
				raft.unlock();
			}

			var resultCode = logSequence.endReceiveInstallSnapshot(installingPath, r);

			assertEquals(InstallSnapshot.ResultCodeSnapshottingConflict, resultCode,
					"must reply SnapshottingConflict so the leader breaks install and retries later");
			assertEquals(0L, logSequence.getLastIndex(), "logs must NOT be dropped/rebuilt at LastIncludedIndex");
			assertEquals(0L, logSequence.getCommitIndex());
			assertTrue(logSequence.logsAvailable, "logsAvailable must be restored in finally");
			assertFalse(Files.exists(installingPath), "orphan .installing file must be cleaned best-effort");
		}
	}
}
