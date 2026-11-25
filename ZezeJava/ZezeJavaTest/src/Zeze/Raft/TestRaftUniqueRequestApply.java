package Zeze.Raft;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Util.Task;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 非 Rocks 状态机的唯一请求在应用后也必须能够回放结果。不开 server，无网络。 */
@Fast
public class TestRaftUniqueRequestApply {
	private static final int SERVER_ID = FastServerIds.TEST_RAFT_UNIQUE_REQUEST_APPLY;

	@TempDir
	Path dbHome;

	private static final class CountingStateMachine extends StateMachine {
		int applied;

		CountingStateMachine() {
			addFactory(CountingLog.TypeId_, CountingLog::new);
		}

		@Override
		public SnapshotResult snapshot(String path) {
			return new SnapshotResult();
		}

		@Override
		public void loadSnapshot(String path) {
		}
	}

	private static final class CountingLog extends Log {
		static final int TypeId_ = Zeze.Transaction.Bean.hash32(CountingLog.class.getName());

		CountingLog() {
			super(null);
		}

		@Override
		public long typeId() {
			return TypeId_;
		}

		@Override
		public void apply(RaftLog holder, StateMachine stateMachine) {
			((CountingStateMachine)stateMachine).applied++;
		}
	}

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
	}

	private void applyAndCheckResult(boolean leader) throws Exception {
		var sm = new CountingStateMachine();
		var raft = new Raft(sm, RaftHeadlessSupport.raftName(SERVER_ID),
				RaftHeadlessSupport.newRaftConfig(SERVER_ID, dbHome.toString()), new Config());
		try {
			var seq = raft.getLogSequence();
			var log = new CountingLog();
			log.getUnique().setClientId("test.nonRocks.apply");
			log.getUnique().setRequestId(1);
			log.setCreateTime(System.currentTimeMillis());
			log.setRpcResult(new Binary(new byte[]{1, 2, 3}));
			var callbacks = new AtomicInteger();
			if (leader) {
				var state = Raft.class.getDeclaredField("state");
				state.setAccessible(true);
				state.set(raft, Raft.RaftState.Leader);
				seq.appendLog(log, (holder, success) -> callbacks.incrementAndGet());
			} else
				seq.saveLog(new RaftLog(0, 1, log));
			seq.tryApply(seq.readLog(1), 1);

			var retry = new Zeze.Builtin.ServiceManagerWithRaft.Login();
			retry.getUnique().setClientId(log.getUnique().getClientId());
			retry.getUnique().setRequestId(log.getUnique().getRequestId());
			retry.setCreateTime(log.getCreateTime());
			var applied = seq.tryGetRequestState(retry);
			assertEquals(1, sm.applied);
			assertEquals(1, seq.getLastApplied());
			assertNotSame(UniqueRequestState.NOT_FOUND, applied);
			assertTrue(requireNonNull(applied).isApplied());
			assertEquals(log.getRpcResult(), applied.getRpcResult());
			assertEquals(leader ? 1 : 0, callbacks.get());
		} finally {
			raft.shutdown();
		}
	}

	@Test
	public void testLeaderApplyCompletesPreApplyStub() throws Exception {
		applyAndCheckResult(true);
	}

	@Test
	public void testFollowerApplyPersistsResultForFailover() throws Exception {
		applyAndCheckResult(false);
	}

	@Test
	public void testInstallSnapshotCleansUncommittedPreApplyStubsAndKeepsAppliedStubs() throws Exception {
		var sm = new CountingStateMachine();
		var raft = new Raft(sm, RaftHeadlessSupport.raftName(SERVER_ID),
				RaftHeadlessSupport.newRaftConfig(SERVER_ID, dbHome.toString()), new Config());
		try {
			var seq = raft.getLogSequence();
			var state = Raft.class.getDeclaredField("state");
			state.setAccessible(true);
			state.set(raft, Raft.RaftState.Leader);

			long now = System.currentTimeMillis();
			var appliedLog = new CountingLog();
			appliedLog.getUnique().setClientId("test.installSnapshot.cleanup");
			appliedLog.getUnique().setRequestId(1);
			appliedLog.setCreateTime(now);
			appliedLog.setRpcResult(new Binary(new byte[]{1, 2, 3}));
			seq.appendLog(appliedLog, null);
			seq.tryApply(seq.readLog(1), 1);

			// 第二条仅 appendLog 未 apply，留下 !isApplied 存根；随后被 InstallSnapshot 整表丢弃日志覆盖。
			var uncommittedLog = new CountingLog();
			uncommittedLog.getUnique().setClientId("test.installSnapshot.cleanup");
			uncommittedLog.getUnique().setRequestId(2);
			uncommittedLog.setCreateTime(now);
			seq.appendLog(uncommittedLog, null);

			var snap = new InstallSnapshot();
			snap.Argument.setTerm(2);
			snap.Argument.setLeaderId(RaftHeadlessSupport.raftName(SERVER_ID + 1));
			snap.Argument.setLastIncludedIndex(10);
			snap.Argument.setLastIncludedTerm(2);
			snap.Argument.setOffset(0);
			snap.Argument.setData(new Binary(new byte[]{9, 8, 7}));
			snap.Argument.setDone(true);
			snap.Argument.setLastIncludedLog(new Binary(new RaftLog(2, 10, new HeartbeatLog()).encode()));

			raft.processInstallSnapshot(snap);
			assertEquals(10, seq.getLastApplied());

			var retryUncommitted = new Zeze.Builtin.ServiceManagerWithRaft.Login();
			retryUncommitted.getUnique().setClientId("test.installSnapshot.cleanup");
			retryUncommitted.getUnique().setRequestId(2);
			retryUncommitted.setCreateTime(now);
			assertSame(UniqueRequestState.NOT_FOUND, seq.tryGetRequestState(retryUncommitted),
					"被 InstallSnapshot 丢弃的未提交日志不得残留 !isApplied 存根阻断客户端重试");

			var retryApplied = new Zeze.Builtin.ServiceManagerWithRaft.Login();
			retryApplied.getUnique().setClientId("test.installSnapshot.cleanup");
			retryApplied.getUnique().setRequestId(1);
			retryApplied.setCreateTime(now);
			var appliedState = seq.tryGetRequestState(retryApplied);
			assertNotSame(UniqueRequestState.NOT_FOUND, appliedState);
			assertTrue(requireNonNull(appliedState).isApplied(), "已应用的终态存根不得被误删");
			assertEquals(appliedLog.getRpcResult(), appliedState.getRpcResult());
		} finally {
			raft.shutdown();
		}
	}
}
