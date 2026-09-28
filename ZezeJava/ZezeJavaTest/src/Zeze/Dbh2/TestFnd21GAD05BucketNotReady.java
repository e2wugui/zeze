package Zeze.Dbh2;

import java.nio.file.Path;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.IModule;
import Zeze.Dbh2.AbstractDbh2;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Net.Binary;
import Zeze.Raft.RaftConfig;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND21 GA-D01 A5回归：meta-less桶的访问返回专用错误码eBucketNotReady（INV4）。
 * 现状：新raft在SetBucketMeta（桶协议第一条）之前bucketMeta==null，Get/Walk/WalkKey/
 * PrepareBatch经inBucket解引用null以框架层NPE面目出现（孤儿收养路径真实触达——如孤儿
 * raft被误连上的诊断访问）。
 * 修复=四入口显式判定返回additive错误码eBucketNotReady（Dbh2模块值4），不以NPE面目
 * 出现、调用方可辨识可重试；SetBucketMeta/SplitPut不拦（初始化入口与目标桶数据灌入通道）。
 * 红因：bug时服务端NPE（框架层异常码/无响应），修复后是确定的模块错误码。
 * 形态：进程内3节点meta-less桶（Fnd19GABucketSupport拓扑，manager=null——本用例不触发
 * 分桶恢复路径）。端口19200-19202段为本用例预留。
 */
@Fast
public class TestFnd21GAD05BucketNotReady {

	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	private static final String RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19200"/>
				<node Host="127.0.0.1" Port="19201"/>
				<node Host="127.0.0.1" Port="19202"/>
			</raft>
			""";

	// eBucketNotReady=Dbh2模块additive错误码值4（直接以IModule.errorCode(11026,4)构造——
	// 不引用新常量，旧基线可编译可运行，红因=真实行为差异：bug时服务端NPE-derived错误码≠本码）。
	private static long expectedNotReady() {
		return IModule.errorCode(AbstractDbh2.ModuleId, 4);
	}

	@Test
	public void testMetaLessAccessReturnsNotReady(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var logDb = new RocksDatabase(tempDir.resolve("gad05-log").toString());
		var nodes = Fnd19GABucketSupport.startBucket(logDb, RAFT, tempDir, taskOneByOne);
		var agent = new Dbh2Agent(RAFT);
		try {
			Fnd19GABucketSupport.waitLeader(nodes);
			// meta-less：bucketMeta==null（未SetBucketMeta）。

			// Get：Dbh2Agent.get对非eBucketMismatch错误码抛RuntimeException（code入消息）。
			var getEx = Assertions.assertThrows(RuntimeException.class,
					() -> agent.get("database", "table1", new Binary(new byte[]{2})),
					"meta-less Get不得以NPE/无响应面目出现");
			Assertions.assertTrue(getEx.getMessage().contains(String.valueOf(expectedNotReady())),
					"meta-less Get必须返回eBucketNotReady（got: " + getEx.getMessage() + "）");

			// Walk/WalkKey：错误在外面处理（返回rpc本体，断言结果码）。
			Assertions.assertEquals(expectedNotReady(),
					agent.walk(Binary.Empty, 10, false, null).getResultCode(),
					"meta-less Walk必须返回eBucketNotReady");
			Assertions.assertEquals(expectedNotReady(),
					agent.walkKey(Binary.Empty, 10, false, null).getResultCode(),
					"meta-less WalkKey必须返回eBucketNotReady");

			// PrepareBatch：future结果码。
			var batch = new BPrepareBatch.Data("", "database", "table1", null);
			batch.getBatch().getPuts().put(new Binary(new byte[]{2}), new Binary(new byte[]{9}));
			batch.getBatch().setTid(1L);
			var f = agent.prepareBatch(batch);
			f.await();
			Assertions.assertEquals(expectedNotReady(), f.get().getResultCode(),
					"meta-less PrepareBatch必须返回eBucketNotReady（不建事务不留tid）");

			// 初始化后恢复可用：SetBucketMeta即转正（守卫不得拦初始化入口）。
			var meta = new Zeze.Builtin.Dbh2.BBucketMeta.Data();
			meta.setDatabaseName("database");
			meta.setTableName("table1");
			meta.setRaftConfig("");
			meta.setKeyFirst(new Binary(new byte[]{1}));
			meta.setKeyLast(Binary.Empty);
			agent.setBucketMeta(meta);
			var f2 = agent.prepareBatch(batch);
			f2.await();
			Assertions.assertEquals(0, f2.get().getResultCode(), "SetBucketMeta后必须恢复正常服务");
			Assertions.assertEquals(0, agent.commitBatch(1L).await().get().getResultCode(), "提交必须成功");
		} finally {
			agent.close();
			for (var n : nodes)
				n.close();
			logDb.close();
		}
	}
}
