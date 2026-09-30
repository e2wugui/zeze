package Zeze.Services.Log4jQuery;

import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import Zeze.Config;
import Zeze.Services.LogService;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import static harness.DirCleanup.deleteBestEffort;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Late Normal-dispatch NewSession requests must not revive a state closed by the selector. */
@Fast
public class TestClosedConnectionRejectsNewSession {
	private static final class PausedLogService extends LogService {
		Log4jFileManager manager;
		CountDownLatch creating;
		CountDownLatch resume;

		PausedLogService() throws Exception {
			super(new Config()); // Allocated without constructing network services.
		}

		@Override
		public Log4jFileManager getLogManager(String name) {
			creating.countDown();
			try {
				if (!resume.await(5, TimeUnit.SECONDS))
					throw new AssertionError("session creation not released");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AssertionError(e);
			}
			return manager;
		}
	}

	@Test
	public void delayedRegistrationCannotSurviveSocketClose() throws Exception {
		Task.tryInitThreadPool();
		var directory = Files.createTempDirectory("log-connection-close-registration");
		Log4jFileManager manager = null;
		ServerUserState state = null;
		var resume = new CountDownLatch(1);
		try {
			var conf = new LogServiceConf.LogConf();
			conf.logActive = "zeze.log";
			conf.logDir = directory.toString();
			manager = new Log4jFileManager(conf);
			var unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
			unsafeField.setAccessible(true);
			var service = (PausedLogService)((Unsafe)unsafeField.get(null)).allocateInstance(PausedLogService.class);
			service.manager = manager;
			service.creating = new CountDownLatch(1);
			service.resume = resume;
			state = new ServerUserState(service);
			var connection = state;
			try (var worker = Executors.newSingleThreadExecutor()) {
				var request = worker.submit(() -> {
					connection.newLogSession("zeze.log", 1);
					return null;
				});
				assertTrue(service.creating.await(5, TimeUnit.SECONDS), "NewSession entered the worker");
				try {
					connection.closeAsync(); // Selector callback must remain independent of the paused worker.
				} finally {
					resume.countDown();
				}
				var rejected = assertThrows(ExecutionException.class, () -> request.get(5, TimeUnit.SECONDS));
				assertTrue(rejected.getCause() instanceof IllegalStateException,
						"Late registration must report that the connection state is closed");
				assertNull(connection.getLogSession(1), "No late session may remain after socket close");
			}
		} finally {
			resume.countDown();
			if (state != null)
				state.close();
			if (manager != null)
				manager.stop();
			deleteBestEffort(directory);
		}
	}
}
