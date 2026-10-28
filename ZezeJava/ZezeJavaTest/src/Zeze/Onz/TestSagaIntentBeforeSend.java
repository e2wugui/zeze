package Zeze.Onz;

import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.RocksDatabase;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.WriteOptions;
import sun.misc.Unsafe;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestSagaIntentBeforeSend {
	@TempDir
	Path directory;

	private static final class TestTransaction extends OnzTransaction<EmptyBean.Data, EmptyBean.Data> {
		@Override
		protected long perform() {
			return 0;
		}
	}

	private static final class RecordingSocket extends AsyncSocket {
		RocksDatabase.Table commitPoint;
		int sends;
		boolean intentAtSend;

		RecordingSocket(RocksDatabase.Table commitPoint) {
			super(new Service("SagaIntentBeforeSend"));
			this.commitPoint = commitPoint;
		}

		@Override
		public boolean Send(byte[] bytes, int offset, int length) {
			sends++;
			try {
				var saved = commitPoint.get(new byte[8]);
				if (saved != null) {
					var state = new BSavedCommits.Data();
					state.decode(ByteBuffer.Wrap(saved));
					intentAtSend = state.getOnzs().contains(OnzTransaction.encodeSagaParticipant("target"));
				}
			} catch (Exception exception) {
				throw new AssertionError(exception);
			}
			return false; // Exercise normal send-failure completion without a network/timeout leak.
		}

		@Override public Type getType() { return Type.eClient; }
		@Override protected void doClose(@Nullable Throwable cause, boolean gracefully) { }
		@Override public @Nullable TimeThrottle getTimeThrottle() { return null; }
		@Override public @Nullable SocketAddress getRemoteAddress() { return null; }
	}

	private static final class RecordingServer extends OnzServer {
		RecordingSocket socket;

		@SuppressWarnings("DataFlowIssue") // Allocated without construction; only persistence and getZezeInstance are used.
		RecordingServer() throws Exception { super("unused=unused", null); }

		@Override
		public AsyncSocket getZezeInstance(String name) { return socket; }

		@Override
		public long nextOnzTid() { return 0; }
	}

	private static void set(Object target, String name, Object value) throws Exception {
		var field = OnzServer.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static RecordingServer server(RocksDatabase database) throws Exception {
		Zeze.Util.Task.tryInitThreadPool();
		var field = Unsafe.class.getDeclaredField("theUnsafe");
		field.setAccessible(true);
		var server = (RecordingServer)((Unsafe)field.get(null)).allocateInstance(RecordingServer.class);
		set(server, "database", database);
		set(server, "commitPoint", database.getOrAddTable("CommitPoint"));
		set(server, "commitIndex", database.getOrAddTable("CommitIndex"));
		set(server, "dbLock", new ReentrantLock());
		set(server, "writeOptions", RocksDatabase.getDefaultWriteOptions());
		server.socket = new RecordingSocket(database.getOrAddTable("CommitPoint"));
		return server;
	}

	private static TestTransaction transaction(RecordingServer server) {
		var transaction = new TestTransaction();
		transaction.setOnzServer(server);
		return transaction;
	}

	@Test
	void persistentIntentAlreadyContainsTargetWhenSendBegins() throws Exception {
		try (var database = new RocksDatabase(directory.resolve("intent").toString())) {
			var server = server(database);
			transaction(server).callSagaAsync("target", "step", EmptyBean.Data.instance, EmptyBean.Data.instance);
			assertEquals(1, server.socket.sends);
			assertTrue(server.socket.intentAtSend);
		}
	}

	@Test
	void persistenceFailurePreventsSendAndAllowsRegistrationRetry() throws Exception {
		try (var database = new RocksDatabase(directory.resolve("failed").toString());
			 var invalid = new WriteOptions().setSync(true).setDisableWAL(true)) {
			var server = server(database);
			var transaction = transaction(server);
			set(server, "writeOptions", invalid); // RocksDB rejects sync writes without WAL.
			assertThrows(Exception.class, () -> transaction.callSagaAsync("target", "step",
					EmptyBean.Data.instance, EmptyBean.Data.instance));
			assertEquals(0, server.socket.sends);
			assertTrue(transaction.buildSavedCommits().getOnzs().isEmpty());
			set(server, "writeOptions", RocksDatabase.getDefaultWriteOptions());
			transaction.callSagaAsync("target", "step", EmptyBean.Data.instance, EmptyBean.Data.instance);
			assertTrue(server.socket.intentAtSend);
		}
	}

	@Test
	void stoppedCoordinatorCannotSendUnrecordedStep() throws Exception {
		try (var database = new RocksDatabase(directory.resolve("stopped").toString())) {
			var server = server(database);
			set(server, "stopped", true);
			assertThrows(IllegalStateException.class, () -> transaction(server).callSagaAsync("target", "step",
					EmptyBean.Data.instance, EmptyBean.Data.instance));
			assertEquals(0, server.socket.sends);
		}
	}

	@Test
	void existingCommitDecisionCannotAdmitAnotherStep() throws Exception {
		try (var database = new RocksDatabase(directory.resolve("committed").toString())) {
			var server = server(database);
			server.saveCommitPoint(new byte[8], new BSavedCommits.Data(), AbstractOnz.eCommitting);
			assertThrows(IllegalStateException.class, () -> transaction(server).callSagaAsync("target", "step",
					EmptyBean.Data.instance, EmptyBean.Data.instance));
			assertEquals(0, server.socket.sends);
			var index = database.getOrAddTable("CommitIndex").get(new byte[8]);
			assertEquals(AbstractOnz.eCommitting, ByteBuffer.Wrap(index).ReadUInt());
		}
	}
}
