package Zeze.Dbh2;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.SetBucketMeta;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Raft.Raft;
import Zeze.Util.Action0;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 分桶收尾门槛（prepareQueue + noTransactionHandle）在失主、换任恢复与收尾完成路径上的释放不变量：
 * 1) 收尾等待期间失主时，门槛与拦截队列必须释放（不永久滞留至复选挂死）；
 * 2) recoverSplitting 换任入口在 splittingMeta==null 时同样无条件清理残留屏障，复选后写请求正常提交；
 * 3) releasePrepareQueue 必须在释放 prepareQueueLock 前向 executeUserTask 提交排空任务，不在调用线程
 *    （如持 raft.lock 的 apply 线程）同步执行用户 RPC，且队列旧请求严格先于解锁后新到任务执行；
 * 4) 陈旧 serialNo 的收尾回调不得误释新轮次已装载的拦截队列。
 */
@Fast
public class TestSplitEndgameStepDownReleasesPrepareQueue {

	private static int[] allocatePorts(int count) throws Exception {
		var sockets = new ArrayList<ServerSocket>(count);
		try {
			var ports = new int[count];
			for (int i = 0; i < count; ++i) {
				var ss = new ServerSocket(0);
				sockets.add(ss);
				ports[i] = ss.getLocalPort();
			}
			return ports;
		} finally {
			for (var ss : sockets)
				ss.close();
		}
	}

	@SuppressWarnings("unchecked")
	private static @Nullable ConcurrentLinkedQueue<Action0> getPrepareQueue(Dbh2.Dbh2RaftServer server) throws Exception {
		Field field = Dbh2.Dbh2RaftServer.class.getDeclaredField("prepareQueue");
		field.setAccessible(true);
		return (ConcurrentLinkedQueue<Action0>)field.get(server);
	}

	private static void setSplitSerialNo(Dbh2 dbh2, long serialNo) throws Exception {
		Field field = Dbh2.class.getDeclaredField("splitSerialNo");
		field.setAccessible(true);
		field.setLong(dbh2, serialNo);
	}

	private static void invokeBlockPrepare(Dbh2 dbh2, boolean isMove, long serialNo) throws Exception {
		Method m = Dbh2.class.getDeclaredMethod("blockPrepareUntilNoTransaction", boolean.class, long.class);
		m.setAccessible(true);
		m.invoke(dbh2, isMove, serialNo);
	}

	private static void invokeConsumePrepareAndBlockAgain(Dbh2 dbh2, boolean isMove, long serialNo) throws Exception {
		Method m = Dbh2.class.getDeclaredMethod("consumePrepareAndBlockAgain", boolean.class, long.class);
		m.setAccessible(true);
		m.invoke(dbh2, isMove, serialNo);
	}

	private static void invokeEndSplit0(Dbh2 dbh2, boolean isMove, long serialNo) throws Exception {
		Method m = Dbh2.class.getDeclaredMethod("endSplit0", boolean.class, long.class);
		m.setAccessible(true);
		m.invoke(dbh2, isMove, serialNo);
	}

	private static void invokeEndSplit1(Dbh2 dbh2, Protocol<?> p, boolean isMove, long serialNo) throws Exception {
		Method m = Dbh2.class.getDeclaredMethod("endSplit1", Protocol.class, boolean.class, long.class);
		m.setAccessible(true);
		m.invoke(dbh2, p, isMove, serialNo);
	}

	private static void invokeRecoverSplitting(Dbh2 dbh2) throws Exception {
		Method m = Dbh2.class.getDeclaredMethod("recoverSplitting");
		m.setAccessible(true);
		m.invoke(dbh2);
	}

	private static String buildRaftConfig(int[] ports) {
		return """
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="">
					<node Host="127.0.0.1" Port="%d"/>
					<node Host="127.0.0.1" Port="%d"/>
					<node Host="127.0.0.1" Port="%d"/>
				</raft>
				""".formatted(ports[0], ports[1], ports[2]);
	}

	private static BBucketMeta.Data defaultMeta() {
		var meta = new BBucketMeta.Data();
		meta.setDatabaseName("database");
		meta.setTableName("table1");
		meta.setRaftConfig("");
		meta.setKeyFirst(Binary.Empty);
		meta.setKeyLast(Binary.Empty);
		return meta;
	}

	@Test
	void testStepDownAndRecoverSplittingReleasePrepareQueue(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var raftConfig = buildRaftConfig(allocatePorts(3));
		var database = new RocksDatabase(tempDir.resolve("endgame_stepdown_release").toString());
		var taskOneByOne = new TaskOneByOneByKey();
		var nodes = RaftBucketTopologySupport.startBucket(database, raftConfig, tempDir, taskOneByOne);
		var agent = new Dbh2Agent(raftConfig);
		try {
			agent.setBucketMeta(defaultMeta());
			var leaderA = RaftBucketTopologySupport.waitLeader(nodes);

			// 挂起一笔在途事务 tid=100，使 blockPrepareUntilNoTransaction 挂起在 noTransactionHandle。
			var batch100 = new BPrepareBatch.Data("", "database", "table1", null);
			batch100.getBatch().setTid(100L);
			batch100.getBatch().getPuts().put(new Binary(new byte[]{1}), new Binary(new byte[]{9}));
			var f100 = agent.prepareBatch(batch100);
			f100.await();
			Assertions.assertEquals(0, f100.get().getResultCode());

			setSplitSerialNo(leaderA, 1L);
			invokeBlockPrepare(leaderA, false, 1L);
			var serverA = (Dbh2.Dbh2RaftServer)leaderA.getRaft().getServer();
			Assertions.assertNotNull(getPrepareQueue(serverA), "进入收尾门槛后 prepareQueue 应已装载");

			// 模拟 Leader A 掉主降级为 Follower。
			leaderA.getRaft().lock();
			try {
				leaderA.getRaft().convertStateTo(Raft.RaftState.Follower);
			} finally {
				leaderA.getRaft().unlock();
			}

			// 掉主后收尾回调（endSplit0 / consumePrepareAndBlockAgain）必须清空 prepareQueue 与 handle。
			invokeConsumePrepareAndBlockAgain(leaderA, false, 1L);
			Assertions.assertNull(getPrepareQueue(serverA), "失主后 prepareQueue 必须释放");
			Assertions.assertFalse(leaderA.getStateMachine().hasNoTransactionHandle(),
					"失主后 noTransactionHandle 必须清空");

			// 结束在途事务，等待新主就绪，再验证 recoverSplitting 兜底清理残留 prepareQueue。
			Assertions.assertEquals(0, agent.commitBatch(100L).await().get().getResultCode());
			var activeLeader = RaftBucketTopologySupport.waitLeader(nodes);
			var activeServer = (Dbh2.Dbh2RaftServer)activeLeader.getRaft().getServer();
			activeServer.setupPrepareQueue();
			Assertions.assertNotNull(getPrepareQueue(activeServer));

			// 此时 splittingMeta==null，recoverSplitting 仍必须无条件释放残留 prepareQueue。
			Assertions.assertNull(requireNonNull(activeLeader.getStateMachine().getBucket()).getSplittingMeta());
			invokeRecoverSplitting(activeLeader);
			Assertions.assertNull(getPrepareQueue(activeServer),
					"recoverSplitting 在 splittingMeta==null 时也必须释放残留 prepareQueue");

			// 新写请求必须正常提交而非永久挂死在拦截队列中。
			var key2 = new Binary(new byte[]{2});
			var val2 = new Binary(new byte[]{7, 7});
			var batch200 = new BPrepareBatch.Data("", "database", "table1", null);
			batch200.getBatch().setTid(200L);
			batch200.getBatch().getPuts().put(key2, val2);
			var f200 = agent.prepareBatch(batch200);
			f200.await();
			Assertions.assertEquals(0, f200.get().getResultCode(), "释放拦截队列后新的 PrepareBatch 必须成功");
			Assertions.assertEquals(0, agent.commitBatch(200L).await().get().getResultCode());
			Assertions.assertEquals(val2, RaftBucketTopologySupport.get(agent, key2));
		} finally {
			RaftBucketTopologySupport.stopBucket(nodes, agent, database);
		}
	}

	@Test
	void testReleasePrepareQueueDrainsAsyncInFifoOrderBeforeNewTasks(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var raftConfig = buildRaftConfig(allocatePorts(3));
		var database = new RocksDatabase(tempDir.resolve("release_queue_fifo").toString());
		var taskOneByOne = new TaskOneByOneByKey();
		var nodes = RaftBucketTopologySupport.startBucket(database, raftConfig, tempDir, taskOneByOne);
		var agent = new Dbh2Agent(raftConfig);
		var fifoBlocked = new CountDownLatch(1);
		try {
			agent.setBucketMeta(defaultMeta());
			var leader = RaftBucketTopologySupport.waitLeader(nodes);
			var server = (Dbh2.Dbh2RaftServer)leader.getRaft().getServer();

			var fifoEntered = new CountDownLatch(1);
			leader.getRaft().executeUserTask(() -> {
				fifoEntered.countDown();
				//noinspection ResultOfMethodCallIgnored
				fifoBlocked.await(10, TimeUnit.SECONDS);
			});
			Assertions.assertTrue(fifoEntered.await(5, TimeUnit.SECONDS));

			var executionOrder = new CopyOnWriteArrayList<String>();
			var allDone = new CountDownLatch(2);
			server.setupPrepareQueue();
			requireNonNull(getPrepareQueue(server)).add(() -> {
				executionOrder.add("queued");
				allDone.countDown();
			});

			// 释放队列：不得在调用线程同步执行，且必须在解锁前把排空任务压入 executeUserTask。
			server.releasePrepareQueue();
			Assertions.assertNull(getPrepareQueue(server), "releasePrepareQueue 必须立即清空 prepareQueue 引用");
			Assertions.assertTrue(executionOrder.isEmpty(),
					"releasePrepareQueue 不得在调用线程（如持 raft.lock 的 apply 线程）同步执行排空任务");

			leader.getRaft().executeUserTask(() -> {
				executionOrder.add("after");
				allDone.countDown();
			});

			fifoBlocked.countDown();
			Assertions.assertTrue(allDone.await(5, TimeUnit.SECONDS), "排空任务与后续任务必须完成");
			Assertions.assertEquals(List.of("queued", "after"), executionOrder,
					"队列内旧请求必须严格先于解锁后新提交任务在 executeUserTask 上串行执行");
		} finally {
			fifoBlocked.countDown();
			RaftBucketTopologySupport.stopBucket(nodes, agent, database);
		}
	}

	@Test
	void testStaleSerialNoDoesNotReleaseNewRoundPrepareQueue(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var raftConfig = buildRaftConfig(allocatePorts(3));
		var database = new RocksDatabase(tempDir.resolve("stale_serial_guard").toString());
		var taskOneByOne = new TaskOneByOneByKey();
		var nodes = RaftBucketTopologySupport.startBucket(database, raftConfig, tempDir, taskOneByOne);
		var agent = new Dbh2Agent(raftConfig);
		try {
			agent.setBucketMeta(defaultMeta());
			var leader = RaftBucketTopologySupport.waitLeader(nodes);
			var server = (Dbh2.Dbh2RaftServer)leader.getRaft().getServer();

			// 模拟新轮次 serialNo=2 正在收尾并持有 prepareQueue。
			setSplitSerialNo(leader, 2L);
			server.setupPrepareQueue();

			// 旧轮次 serialNo=1 的陈旧回调到达，不得清掉轮次 2 的拦截队列。
			invokeEndSplit0(leader, false, 1L);
			invokeConsumePrepareAndBlockAgain(leader, false, 1L);
			var setMeta = new SetBucketMeta();
			setMeta.setResultCode(0);
			invokeEndSplit1(leader, setMeta, false, 1L);

			Assertions.assertNotNull(getPrepareQueue(server),
					"Leader 上旧轮次陈旧收尾回调不得释放新轮次已装载的 prepareQueue");
			server.releasePrepareQueue();
		} finally {
			RaftBucketTopologySupport.stopBucket(nodes, agent, database);
		}
	}
}
