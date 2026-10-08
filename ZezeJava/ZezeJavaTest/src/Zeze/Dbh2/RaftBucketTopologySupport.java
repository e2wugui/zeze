package Zeze.Dbh2;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Util.RocksDatabase;
import Zeze.Util.TaskOneByOneByKey;
import org.junit.jupiter.api.Assertions;

/**
 * FND19 GA系测试共享的进程内3节点raft桶拓扑（TestFnd19GA01/GA02/GAD02，形态镜像
 * Dbh2Test.Bucket；三测试端口段错开：GA01=19130-32、GA02=19140-42、GAD02=19150-52，
 * 互不冲突也不与Dbh2Test.Bucket冲突）。
 * 每个节点必须独立loadFromString一份RaftConfig（Raft构造会改写配置对象，共享会导致
 * 节点身份错乱）；显式设置DbHome后Raft不再按节点名在cwd下建目录，所有节点目录落在
 * tempDir下，由@TempDir统一清理。
 * taskOneByOne由各测试自备实例传入：Raft的userTaskOneByOneKey按raft配置名聚合，共享
 * 静态实例会让并行执行的测试桶在同一个键上互相串行。
 */
final class RaftBucketTopologySupport {
	private RaftBucketTopologySupport() {
	}

	static ArrayList<Zeze.Dbh2.Dbh2> startBucket(RocksDatabase database, String raftConfigString, Path tempDir,
												TaskOneByOneByKey taskOneByOne) {
		var nodes = new ArrayList<Zeze.Dbh2.Dbh2>();
		for (var config : RaftConfig.loadFromString(raftConfigString).getNodes().values()) {
			var nodeConfig = raftConfigString.replaceFirst("<raft ",
					"<raft DbHome=\"" + java.util.regex.Matcher.quoteReplacement(String.valueOf(tempDir.resolve(config.getName().replace(':', '_')))) + "\" ");
			nodes.add(new Zeze.Dbh2.Dbh2(null, config.getName(), database,
					RaftConfig.loadFromString(nodeConfig), null, false, taskOneByOne));
		}
		return nodes;
	}

	static void stopBucket(ArrayList<Zeze.Dbh2.Dbh2> nodes, Dbh2Agent agent, RocksDatabase database)
			throws Exception {
		for (var dbh2 : nodes) {
			dbh2.close();
			LogSequence.deleteDirectory(new File(dbh2.getRaft().getRaftConfig().getDbHome()));
		}
		agent.close();
		database.close();
	}

	// PrepareBatch是leader-only的RaftRpc：取leader节点用于检查其内存事务表。
	static Zeze.Dbh2.Dbh2 waitLeader(ArrayList<Zeze.Dbh2.Dbh2> nodes) throws InterruptedException {
		for (int i = 0; i < 300; ++i) { // 选举最多等15s
			for (var node : nodes)
				if (node.getRaft().isLeader())
					return node;
			//noinspection BusyWait
			Thread.sleep(50);
		}
		throw new IllegalStateException("no leader elected");
	}

	static Binary get(Dbh2Agent agent, Binary key) {
		var kv = agent.get("database", "table1", key);
		Assertions.assertTrue(kv.getKey());
		return kv.getValue() == null ? null : new Binary(kv.getValue().Bytes, kv.getValue().ReadIndex, kv.getValue().size());
	}
}
