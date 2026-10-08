package Zeze.Dbh2;

import java.nio.file.Path;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * serialize模式（默认）记录锁的失败者语义：prepare冲突失败的构造方回滚close()只释放
 * 自己成功获取的键锁，不得触碰共享canonical Lockey上持有者的信号量。
 * Lockey是Locks注册表按值去重的共享实例：失败键在构造中途已入map，而其locked=true由
 * 持有者置位——失败者若按locked标志release即误放持有者的锁，互斥即刻破坏（第三个事务
 * 与在途持有者并发持键，读-改-写丢失更新），且unlock不复位locked使permit计数只增不减。
 * 钉三个契约：T1{k1,k2}在途时T2{j1,k2}冲突失败后，(1){j1}仍可立即prepare（失败者自己
 * 获取的键必须回滚释放）；(2){k2}的prepare必须仍失败（持有者互斥保持）；(3)T1提交后
 * {k2}恰好可再持有一个事务（permit不膨胀，第二个并发prepare必须失败）。
 * 形态镜像TestGA02PrepareConflictRollbackLocks（进程内3节点桶，端口段19160-62错开）。
 */
public class TestPrepareConflictLoserKeepsHolderLock {
	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	private static long prepare(Dbh2Agent agent, long tid, Binary... puts) throws Exception {
		var batch = new BPrepareBatch.Data("", "database", "table1", null);
		for (var key : puts)
			batch.getBatch().getPuts().put(key, new Binary(new byte[]{7}));
		batch.getBatch().setTid(tid);
		var f = agent.prepareBatch(batch);
		f.await();
		return f.get().getResultCode();
	}

	@Test
	public void testConflictLoserReleasesOnlyOwnLocks(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var database = new RocksDatabase(tempDir.resolve("prepareConflictLoserKeepsHolder").toString());
		var raftConfig = """
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="">
					<node Host="127.0.0.1" Port="19160"/>
					<node Host="127.0.0.1" Port="19161"/>
					<node Host="127.0.0.1" Port="19162"/>
				</raft>
				""";
		var nodes = RaftBucketTopologySupport.startBucket(database, raftConfig, tempDir, taskOneByOne);
		var agent = new Dbh2Agent(raftConfig);
		try {
			var meta = new BBucketMeta.Data();
			meta.setDatabaseName("database");
			meta.setTableName("table1");
			meta.setRaftConfig("");
			meta.setKeyFirst(Binary.Empty);
			meta.setKeyLast(Binary.Empty);
			agent.setBucketMeta(meta);

			var k1 = new Binary(new byte[]{1});
			var k2 = new Binary(new byte[]{2});
			var j1 = new Binary(new byte[]{0x10});

			// T1: {k1,k2} prepare成功，锁持有到commit apply为止（2PC在途，不收尾）。
			Assertions.assertEquals(0, prepare(agent, 1, k1, k2));

			// T2: {j1,k2}：j1获取成功后在k2上冲突抛出，构造器回滚close()后异常返回。
			var rc2 = prepare(agent, 2, j1, k2);
			Assertions.assertTrue(rc2 != 0 && rc2 != Procedure.RaftApplied,
					"conflict prepare must fail (lock timeout)");

			// 契约1：失败者自己获取的键（j1）必须已回滚释放，立即可用。
			Assertions.assertEquals(0, prepare(agent, 3, j1), "loser must release its own acquired keys");
			agent.commitBatch(3).await();

			// 契约2（红核）：持有者T1仍在途，k2的锁必须仍互斥——失败者close()误放持有者
			// 信号量时这里prepare成功，T4可与T1并发持k2提交，读-改-写丢失更新。
			var rc4 = prepare(agent, 4, k2);
			Assertions.assertTrue(rc4 != 0 && rc4 != Procedure.RaftApplied,
					"mutex must hold: loser's rollback must not release holder's lock");

			// T1收尾，k2的permit应恰好回到1。
			agent.commitBatch(1).await();

			// 契约3（红核）：permit不膨胀——k2上恰好一个并发持有者，第二个必须失败。
			Assertions.assertEquals(0, prepare(agent, 5, k2));
			var rc6 = prepare(agent, 6, k2);
			Assertions.assertTrue(rc6 != 0 && rc6 != Procedure.RaftApplied,
					"permit count must not inflate: only one concurrent holder per key");
			agent.commitBatch(5).await();

			// 多轮冲突循环后锁语义仍完好：k2可再次正常获取。
			Assertions.assertEquals(0, prepare(agent, 7, k2));
			agent.commitBatch(7).await();
		} finally {
			RaftBucketTopologySupport.stopBucket(nodes, agent, database);
		}
	}
}
