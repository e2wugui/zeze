package Zeze.Raft;

import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestSnapshotBoundaryReplicationTrigger {
	private static final class StubAsyncSocket extends AsyncSocket {
		StubAsyncSocket(Service service) {
			super(service);
			setHandshakeDone(true);
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		protected void doClose(@Nullable Throwable ex, boolean gracefully) {
		}

		@Override
		public boolean Send(byte[] bytes, int offset, int length) {
			return true;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}
	}

	private static final class ReadyConnectorEx extends Server.ConnectorEx {
		@SuppressWarnings("unchecked")
		ReadyConnectorEx(String host, int port, AsyncSocket readySocket) throws Exception {
			super(host, port);
			var futureField = Zeze.Net.Connector.class.getDeclaredField("futureSocket");
			futureField.setAccessible(true);
			var future = (Zeze.Util.TaskCompletionSource<AsyncSocket>)futureField.get(this);
			future.setResult(readySocket);
		}
	}

	private static Raft newLeaderWithSnapshot(int portBase, Path home) throws Exception {
		var name = "127.0.0.1:" + portBase;
		var conf = RaftConfig.loadFromString("""
				<raft Name="%s" DbHome="%s">
				  <node Host="127.0.0.1" Port="%d"/>
				  <node Host="127.0.0.1" Port="%d"/>
				  <node Host="127.0.0.1" Port="%d"/>
				</raft>
				""".formatted(name, home, portBase, portBase + 1, portBase + 2));
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
		var stateField = Raft.class.getDeclaredField("state");
		stateField.setAccessible(true);
		stateField.set(raft, Raft.RaftState.Leader);

		seq.saveLog(new RaftLog(1, 100, new HeartbeatLog()));
		seq.saveLog(new RaftLog(1, 120, new HeartbeatLog()));
		Files.write(seq.genSnapshotPath(100), new byte[]{1, 2, 3, 4});
		var firstIndexField = LogSequence.class.getDeclaredField("firstIndex");
		firstIndexField.setAccessible(true);
		firstIndexField.setLong(seq, 100);
		var lastIndexField = LogSequence.class.getDeclaredField("lastIndex");
		lastIndexField.setAccessible(true);
		lastIndexField.setLong(seq, 120);
		return raft;
	}

	@Test
	void trySendAppendEntriesStartsSnapshotWhenNextIndexBelowFirstIndex(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		var raft = newLeaderWithSnapshot(19780, home);
		try {
			var seq = raft.getLogSequence();
			var connector = new ReadyConnectorEx("127.0.0.1", 19781, new StubAsyncSocket(raft.getServer()));
			connector.setNextIndex(50);

			raft.lock();
			try {
				seq.trySendAppendEntries(connector, null);
				assertEquals(100, connector.getNextIndex(), "nextIndex < firstIndex 必须钳位到 firstIndex");
				assertTrue(seq.getSendSnapshotting().contains(connector.getName()),
						"nextIndex < firstIndex 时必须立即启动 InstallSnapshot");
			} finally {
				raft.unlock();
			}
		} finally {
			raft.shutdown();
		}
	}

	@Test
	void processAppendEntriesResultClampsNextIndexToFirstIndexAndStartsSnapshot(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		var raft = newLeaderWithSnapshot(19790, home);
		try {
			var seq = raft.getLogSequence();
			var connector = new ReadyConnectorEx("127.0.0.1", 19791, new StubAsyncSocket(raft.getServer()));
			connector.setNextIndex(50);

			var ae = new AppendEntries();
			ae.Argument.setTerm(1);
			ae.Result.setTerm(1);
			ae.Result.setSuccess(false);
			ae.Result.setNextIndex(0);
			connector.setPending(ae);

			var method = LogSequence.class.getDeclaredMethod("processAppendEntriesResult",
					Server.ConnectorEx.class, Protocol.class);
			method.setAccessible(true);
			method.invoke(seq, connector, ae);

			assertEquals(100, connector.getNextIndex(), "失败回退不得越过 firstIndex 向后回退到 49");
			assertTrue(seq.getSendSnapshotting().contains(connector.getName()),
					"钳位到 firstIndex 后必须立即触发 InstallSnapshot");
		} finally {
			raft.shutdown();
		}
	}
}
