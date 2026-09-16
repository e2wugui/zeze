package UnitTest.Zeze.Component;

import harness.Fast;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Builtin.Threading.KeepAlive;
import Zeze.Builtin.Threading.ReadWriteLockOperate;
import Zeze.Builtin.Threading.SemaphoreRelease;
import Zeze.Builtin.Threading.SemaphoreTryAcquire;
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
 * VB①（FND7-64同型）回归：Threading.Semaphore.tryAcquire 与 ReadWriteLock.tryOperate
 * 客户端rpc超时（应答迟到/丢失）时服务端可能已发放许可/已enter读写锁。客户端进程活着时
 * keepAlive每10s刷新服务端activeTime，timeoutRelease永不触发——已获取的资源无人释放，
 * 无限期悬挂（信号量许可永久短缺/读写锁永久持有）。
 * 修复后：超时按未获取返回false，并对同lockName补发release/同模式exit（fire-and-forget）；
 * 服务端对无条目幂等应答0，未真获取时无害。
 */
@Fast
public class TestThreadingAcquireTimeoutCompensate {

	/** 模拟"应答迟到"的服务端：acquire/enter立即成功但延迟8秒才应答（客户端rpc超时≥5秒，
	 * 必迟到）；release/exit记录后立即幂等应答0。 */
	private static final class DelayedGrantServer extends Service {
		final ConcurrentLinkedQueue<Object[]> releasedNames = new ConcurrentLinkedQueue<>(); // [name, permits]
		final ConcurrentLinkedQueue<Object[]> exitedNames = new ConcurrentLinkedQueue<>(); // [name, operateType]

		DelayedGrantServer() {
			super("TestVb1DelaySrv");
			AddFactoryHandle(SemaphoreTryAcquire.TypeId_, new ProtocolFactoryHandle<>(SemaphoreTryAcquire::new, r -> {
				TaskSpec.ofAction(() -> r.SendResultCode(0)).scheduleNow(8000); // 发放但迟到
				return 0L;
			}, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(SemaphoreRelease.TypeId_, new ProtocolFactoryHandle<>(SemaphoreRelease::new, r -> {
				releasedNames.add(new Object[]{r.Argument.getLockName().getName(), r.Argument.getPermits()});
				r.SendResultCode(0); // 对齐ThreadingServer：无条目幂等应答0
				return 0L;
			}, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(ReadWriteLockOperate.TypeId_, new ProtocolFactoryHandle<>(ReadWriteLockOperate::new, r -> {
				var type = r.Argument.getOperateType();
				if (type == Threading.eEnterRead || type == Threading.eEnterWrite)
					TaskSpec.ofAction(() -> r.SendResultCode(0)).scheduleNow(8000); // enter成功但迟到
				else {
					exitedNames.add(new Object[]{r.Argument.getLockName().getName(), type});
					r.SendResultCode(0); // 对齐ThreadingServer：无条目幂等应答0
				}
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

	// 主路径1：tryAcquire超时后必须补发release（含permits），且按未获取（false）继续。
	// 修复前：超时以CompletionException呈现且无补偿，服务端已发放的许可永久短缺。
	@Test
	public void testSemaphoreTryAcquireTimeoutSendsCompensatingRelease() throws Exception {
		Task.tryInitThreadPool();
		var server = new DelayedGrantServer();
		int port = listenPort(server);
		var client = new Service("TestVb1DelayCli1");
		try {
			client.newClientSocket("127.0.0.1", port, null, null);
			await("client connected", 10_000, () -> client.GetSocket() != null);

			var threading = new Threading(client, 1);
			threading.RegisterProtocols(client);
			var semaphore = threading.openSemaphore("vb1.timeout.semaphore");
			try {
				boolean acquired;
				try {
					acquired = semaphore.tryAcquire(2, 0); // 客户端5秒rpc超时；服务端8秒后才应答"已发放"
				} catch (CompletionException e) {
					acquired = false; // 修复前：超时以异常呈现且无任何补偿
				}
				Assertions.assertFalse(acquired, "客户端rpc超时必须按未获取处理");

				await("compensating release", 5_000, () -> !server.releasedNames.isEmpty());
				var release = server.releasedNames.peek();
				Assertions.assertEquals("vb1.timeout.semaphore", release[0],
						"tryAcquire超时后必须对同lockName补发release");
				Assertions.assertEquals(2, release[1], "补发release必须携带获取的permits");
				Assertions.assertTrue(server.exitedNames.isEmpty(), "信号量路径不得产生rwlock exit");
			} finally {
				threading.close();
			}
		} finally {
			client.stop();
			server.stop();
		}
	}

	// 主路径2：tryEnterWrite超时后必须补发同模式exit（eExitWrite），且按未进入（false）继续。
	@Test
	public void testRwLockEnterTimeoutSendsCompensatingExit() throws Exception {
		Task.tryInitThreadPool();
		var server = new DelayedGrantServer();
		int port = listenPort(server);
		var client = new Service("TestVb1DelayCli2");
		try {
			client.newClientSocket("127.0.0.1", port, null, null);
			await("client connected", 10_000, () -> client.GetSocket() != null);

			var threading = new Threading(client, 2);
			threading.RegisterProtocols(client);
			var rwlock = threading.openReadWriteLock("vb1.timeout.rwlock");
			try {
				boolean entered;
				try {
					entered = rwlock.tryEnterWrite(0); // 客户端5秒rpc超时；服务端8秒后才应答"已进入"
				} catch (CompletionException e) {
					entered = false; // 修复前：超时以异常呈现且无任何补偿
				}
				Assertions.assertFalse(entered, "客户端rpc超时必须按未进入处理");

				await("compensating exit", 5_000, () -> !server.exitedNames.isEmpty());
				var exit = server.exitedNames.peek();
				Assertions.assertEquals("vb1.timeout.rwlock", exit[0],
						"tryEnterWrite超时后必须对同lockName补发exit");
				Assertions.assertEquals(Threading.eExitWrite, exit[1],
						"补发exit必须与进入模式对称（写进入→写退出）");
				Assertions.assertTrue(server.releasedNames.isEmpty(), "rwlock路径不得产生semaphore release");
			} finally {
				threading.close();
			}
		} finally {
			client.stop();
			server.stop();
		}
	}

	// 前提守卫（真实ThreadingServer）：补发release/exit可能落在"未真获取"上——对无条目
	// 必须幂等（应答0不报错），且多余释放后资源必须能被其他线程（不同globalThreadId=
	// 不同SimulateThread）立即获取，无残留占用。
	@Test
	public void testExtraReleaseAndExitIdempotentOnRealThreadingServer() throws Exception {
		Task.tryInitThreadPool();
		var serverService = new Service("TestVb1RealSrv");
		var threadingServer = new ThreadingServer(serverService, new ServiceManagerServer.Conf());
		threadingServer.RegisterProtocols(serverService);
		int port = listenPort(serverService);
		var client = new Service("TestVb1RealCli");
		try {
			client.newClientSocket("127.0.0.1", port, null, null);
			await("client connected", 10_000, () -> client.GetSocket() != null);

			var threading = new Threading(client, 3);
			threading.RegisterProtocols(client);
			try {
				// 信号量：acquire→release→多余release（幂等）→他人可再取
				var semaphore = threading.createSemaphore("vb1.idempotent.semaphore", 1);
				Assertions.assertTrue(semaphore.tryAcquire(1, 0), "空闲信号量tryAcquire必须成功");
				semaphore.release(1);
				semaphore.release(1); // 补发落在未持有上：必须幂等（应答0），不得报错/挂起

				var acquired = new AtomicBoolean(false);
				var other1 = new Thread(() -> {
					acquired.set(semaphore.tryAcquire(1, 0));
					if (acquired.get())
						semaphore.release(1);
				});
				other1.start();
				other1.join(10_000);
				Assertions.assertTrue(acquired.get(), "多余release后信号量必须可被其他线程立即获取（无残留占用）");

				// 读写锁：enterWrite→exitWrite→多余exitWrite（幂等）→他人可再进入
				var rwlock = threading.openReadWriteLock("vb1.idempotent.rwlock");
				Assertions.assertTrue(rwlock.tryEnterWrite(0), "空闲写锁tryEnterWrite必须成功");
				rwlock.exitWrite();
				rwlock.exitWrite(); // 补发落在未持有上：必须幂等（应答0），不得报错/挂起

				var entered = new AtomicBoolean(false);
				var other2 = new Thread(() -> {
					entered.set(rwlock.tryEnterWrite(0));
					if (entered.get())
						rwlock.exitWrite();
				});
				other2.start();
				other2.join(10_000);
				Assertions.assertTrue(entered.get(), "多余exitWrite后写锁必须可被其他线程立即获取（无残留持有）");
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
