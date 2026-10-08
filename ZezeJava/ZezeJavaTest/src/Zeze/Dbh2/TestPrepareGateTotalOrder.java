package Zeze.Dbh2;

import harness.Extra;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
 * PrepareBatch 拦截门槛与事务注册的全序不变量（dbh2-01）：分桶收尾装载拦截队列后，
 * "无在途事务"门槛的判定（one-shot/trigger）必须与 PrepareBatch 的注册落在同一条
 * raft 串行 FIFO 序上。bug 时 triggerNoTransactionIf 在 raft apply 线程内联运行
 * handle——被拦截的 PrepareBatch 尚未排空注册，门槛已关闭，其写入将落在收尾的
 * deleteToEnd 键域内被静默丢弃。修复后 trigger 锁内摘 handle 转投 raft 串行执行器
 * 重新过闸：排空注册在先则看到非空事务而推迟，终局不启动。形制对齐
 * TestUndoCommitFenceResurrect（进程内 raft 桶直驱 leader 状态机）。
 */
@Fast
@Extra
public class TestPrepareGateTotalOrder {
	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	private static final String RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19320"/>
				<node Host="127.0.0.1" Port="19321"/>
				<node Host="127.0.0.1" Port="19322"/>
			</raft>
			""";

	private static List<Dbh2> startBucket(RocksDatabase database, Path tempDir) {
		var nodes = new ArrayList<Dbh2>();
		for (var config : RaftConfig.loadFromString(RAFT).getNodes().values()) {
			var nodeConfig = RAFT.replaceFirst("<raft ",
					"<raft DbHome=\"" + tempDir.resolve(config.getName().replace(':', '_')) + "\" ");
			nodes.add(new Dbh2(null, config.getName(), database,
					RaftConfig.loadFromString(nodeConfig), null, false, taskOneByOne));
		}
		return nodes;
	}

	private static Dbh2StateMachine leaderStateMachine(List<Dbh2> nodes) throws InterruptedException {
		for (var i = 0; i < 300; ++i) { // 选举收敛等待（对齐 harness 常规轮询形态）
			for (var n : nodes)
				if (n.getStateMachine().getRaft().isLeader())
					return n.getStateMachine();
			Thread.sleep(50);
		}
		throw new AssertionError("raft leader not elected");
	}

	private static void stopBucket(List<Dbh2> nodes) {
		for (var n : nodes) {
			n.close();
			Zeze.Raft.LogSequence.deleteDirectory(new java.io.File(n.getRaft().getRaftConfig().getDbHome()));
		}
	}

	/** 经 raft prepare 一笔单 key 事务，返回批次（tid 已设）。 */
	private static BPrepareBatch.Data prepare(Dbh2Agent agent, long tid, Binary key) throws Exception {
		var batch = new BPrepareBatch.Data("", "database", "table1", null);
		batch.getBatch().getPuts().put(key, new Binary(new byte[]{(byte)9}));
		batch.getBatch().setTid(tid);
		agent.prepareBatch(batch).await();
		return batch;
	}

	private static void waitTransactionApplied(Dbh2StateMachine leader, long tid) throws InterruptedException {
		for (var i = 0; i < 300; ++i) {
			if (leader.getTransactions().containsKey(tid))
				return;
			Thread.sleep(50);
		}
		throw new AssertionError("prepare not applied: tid=" + tid);
	}

	// 观测拦截队列非空（反射窥视，不取走）：拦截成立与"请求已到达"的确定性判据。
	private static boolean queueHasItem(Dbh2.Dbh2RaftServer server) throws Exception {
		var field = Dbh2.Dbh2RaftServer.class.getDeclaredField("prepareQueue");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		var queue = (java.util.concurrent.ConcurrentLinkedQueue<Object>)field.get(server);
		return null != queue && !queue.isEmpty();
	}

	/**
	 * 门槛全序：装载拦截队列+装备 one-shot（挂起事务在场）→拦截一个 PrepareBatch→
	 * apply 侧终结挂起事务。门槛必须等到拦截排空（注册）之后才允许关闭；事务非空时
	 * 推迟（重新武装），终局不启动；事务真正排空后终局启动（防永久推迟的闭环）。
	 */
	@Test
	public void testGateWaitsForDrainedPrepareRegistration(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var rocks = new RocksDatabase(tempDir.resolve("dbh2GateTotalOrder").toString());
		var nodes = startBucket(rocks, tempDir);
		var agent = new Dbh2Agent(RAFT);
		// 堵塞任务自释兜底：红路径断言失败时 latch 未放行，close 前的 finally 负责放行，
		// 双保险避免 raft 串行执行器被测试残留任务钉死。
		var fifoBlocked = new CountDownLatch(1);
		try {
			var meta = new BBucketMeta.Data();
			meta.setDatabaseName("database");
			meta.setTableName("table1");
			meta.setRaftConfig("");
			meta.setKeyFirst(Binary.Empty);
			meta.setKeyLast(Binary.Empty);
			agent.setBucketMeta(meta);
			var leader = leaderStateMachine(nodes);

			// 挂起事务 tid=100：真实 prepare 已注册、未提交。
			prepare(agent, 100L, new Binary(new byte[]{1}));
			waitTransactionApplied(leader, 100L);

			// 堵住 raft 串行执行器（FIFO）：其后提交的任务全部排队，测试控制串行序。
			var fifoEntered = new CountDownLatch(1);
			leader.getRaft().executeUserTask(() -> {
				fifoEntered.countDown();
				//noinspection ResultOfMethodCallIgnored
				fifoBlocked.await(30, TimeUnit.SECONDS);
			});
			Assertions.assertTrue(fifoEntered.await(10, TimeUnit.SECONDS), "FIFO 堵塞任务必须先运行");

			// 装载拦截队列并装备 one-shot 门槛（对齐 blockPrepareUntilNoTransaction 的
			// setupPrepareQueue+setupOneShotIfNoTransaction 两步）：挂起事务在场→只装备不触发。
			var server = (Dbh2.Dbh2RaftServer)leader.getRaft().getServer();
			server.setupPrepareQueue();
			var endgameStarted = new AtomicBoolean(false);
			leader.setupOneShotIfNoTransaction(() -> endgameStarted.set(true));
			Assertions.assertTrue(leader.hasNoTransactionHandle(), "挂起事务在场时 handle 只装备不触发");

			// 拦截一个 PrepareBatch（tid=101）：真实派发路径入队，不得注册。
			var batch101 = new BPrepareBatch.Data("", "database", "table1", null);
			batch101.getBatch().getPuts().put(new Binary(new byte[]{2}), new Binary(new byte[]{(byte)9}));
			batch101.getBatch().setTid(101L);
			agent.prepareBatch(batch101); // 不 await：被拦截，无应答
			for (var i = 0; i < 200 && !queueHasItem(server); ++i)
				Thread.sleep(50); // 等真实派发路径把请求落入拦截队列
			Assertions.assertTrue(queueHasItem(server), "PrepareBatch 必须被拦截队列接住");
			Assertions.assertFalse(leader.getTransactions().containsKey(101L),
					"拦截期间 PrepareBatch 不得注册");

			// apply 侧终结挂起事务（模拟最后在途事务的 LogCommitBatch apply）。
			leader.commitBatch(100L);

			// 【红】现状：trigger 在 apply 线程内联运行 handle，门槛在拦截排空注册前关闭。
			Assertions.assertFalse(endgameStarted.get(),
					"门槛必须在拦截队列排空（tid=101 注册）之后才能关闭");

			// 排空拦截队列（performPrepareQueue 形态）：tid=101 同步注册。
			var intercepted = server.takePrepareQueue();
			Assertions.assertNotNull(intercepted, "拦截队列必须持有 tid=101");
			for (var action : intercepted)
				action.run();
			waitTransactionApplied(leader, 101L);

			// 释放 FIFO：转投的门槛任务在注册之后过闸→看到非空事务→推迟（重新武装），终局不启动。
			fifoBlocked.countDown();
			for (var i = 0; i < 200 && !leader.hasNoTransactionHandle(); ++i)
				Thread.sleep(50); // 等转投任务在 FIFO 上运行（摘取后 handle 位为空，重新武装才回 true）
			Assertions.assertTrue(leader.hasNoTransactionHandle(),
					"非空事务时门槛必须推迟并重新武装 handle");
			Assertions.assertFalse(endgameStarted.get(), "门槛推迟期间终局不得启动");

			// 闭环：终结 tid=101→再次过闸→事务排空→终局启动。
			leader.commitBatch(101L);
			for (var i = 0; i < 200 && !endgameStarted.get(); ++i)
				Thread.sleep(50);
			Assertions.assertTrue(endgameStarted.get(), "事务真正排空后终局必须启动");
		} finally {
			fifoBlocked.countDown();
			stopBucket(nodes);
			agent.close();
			rocks.close();
		}
	}
}
