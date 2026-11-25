package Zeze.Net;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Util.Task;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * TcpSocket 异步建连失败回调与 accept/connect 内主动关闭的生命周期回归。
 */
@Fast
public class TestTcpSocketLifecycleCallbacks {

	@Test
	public void testDnsResolutionFailureInvokesConnectErrorAndClose() throws Exception {
		Task.tryInitThreadPool();
		var closeCount = new AtomicInteger();
		var connectErrorCount = new AtomicInteger();
		var closeCause = new AtomicReference<@Nullable Throwable>();
		var connectErrorCause = new AtomicReference<@Nullable Throwable>();
		var closeLatch = new CountDownLatch(1);
		var connectErrorLatch = new CountDownLatch(1);

		var service = new Service("test.tcp.dns.error") {
			@Override
			protected InetAddress resolveAddress(@Nullable String hostNameOrAddress) throws IOException {
				throw new UnknownHostException("simulated dns failure: " + hostNameOrAddress);
			}

			@Override
			public void OnSocketClose(AsyncSocket so, @Nullable Throwable e) throws Exception {
				closeCount.incrementAndGet();
				closeCause.set(e);
				closeLatch.countDown();
				super.OnSocketClose(so, e);
			}

			@Override
			public void OnSocketConnectError(AsyncSocket so, @Nullable Throwable e) throws Exception {
				connectErrorCount.incrementAndGet();
				connectErrorCause.set(e);
				connectErrorLatch.countDown();
				super.OnSocketConnectError(so, e);
			}
		};

		try {
			service.start();
			service.newClientSocket("no-such-host.invalid", 12345, null, null);

			Assertions.assertTrue(closeLatch.await(10, TimeUnit.SECONDS), "DNS失败须回调OnSocketClose");
			Assertions.assertTrue(connectErrorLatch.await(10, TimeUnit.SECONDS), "DNS失败须回调OnSocketConnectError");
			Assertions.assertEquals(1, closeCount.get());
			Assertions.assertEquals(1, connectErrorCount.get());
			Assertions.assertInstanceOf(UnknownHostException.class, closeCause.get());
			Assertions.assertInstanceOf(UnknownHostException.class, connectErrorCause.get());
		} finally {
			service.stop();
		}
	}

	@Test
	public void testConnectorStopDuringDnsDoesNotInvokeConnectError() throws Exception {
		Task.tryInitThreadPool();
		var dnsEntered = new CountDownLatch(1);
		var releaseDns = new CountDownLatch(1);
		var dnsExited = new CountDownLatch(1);
		var closeCount = new AtomicInteger();
		var connectErrorCount = new AtomicInteger();

		var service = new Service("test.tcp.dns.stop") {
			@Override
			protected InetAddress resolveAddress(@Nullable String hostNameOrAddress) throws IOException {
				dnsEntered.countDown();
				try {
					releaseDns.await(10, TimeUnit.SECONDS);
					throw new UnknownHostException("late dns failure after stop");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException(e);
				} finally {
					dnsExited.countDown();
				}
			}

			@Override
			public void OnSocketClose(AsyncSocket so, @Nullable Throwable e) throws Exception {
				closeCount.incrementAndGet();
				super.OnSocketClose(so, e);
			}

			@Override
			public void OnSocketConnectError(AsyncSocket so, @Nullable Throwable e) throws Exception {
				connectErrorCount.incrementAndGet();
				super.OnSocketConnectError(so, e);
			}
		};

		try {
			var connector = new Connector("late-dns.invalid", 12345, false);
			service.getConfig().addConnector(connector);
			service.start();

			Assertions.assertTrue(dnsEntered.await(10, TimeUnit.SECONDS), "DNS解析须已进入");
			connector.stop();
			releaseDns.countDown();
			Assertions.assertTrue(dnsExited.await(10, TimeUnit.SECONDS), "DNS解析须已退出");
			// 给dnsResolver线程留出执行catch分支的时间窗
			Thread.sleep(100);

			Assertions.assertEquals(1, closeCount.get(), "Connector.stop已关闭连接恰好一次");
			Assertions.assertEquals(0, connectErrorCount.get(), "已被stop关闭的连接不得再触发OnSocketConnectError");
		} finally {
			releaseDns.countDown();
			service.stop();
		}
	}

	@Test
	public void testCloseInOnSocketAcceptDoesNotTriggerAcceptError() throws Exception {
		Task.tryInitThreadPool();
		var acceptedLatch = new CountDownLatch(1);
		var acceptErrorCount = new AtomicInteger();
		var acceptErrorRef = new AtomicReference<@Nullable Throwable>();

		var service = new Service("test.tcp.accept.close") {
			@Override
			public void OnSocketAccept(AsyncSocket so) {
				so.close();
				acceptedLatch.countDown();
			}

			@Override
			public void OnSocketAcceptError(AsyncSocket listener, Throwable e) {
				acceptErrorCount.incrementAndGet();
				acceptErrorRef.set(e);
			}
		};

		TcpSocket listenerSocket = null;
		try {
			service.start();
			listenerSocket = (TcpSocket)service.newServerSocket("127.0.0.1", 0, null);
			int port = requireNonNull(listenerSocket.getLocalInet()).getPort();
			try (var client = new Socket("127.0.0.1", port)) {
				client.setSoTimeout(5_000);
				Assertions.assertTrue(acceptedLatch.await(10, TimeUnit.SECONDS), "OnSocketAccept须已触发");
				// 等待服务端关闭连接并对端观测到EOF，确保doHandle(OP_ACCEPT)执行完毕
				try {
					client.getInputStream().read();
				} catch (IOException ignored) {
				}
			}
			Thread.sleep(50);
			Assertions.assertEquals(0, acceptErrorCount.get(),
					"OnSocketAccept内正常关闭连接不得误触发OnSocketAcceptError: " + acceptErrorRef.get());
		} finally {
			if (listenerSocket != null)
				listenerSocket.close();
			service.stop();
		}
	}

	@Test
	public void testCloseInOnSocketConnectedDoesNotTriggerConnectError() throws Exception {
		Task.tryInitThreadPool();
		var connectedLatch = new CountDownLatch(1);
		var closeLatch = new CountDownLatch(1);
		var connectErrorCount = new AtomicInteger();
		var connectErrorRef = new AtomicReference<@Nullable Throwable>();

		var service = new Service("test.tcp.connected.close") {
			@Override
			public void OnSocketConnected(AsyncSocket so) {
				so.close();
				connectedLatch.countDown();
			}

			@Override
			public void OnSocketClose(AsyncSocket so, @Nullable Throwable e) throws Exception {
				closeLatch.countDown();
				super.OnSocketClose(so, e);
			}

			@Override
			public void OnSocketConnectError(AsyncSocket so, @Nullable Throwable e) {
				connectErrorCount.incrementAndGet();
				connectErrorRef.set(e);
			}
		};

		try (var server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
			service.start();
			service.newClientSocket("127.0.0.1", server.getLocalPort(), null, null);
			Assertions.assertTrue(connectedLatch.await(10, TimeUnit.SECONDS), "OnSocketConnected须已触发");
			Assertions.assertTrue(closeLatch.await(10, TimeUnit.SECONDS), "OnSocketClose须已触发");
			try (var accepted = server.accept()) {
				accepted.setSoTimeout(5_000);
				try {
					accepted.getInputStream().read();
				} catch (IOException ignored) {
				}
			}
			Thread.sleep(50);
			Assertions.assertEquals(0, connectErrorCount.get(),
					"OnSocketConnected内正常关闭连接不得误触发OnSocketConnectError: " + connectErrorRef.get());
		} finally {
			service.stop();
		}
	}
}
