package Zeze.Raft;

import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestInternalRpcAfterShutdown {
	@Test
	void internalRpcHandlersReturnSafelyAfterShutdownWithoutLeaks(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		var conf = RaftConfig.loadFromString("""
				<raft Name="127.0.0.1:19810" DbHome="%s">
				  <node Host="127.0.0.1" Port="19810"/>
				  <node Host="127.0.0.1" Port="19811"/>
				  <node Host="127.0.0.1" Port="19812"/>
				</raft>
				""".formatted(home));
		var raft = new Raft(new StateMachine() {
			@Override
			public SnapshotResult snapshot(String path) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void loadSnapshot(String path) {
			}
		}, null, conf, new Config());

		var seq = raft.getLogSequence();
		seq.trySetTerm(1);
		raft.shutdown();

		// 1. 关停后投递同任期 InstallSnapshot(offset=0) 不得新建 .installing 文件或登记会话。
		var snap = new InstallSnapshot();
		snap.Argument.setTerm(1);
		snap.Argument.setLeaderId("127.0.0.1:19811");
		snap.Argument.setLastIncludedIndex(10);
		snap.Argument.setLastIncludedTerm(1);
		snap.Argument.setOffset(0);
		snap.Argument.setData(new Binary(new byte[]{1, 2, 3, 4}));
		snap.Argument.setDone(false);
		assertEquals(0L, raft.processInstallSnapshot(snap));
		assertFalse(raft.isReceivingSnapshot(), "关停后不得登记接收快照会话");
		try (var stream = Files.list(home)) {
			assertTrue(stream.noneMatch(p -> p.getFileName().toString().contains(".installing.")),
					"关停后不得创建 .installing 文件句柄");
		}

		// 2. 关停后投递 AppendEntries / RequestVote / PreVote 及应答回调均不得抛 NPE。
		var processAe = Raft.class.getDeclaredMethod("processAppendEntries", AppendEntries.class);
		processAe.setAccessible(true);
		var ae = new AppendEntries();
		ae.Argument.setTerm(2);
		ae.Argument.setLeaderId("127.0.0.1:19811");
		assertEquals(0L, processAe.invoke(raft, ae));

		var processVote = Raft.class.getDeclaredMethod("processRequestVote", RequestVote.class);
		processVote.setAccessible(true);
		var rv = new RequestVote();
		rv.Argument.setTerm(2);
		rv.Argument.setCandidateId("127.0.0.1:19811");
		assertEquals(0L, processVote.invoke(raft, rv));

		var processPreVote = Raft.class.getDeclaredMethod("processPreVote", PreVote.class);
		processPreVote.setAccessible(true);
		var pv = new PreVote();
		pv.Argument.setTerm(2);
		pv.Argument.setCandidateId("127.0.0.1:19811");
		assertEquals(0L, processPreVote.invoke(raft, pv));

		var peer = new Server.ConnectorEx("127.0.0.1", 19811);
		var processVoteRes = Raft.class.getDeclaredMethod("processRequestVoteResult", RequestVote.class, Connector.class);
		processVoteRes.setAccessible(true);
		var rvRes = new RequestVote();
		rvRes.Argument.setTerm(1);
		rvRes.Result.setTerm(2);
		assertEquals(0L, processVoteRes.invoke(raft, rvRes, peer));

		var processPreVoteRes = Raft.class.getDeclaredMethod("processPreVoteResult", PreVote.class, Connector.class);
		processPreVoteRes.setAccessible(true);
		var pvRes = new PreVote();
		pvRes.Argument.setTerm(2);
		pvRes.Result.setTerm(2);
		assertEquals(0L, processPreVoteRes.invoke(raft, pvRes, peer));

		var processAeRes = LogSequence.class.getDeclaredMethod("processAppendEntriesResult",
				Server.ConnectorEx.class, Protocol.class);
		processAeRes.setAccessible(true);
		var aeRes = new AppendEntries();
		aeRes.Argument.setTerm(1);
		aeRes.Result.setTerm(2);
		assertEquals(0L, processAeRes.invoke(seq, peer, aeRes));

		var snapState = new InstallSnapshotState();
		var processSnapRes = InstallSnapshotState.class.getDeclaredMethod("processResult",
				LogSequence.class, Server.ConnectorEx.class, Protocol.class);
		processSnapRes.setAccessible(true);
		var snapRes = new InstallSnapshot();
		snapRes.Argument.setTerm(1);
		snapRes.Result.setTerm(2);
		assertEquals(0L, processSnapRes.invoke(snapState, seq, peer, snapRes));
	}
}
