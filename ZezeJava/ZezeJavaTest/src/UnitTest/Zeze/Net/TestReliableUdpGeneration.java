package UnitTest.Zeze.Net;

import harness.Fast;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Net.ReliableUdp;
import Zeze.Net.ReliableUdpHandle;
import Zeze.Util.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 会话代际（FND3-26）：任一端进程重启后 serialId 空间错位。
 * ① 发送端重启：新会话序号从 1 起，接收端旧会话按重复包丢弃但照常回 Ack——前 lastDispatched 个包静默丢失。
 * ② 接收端重启：新会话 lastDispatched=0，发送端续高序号全部滞留 recvWindow，整条流永久停摆。
 * 修复：会话代际随包携带，全新代际重建会话状态。
 */
@Fast
public class TestReliableUdpGeneration {

	// 收到的数据与代际重置计数。handle 在线程池中执行，只做无锁收集。
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
			Thread.sleep(1);
		}
	}

	private static void awaitReceived(Collector c, int count) throws InterruptedException {
		await("received " + count + " packets, got " + c.received.size(), 10_000, () -> c.received.size() >= count);
	}

	// ① 发送端重启：同一 UDP 端口上重建会话（= 新进程代，序号从 1 重新开始），
	// 接收端必须重置接收状态并完整接收新代数据，而不是按重复包丢弃。
	@Test
	public void testSenderRestart() throws Exception {
		Task.tryInitThreadPool();
		var serverHandle = new Collector();
		var clientHandle = new Collector();
		var server = new ReliableUdp("127.0.0.1", 0, serverHandle);
		var client = new ReliableUdp("127.0.0.1", 0, clientHandle);
		try {
			var serverPort = server.getLocalInetAddress().getPort();

			var oldSession = client.open("127.0.0.1", serverPort, clientHandle);
			for (int i = 1; i <= 5; i++)
				oldSession.send(("old-" + i).getBytes(), 0, ("old-" + i).getBytes().length);
			awaitReceived(serverHandle, 5);

			// 模拟发送端进程重启：client 的 UDP 端口不变（对端会话 key 不变），
			// openReplace 重建会话 = 新代际 + 序号从 1 重新开始（open已改先建者胜，不再覆盖）。
			var newSession = client.openReplace("127.0.0.1", serverPort, clientHandle);
			for (int i = 1; i <= 5; i++)
				newSession.send(("new-" + i).getBytes(), 0, ("new-" + i).getBytes().length);
			awaitReceived(serverHandle, 10);

			var got = new HashSet<>(serverHandle.received);
			for (int i = 1; i <= 5; i++) {
				Assertions.assertTrue(got.contains("old-" + i), "missing old-" + i);
				Assertions.assertTrue(got.contains("new-" + i), "sender-restart packet new-" + i + " lost as duplicate");
			}
			Assertions.assertEquals(10, serverHandle.received.size(), "duplicate dispatch");
			// 接收端检测到对端换代（接收方向重置通知）
			await("server session reset", 10_000, () -> serverHandle.resets.get() >= 1);
		} finally {
			client.close();
			server.close();
		}
	}

	// ② 接收端重启：接收端丢失全部会话状态（进程内等价：清空会话表，端口不变）。
	// 发送端必须检测到对端换代：丢弃在途包（回调通知）并从 1 重新编号，
	// 后续数据必须恢复派发——而不是滞留接收窗口造成整条流永久停摆。
	@Test
	public void testReceiverRestart() throws Exception {
		Task.tryInitThreadPool();
		var serverHandle = new Collector();
		var clientHandle = new Collector();
		var server = new ReliableUdp("127.0.0.1", 0, serverHandle);
		var client = new ReliableUdp("127.0.0.1", 0, clientHandle);
		try {
			var session = client.open("127.0.0.1", server.getLocalInetAddress().getPort(), clientHandle);
			for (int i = 1; i <= 5; i++)
				session.send(("a-" + i).getBytes(), 0, ("a-" + i).getBytes().length);
			awaitReceived(serverHandle, 5);

			// 模拟接收端进程重启：全部会话关闭（新会话 lastDispatched=0、新代际），UDP 端口不变。
			for (var s : server.getSessions().values())
				s.close();

			// 旧实现：序号 6 滞留新会话 recvWindow 永不派发。
			// 新实现：第一个 Ack 携带新代际，发送端重整（在途包 b-1 被丢弃=设计语义）。
			session.send("b-1".getBytes(), 0, "b-1".getBytes().length);
			await("client rebase onSessionReset", 10_000, () -> clientHandle.resets.get() >= 1);

			// 重整完成后新数据必须恢复投递，不卡死。
			session.send("c-1".getBytes(), 0, "c-1".getBytes().length);
			await("server dispatch c-1 after receiver restart", 10_000,
					() -> serverHandle.received.contains("c-1"));

			var got = new HashSet<>(serverHandle.received);
			for (int i = 1; i <= 5; i++)
				Assertions.assertTrue(got.contains("a-" + i), "missing a-" + i);
			Assertions.assertFalse(got.contains("b-1"), "b-1 should be dropped on rebase (at-least-once semantics)");
			Assertions.assertEquals(6, serverHandle.received.size(), "unexpected packet set: " + serverHandle.received);
		} finally {
			client.close();
			server.close();
		}
	}

	// NoSession（对端 client 模式拒建会话）：发送端收到后应丢弃在途包并通知，
	// 且不再持有窗口永久重发。
	@Test
	public void testNoSessionDropsInFlight() throws Exception {
		Task.tryInitThreadPool();
		var refusingHandle = new Collector();
		var clientHandle = new Collector();
		var server = new ReliableUdp("127.0.0.1", 0, refusingHandle) {
			@Override
			protected Session dynamicCreateSession(SocketAddress source) {
				return null; // client 模式：拒绝对任何来源动态建会话
			}
		};
		var client = new ReliableUdp("127.0.0.1", 0, clientHandle);
		try {
			var session = client.open("127.0.0.1", server.getLocalInetAddress().getPort(), clientHandle);
			session.send("x".getBytes(), 0, 1);
			await("client onSessionReset after NoSession", 10_000, () -> clientHandle.resets.get() >= 1);
		} finally {
			client.close();
			server.close();
		}
	}

	// send 长度上限必须预留线上编码开销（type1+代际9+序号9+长度前缀≤5，最坏 24B）：
	// 接收端以 allocate(MaxPacketLength) 为接收缓冲，数据报超出部分被静默截断、
	// decode 按畸形包丢弃——send(MaxPacketLength) 恒发必坏包，入口应直接拒绝。
	@Test
	public void testSendLengthReservesWireOverhead() throws Exception {
		Task.tryInitThreadPool();
		var handle = new Collector();
		var a = new ReliableUdp("127.0.0.1", 0, handle);
		var b = new ReliableUdp("127.0.0.1", 0, handle);
		try {
			var session = a.open("127.0.0.1", b.getLocalInetAddress().getPort(), handle);
			var max = a.getMaxPacketLength();
			Assertions.assertThrows(IllegalArgumentException.class, () -> session.send(new byte[max], 0, max));
			session.send(new byte[max - 32], 0, max - 32); // 预留后边界内正常发送
		} finally {
			a.close();
			b.close();
		}
	}
}
