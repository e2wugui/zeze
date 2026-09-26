package Dbh2;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Dbh2.AbstractDbh2;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Transaction.Procedure;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND19 GA-C01回归：PrepareBatch重复投递撞eDuplicateTid时，finally的undo不能误删
 * map中先到的存活事务（两参remove仅删本次txn）。bug链：误删后CommitBatch在桶侧
 * transactions.remove(tid)得到null被静默跳过——已决定提交的数据不落盘而回包仍是成功。
 * 测试用不相交的key构造重复tid（serialize默认开启，重叠key会被构造器锁冲突先行拦截，
 * 到不了putIfAbsent分支），钉住两个契约：重复拒绝后存活事务仍在map；其commit数据可读。
 */
public class TestFnd19GA01DuplicateTidKeepLiveTransaction {
	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	// 进程内启动一个3节点raft桶（镜像Dbh2Test.Bucket，端口错开不与其冲突）。
	// 每个节点必须独立loadFromString一份RaftConfig（Raft构造会改写配置对象，共享会导致节点身份错乱）；
	// 显式设置DbHome后Raft不再按节点名在cwd下建目录，所有节点目录落在tempDir下，由@TempDir统一清理。
	private static ArrayList<Zeze.Dbh2.Dbh2> startBucket(RocksDatabase database, String raftConfigString, Path tempDir) {
		var nodes = new ArrayList<Zeze.Dbh2.Dbh2>();
		for (var config : RaftConfig.loadFromString(raftConfigString).getNodes().values()) {
			var nodeConfig = raftConfigString.replaceFirst("<raft ",
					"<raft DbHome=\"" + tempDir.resolve(config.getName().replace(':', '_')) + "\" ");
			nodes.add(new Zeze.Dbh2.Dbh2(null, config.getName(), database,
					RaftConfig.loadFromString(nodeConfig), null, false, taskOneByOne));
		}
		return nodes;
	}

	private static void stopBucket(ArrayList<Zeze.Dbh2.Dbh2> nodes, Dbh2Agent agent, RocksDatabase database)
			throws Exception {
		for (var dbh2 : nodes) {
			dbh2.close();
			LogSequence.deleteDirectory(new File(dbh2.getRaft().getRaftConfig().getDbHome()));
		}
		agent.close();
		database.close();
	}

	private static Binary get(Dbh2Agent agent, Binary key) {
		var kv = agent.get("database", "table1", key);
		Assertions.assertTrue(kv.getKey());
		return kv.getValue() == null ? null : new Binary(kv.getValue().Bytes, kv.getValue().ReadIndex, kv.getValue().size());
	}

	// PrepareBatch是leader-only的RaftRpc：取leader节点用于检查其内存事务表。
	private static Zeze.Dbh2.Dbh2 waitLeader(ArrayList<Zeze.Dbh2.Dbh2> nodes) throws InterruptedException {
		for (int i = 0; i < 300; ++i) { // 选举最多等15s
			for (var node : nodes)
				if (node.getRaft().isLeader())
					return node;
			//noinspection BusyWait
			Thread.sleep(50);
		}
		throw new IllegalStateException("no leader elected");
	}

	@Test
	public void testDuplicateTidKeepsLiveTransaction(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var database = new RocksDatabase(tempDir.resolve("fnd19ga01").toString());
		var raftConfig = """
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="">
					<node Host="127.0.0.1" Port="19130"/>
					<node Host="127.0.0.1" Port="19131"/>
					<node Host="127.0.0.1" Port="19132"/>
				</raft>
				""";
		var nodes = startBucket(database, raftConfig, tempDir);
		var agent = new Dbh2Agent(raftConfig);
		try {
			var meta = new BBucketMeta.Data();
			meta.setDatabaseName("database");
			meta.setTableName("table1");
			meta.setRaftConfig("");
			meta.setKeyFirst(Binary.Empty);
			meta.setKeyLast(Binary.Empty);
			agent.setBucketMeta(meta);

			var key1 = new Binary(new byte[]{1});
			var key9 = new Binary(new byte[]{9});
			var value = new Binary(new byte[]{7});

			// 事务T1进入2PC进行中（prepare后不commit），存活于桶侧事务表。
			{
				var batch = new BPrepareBatch.Data("", "database", "table1", null);
				batch.getBatch().getPuts().put(key1, value);
				batch.getBatch().setTid(1);
				var f = agent.prepareBatch(batch);
				f.await();
				Assertions.assertEquals(0, f.get().getResultCode());
			}

			var leader = waitLeader(nodes);
			Assertions.assertTrue(leader.getStateMachine().getTransactions().containsKey(1L),
					"prepare applied must leave live transaction in leader map");

			// 同tid重复投递（key与T1不相交，serialize锁不拦截，直达putIfAbsent分支）：
			// 必须被eDuplicateTid拒绝。
			{
				var dup = new BPrepareBatch.Data("", "database", "table1", null);
				dup.getBatch().getPuts().put(key9, value);
				dup.getBatch().setTid(1);
				var f = agent.prepareBatch(dup);
				f.await();
				var rc = f.get().getResultCode();
				Assertions.assertTrue(rc != 0 && rc != Procedure.RaftApplied, "duplicate tid must be rejected");
				Assertions.assertEquals(AbstractDbh2.eDuplicateTid, IModule.getErrorCode(rc));
			}

			// bug时：finally的单参remove把T1误删出leader map。
			Assertions.assertTrue(leader.getStateMachine().getTransactions().containsKey(1L),
					"duplicate prepare must NOT remove the live transaction");

			// T1最终提交：数据必须落盘可读（bug时leader侧remove(tid)得null，commit被静默跳过）。
			agent.commitBatch(1).await();
			Assertions.assertEquals(value, get(agent, key1));
		} finally {
			stopBucket(nodes, agent, database);
		}
	}
}
