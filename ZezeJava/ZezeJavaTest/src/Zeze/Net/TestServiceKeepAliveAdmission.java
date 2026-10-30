package Zeze.Net;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Util.Task;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestServiceKeepAliveAdmission {
	private static final class GatedService extends Service {
		final CountDownLatch entered = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);

		GatedService() {
			super("TestServiceKeepAliveAdmission");
			getConfig().getHandshakeOptions().setKeepCheckPeriod(3600);
		}

		@Override
		public boolean tryLock() {
			entered.countDown();
			try {
				assertTrue(release.await(5, TimeUnit.SECONDS));
			} catch (InterruptedException e) {
				throw new AssertionError(e);
			}
			return super.tryLock();
		}
	}

	@Test
	void stoppedServiceRejectsAnAlreadyAdmittedLazyStartAndCanRestart() throws Exception {
		Task.tryInitThreadPool();
		var service = new GatedService();
		var failure = new AtomicReference<@Nullable Throwable>();
		var starter = new Thread(() -> {
			try {
				service.tryStartKeepAliveCheckTimer();
			} catch (Throwable e) {
				failure.set(e);
			}
		});
		starter.start();
		try {
			assertTrue(service.entered.await(5, TimeUnit.SECONDS));
			service.stop();
			service.release.countDown();
			starter.join(5000);
			assertFalse(starter.isAlive());
			assertNull(failure.get());
			assertNull(service.keepCheckTimer, "stop返回后，已过快路径的懒启动不得重新登记定时器");
			service.start();
			assertNotNull(service.keepCheckTimer, "显式重启仍应恢复定时器");
		} finally {
			service.release.countDown();
			starter.join(5000);
			service.stop();
		}
	}
}
