package Zeze.Raft;

import java.io.File;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LogSequence.endReceiveInstallSnapshot 锁外等待后必须复查 term（FND-R1-4）。
 * processInstallSnapshot 在 Raft 锁内校验 term 后即释放锁（Raft.java:449-451），
 * endReceiveInstallSnapshot 随后 await removeLogBeforeFuture / raft.lock() 的等待
 * 窗口内 term/leader/state 可任意变化（新 leader 当选、本节点发起选举）。旧实现
 * 等待返回后不复查即执行破坏性重置：commitIndex 已推进时误触发 fatalKill 杀死
 * 健康节点；否则丢弃新 leader 已复制的日志回退重放；窗口内本节点已自投票时
 * setVoteFor(当前leaderId="") 抹掉自投票，破坏"每 term 至多一票"。
 * 修复：raft.lock() 拿到锁后（term 写点全在 Raft 锁内，此后到重置完成 term 不会再变）
 * 复查 r.Argument.getTerm() 与当前 term 一致才执行重置；不一致放弃并应答新 term。
 * 这里用 Rocks 直接构造 LogSequence（不 start server），用未完成的
 * removeLogBeforeFuture 把 endReceiveInstallSnapshot 挡在锁外等待点，期间由另一
 * 线程持 Raft 锁推进 term 并模拟自投票（等价于"新 leader 的 AppendEntries 已被
 * 处理 + 本节点已发起选举"的最小合成状态），随后放行：确定性触发竞态窗口。
 * 旧代码会继续执行重置（lastIndex 被改写为 LastIncludedLog.index、voteFor 被抹为
 * leaderId("")），断言失败，有区分度。
 */
@Fast
public class TestEndReceiveInstallSnapshotStaleTerm {
	private static final String raftName = "127.0.0.1:17640";
	private static final String dbHome = "TestEndReceiveInstallSnapshotStaleTerm.raft";

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17640" DbHome="TestEndReceiveInstallSnapshotStaleTerm.raft">
					<node Host="127.0.0.1" Port="17640"/>
					<node Host="127.0.0.1" Port="17641"/>
					<node Host="127.0.0.1" Port="17642"/>
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
	public void testStaleTermAbortsDestructiveReset() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false)) {
			var raft = rocks.getRaft();
			var logSequence = raft.getLogSequence();

			// 模拟 processInstallSnapshot 锁内校验通过后的进入状态：
			// Argument.term 与当前 term 一致，done=true 的最后一个 trunk。
			var r = new InstallSnapshot();
			r.Argument.setTerm(logSequence.getTerm()); // 初始 0
			r.Argument.setLeaderId("127.0.0.1:17641");
			r.Argument.setLastIncludedIndex(5);
			r.Argument.setLastIncludedTerm(0);
			r.Argument.setDone(true);
			r.Argument.setLastIncludedLog(new Binary(new RaftLog(0, 5, new HeartbeatLog()).encode()));

			// 卡住锁外等待点：endReceiveInstallSnapshot 将 await 这个未完成的 future。
			var gate = new TaskCompletionSource<Boolean>();
			logSequence.removeLogBeforeFuture = gate;

			var racer = new Thread(() -> {
				raft.lock(); // term 的全部写点都在 Raft 锁内，持锁修改即等价于真实消息处理路径
				try {
					logSequence.trySetTerm(logSequence.getTerm() + 1); // 新 leader 当选，term 推进（leaderId 被重置为 ""）
					logSequence.setVoteFor("self"); // 本节点随即发起选举投自己（PreVote=false 路径）
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					raft.unlock();
				}
				gate.setResult(true); // 放行 endReceiveInstallSnapshot
			});
			racer.start();

			// path 不需要真实存在：修复后的放弃路径不触碰快照文件。
			logSequence.endReceiveInstallSnapshot("nonexistent.raft" + File.separator + "snapshot.dat", r);
			racer.join(10_000);

			// 等待窗口内 term 已推进：为旧 term 准备的破坏性重置必须被放弃。
			assertEquals(1L, logSequence.getTerm());
			assertEquals(0L, logSequence.getLastIndex(), "logs must NOT be dropped/rebuilt at LastIncludedIndex");
			assertEquals(0L, logSequence.getCommitIndex());
			assertEquals("self", logSequence.getVoteFor(), "voteFor(self) must NOT be overwritten by stale reset");
			assertTrue(logSequence.logsAvailable, "logsAvailable must be restored in finally");
			assertEquals(1L, r.Result.getTerm(), "reply must carry the current (newer) term back to the stale leader");
		}
	}
}
