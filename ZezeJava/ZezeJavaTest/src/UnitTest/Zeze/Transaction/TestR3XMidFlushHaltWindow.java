package UnitTest.Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.Application;
import Zeze.Config;
import Zeze.Onz.OnzProcedure;
import Zeze.Transaction.Record;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * R3-X①回归：mid-flush halt窄窗——FND7-54的停机拒绝只拦"新提交"，不等待已过门的在飞flush
 * （Immediately模式业务线程的checkpoint.flush、Reduce降级flush、checkpointRun的runOnce）：
 * 它们在Checkpoint.flush worker内已打开LocalRocksCacheDb事务，Application.stop在终检点后
 * 直接close+deleteDirectory与之并发，是ad5801593同型的"close与数据通路并发native UAF"。
 * 修复：Checkpoint.flush入口/finally维护activeFlush计数（锁内inc/dec），stop在checkpoint
 * join后、LocalRocksCacheDb.close前有界等待activeFlush==0（30s上限，超时告警继续）。
 * 测试用受控OnzProcedure把一个真实flush（空记录集仍会打开LocalRocksCacheDb事务）钉死在
 * worker内的sendFlushAndWait点：stop必须等它归零才关库；放行后flush干净完成、stop必达eStopped。
 * 修复前红：stop在在飞flush未结束时即完成（不等），放行后flush踩已关闭/已删目录的库报错。
 */
@Fast
public class TestR3XMidFlushHaltWindow {
	// 独立serverId+url：@Fast类并行时避免本地RocksCache目录互撞（对齐TestFnd754StopCommitGate）。
	private static final int SERVER_ID = 7310;

	private Application app;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("r3x_mid_flush_halt_" + SERVER_ID);
		conf.getDatabaseConfMap().put("", dbConf);
		app = new Application("TestR3XMidFlushHaltWindow", conf);
		app.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (app.getStartState() != Application.StartState.eStopped)
			app.stop(); // 已停实例幂等
	}

	/** 受控在飞flush：真实进入Checkpoint.flush worker（打开LocalRocksCacheDb事务）后停在sendFlushAndWait。 */
	private static final class StalledFlushOnz extends OnzProcedure {
		final CountDownLatch entered = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);

		StalledFlushOnz() {
			super(null, null, null, null, null); // flush路径只使用isEnd/sendFlushReady
		}

		@Override
		protected TaskCompletionSource<Long> sendFlushReady() {
			entered.countDown();
			try {
				release.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			var future = new TaskCompletionSource<Long>();
			future.setResult(0L);
			return future;
		}
	}

	@Test
	public void testStopWaitsInFlightFlushBeforeCloseDb() throws Exception {
		var checkpoint = app.getCheckpoint();
		var onz = new StalledFlushOnz();
		var flushError = new AtomicReference<Throwable>();
		var flushDone = new CountDownLatch(1);
		var flushThread = Thread.ofPlatform().daemon().start(() -> {
			try {
				// 空记录集：worker仍会打开LocalRocksCacheDb事务并到达Onz等待点——真实的在飞flush。
				checkpoint.flush(List.<Record>of(), Set.<OnzProcedure>of(onz), null);
			} catch (Throwable e) {
				flushError.set(e);
			} finally {
				flushDone.countDown();
			}
		});
		assertTrue(onz.entered.await(10, TimeUnit.SECONDS),
				"flush必须进入worker内的Onz等待点（此时已打开LocalRocksCacheDb事务）");

		var stopError = new AtomicReference<Throwable>();
		var stopThread = Thread.ofPlatform().daemon().start(() -> {
			try {
				app.stop();
			} catch (Throwable e) {
				stopError.set(e);
			}
		});

		// 在飞flush未结束：stop必须停在有界等待上，不得完成（修复前：直接完成并关库）。
		Thread.sleep(500);
		assertTrue(stopThread.isAlive(),
				"activeFlush!=0时stop必须等待，不得先关LocalRocksCacheDb（R3-X①）");

		// 放行在飞flush：stop等到归零后继续关库，两者不再竞争。
		onz.release.countDown();
		stopThread.join(10_000);
		assertFalse(stopThread.isAlive(), "放行后stop必须完成（有界等待不死锁）");
		assertTrue(flushDone.await(10, TimeUnit.SECONDS), "flush线程必须结束");
		assertNull(flushError.get(), "stop等待归零后flush干净完成，不得踩已关闭的库（修复前：close并发→异常/原生崩溃）");
		assertNull(stopError.get(), "stop不得抛异常");
		assertEquals(Application.StartState.eStopped, app.getStartState(), "终态必达eStopped（FND7-56不变量保持）");
	}

	/** 无在飞flush时stop照常完成（等待不引入额外阻塞）。 */
	@Test
	public void testStopUnblockedWithoutInFlightFlush() throws Exception {
		app.stop();
		assertEquals(Application.StartState.eStopped, app.getStartState());
	}
}
