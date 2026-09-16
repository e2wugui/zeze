package UnitTest.Zeze.Component;

import harness.Fast;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Builtin.Threading.KeepAlive;
import Zeze.Builtin.Threading.MutexTryLock;
import Zeze.Builtin.Threading.MutexUnlock;
import Zeze.Component.Threading;
import Zeze.Component.ThreadingServer;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Services.ServiceManagerServer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-64回归：Threading.Mutex.tryLock客户端rpc超时（应答迟到/丢失）时服务端可能已授予，
 * 无人unlock。客户端进程活着时keepAlive每10s刷新服务端activeTime，timeoutRelease永不
 * 触发——授予的锁无限期悬挂（同globalThreadId重试还会holdCount累积）。
 * 修复后：tryLock超时按未获锁返回false，并对同lockName补发unlock（fire-and-forget）；
 * 服务端ProcessMutexUnlockRequest对无条目幂等应答0，未真获锁时无害。
 */
@Fast
public class TestFnd764ThreadingTryLockTimeoutCompensate {

	/** 模拟"应答迟到"的服务端：tryLock立即授予但延迟8秒才应答（客户端rpc超时≥5秒，必迟到）；
	 * unlock记录后立即幂等应答0。 */
	private static final class DelayedGrantServer extends Service {
		final ConcurrentLinkedQueue<String> unlockedNames = new ConcurrentLinkedQueue<>();

		DelayedGrantServer() {
			super("TestFnd764DelaySrv");
			AddFactoryHandle(MutexTryLock.TypeId_, new ProtocolFactoryHandle<>(MutexTryLock::new, r -> {
				TaskSpec.ofAction(() -> r.SendResultCode(0)).scheduleNow(8000); // 授予但迟到
				return 0L;
			}, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(MutexUnlock.TypeId_, new ProtocolFactoryHandle<>(MutexUnlock::new, r -> {
				unlockedNames.add(r.Argument.getLockName().getName());
				r.SendResultCode(0); // 对齐ThreadingServer：无条目幂等应答0
				return 0L;
			}, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(KeepAlive.TypeId_, new ProtocolFactoryHandle<>(KeepAlive::new, r -> 0L,
					TransactionLevel.None, DispatchMode.Direct));
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

	// 主路径：客户端rpc超时后必须补发unlock，且按未获锁（false）继续。
	// 修复前：超时以CompletionException呈现且无补偿，服务端已授予的锁无限期悬挂。
	@Test
	public void testTryLockTimeoutSendsCompensatingUnlock() throws Exception {
		Task.tryInitThreadPool();
		var server = new DelayedGrantServer();
		int port = listenPort(server);
		var client = new Service("TestFnd764DelayCli");
		try {
			client.newClientSocket("127.0.0.1", port, null, null);
			await("client connected", 10_000, () -> client.GetSocket() != null);

			var threading = new Threading(client, 1);
			threading.RegisterProtocols(client); // 对齐Agent：客户端也要注册工厂（应答按同TypeId解码派发）
			var mutex = threading.openMutex("fnd764.timeout.mutex");
			try {
				boolean locked;
				try {
					locked = mutex.tryLock(0); // 客户端5秒rpc超时；服务端8秒后才应答"已授予"
				} catch (CompletionException e) {
					locked = false; // 修复前：超时以异常呈现且无任何补偿
				}
				Assertions.assertFalse(locked, "客户端rpc超时必须按未获锁处理");

				await("compensating unlock", 5_000, () -> !server.unlockedNames.isEmpty());
				Assertions.assertEquals("fnd764.timeout.mutex", server.unlockedNames.peek(),
						"tryLock超时后必须对同lockName补发unlock");
			} finally {
				threading.close();
			}
		} finally {
			client.stop();
			server.stop();
		}
	}

	// 前提守卫（真实ThreadingServer）：补发unlock可能落在"未真获锁"上——unlock对无条目必须
	// 幂等（应答0不报错），且多余unlock后锁必须能被其他线程（不同globalThreadId=不同
	// SimulateThread，同名ReentrantLock真实竞争）立即获取，无残留持有。
	@Test
	public void testExtraUnlockIdempotentOnRealThreadingServer() throws Exception {
		Task.tryInitThreadPool();
		var serverService = new Service("TestFnd764RealSrv");
		var threadingServer = new ThreadingServer(serverService, new ServiceManagerServer.Conf());
		threadingServer.RegisterProtocols(serverService);
		int port = listenPort(serverService);
		var client = new Service("TestFnd764RealCli");
		try {
			client.newClientSocket("127.0.0.1", port, null, null);
			await("client connected", 10_000, () -> client.GetSocket() != null);

			var threading = new Threading(client, 2);
			threading.RegisterProtocols(client); // 对齐Agent：客户端也要注册工厂（应答按同TypeId解码派发）
			var mutex = threading.openMutex("fnd764.idempotent.mutex");
			try {
				Assertions.assertTrue(mutex.tryLock(0), "空闲锁tryLock必须成功");
				mutex.unlock();
				mutex.unlock(); // 补发落在未持有上：必须幂等（应答0），不得报错/挂起

				var acquired = new AtomicBoolean(false);
				var other = new Thread(() -> {
					acquired.set(mutex.tryLock(0));
					if (acquired.get())
						mutex.unlock();
				});
				other.start();
				other.join(10_000);
				Assertions.assertTrue(acquired.get(), "多余unlock后锁必须可被其他线程立即获取（无残留持有）");
			} finally {
				threading.close();
			}
		} finally {
			client.stop();
			serverService.stop();
			threadingServer.close();
		}
	}
}
