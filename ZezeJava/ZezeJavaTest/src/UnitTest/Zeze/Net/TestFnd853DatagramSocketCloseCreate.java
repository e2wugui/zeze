package UnitTest.Zeze.Net;

import harness.Fast;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Net.AsyncSocket;
import Zeze.Net.DatagramSession;
import Zeze.Net.DatagramSocket;
import Zeze.Net.Service;
import Zeze.Util.ReplayAttackPolicy;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-53回归：DatagramSocket.close()对tokens的清理是弱一致快照+逐个close，且无终态
 * 标志——快照后新建的会话不在任何关闭路径上（OnSocketClose永不触发、isClosed恒false
 * 活性误报）；close后createSession仍成功，二次close因幂等门提前返回连补救机会也没有。
 * 修复后：createSession/createSessionServer与close双侧同锁检查终态（selectionKey锁内置空
  * 为标记），已关抛IllegalStateException——锁内互斥下单次快照即充分。
 */
@Fast
public class TestFnd853DatagramSocketCloseCreate {

	private static final class CloseRecordService extends Service {
		final List<DatagramSession> closedSessions = new CopyOnWriteArrayList<>();

		CloseRecordService(String name) {
			super(name);
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			if (so instanceof DatagramSession ds)
				closedSessions.add(ds);
			super.OnSocketClose(so, e);
		}
	}

	// close后新建会话必须被拒（ISE），不得产生永不被关闭的漏网会话
	@Test
	public void testCreateAfterCloseRejected() throws Exception {
		Task.tryInitThreadPool();
		var service = new CloseRecordService("test.fnd853.a");
		try {
			var socket = service.bindUdp(new InetSocketAddress(0));
			var pre = socket.createSessionServer(
					new InetSocketAddress("127.0.0.1", 1), null, ReplayAttackPolicy.AllowDisorder);
			socket.close();
			Assertions.assertTrue(pre.isClosed(), "既有会话被级联关闭");

			Assertions.assertThrows(IllegalStateException.class, () -> socket.createSession(
					new InetSocketAddress("127.0.0.1", 1), 42, null, ReplayAttackPolicy.AllowDisorder),
					"close后createSession必须拒绝");
			Assertions.assertThrows(IllegalStateException.class, () -> socket.createSessionServer(
					new InetSocketAddress("127.0.0.1", 1), null, ReplayAttackPolicy.AllowDisorder),
					"close后createSessionServer必须拒绝");
		} finally {
			service.Stop();
		}
	}

	// 并发交错（close与createSession同时进行）：锁内互斥保证结果二选一——要么会话建立并被级联
	// 关闭（closedSessions收录），要么建立被拒（ISE）——绝不允许"建立成功但永不被关闭"的漏网态
	@Test
	public void testConcurrentCreateCloseNoLeakedSession() throws Exception {
		Task.tryInitThreadPool();
		var service = new CloseRecordService("test.fnd853.b");
		try {
			for (int round = 0; round < 200; round++) {
				final var tokenId = 1000 + round;
				var socket = service.bindUdp(new InetSocketAddress(0));
				var barrier = new CountDownLatch(1);
				var created = new AtomicReference<DatagramSession>();
				var failure = new AtomicReference<IllegalStateException>();
				var creator = new Thread(() -> {
					try {
						barrier.await();
						created.set(socket.createSession(
								new InetSocketAddress("127.0.0.1", 1), tokenId, null,
								ReplayAttackPolicy.AllowDisorder));
					} catch (IllegalStateException e) {
						failure.set(e); // close先置终态：合法拒绝
					} catch (InterruptedException ignored) {
					}
				}, "fnd853-creator");
				creator.start();
				barrier.countDown();
				socket.close(); // 与createSession真并发
				creator.join(10_000);
				Assertions.assertFalse(creator.isAlive());

				var session = created.get();
				if (session != null) {
					// 建立成功（快照可见）：必须被close级联关闭，OnSocketClose恰好触发一次
					Assertions.assertTrue(session.isClosed(), "round=" + round + "：建立的会话必须被级联关闭");
					Assertions.assertTrue(service.closedSessions.contains(session),
							"round=" + round + "：会话的OnSocketClose必须触发");
					Assertions.assertFalse(socket.containsSession(session), "round=" + round + "：tokens须自清");
				} else {
					Assertions.assertNotNull(failure.get(), "round=" + round + "：未建立必须因终态被拒（ISE）");
				}
				service.closedSessions.clear();
			}
		} finally {
			service.Stop();
		}
	}
}
