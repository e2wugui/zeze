package Zeze.Services;

import java.util.concurrent.atomic.AtomicLong;
import Zeze.Services.ServiceManager.Id128UdpClient;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestId128UdpClientClosedAdmission {
	@Test
	public void stoppedClientRejectsAllocationBeforeCreatingAnRpc() throws Exception {
		var sessionIds = new AtomicLong();
		var client = new Id128UdpClient(null, "127.0.0.1", 1, sessionIds::incrementAndGet);
		client.stop();

		assertTrue(client.allocateFuture("closed-client", 16).isCompletedExceptionally());
		assertEquals(0, sessionIds.get());
	}
}
