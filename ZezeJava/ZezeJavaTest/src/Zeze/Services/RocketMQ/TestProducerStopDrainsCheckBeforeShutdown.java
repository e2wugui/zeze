package Zeze.Services.RocketMQ;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import harness.FastServerIds;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * stop() 的动作次序：回查线程池的有界排空必须先于 producer.shutdown()——回查任务不止
 * "查表"（checkLocalTransaction 触 tSent），得出 COMMIT 决策后还要经客户端把应答发回
 * broker（endTransactionOneway）。客户端先关则排空窗口内执行完的在飞/排队回查的决策
 * 确定性送不出去（通道已关且 producer 已从 broker 反注册），broker 回查次数耗尽后丢弃
 * 半消息——停机超过回查窗口即"本地事务已提交而消息灭失"，恰是本桥设计承诺要防住的
 * 区间。修复前顺序为 producer.shutdown() → checkExecutor 排空。
 */
@Fast
@ResourceLock("rocketmq.producer.processSlot") // 构造 Producer 占用进程级静态计数，与其它 Producer 测试串行
public class TestProducerStopDrainsCheckBeforeShutdown {
	private static final int SERVER_ID = FastServerIds.TEST_PRODUCER_STOP_DRAINS_CHECK_BEFORE_SHUTDOWN;

	private Application app;

	private static Application newApp(int serverId, String name) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("rocketmq_stop_drain_" + serverId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application(name, conf);
	}

	@BeforeEach
	public void setUp() throws Exception {
		app = newApp(SERVER_ID, "TestProducerStopDrainsCheckBeforeShutdown");
	}

	@AfterEach
	public void tearDown() throws Exception {
		app.stop();
	}

	/** 记录 shutdown 调用时点的客户端替身（未 start 的客户端无网络副作用，stop 的次序可观测）。 */
	public static final class RecordingProducer extends TransactionMQProducer {
		final CountDownLatch shutdownCalled = new CountDownLatch(1);
		final AtomicLong shutdownAt = new AtomicLong();

		public RecordingProducer() {
			super("testStopDrain");
		}

		@Override
		public void shutdown() {
			shutdownAt.set(System.nanoTime());
			shutdownCalled.countDown();
		}
	}

	/**
	 * 注入慢回查（占住回查线程池）后调用 stop()：排空窗口内客户端不得被关（修复前
	 * producer.shutdown() 先行切断应答通道）；释放慢回查后 stop 才关客户端，且关不早于
	 * 在飞回查完成——决策后的应答发送有完整客户端存活。
	 */
	@Test
	public void stopDrainsInFlightCheckBeforeClientShutdown() throws Exception {
		var producer = new Producer(app, "testStopDrain", new ClientConfig());
		try {
			var recorder = new RecordingProducer();
			swapProducer(producer, recorder);

			var taskStarted = new CountDownLatch(1);
			var releaseTask = new CountDownLatch(1);
			var taskFinishedAt = new AtomicLong();
			checkExecutorOf(producer).execute(() -> {
				taskStarted.countDown();
				try {
					assertTrue(releaseTask.await(30, TimeUnit.SECONDS), "测试注入被意外中断");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				taskFinishedAt.set(System.nanoTime()); // 决策完成点：其后才是应答发送
			});
			assertTrue(taskStarted.await(10, TimeUnit.SECONDS), "慢回查任务须已占住线程池");

			var stopThread = new Thread(producer::stop, "test-stop-drain");
			stopThread.start();
			assertFalse(recorder.shutdownCalled.await(1500, TimeUnit.MILLISECONDS),
					"回查排空完成前不得shutdown客户端（修复前先行关闭，在飞回查的COMMIT决策送不出去）");

			releaseTask.countDown(); // 慢回查放行：排空随即完成
			stopThread.join(TimeUnit.SECONDS.toMillis(30));
			assertFalse(stopThread.isAlive(), "stop须在排空预算内返回");
			assertTrue(recorder.shutdownCalled.await(1, TimeUnit.SECONDS), "排空完成后必须关闭客户端");
			assertTrue(recorder.shutdownAt.get() >= taskFinishedAt.get(),
					"shutdown不得早于在飞回查完成（应答发送须有存活的客户端）");
		} finally {
			producer.stop();
		}
	}

	// producer/checkExecutor 为 private final：测试经反射替换/读取（次序可观测性，
	// 不为测试放宽封装；final实例字段在setAccessible后可写）。
	private static void swapProducer(Producer owner, TransactionMQProducer replacement) throws Exception {
		Field field = Producer.class.getDeclaredField("producer");
		field.setAccessible(true);
		field.set(owner, replacement);
	}

	private static ThreadPoolExecutor checkExecutorOf(Producer owner) throws Exception {
		Field field = Producer.class.getDeclaredField("checkExecutor");
		field.setAccessible(true);
		return (ThreadPoolExecutor)field.get(owner);
	}
}
