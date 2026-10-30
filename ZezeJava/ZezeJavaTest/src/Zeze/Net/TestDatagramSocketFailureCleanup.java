package Zeze.Net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import Zeze.Util.ReplayAttackPolicy;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestDatagramSocketFailureCleanup {
	@Test
	void receiveFailureClosesTheChannelAndItsSessions() throws Exception {
		var failed = new CountDownLatch(1);
		var closed = new CountDownLatch(1);
		var disposed = new CountDownLatch(1);
		var selectors = new Selectors("DatagramFailureCleanup", 1);
		var service = new Service("DatagramFailureCleanup") {
			@Override
			public void OnSocketClose(AsyncSocket so, @Nullable Throwable cause) throws Exception {
				super.OnSocketClose(so, cause);
				closed.countDown();
			}

			@Override
			public void OnSocketDisposed(AsyncSocket so) throws Exception {
				super.OnSocketDisposed(so);
				disposed.countDown();
			}
		};
		service.setSelectors(selectors);
		try (var sender = new java.net.DatagramSocket(new InetSocketAddress("127.0.0.1", 0));
				var socket = new DatagramSocket(service, new InetSocketAddress("127.0.0.1", 0)) {
					@Override
					public void doHandle(SelectionKey key) throws IOException {
						failed.countDown();
						throw new IOException("controlled receive failure");
					}
				}) {
			var session = socket.createSession((InetSocketAddress)sender.getLocalSocketAddress(),
					17, null, ReplayAttackPolicy.IncreasingOnly);
			assertNotNull(session);
			sender.send(new DatagramPacket(new byte[1], 1, socket.getLocal()));
			assertTrue(failed.await(5, TimeUnit.SECONDS), "真实可读事件须到达错误处理路径");
			assertTrue(closed.await(5, TimeUnit.SECONDS), "通道级接收失败必须级联关闭会话");
			assertTrue(disposed.await(5, TimeUnit.SECONDS));
			assertTrue(session.isClosed());
			assertFalse(socket.containsSession(session));
			assertNull(socket.getLocal());
			assertThrows(IllegalStateException.class, () -> socket.createSessionServer(
					new InetSocketAddress("127.0.0.1", 1), null, ReplayAttackPolicy.IncreasingOnly));
		} finally {
			service.stop();
			selectors.close();
		}
	}
}
