package Dbh2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.SplitPut;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 分桶同步（SplitPut）的 apply 语义：
 * Dbh2.onCommitBatch 把分桶期间的 delete 编码为 Binary.Empty 的 put（墓碑协议，Dbh2.java），
 * applySplitPut 收到空 value 必须转成真正的 delete；
 * 否则已删除记录在新桶复活为空记录，get 返回"存在，值为空"。
 */
public class TestSplitPutTombstone {
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
			throws IOException, Exception {
		for (var dbh2 : nodes) {
			dbh2.close();
			// 目录最终由@TempDir清理；这里close后先删一次，给Windows下句柄延迟释放留出重试缓冲，
			// 降低JUnit收尾删除失败把测试搞红的概率。
			LogSequence.deleteDirectory(new File(dbh2.getRaft().getRaftConfig().getDbHome()));
		}
		agent.close();
		database.close();
	}

	// 发送SplitPut并等待应用完成。
	private static void splitPut(Dbh2Agent agent, boolean fromTransaction, Binary key, Binary value) {
		var r = new SplitPut();
		r.Argument.setFromTransaction(fromTransaction);
		r.Argument.getPuts().put(key, value);
		r.setTimeout(30_000);
		agent.getRaftAgent().sendForWait(r).await();
		Assertions.assertEquals(0, r.getResultCode());
	}

	private static Binary get(Dbh2Agent agent, Binary key) {
		var kv = agent.get("database", "table1", key);
		Assertions.assertTrue(kv.getKey());
		return kv.getValue() == null ? null : new Binary(kv.getValue().Bytes, kv.getValue().ReadIndex, kv.getValue().size());
	}

	@Test
	public void testTombstone(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var database = new RocksDatabase(tempDir.resolve("dbh2TestSplitTombstone").toString());
		var nodes = startBucket(database, """
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="">
					<node Host="127.0.0.1" Port="19100"/>
					<node Host="127.0.0.1" Port="19101"/>
					<node Host="127.0.0.1" Port="19102"/>
				</raft>
				""", tempDir);
		var agent = new Dbh2Agent("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="">
					<node Host="127.0.0.1" Port="19100"/>
					<node Host="127.0.0.1" Port="19101"/>
					<node Host="127.0.0.1" Port="19102"/>
				</raft>
				""");
		try {
			var meta = new BBucketMeta.Data();
			meta.setDatabaseName("database");
			meta.setTableName("table1");
			meta.setRaftConfig("");
			meta.setKeyFirst(Binary.Empty);
			meta.setKeyLast(Binary.Empty);
			agent.setBucketMeta(meta);

			var key1 = new Binary(new byte[]{1});
			var key2 = new Binary(new byte[]{2});
			var value = new Binary(new byte[]{9});
			var valueNew = new Binary(new byte[]{8});

			// 正常事务写入两条记录。
			{
				var batch = new BPrepareBatch.Data("", "database", "table1", null);
				batch.getBatch().getPuts().put(key1, value);
				batch.getBatch().getPuts().put(key2, value);
				batch.getBatch().setTid(1);
				agent.prepareBatch(batch).await();
				agent.commitBatch(batch.getBatch().getTid()).await();
			}
			Assertions.assertEquals(value, get(agent, key1));
			Assertions.assertEquals(value, get(agent, key2));

			// 分桶事务同步：delete 被编码为 Binary.Empty 的 put（墓碑）。
			splitPut(agent, true, key1, Binary.Empty);
			// 墓碑必须删除记录：读不到（bug时返回非null的空value记录）。
			Assertions.assertNull(get(agent, key1));

			// 分桶事务同步：非空 put 是 replace 语义。
			splitPut(agent, true, key2, valueNew);
			Assertions.assertEquals(valueNew, get(agent, key2));

			// 分桶数据复制：putIfAbsent，不能覆盖已有值。
			splitPut(agent, false, key2, value);
			Assertions.assertEquals(valueNew, get(agent, key2));

			// 墓碑删除后可以重新写入。
			splitPut(agent, true, key1, value);
			Assertions.assertEquals(value, get(agent, key1));
		} finally {
			stopBucket(nodes, agent, database);
		}
	}
}
