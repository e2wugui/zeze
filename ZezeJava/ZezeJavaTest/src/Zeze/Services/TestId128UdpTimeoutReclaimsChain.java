package Zeze.Services;

import java.net.DatagramSocket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Services.ServiceManager.AllocateId128;
import Zeze.Services.ServiceManager.Id128UdpClient;
import Zeze.Util.LongConcurrentHashMap;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestId128UdpTimeoutReclaimsChain {
	private static Object field(Object instance, String name) throws Exception {
		var field = instance.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(instance);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void timeoutReclaimsCompletedNodesAndPreservesPendingNodes() throws Exception {
		// Bound socket accepts datagrams but sends no replies; no worker or wall-clock wait.
		try (var sink = new DatagramSocket(0)) {
			var client = new Id128UdpClient(null, "127.0.0.1", sink.getLocalPort(), new AtomicLong()::incrementAndGet);
			try {
				var first = client.allocateFuture("chain", 16);
				var middle = client.allocateFuture("chain", 16);
				var last = client.allocateFuture("chain", 16);
				var pending = (LongConcurrentHashMap<AllocateId128>)field(client, "pendingRpc");
				for (var it = pending.entryIterator(); it.moveToNext(); )
					it.value().setTimeout(it.value().getFutureNode() == middle ? 0 : 60_000);
				tick(client);
				assertTrue(middle.isCompletedExceptionally());
				assertFalse(first.isDone());
				assertFalse(last.isDone());
				var tails = (Map<String, Id128UdpClient.FutureNode>)field(client, "tailFuture");
				assertSame(last, tails.get("chain"));
				assertSame(first, field(last, "prev"));
				assertNull(field(middle, "prev"));
				for (var it = pending.entryIterator(); it.moveToNext(); )
					it.value().setTimeout(0);
				tick(client);
				assertTrue(first.isCompletedExceptionally());
				assertTrue(last.isCompletedExceptionally());
				assertTrue(tails.isEmpty());
				assertNull(field(last, "prev"));
			} finally {
				client.stop();
			}
		}
	}

	private static void tick(Id128UdpClient client) throws Exception {
		var time = Id128UdpClient.class.getDeclaredField("lastRpcTimeoutCheckTime");
		time.setAccessible(true);
		time.setLong(client, System.currentTimeMillis() - 2000);
		var tick = Id128UdpClient.class.getDeclaredMethod("processTick");
		tick.setAccessible(true);
		tick.invoke(client);
	}
}
