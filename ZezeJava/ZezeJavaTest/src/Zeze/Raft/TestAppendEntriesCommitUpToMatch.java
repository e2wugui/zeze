package Zeze.Raft;

import java.io.File;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Net.Binary;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * follower 的 commitIndex 推进必须受本次 AppendEntries 证明匹配的尾索引约束，
 * 不能取本地 lastIndex：本地 lastIndex 可能仍带着未被本批覆盖的旧分叉后缀。
 * 触发场景：follower 持有公共前缀 1 与旧 leader 未提交的 term2 分叉 2..3；
 * term3 leader 已在多数派提交自己的 2..3，分批追赶时第一批只携带索引 1。
 * 旧实现 min(leaderCommit, lastIndex) 会把未证明的旧后缀一并提交应用，
 * 后续合法的冲突覆盖还会命中"truncate committed entries" fatalKill。
 */
@Fast
public class TestAppendEntriesCommitUpToMatch {
	private static final String raftName = "127.0.0.1:17685";
	private static final String dbHome = "TestAppendEntriesCommitUpToMatch.raft";

	// 3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17685" DbHome="TestAppendEntriesCommitUpToMatch.raft">
					<node Host="127.0.0.1" Port="17685"/>
					<node Host="127.0.0.1" Port="17686"/>
					<node Host="127.0.0.1" Port="17687"/>
				</raft>
				""");
	}

	private static final class MinimalStateMachine extends StateMachine {
		@Override
		public SnapshotResult snapshot(String path) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void loadSnapshot(String path) {
		}
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

	private static AppendEntries appendEntries(int term, String leaderId, long prevLogIndex, int prevLogTerm,
			long leaderCommit, RaftLog... entries) {
		var r = new AppendEntries();
		r.Argument.setTerm(term);
		r.Argument.setLeaderId(leaderId);
		r.Argument.setPrevLogIndex(prevLogIndex);
		r.Argument.setPrevLogTerm(prevLogTerm);
		r.Argument.setLeaderCommit(leaderCommit);
		for (var log : entries)
			r.Argument.getEntries().add(new Binary(log.encode()));
		r.Argument.setLastEntryIndex(entries.length == 0 ? prevLogIndex : entries[entries.length - 1].getIndex());
		return r;
	}

	@Test
	public void batchedAppendCommitsOnlyMatchedPrefix() throws Exception {
		var raft = new Raft(new MinimalStateMachine(), raftName, newRaftConfig());
		try {
			var logSequence = raft.getLogSequence();
			logSequence.setWriteOptions(RocksDatabase.getDefaultWriteOptions());
			raft.lock();
			try {
				// 旧leader(term2)留下的本地日志：公共前缀 1(t1)，未提交分叉后缀 2..3(t2)。
				logSequence.followerOnAppendEntries(appendEntries(1, "127.0.0.1:17685",
						0, 0, 0, new RaftLog(1, 1, new HeartbeatLog())));
				logSequence.followerOnAppendEntries(appendEntries(2, "127.0.0.1:17685",
						1, 1, 0, new RaftLog(2, 2, new HeartbeatLog()), new RaftLog(2, 3, new HeartbeatLog())));
				assertEquals(3L, logSequence.getLastIndex());

				// term3 leader 已在多数派提交 2..3，分批追赶本节点，本批仅携带索引 1。
				// 本地 2..3(t2) 是旧分叉，未经本批证明匹配，不得计入提交上限。
				logSequence.followerOnAppendEntries(appendEntries(3, "127.0.0.1:17686",
						0, 0, 3, new RaftLog(1, 1, new HeartbeatLog())));

				assertEquals(1L, logSequence.getCommitIndex(),
						"commit must be bounded by this batch's matched tail, not local lastIndex");
				assertEquals(1L, logSequence.getLastApplied(),
						"old fork suffix must not be applied");
				assertEquals(2, logSequence.readLog(3).getTerm(),
						"old fork suffix must survive until a later batch overwrites it");

				// 后续批次覆盖分叉后缀并完成追赶：合法覆盖不得被"已提交"挡住。
				logSequence.followerOnAppendEntries(appendEntries(3, "127.0.0.1:17686",
						1, 1, 3, new RaftLog(3, 2, new HeartbeatLog()), new RaftLog(3, 3, new HeartbeatLog())));

				assertEquals(3L, logSequence.getCommitIndex());
				assertEquals(3L, logSequence.getLastApplied());
				assertEquals(3, logSequence.readLog(2).getTerm(), "fork suffix must be overwritten by the leader's entries");
			} finally {
				raft.unlock();
			}
		} finally {
			raft.shutdown();
		}
	}
}
