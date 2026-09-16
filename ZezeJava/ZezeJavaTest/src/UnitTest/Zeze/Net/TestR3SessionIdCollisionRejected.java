package UnitTest.Zeze.Net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * R3-C②回归（SM撞号产品缺陷，TestGameTimer根因链）：同JVM多App各自调用全局静态
 * AsyncSocket.setSessionIdGenFunc（Game.App/linkd等用PersistentAtomicLong持久发号）——
 * 后装的发号器整体替换全局函数，与本Service既有连接发出的sessionId重叠时，
 * addSocket的putIfAbsent静默吞掉新连接：GetSocket(sessionId)永远返回旧socket，
 * isSenderAlive等按id找连接的逻辑全部被误导（TestGameTimer的ErrorNotLogin即此链）。
 * 修复：addSocket互撞时error日志+关闭撞号的新连接（先注册者胜），默认OnSocketAccept/
 * OnSocketConnected拿到false不再回调OnHandshakeDone。
 * 测试用固定发号器（() -> 42L）让同一Service的两条真实accepted连接同号：
 * 第一条正常登记；第二条必须被服务端关闭（客户端读到EOF），先注册者保留。
 * 修复前红：撞号连接静默吞掉后仍保持打开，客户端read等不到EOF直到超时。
 * 修改全局静态发号器，@Isolated独占JVM运行（对齐af52ff103判例）。
 */
@Fast
@Isolated
public class TestR3SessionIdCollisionRejected {
	@Test
	public void testDuplicateSessionIdSecondSocketRejectedAndClosed() throws Exception {
		Task.tryInitThreadPool();
		var server = new Service("TestR3SidCollide", new Config());
		try {
			int port;
			try (var ss = new ServerSocket()) {
				ss.bind(new InetSocketAddress("127.0.0.1", 0));
				port = ss.getLocalPort();
			}
			var listener = (TcpSocket)server.newServerSocket(new InetSocketAddress("127.0.0.1", port), null);
			try {
				// 固定发号（实例级，FND7-19/R3发号下沉后的正确用法）：该Service此后构造的
				// AsyncSocket的sessionId恒为42——构造两条accepted连接必互撞。
				server.setSessionIdGenerator(() -> 42L);
				try (var first = new Socket("127.0.0.1", port)) {
					assertTrue(waitSocketCount(server, 1, 10),
							"第一条连接必须正常登记（先注册者）");
					assertEquals(1, server.getSocketCount());

					try (var second = new Socket("127.0.0.1", port)) {
						second.setSoTimeout(10_000);
						// 撞号连接必须被服务端显式关闭：客户端读到EOF（修复前：静默吞掉，连接保持打开，read超时假红。
						int b = second.getInputStream().read();
						assertTrue(b == -1, "撞号连接必须被服务端关闭（读到EOF），实际读到: " + b);

						// 先注册者保留：登记数不因撞号回落为0，也不因吞掉第二条而变成2。
						assertTrue(waitSocketCount(server, 1, 10),
								"先注册的连接必须保留（GetSocket语义不被污染）");
					}
				}
			} finally {
				server.setSessionIdGenerator(null); // 恢复默认共享随机基址发号
				listener.close();
			}
		} finally {
			server.stop();
		}
	}

	private static boolean waitSocketCount(Service server, int expected, int timeoutSeconds)
			throws InterruptedException {
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
		while (System.nanoTime() < deadline) {
			if (server.getSocketCount() == expected)
				return true;
			Thread.sleep(50);
		}
		return server.getSocketCount() == expected;
	}
}
