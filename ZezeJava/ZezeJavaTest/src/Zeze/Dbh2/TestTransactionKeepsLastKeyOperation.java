package Zeze.Dbh2;

import java.nio.file.Path;
import Zeze.Builtin.Dbh2.Commit.BPrepareBatches;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Net.Binary;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Database operations must retain their order before the bucket applies a write batch. */
@Fast
public class TestTransactionKeepsLastKeyOperation {

	private static final class LocalApplyManager extends Dbh2AgentManager {
		private final Bucket bucket;

		LocalApplyManager(Path tempDir, Bucket bucket) throws Exception {
			super(new Dbh2AgentStubSupport.NullServiceAgent(),
					Config.load(Dbh2AgentStubSupport.writeRemoteCommitConfig(tempDir).toString()));
			this.bucket = bucket;
		}

		@Override
		public MasterAgent openDatabase(String masterName, String databaseName) {
			return null; // Stable routing and the real bucket store replace the network boundary.
		}

		@Override
		public String locateBucket(MasterAgent masterAgent, String masterName, String databaseName,
				String tableName, Binary key) {
			return "local-bucket";
		}

		@Override
		public void commit(BPrepareBatches.Data batches) {
			try {
				for (var batch : batches.getDatas().values()) {
					try (var transaction = new Dbh2Transaction(batch.getBatch())) {
						transaction.commitBatch(bucket); // Production apply code, with a real RocksDB store.
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static void checkOperations(Path tempDir, int operation) throws Exception {
		Task.tryInitThreadPool();
		var raftConfig = RaftConfig.loadFromString("<raft Name=\"\"><node Host=\"127.0.0.1\" Port=\"1\"/></raft>");
		raftConfig.setDbHome(tempDir.resolve("bucket").toString());
		var bucket = new Bucket(raftConfig);
		var manager = new LocalApplyManager(tempDir, bucket);
		try {
			var conf = new Config.DatabaseConf();
			conf.setDatabaseUrl("dbh2://127.0.0.1:1/database");
			var database = new Database(null, manager, conf);
			var key = ByteBuffer.Wrap(new byte[]{1});
			var firstValue = ByteBuffer.Wrap(new byte[]{2});
			var lastValue = ByteBuffer.Wrap(new byte[]{3});
			bucket.getData().put(key.Copy(), firstValue.Copy());
			try (var transaction = (Database.Dbh2Transaction)database.beginTransaction()) {
				if (operation != 0)
					transaction.replace("table", key, firstValue);
				if (operation != 3)
					transaction.remove("table", key);
				if (operation != 1)
					transaction.replace("table", key, lastValue);
				transaction.commit();
			}
			var actual = bucket.getData().get(key.Copy());
			if (operation == 1)
				assertNull(actual, "The final remove must delete the record");
			else
				assertArrayEquals(lastValue.Copy(), actual, "The final replace must persist its value");
		} finally {
			manager.stop();
			bucket.close();
		}
	}

	@Test
	void removeThenReplaceWritesReplacement(@TempDir Path tempDir) throws Exception {
		checkOperations(tempDir, 0);
	}

	@Test
	void replaceThenRemoveDeletesRecord(@TempDir Path tempDir) throws Exception {
		checkOperations(tempDir, 1);
	}

	@Test
	void replaceRemoveReplaceKeepsLastValue(@TempDir Path tempDir) throws Exception {
		checkOperations(tempDir, 2);
	}

	@Test
	void repeatedReplacementWritesLastValue(@TempDir Path tempDir) throws Exception {
		checkOperations(tempDir, 3);
	}
}
