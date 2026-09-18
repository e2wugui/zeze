package UnitTest.Zeze.Net;

import harness.Fast;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Net.ReliableUdp;
import Zeze.Net.ReliableUdpHandle;
import Zeze.Util.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-45回归：Session构造曾用sessions.put无条件覆盖同地址旧会话——新会话接收状态
 * 被静默清零导致重复投递；被覆盖旧会话的Ack被新会话代际门拒收，重发定时器无人cancel
 * （孤儿定时器连ReliableUdp.close()都停不掉）。
 * 修复后：一个peer地址至多一个活会话（open先建者胜），openReplace显式重整并失效旧会话，
 * 会话驱逐一律Session.close()（置失效+取消重发定时器+摘表），getSessions()只读——
 * 表外带活定时器的会话从构造上不存在。
 */
@Fast
public class TestFnd845ReliableUdpSessionOverride {

	private static final class Collector implements ReliableUdpHandle {
		final ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();
		final AtomicInteger resets = new AtomicInteger();

		@Override
		public void handle(ReliableUdp.Session session, ReliableUdp.Packet packet) {
			received.add(new String(packet.bytes, packet.offset, packet.length));
		}

		@Override
		public void onSessionReset(ReliableUdp.Session session) {
			resets.incrementAndGet();
		}
	}

	private static void await(String what, int timeoutMillis, java.util.function.BooleanSupplier cond)
			throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (!cond.getAsBoolean()) {
			if (System.currentTimeMillis() > deadline)
				Assertions.fail("timeout waiting: " + what);
			//noinspection BusyWait
			Thread.sleep(5);
		}
	}

	// open()对既有会话（含动态建）先建者胜：不覆盖、handle以先建者为准（不被静默丢弃）、
	// 接收状态不清零（不重复投递、不触发重置）。
	@Test
	public void testOpenKeepsExistingSession() throws Exception {
		Task.tryInitThreadPool();
		var serverHandle = new Collector();
		var clientHandle = new Collector();
		var server = new ReliableUdp("127.0.0.1", 0, serverHandle);
		var client = new ReliableUdp("127.0.0.1", 0, clientHandle);
		try {
			var clientSession = client.open("127.0.0.1", server.getLocalInetAddress().getPort(), clientHandle);
			clientSession.send("p1".getBytes(), 0, 2);
			clientSession.send("p2".getBytes(), 0, 2);
			await("server received p2", 10_000,
					() -> serverHandle.received.contains("p1") && serverHandle.received.contains("p2"));

			// 包先到→server动态建会话；随后应用open同peer：先建者胜，不得覆盖
			var dynamic = server.getSessions().get(client.getLocalInetAddress());
			Assertions.assertNotNull(dynamic, "server dynamic session not created");
			var otherHandle = new Collector();
			var opened = server.open("127.0.0.1", client.getLocalInetAddress().getPort(), otherHandle);
			Assertions.assertSame(dynamic, opened, "open不得覆盖既有会话（先建者胜）");
			Assertions.assertSame(dynamic, server.getSessions().get(client.getLocalInetAddress()));

			// 覆盖前的接收状态必须保留：p3按原水位顺序派发，一次且仅一次，handle不被换掉
			clientSession.send("p3".getBytes(), 0, 2);
			await("server received p3", 10_000, () -> serverHandle.received.contains("p3"));
			Assertions.assertEquals(0, otherHandle.received.size(), "传入handle被静默换用");
			Assertions.assertEquals(0, serverHandle.resets.get(), "会话被覆盖式重建（接收状态被清零）");
			Assertions.assertEquals(3, serverHandle.received.size(), "duplicate dispatch: " + serverHandle.received);
		} finally {
			client.close();
			server.close();
		}
	}

	// openReplace显式重整：旧会话置失效（send返回false）且重发定时器被取消——
	// 对端（不回Ack保持定时器在途）一个重发周期内不得再收到旧会话的数据报。
	@Test
	public void testOpenReplaceInvalidatesOldAndStopsResend() throws Exception {
		Task.tryInitThreadPool();
		var client = new ReliableUdp("127.0.0.1", 0, new Collector());
		try (var peer = new DatagramSocket(new InetSocketAddress("127.0.0.1", 0))) {
			var old = client.open("127.0.0.1", peer.getLocalPort(), new Collector());
			Assertions.assertTrue(old.send("x".getBytes(), 0, 1));

			var buf = new byte[2048];
			var dg = new DatagramPacket(buf, buf.length);
			peer.setSoTimeout(10_000);
			peer.receive(dg); // 首发到达；不回Ack，旧会话的重发定时器保持活动

			var replaced = client.openReplace("127.0.0.1", peer.getLocalPort(), new Collector());
			Assertions.assertNotSame(old, replaced);
			Assertions.assertSame(replaced, client.getSessions().get(new InetSocketAddress("127.0.0.1",
					peer.getLocalPort())));
			Assertions.assertTrue(old.isClosed(), "被替换会话必须置失效");
			Assertions.assertFalse(old.send("y".getBytes(), 0, 1), "被替换会话send必须返回false");

			// 旧会话重发定时器已取消：超过一个重发周期（3s）不得再收到任何数据报
			peer.setSoTimeout(4_500);
			Assertions.assertThrows(java.net.SocketTimeoutException.class, () -> peer.receive(dg),
					"replaced session resent packet: resend timer not cancelled");
		} finally {
			client.close();
		}
	}

	// Session.close()必须：置失效（send返回false）、从sessions表摘除自己、取消重发定时器——
	// 对端（不回Ack保持定时器在途）超过一个重发周期不得再收到数据报。
	// （旧设计getSessions()暴露可变表：应用手工删表会造出表外孤儿，ReliableUdp.close()仅遍历
	//  表清扫不到其定时器，曾需要allSessions全量登记兜底；现表只读、驱逐一律走Session.close()，
	//  表外带活定时器的会话从构造上不存在。）
	@Test
	public void testSessionCloseStopsResendAndRemoves() throws Exception {
		Task.tryInitThreadPool();
		var client = new ReliableUdp("127.0.0.1", 0, new Collector());
		try (var peer = new DatagramSocket(new InetSocketAddress("127.0.0.1", 0))) {
			Assertions.assertThrows(UnsupportedOperationException.class, () -> client.getSessions().clear(),
					"sessions表必须只读（驱逐走Session.close()）");

			var session = client.open("127.0.0.1", peer.getLocalPort(), new Collector());
			Assertions.assertTrue(session.send("x".getBytes(), 0, 1));

			var buf = new byte[2048];
			var dg = new DatagramPacket(buf, buf.length);
			peer.setSoTimeout(10_000);
			peer.receive(dg); // 首发到达；不回Ack，重发定时器保持活动

			// 驱逐一律走Session.close()：置失效+取消重发定时器+从表摘除
			session.close();

			Assertions.assertTrue(session.isClosed(), "close必须置失效");
			Assertions.assertFalse(session.send("z".getBytes(), 0, 1), "close后send必须返回false");
			Assertions.assertNull(client.getSessions().get(new InetSocketAddress("127.0.0.1",
					peer.getLocalPort())), "close必须从表摘除自己");

			// 重发定时器已取消：超过一个重发周期（3s）不得再收到任何数据报
			peer.setSoTimeout(4_500);
			Assertions.assertThrows(java.net.SocketTimeoutException.class, () -> peer.receive(dg),
					"closed session resent packet: resend timer not cancelled");
		}
	}
}
