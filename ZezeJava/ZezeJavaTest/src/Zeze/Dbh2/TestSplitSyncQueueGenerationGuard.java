package Zeze.Dbh2;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Supplier;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
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
 * FND24 dbh2-01（caa82be7f 分桶事务同步持久队列）+ 审视波世代戳加固的直驱守卫：
 * 原bug=分桶期间急切同步终局失败后以重拷贝兜底，已提交的写与删被静默丢失/旧值复活；
 * 修复=commitBatch apply 在 splittingMeta!=null（日志序边界）时全量入队（delete→墓碑）、
 * driveSplitSync 单飞FIFO投递、ACK推进水位、endSplit0 全送达门槛。
 * 钉住：
 *  - 边界：LogSetSplittingMeta apply 之前的提交不入队；
 *  - 入队：边界后目标键域 puts 原值入队、delete 编码 Binary.Empty 墓碑、非目标键域排除；
 *  - 副本：follower 同样入队（队列随raft apply重放，换主后新leader天然持有）；
 *  - 合并：同批多事务同key按提交序合并（后写覆盖，墓碑胜出）；
 *  - 水位：poll不推进、advance才推进；EndSplit apply 清空并换代；
 *  - 世代戳：换代后陈旧ACK按世代拒绝——seq从1重新分配，旧世代迟到ACK推进会跳过
 *    新世代未投递的同号记录（endSplit0门槛假通）。加固前该推进不被拦截。
 *  - 投递链：driveSplitSync 围栏内不投递、围栏后送达且replace生效、ACK推进水位。
 * 端口段与既有Dbh2测试错开：源桶=19230-32、目标桶=19240-42（AET=1000：围栏=
 * AgentTimeout+2000=5000ms，使围栏断言可在确定的时间窗内观察）。
 */
@Fast
public class TestSplitSyncQueueGenerationGuard {

	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	private static final String SOURCE_RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19230"/>
				<node Host="127.0.0.1" Port="19231"/>
				<node Host="127.0.0.1" Port="19232"/>
			</raft>
			""";

	// 目标桶AppendEntriesTimeout=1000（合法下限）：AgentTimeout=1000+2000=3000，
	// 围栏=AgentTimeout+2000=5000ms——投递链测试的时间轴由此确定。
	private static final String TARGET_RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="" AppendEntriesTimeout="1000">
				<node Host="127.0.0.1" Port="19240"/>
				<node Host="127.0.0.1" Port="19241"/>
				<node Host="127.0.0.1" Port="19242"/>
			</raft>
			""";

	private static final java.util.concurrent.atomic.AtomicInteger tid =
			new java.util.concurrent.atomic.AtomicInteger();

	private static Binary key(int i) {
		return new Binary(new byte[]{(byte)i});
	}

	private static Binary value(int i) {
		return new Binary(new byte[]{(byte)(0x10 + i)});
	}

	private static ArrayList<Zeze.Dbh2.Dbh2> startBucket(RocksDatabase database, String raftConfigString,
														Path tempDir, String homePrefix) {
		var nodes = new ArrayList<Zeze.Dbh2.Dbh2>();
		for (var config : RaftConfig.loadFromString(raftConfigString).getNodes().values()) {
			// 每节点独立loadFromString（Raft构造改写配置对象，共享致节点身份错乱）。
			var nodeConfig = raftConfigString.replaceFirst("<raft ",
					"<raft DbHome=\"" + tempDir.resolve(homePrefix + config.getName().replace(':', '_')) + "\" ");
			nodes.add(new Zeze.Dbh2.Dbh2(null, config.getName(), database,
					RaftConfig.loadFromString(nodeConfig), null, false, taskOneByOne));
		}
		return nodes;
	}

	private static void stopBucket(ArrayList<Zeze.Dbh2.Dbh2> nodes) throws Exception {
		for (var dbh2 : nodes) {
			dbh2.close();
			Zeze.Raft.LogSequence.deleteDirectory(new java.io.File(dbh2.getRaft().getRaftConfig().getDbHome()));
		}
	}

	private static Zeze.Dbh2.Dbh2 waitLeader(ArrayList<Zeze.Dbh2.Dbh2> nodes) throws InterruptedException {
		return Fnd19GABucketSupport.waitLeader(nodes);
	}

	private static void setBucketMeta(Dbh2Agent agent, Binary keyFirst, Binary keyLast) {
		var meta = new BBucketMeta.Data();
		meta.setDatabaseName("database");
		meta.setTableName("table1");
		meta.setRaftConfig("");
		meta.setKeyFirst(keyFirst);
		meta.setKeyLast(keyLast);
		agent.setBucketMeta(meta);
	}

	private static BBucketMeta.Data splittingMeta(Binary keyFirst, Binary keyLast, String raftConfig) {
		var meta = new BBucketMeta.Data();
		meta.setDatabaseName("database");
		meta.setTableName("table1");
		meta.setRaftConfig(raftConfig);
		meta.setKeyFirst(keyFirst);
		meta.setKeyLast(keyLast);
		return meta;
	}

	private static void commitPuts(Dbh2Agent agent, java.util.Map<Binary, Binary> puts) throws Exception {
		var batch = new BPrepareBatch.Data("", "database", "table1", null);
		batch.getBatch().getPuts().putAll(puts);
		batch.getBatch().setTid(tid.incrementAndGet());
		var f = agent.prepareBatch(batch);
		f.await();
		Assertions.assertEquals(0, f.get().getResultCode(), "prepare必须成功");
		var c = agent.commitBatch(batch.getBatch().getTid()).await();
		Assertions.assertEquals(0, c.get().getResultCode(), "commit必须成功");
	}

	private static void commitDelete(Dbh2Agent agent, Binary del) throws Exception {
		var batch = new BPrepareBatch.Data("", "database", "table1", null);
		batch.getBatch().getDeletes().add(del);
		batch.getBatch().setTid(tid.incrementAndGet());
		var f = agent.prepareBatch(batch);
		f.await();
		Assertions.assertEquals(0, f.get().getResultCode(), "prepare必须成功");
		var c = agent.commitBatch(batch.getBatch().getTid()).await();
		Assertions.assertEquals(0, c.get().getResultCode(), "commit必须成功");
	}

	private static void waitUntil(long timeoutMs, Supplier<Boolean> cond, String what) throws InterruptedException {
		var deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (Boolean.TRUE.equals(cond.get()))
				return;
			//noinspection BusyWait
			Thread.sleep(20);
		}
		Assertions.fail("timeout wait: " + what);
	}

	private static long smLong(Zeze.Dbh2.Dbh2 dbh2, String field) throws Exception {
		var f = Zeze.Dbh2.Dbh2StateMachine.class.getDeclaredField(field);
		f.setAccessible(true);
		return f.getLong(dbh2.getStateMachine());
	}

	private static void setDbh2Field(Zeze.Dbh2.Dbh2 dbh2, String field, Object value) throws Exception {
		var f = Zeze.Dbh2.Dbh2.class.getDeclaredField(field);
		f.setAccessible(true);
		f.set(dbh2, value);
	}

	private static void setDbh2Long(Zeze.Dbh2.Dbh2 dbh2, String field, long value) throws Exception {
		var f = Zeze.Dbh2.Dbh2.class.getDeclaredField(field);
		f.setAccessible(true);
		f.setLong(dbh2, value);
	}

	// 目标侧读：null=不存在（含墓碑）。
	private static Binary get(Dbh2Agent agent, Binary key) {
		var kv = agent.get("database", "table1", key);
		Assertions.assertTrue(kv.getKey(), "key必须在桶内");
		return kv.getValue() == null ? null : new Binary(kv.getValue().Bytes, kv.getValue().ReadIndex, kv.getValue().size());
	}

	/**
	 * 队列核心：日志序边界、墓碑编码、跨副本入队、合并序、水位推进、EndSplit清空换代、
	 * 陈旧世代ACK拒绝。
	 */
	@Test
	public void testQueueLifecycleTombstoneBoundaryAndGeneration(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var logDb = new RocksDatabase(tempDir.resolve("fnd24syncq-log").toString());
		var source = startBucket(logDb, SOURCE_RAFT, tempDir, "src");
		var agent = new Dbh2Agent(SOURCE_RAFT);
		try {
			var leader = waitLeader(source);
			setBucketMeta(agent, key(2), key(8));

			// 【边界】LogSetSplittingMeta之前的提交不入队（其值由复制迭代器视图承载）。
			commitPuts(agent, java.util.Map.of(key(6), value(1)));
			Assertions.assertFalse(leader.getStateMachine().hasPendingSplitSync(),
					"meta边界前的提交不得入队");

			// 进入分桶：splitting=[5,8)。
			leader.getRaft().appendLog(new LogSetSplittingMeta(splittingMeta(key(5), key(8), TARGET_RAFT)));
			waitUntil(10_000, () -> leader.getStateMachine().getBucket().getSplittingMeta() != null,
					"LogSetSplittingMeta apply");

			// 【入队】目标键域puts入队、非目标键域排除；【合并】事务A的v2被事务B的delete墓碑覆盖。
			commitPuts(agent, java.util.Map.of(key(6), value(2), key(3), value(3)));
			commitDelete(agent, key(6));

			Assertions.assertTrue(leader.getStateMachine().hasPendingSplitSync(), "分桶期间的提交必须入队");
			Assertions.assertEquals(2, smLong(leader, "splitSyncSeq"), "两条目标键域记录各占一个seq");

			// 【副本】follower同样入队——队列随raft apply重放，换主后新leader天然持有。
			var follower = source.stream().filter(n -> n != leader).findFirst().orElseThrow();
			waitUntil(10_000, () -> follower.getStateMachine().hasPendingSplitSync(),
					"follower apply后同样入队");

			// 【取出】poll不推进水位；合并结果=墓碑胜出、key(3)排除。
			var batch = leader.getStateMachine().pollSplitSync(10);
			Assertions.assertNotNull(batch);
			Assertions.assertTrue(batch.data.isFromTransaction(), "事务同步投递必须是replace语义");
			Assertions.assertEquals(2, batch.lastSeq);
			Assertions.assertEquals(0, batch.generation, "首世代");
			var puts = batch.data.getPuts();
			Assertions.assertEquals(1, puts.size(), "非目标键域必须排除，同key多事务合并为一条");
			Assertions.assertEquals(Binary.Empty, puts.get(key(6)), "delete必须编码为Binary.Empty墓碑且胜出同批put");
			Assertions.assertTrue(leader.getStateMachine().hasPendingSplitSync(), "poll不得推进水位");

			// 【推进】世代匹配的ACK推进；随后队列排空。
			leader.getStateMachine().advanceSplitSyncWatermark(batch);
			Assertions.assertFalse(leader.getStateMachine().hasPendingSplitSync(), "ACK后全送达");
			Assertions.assertNull(leader.getStateMachine().pollSplitSync(10), "排空后无可投递");

			// 【清空换代】EndSplit apply=迁移完结：计数器归零、世代+1。
			var from = leader.getStateMachine().getBucket().getBucketMeta().copy();
			from.setKeyLast(key(5));
			leader.getRaft().appendLog(new LogEndSplit(from, splittingMeta(key(5), key(8), TARGET_RAFT)));
			waitUntil(10_000,
					() -> leader.getStateMachine().getBucket().getBucketMeta().getKeyLast().compareTo(key(5)) == 0,
					"LogEndSplit apply");
			Assertions.assertEquals(0, smLong(leader, "splitSyncSeq"), "EndSplit清空seq");
			Assertions.assertEquals(0, smLong(leader, "splitSyncWatermark"), "EndSplit清空水位");
			Assertions.assertEquals(1, smLong(leader, "splitSyncGeneration"), "EndSplit换代");

			// 【世代戳】新世代入队后，旧世代（generation=0）迟到ACK不得推进水位——
			// seq从1重新分配，假推进会跳过新世代未投递记录（endSplit0门槛假通）。
			leader.getRaft().appendLog(new LogSetSplittingMeta(splittingMeta(key(4), key(5), TARGET_RAFT)));
			waitUntil(10_000, () -> leader.getStateMachine().getBucket().getSplittingMeta() != null
					&& leader.getStateMachine().getBucket().getSplittingMeta().getKeyFirst().compareTo(key(4)) == 0,
					"LogSetSplittingMeta(新世代) apply");
			commitPuts(agent, java.util.Map.of(key(4), value(4)));
			Assertions.assertEquals(1, smLong(leader, "splitSyncSeq"), "新世代seq从1重新分配");

			var stale = new Zeze.Dbh2.Dbh2StateMachine.SplitSyncBatch(batch.data, 2, 0);
			leader.getStateMachine().advanceSplitSyncWatermark(stale);
			Assertions.assertTrue(leader.getStateMachine().hasPendingSplitSync(),
					"陈旧世代ACK必须被拒绝（加固前：水位被污染、门槛假通）");
			Assertions.assertEquals(0, smLong(leader, "splitSyncWatermark"), "水位未被陈旧ACK推进");

			// 新世代自身的ACK照常推进。
			var batch2 = leader.getStateMachine().pollSplitSync(10);
			Assertions.assertNotNull(batch2);
			Assertions.assertEquals(1, batch2.generation, "新世代批次携新世代戳");
			Assertions.assertEquals(value(4), batch2.data.getPuts().get(key(4)));
			leader.getStateMachine().advanceSplitSyncWatermark(batch2);
			Assertions.assertFalse(leader.getStateMachine().hasPendingSplitSync());
		} finally {
			agent.close();
			stopBucket(source);
			logDb.close();
		}
	}

	/**
	 * 投递链：围栏窗口内不投递、围栏后单飞送达（replace语义在目标生效）、ACK推进水位。
	 * dbh2Splitting/splitLeaderReadyTime以反射直置（不驱动startSplit全流程，观测集中）。
	 */
	@Test
	public void testDriveSplitSyncFenceAndDelivery(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var logDb = new RocksDatabase(tempDir.resolve("fnd24syncd-log").toString());
		var source = startBucket(logDb, SOURCE_RAFT, tempDir, "src");
		var target = startBucket(logDb, TARGET_RAFT, tempDir, "dst");
		var sourceAgent = new Dbh2Agent(SOURCE_RAFT);
		var targetAgent = new Dbh2Agent(TARGET_RAFT);
		Dbh2Agent splittingAgent = null;
		try {
			var leader = waitLeader(source);
			waitLeader(target);
			setBucketMeta(sourceAgent, key(2), key(8));
			setBucketMeta(targetAgent, key(5), key(8));

			// 边界前旧值（不入队，由复制视图承载）。
			commitPuts(sourceAgent, java.util.Map.of(key(6), value(1)));

			leader.getRaft().appendLog(new LogSetSplittingMeta(splittingMeta(key(5), key(8), TARGET_RAFT)));
			waitUntil(10_000, () -> leader.getStateMachine().getBucket().getSplittingMeta() != null,
					"LogSetSplittingMeta apply");

			// 分桶期间提交v2：入队待投递。
			commitPuts(sourceAgent, java.util.Map.of(key(6), value(2)));
			Assertions.assertTrue(leader.getStateMachine().hasPendingSplitSync());

			// 直置投递上下文：agent就位+换主围栏锚点=now。围栏=AgentTimeout(3000)+2000=5000ms。
			splittingAgent = new Dbh2Agent(TARGET_RAFT);
			splittingAgent.getRaftAgent().setPendingLimit(Integer.MAX_VALUE);
			setDbh2Field(leader, "dbh2Splitting", splittingAgent);
			var fenceStart = System.currentTimeMillis();
			setDbh2Long(leader, "splitLeaderReadyTime", fenceStart);

			leader.driveSplitSync();

			// 【围栏】fenceStart+5000前不得投递（锚点+AgentTimeout+2000；留1.5s调度裕量取观测点）。
			Thread.sleep(3500);
			Assertions.assertNull(get(targetAgent, key(6)), "围栏窗口内不得投递（迟到旧值覆盖防护）");
			Assertions.assertTrue(leader.getStateMachine().hasPendingSplitSync(), "围栏窗口内水位不得推进");

			// 【送达】围栏过后投递、ACK推进水位、目标replace生效。
			waitUntil(15_000, () -> !leader.getStateMachine().hasPendingSplitSync(), "围栏后投递并ACK");
			Assertions.assertEquals(1, smLong(leader, "splitSyncWatermark"));
			Assertions.assertEquals(value(2), get(targetAgent, key(6)), "目标桶必须收到提交序终值(replace)");
		} finally {
			if (null != splittingAgent)
				splittingAgent.close();
			sourceAgent.close();
			targetAgent.close();
			stopBucket(source);
			stopBucket(target);
			logDb.close();
		}
	}
}
