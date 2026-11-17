package Zeze.Dbh2;

import java.io.IOException;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestMasterAgentStartCleanup {
	private static final class FailingService extends MasterAgent.Service {
		final IOException startFailure = new IOException("start failed after resource acquisition");
		final IOException stopFailure = new IOException("cleanup failed");
		final boolean failCleanup;
		int stops;

		FailingService(boolean failCleanup) {
			super(new Config());
			this.failCleanup = failCleanup;
		}

		@Override
		public void start() throws Exception {
			super.start();
			throw startFailure;
		}

		@Override
		public void stop() throws Exception {
			stops++;
			super.stop();
			if (failCleanup)
				throw stopFailure;
		}
	}

	@Test
	void failedStartupClosesTheUnpublishedService() {
		var service = new FailingService(false);
		var agent = new MasterAgent(new Config(), p -> 0, p -> 0, service);
		var failure = assertThrows(RuntimeException.class, agent::startAndWaitConnectionReady);
		assertSame(service.startFailure, failure.getCause());
		assertEquals(1, service.stops);
	}

	@Test
	void cleanupFailurePreservesTheStartupCause() {
		var service = new FailingService(true);
		var agent = new MasterAgent(new Config(), p -> 0, p -> 0, service);
		var failure = assertThrows(RuntimeException.class, agent::startAndWaitConnectionReady);
		assertSame(service.startFailure, failure.getCause());
		assertArrayEquals(new Throwable[]{service.stopFailure}, service.startFailure.getSuppressed());
		assertEquals(1, service.stops);
	}
}
