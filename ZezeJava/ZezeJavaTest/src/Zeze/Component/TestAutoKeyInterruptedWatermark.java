package Zeze.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongUnaryOperator;
import Zeze.Application;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestAutoKeyInterruptedWatermark {
	@Test
	public void interruptionCannotRestoreOldRangeBeforeWatermarkCommits() throws Exception {
		var config = TakeoverTestEnv.newConf("off", 600_000, 600_000);
		var app = new Application("InterruptedWatermark" + config.getServerId(), config);
		app.start();
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var failure = new AtomicReference<Throwable>();
		var completed = new CountDownLatch(1);
		var key = app.getAutoKey("interrupted-watermark");
		key.nextId();
		// 注入水位计算函数中的屏障，精确卡住setSeed/increaseSeed共用的写库任务。
		var raise = AutoKey.class.getDeclaredMethod("raiseWatermark", String.class, LongUnaryOperator.class);
		raise.setAccessible(true);
		LongUnaryOperator raiser = current -> {
			entered.countDown();
			try {
				Assertions.assertTrue(release.await(10, TimeUnit.SECONDS));
			} catch (InterruptedException e) {
				throw new AssertionError(e);
			}
			return 1_000_000;
		};
		var setter = new Thread(() -> {
			try {
				Assertions.assertEquals(true, raise.invoke(key, "AutoKey.setSeed", raiser));
				Assertions.assertTrue(Thread.currentThread().isInterrupted(), "preserve interrupt status");
			} catch (Throwable e) {
				failure.set(e);
			} finally {
				completed.countDown();
			}
		});
		try {
			setter.start();
			Assertions.assertTrue(entered.await(5, TimeUnit.SECONDS));
			setter.interrupt();
			Assertions.assertFalse(completed.await(100, TimeUnit.MILLISECONDS),
					"must retain the allocation lock until the submitted write finishes");
			release.countDown();
			Assertions.assertTrue(completed.await(5, TimeUnit.SECONDS));
			Assertions.assertNull(failure.get());
			var encoded = ByteBuffer.Allocate();
			encoded.WriteUInt(config.getServerId());
			encoded.WriteULong(1_000_000);
			Assertions.assertTrue(key.nextId() > ByteBuffer.ToLongBE(encoded.Bytes, 0, encoded.WriteIndex));
		} finally {
			release.countDown();
			setter.join(5_000);
			app.stop();
		}
	}
}
