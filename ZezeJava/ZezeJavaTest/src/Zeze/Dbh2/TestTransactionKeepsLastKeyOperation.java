package Zeze.Dbh2;

import org.jspecify.annotations.Nullable;

import harness.Extra;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Database operations must retain their order before the bucket applies a write batch. */
@Fast
@Extra
public class TestTransactionKeepsLastKeyOperation {

	private static final class LocalApplyManager extends Dbh2AgentManager {
		private final Bucket bucket;

		LocalApplyManager(Path tempDir, Bucket bucket) throws Exception {
			super(new Dbh2AgentStubSupport.NullServiceAgent(),
					Config.load(Dbh2AgentStubSupport.writeRemoteCommitConfig(tempDir).toString()));
			this.bucket = bucket;
		}

		@Override
		@SuppressWarnings("DataFlowIssue") // 本地 fake 绕过网络，下游 locateBucket 覆写不使用 MasterAgent。
		public MasterAgent openDatabase(String masterName, String databaseName) {
			return null; // Stable routing and the real bucket store replace the network boundary.
		}

		@Override
		public String locateBucket(@Nullable MasterAgent masterAgent, String masterName, String databaseName,
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

	@ParameterizedTest
	@ValueSource(strings = {"replace-key", "replace-value", "remove-key"})
	void stagingAliasesPassedBuffers(String operation, @TempDir Path tempDir) throws Exception {
		// 约定（对齐 IApplyTable，见 Database.Dbh2Transaction.replace 注释）：key/value 传入即放弃所有权，
		// 暂存与缓冲区别名。本测试钉别名语义：暂存后修改缓冲，提交内容随之变化。
		Task.tryInitThreadPool();
		var raftConfig = RaftConfig.loadFromString("<raft Name=\"\"><node Host=\"127.0.0.1\" Port=\"1\"/></raft>");
		raftConfig.setDbHome(tempDir.resolve("bucket").toString());
		var bucket = new Bucket(raftConfig);
		var manager = new LocalApplyManager(tempDir, bucket);
		try {
			var conf = new Config.DatabaseConf();
			conf.setDatabaseUrl("dbh2://127.0.0.1:1/database");
			var database = new Database(null, manager, conf);
			var key = ByteBuffer.Wrap(new byte[]{9, 1, 9}, 1, 1);
			var value = ByteBuffer.Wrap(new byte[]{9, 2, 9}, 1, 1);
			var originalKey = key.Copy();
			var originalValue = value.Copy();
			bucket.getData().put(originalKey, new byte[]{3});
			boolean remove = operation.equals("remove-key");
			try (var transaction = (Database.Dbh2Transaction)database.beginTransaction()) {
				if (remove)
					transaction.remove("table", key);
				else
					transaction.replace("table", key, value);
				if (operation.equals("replace-value"))
					value.Bytes[value.ReadIndex] = 8;
				else
					key.Bytes[key.ReadIndex] = 8;
				transaction.commit();
			}
			if (operation.equals("replace-key")) {
				// 键别名：replace 落到修改后的键上，原键保留桶里的旧值。
				assertArrayEquals(new byte[]{3}, bucket.getData().get(originalKey));
				assertArrayEquals(originalValue, bucket.getData().get(new byte[]{8}));
			} else if (operation.equals("replace-value")) {
				// 值别名：提交的是修改后的值。
				assertArrayEquals(new byte[]{8}, bucket.getData().get(originalKey));
			} else {
				// 删除键别名：删除落到修改后的键上，原键幸存。
				assertArrayEquals(new byte[]{3}, bucket.getData().get(originalKey));
			}
		} finally {
			manager.stop();
			bucket.close();
		}
	}
}
