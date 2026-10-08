package Zeze.Dbh2;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 迁移后的死桶对陈旧路由PrepareBatch必须refused重定向自愈，不得返回终局
 * eBucketNotFound——尤其当桶的分裂历史条目keyFirst为单字节[0x01]时：
 * 死桶meta哨兵{1},{1}（DeadBucketMetaBound）与真实单字节keyFirst[0x01]
 * 字节内容相等，自指守卫若拿"当前meta的keyFirst"判等，会把"locate到迁移
 * 目标"误判成"又找到了自己"，返回终局错误码。客户端（CommitRocks.
 * processPrepareFutures）只对refused应答触发startRefreshMasterTable刷新
 * 路由，对非零码直接抛异常undo——纯写负载（无读路径的eBucketMismatch/
 * bucketRefuse自愈）下该表写入持续失败直至进程重启。
 * 用例：
 * ①死桶重定向（红）：源桶keyFirst=[0x01]迁移后置死，陈旧路由写[0x03]——
 * 必须rc=0且refused携带迁移目标raftConfig与被拒批（bug：rc=eBucketNotFound
 * 且重复写入恒失败）；按refused重定向到目标桶后写入-提交-可读（自愈闭环）。
 * ②活桶自指仍终局（不过度收紧的回归锚）：活桶历史被裁剪只剩首键条目（指向
 * 自身）时，越上界键locate落回自身条目，eBucketNotFound语义保持——活桶
 * 首键条目恒指向本桶（endSplit的from原地刷新），重定向会自旋。
 * 端口19280-19285段为本用例族预留。
 */
@Fast
public class TestDeadBucketPrepareRedirectsSingleByteKeyFirst {

	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	// 端口段：源桶=19280-82，目标桶=19283-85。
	private static final String SOURCE_RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19280"/>
				<node Host="127.0.0.1" Port="19281"/>
				<node Host="127.0.0.1" Port="19282"/>
			</raft>
			""";

	private static final String TARGET_RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19283"/>
				<node Host="127.0.0.1" Port="19284"/>
				<node Host="127.0.0.1" Port="19285"/>
			</raft>
			""";

	private static final AtomicInteger tid = new AtomicInteger();

	private static Binary key(int i) {
		return new Binary(new byte[]{(byte)i});
	}

	private static Binary value(int i) {
		return new Binary(new byte[]{(byte)(0x10 + i)});
	}

	private static BBucketMeta.Data metaOf(Binary keyFirst, Binary keyLast, String raftConfig) {
		var meta = new BBucketMeta.Data();
		meta.setDatabaseName("database");
		meta.setTableName("table1");
		meta.setRaftConfig(raftConfig);
		meta.setKeyFirst(keyFirst);
		meta.setKeyLast(keyLast);
		return meta;
	}

	private static BPrepareBatch.Data prepareOf(Binary k, Binary v) {
		var batch = new BPrepareBatch.Data("", "database", "table1", null);
		batch.getBatch().getPuts().put(k, v);
		batch.getBatch().setTid(tid.incrementAndGet());
		return batch;
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

	/**
	 * ①死桶重定向（红）：单字节keyFirst[0x01]的桶迁移（move）完结后，源桶置死
	 * （哨兵meta{1},{1}），分裂历史首键条目=迁移目标（keyFirst[0x01]）。持有陈旧
	 * 路由的客户端继续向死桶写[0x03]：必须refused重定向（客户端据此刷新路由并把
	 * 被拒批转发迁移目标），不得终局eBucketNotFound把纯写负载钉死在失败循环里。
	 */
	@Test
	public void testMovedDeadBucketRedirectsStaleSingleByteKeyPrepare(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var sourceLogDb = new RocksDatabase(tempDir.resolve("src-log").toString());
		var targetLogDb = new RocksDatabase(tempDir.resolve("dst-log").toString());
		var source = RaftBucketTopologySupport.startBucket(sourceLogDb, SOURCE_RAFT, tempDir.resolve("src"), taskOneByOne);
		var target = RaftBucketTopologySupport.startBucket(targetLogDb, TARGET_RAFT, tempDir.resolve("dst"), taskOneByOne);
		var sourceAgent = new Dbh2Agent(SOURCE_RAFT);
		var targetAgent = new Dbh2Agent(TARGET_RAFT);
		try {
			var leader = RaftBucketTopologySupport.waitLeader(source);
			RaftBucketTopologySupport.waitLeader(target);

			// 源桶真实键域[[0x01],∞)（单字节keyFirst），目标桶meta就位（迁移收尾的
			// setBucketMetaAsync等价形态），随后源桶迁移完结置死（endMove等价形态：
			// meta={1},{1}哨兵+历史首键条目被迁移目标同键覆写）。
			sourceAgent.setBucketMeta(metaOf(key(1), Binary.Empty, ""));
			targetAgent.setBucketMeta(metaOf(key(1), Binary.Empty, TARGET_RAFT));
			leader.getStateMachine().endMove(metaOf(key(1), Binary.Empty, TARGET_RAFT));

			// 陈旧路由的纯写负载：向死桶发PrepareBatch（客户端路由缓存未刷新的形态）。
			var batch = prepareOf(key(3), value(3));
			var f = sourceAgent.prepareBatch(batch);
			f.await();
			Assertions.assertEquals(0, f.get().getResultCode(),
					"死桶必须以rc=0+refused重定向应答（bug：单字节keyFirst撞哨兵被判自指，"
							+ "终局eBucketNotFound让陈旧路由纯写负载持续失败）");
			var refused = f.get().Result.getRefused();
			Assertions.assertEquals(1, refused.size(), "被拒批必须归入唯一的重定向目标");
			var redirect = refused.get(TARGET_RAFT);
			Assertions.assertNotNull(redirect, "重定向目标必须是分裂历史命中的迁移目标桶");
			Assertions.assertEquals(value(3), redirect.getPuts().get(key(3)), "被拒的put必须随重定向携带");

			// 自愈闭环：客户端对refused刷新路由并把被拒批转发目标桶（processPrepareFutures同款）。
			var forward = new BPrepareBatch.Data("", "database", "table1", null);
			forward.getBatch().setTid(tid.incrementAndGet());
			forward.getBatch().getPuts().putAll(redirect.getPuts());
			forward.getBatch().getDeletes().addAll(redirect.getDeletes());
			var f2 = targetAgent.prepareBatch(forward);
			f2.await();
			Assertions.assertEquals(0, f2.get().getResultCode(), "重定向目标必须接受被拒批");
			Assertions.assertTrue(f2.get().Result.getRefused().isEmpty(), "目标桶不得再拒绝（键域归属正确）");
			Assertions.assertEquals(0, targetAgent.commitBatch(forward.getBatch().getTid())
					.await().get().getResultCode(), "重定向后提交必须成功");
			var kv = targetAgent.get("database", "table1", key(3));
			Assertions.assertTrue(kv.getKey(), "迁移目标必须可读回重定向写入的数据");
			Assertions.assertNotNull(kv.getValue(), "重定向写入的数据必须在迁移目标可读（写入自愈成立）");
		} finally {
			sourceAgent.close();
			targetAgent.close();
			for (var n : source)
				n.close();
			for (var n : target)
				n.close();
			sourceLogDb.close();
			targetLogDb.close();
		}
	}

	/**
	 * ②活桶自指仍终局（回归锚）：活桶[F,L)=[2,8)的历史被裁剪只剩首键条目（指向
	 * 本桶，endSplit的from原地刷新形态），越上界键[9]的locate落回自身条目——
	 * 此时"又找到了自己"成立，重定向会自旋，必须保持终局eBucketNotFound，
	 * 证明死桶短路没有过度放宽守卫。
	 */
	@Test
	public void testLiveBucketSelfHistoryStillTerminal(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var logDb = new RocksDatabase(tempDir.resolve("live-log").toString());
		var nodes = RaftBucketTopologySupport.startBucket(logDb, SOURCE_RAFT, tempDir.resolve("live"), taskOneByOne);
		var agent = new Dbh2Agent(SOURCE_RAFT);
		try {
			var leader = RaftBucketTopologySupport.waitLeader(nodes);
			agent.setBucketMeta(metaOf(key(2), key(8), SOURCE_RAFT));
			// 历史仅剩首键条目且指向本桶（超裁剪代的终局形态）。
			leader.getStateMachine().getBucket().addMoveMetaHistory(metaOf(key(2), key(8), SOURCE_RAFT));

			var f = agent.prepareBatch(prepareOf(key(9), value(9)));
			f.await();
			Assertions.assertEquals(Zeze.IModule.errorCode(AbstractDbh2.ModuleId, AbstractDbh2.eBucketNotFound),
					f.get().getResultCode(), "活桶locate落回自身条目必须保持终局eBucketNotFound（重定向会自旋）");
		} finally {
			stopBucket(nodes, agent, logDb);
		}
	}
}
