package Zeze.Dbh2;

import java.nio.file.Path;
import Zeze.Builtin.Dbh2.BBatchTid;
import Zeze.Builtin.Dbh2.Commit.BTransactionState;
import Zeze.Builtin.Dbh2.CommitBatch;
import Zeze.Raft.RaftRpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.RocksDatabase;
import Zeze.Util.TaskCompletionSource;
import harness.Extra;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Extra
public class TestCommitReplayUsesPersistedDecision {
	@Test
	public void replayCommitsWhenTheIndexViewStillShowsPreparing(@TempDir Path home) throws Exception {
		var manager = mock(Dbh2AgentManager.class);
		var bucket = mock(Dbh2Agent.class);
		when(manager.openBucket("bucket")).thenReturn(bucket);
		when(manager.getDbh2Config()).thenReturn(new Dbh2Config());
		var response = new TaskCompletionSource<RaftRpc<BBatchTid.Data, EmptyBean.Data>>();
		response.setResult(new CommitBatch());
		when(bucket.commitBatch(1)).thenReturn(response);

		var property = "Dbh2CommitRocksHome";
		var previous = System.setProperty(property, home.resolve("commit").toString());
		CommitRocks rocks;
		try {
			rocks = new CommitRocks(manager, 1);
		} finally {
			if (previous == null)
				System.clearProperty(property);
			else
				System.setProperty(property, previous);
		}
		try {
			var key = new byte[8];
			ByteBuffer.longBeHandler.set(key, 0, 1L);
			var state = new BTransactionState.Data();
			state.setState(Commit.eCommitting);
			state.getBuckets().add("bucket");
			rocks.getCommitPoint().put(key, ByteBuffer.encode(state).Copy());

			var indexField = CommitRocks.class.getDeclaredField("commitIndex");
			indexField.setAccessible(true);
			var index = (RocksDatabase.Table)indexField.get(rocks);
			var oldIndex = ByteBuffer.Allocate();
			oldIndex.WriteUInt(Commit.ePreparing);
			index.put(key, oldIndex.Copy());

			var redo = CommitRocks.class.getDeclaredMethod("redoTimer");
			redo.setAccessible(true);
			redo.invoke(rocks);
			verify(bucket).commitBatch(1);
			verify(bucket, never()).undoBatch(1);
			assertNull(rocks.query(1));
		} finally {
			rocks.close();
		}
	}
}
