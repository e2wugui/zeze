package Zeze.Raft;

import java.io.File;
import java.util.Arrays;

import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND12 raft-01回归（D1修复·通路一）：follower 观察到自己 commitIndex 推进时置 NodeReady。
 * 缺陷：nodeReady 唯一推进条件是观察到 leaderCommit 严格增长（哨兵基线只记不置），
 * 空闲期加入/追赶完成的节点收到恒定 leaderCommit 永不就绪；ready 与 never-ready 候选人
 * 按真值表双向拒投（RequestVote 与 PreVote 同门），leader 死亡后存活多数派选举永久互拒。
 * 修复：commitIndex 推进即已持有多数派提交的数据（leaderCommit 只携带多数派已提交边界，
 * 追赶完成的节点 commitIndex 必然推进），followerOnAppendEntries 据此 trySetNodeReady。
 * <p>
 * 复现：headless 构造 Raft（不起 server，无网络），直接驱动 followerOnAppendEntries：
 * 空entries外的一条心跳日志 + leaderCommit=1 使 commitIndex 0→1 推进（模拟空闲集群
 * 追赶完成后的首次携带边界的消息）。修复前 NodeReady 恒 false（基线只记不增长），断言失败。
 */
@Fast
public class TestNodeReadyOnCommitAdvance {
	private static final String raftName = "127.0.0.1:17770";
	private static final String dbHome = "TestNodeReadyOnCommitAdvance.raft";

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17770" DbHome="TestNodeReadyOnCommitAdvance.raft">
					<node Host="127.0.0.1" Port="17770"/>
					<node Host="127.0.0.1" Port="17771"/>
					<node Host="127.0.0.1" Port="17772"/>
				</raft>
				""");
	}

	// 最小状态机：本测试只走日志复制/提交推进，不到达 snapshot/loadSnapshot。
	static final class NullStateMachine extends StateMachine {
		@Override
		public SnapshotResult snapshot(String path) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void loadSnapshot(String path) {
			throw new UnsupportedOperationException();
		}
	}

	private Raft raft;

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() throws Exception {
		raft.getLogSequence().close();
		raft.shutdown();
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	@Test
	public void testCommitAdvanceSetsNodeReady() throws Exception {
		raft = new Raft(new NullStateMachine(), raftName, newRaftConfig());
		var logSequence = raft.getLogSequence();
		logSequence.setWriteOptions(RocksDatabase.getDefaultWriteOptions());

		// 新盘节点：nodeReady=false；日志仅有构造期哨兵（term=0, index=0）。
		assertTrue(!logSequence.getNodeReady(), "正控：全新节点初始为 never-ready");

		// 模拟追赶完成后的携带边界消息：term=1 的合法 Leader 追加一条日志并告知 leaderCommit=1。
		// commitIndex = min(1, lastIndex=1) = 1，从 0 严格推进。
		var r = new AppendEntries();
		r.Argument.setTerm(1);
		r.Argument.setLeaderId("127.0.0.1:17771");
		r.Argument.setPrevLogIndex(0);
		r.Argument.setPrevLogTerm(0);
		r.Argument.setLeaderCommit(1);
		r.Argument.getEntries().add(new Binary(new RaftLog(1, 1, new HeartbeatLog()).encode()));
		assertEquals(Procedure.Success, logSequence.followerOnAppendEntries(r));
		assertTrue(r.Result.getSuccess(), "正控：复制成功（prevLog 匹配、日志落盘）");

		// 修复前：leaderCommit 恒定（无后续增长可观察），NodeReady 永远 false。
		assertTrue(logSequence.getNodeReady(),
				"commitIndex 推进即持有已提交数据，追赶完成的节点必须就绪（FND12 raft-01）");
	}
}
