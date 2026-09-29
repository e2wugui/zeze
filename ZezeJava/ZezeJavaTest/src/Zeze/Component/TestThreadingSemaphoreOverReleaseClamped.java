package Zeze.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import Zeze.Component.Threading;
import Zeze.Component.ThreadingServer;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Services.ServiceManagerServer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * kimi-audit01 C-08回归：SemaphoreRelease不钳制超量释放，直接膨胀JDK信号量真实容量。
 * 服务端动作内无条件semaphore.release(permits)（permits只校验&gt;0）：持有1个许可时
 * release(100)，真实容量0→100（曾发放口径101），账面1-100=-99≤0条目被删，膨胀永久保留
 * 至服务重启——容量1的互斥/限流语义被静默击穿。
 * 修复：钳制为min(permits, 入账持有量)。测试：容量1信号量，acquire→超量release(100)→
 * 再acquire后，第二个线程必须取不到（修复前容量已膨胀，两个线程同时持有）。
 */
@Fast
public class TestThreadingSemaphoreOverReleaseClamped {

	@Test
	public void testOverReleaseDoesNotInflateRealCapacity() throws Exception {
		Task.tryInitThreadPool();
		var serverService = new Service("TestC08ClampSrv");
		var threadingServer = new ThreadingServer(serverService, new ServiceManagerServer.Conf());
		threadingServer.RegisterProtocols(serverService);
		int port = listenPort(serverService);
		var client = new Service("TestC08ClampCli");
		try {
			client.newClientSocket("127.0.0.1", port, null, null);
			await("client connected", 10_000, () -> client.GetSocket() != null);

			var threading = new Threading(client, 1);
			threading.RegisterProtocols(client);
			try {
				var semaphore = threading.createSemaphore("c8.clamp.semaphore", 1);
				// 占满容量1，随后超量释放100：服务端必须钳制为1，不得膨胀真实容量
				Assertions.assertTrue(semaphore.tryAcquire(1, 0));
				semaphore.release(100);

				// 钳制后容量必须恢复为恰好1：本次取走后容量归0
				Assertions.assertTrue(semaphore.tryAcquire(1, 0), "钳制后容量必须恢复为1");

				// 核心（红）：第二个线程不得再取到——修复前真实容量已膨胀到100
				var acquired = new AtomicBoolean(false);
				var other = new Thread(() -> acquired.set(semaphore.tryAcquire(1, 0)));
				other.start();
				other.join(10_000);
				Assertions.assertFalse(acquired.get(), "超量release不得膨胀信号量真实容量");

				// 释放自己持有的，恢复现场
				semaphore.release(1);
			} finally {
				threading.close();
			}
		} finally {
			client.stop();
			serverService.stop();
			threadingServer.close();
		}
	}

	private static int listenPort(Service service) throws Exception {
		var listener = (TcpSocket)service.newServerSocket(new InetSocketAddress("127.0.0.1", 0), null);
		var local = listener.getLocalInet();
		Assertions.assertNotNull(local);
		return local.getPort();
	}

	private static void await(String what, long timeoutMillis, java.util.function.BooleanSupplier cond)
			throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (!cond.getAsBoolean()) {
			if (System.currentTimeMillis() > deadline)
				Assertions.fail("timeout waiting: " + what);
			//noinspection BusyWait
			Thread.sleep(10);
		}
	}
}
