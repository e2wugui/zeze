package Zeze.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Builtin.Timer.BCronTimer;
import Zeze.Builtin.Timer.BSimpleTimer;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import Zeze.Util.TaskOneByOneQueue;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestTimerMissfireIdentity {
	private static final AtomicInteger fired = new AtomicInteger();

	public static class CountingHandle implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) {
			fired.incrementAndGet();
		}
	}

	@Test
	void simpleMissfireKeepsLoadedIdentity() throws Exception {
		checkIdentity(false);
	}

	@Test
	void cronMissfireKeepsLoadedIdentity() throws Exception {
		checkIdentity(true);
	}

	private static void checkIdentity(boolean cron) throws Exception {
		Task.tryInitThreadPool();
		fired.set(0);
		var app = new Application("TimerMissfireIdentity", TakeoverTestEnv.newConf("off", 60_000, 60_000));
		var timer = new TakeoverTestEnv.AccessibleTimer(new TakeoverTestEnv.TestAppBase(app));
		var key = "TimerMissfireIdentity-" + app.getConfig().getServerId();
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var drained = new CountDownLatch(1);
		try {
			app.start();
			timer.loadCustomClassAnd();
			timer.start();
			var id = new String[1];
			assertEquals(0, app.newProcedure(() -> {
				var spec = cron ? TimerSpec.ofCron("0 0 0 1 1 ? 2040").oneByOneKey(key)
						.missfirePolicy(AbstractTimer.eMissfirePolicyRunOnce)
						: TimerSpec.ofDelay(3_600_000).period(60_000).oneByOneKey(key)
						.missfirePolicy(AbstractTimer.eMissfirePolicyRunOnce);
				id[0] = timer.schedule(spec,
						CountingHandle.class, null);
				var bean = timer.getTimer(id[0]).getTimerObj().getBean();
				if (bean instanceof BSimpleTimer simple)
					simple.setNextExpectedTime(System.currentTimeMillis() - 60_000);
				else
					((BCronTimer)bean).setNextExpectedTime(System.currentTimeMillis() - 60_000);
				return 0;
			}, "TimerMissfireIdentity.seed").call());
			timer.stop();
			TaskSpec.ofAction(() -> {
				entered.countDown();
				assertTrue(release.await(20, TimeUnit.SECONDS));
			}).executeOneByOne(key);
			assertTrue(entered.await(10, TimeUnit.SECONDS));
			timer.start();
			// 等补触发已经排到被阻塞的同key队列，避免用sleep猜测线程执行时点。
			var getQueue = TaskOneByOneByKey.class.getDeclaredMethod("getAndLockQueue", Object.class);
			getQueue.setAccessible(true);
			var queue = (TaskOneByOneQueue)getQueue.invoke(Task.getOneByOne(), key);
			queue.unlock();
			var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
			while (queue.size() < 2) { // 队列包含正在执行的阻塞动作。
				assertTrue(System.nanoTime() < deadline, "补触发未入队");
				Thread.sleep(10);
			}
			assertEquals(0, app.newProcedure(() -> {
				var index = timer._tIndexs.get(id[0]);
				index.setSerialId(index.getSerialId() + 1); // 同名定时器进入新一轮身份。
				return 0;
			}, "TimerMissfireIdentity.replace").call());
			TaskSpec.ofAction(drained::countDown).executeOneByOne(key);
			release.countDown();
			assertTrue(drained.await(10, TimeUnit.SECONDS));
			assertEquals(0, fired.get(), "旧装载动作必须保留旧serial并被现有身份检查拒绝");
		} finally {
			release.countDown();
			timer.stop();
			app.stop();
		}
	}
}
