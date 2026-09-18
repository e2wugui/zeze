package UnitTest.Zeze.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Builtin.Threading.KeepAlive;
import Zeze.Builtin.Threading.SemaphoreRelease;
import Zeze.Builtin.Threading.SemaphoreTryAcquire;
import Zeze.Component.Threading;
import Zeze.Component.ThreadingServer;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Services.ServiceManagerServer;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.Task;
import Zeze.Transaction.TransactionLevel;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-73回归：tryAcquire超时补偿曾无条件发送SemaphoreRelease——信号量持有者无获取
 * 优先权，"持有中再获取"会真实失败（应答2），其迟到应答触发客户端rpc超时后，补偿误释放
 * 先前持有的许可：服务端记账归零、真实余量虚增，容量1的信号量被双持有，互斥契约破坏。
 * 修复（不动协议的热修形态）：本地按(serverId,threadId,name)集中持有计数门控——
 * 成功应答加、显式release成功减；计数==0（服务端条目至多来自本次未知获取）才补发，
 * 计数>0放弃补偿并告警（宁漏勿误：悬挂许可交给timeoutRelease清算）。
 * 护栏：release归零后超时仍补发（FND7-64主场景不回归）、真实ThreadingServer常规
 * 往返语义不变。
 */
@Fast
public class TestFnd873SemaphoreCompensateGuard {

	/**
	 * 场景服务端：第N次acquire的应答模式可编程（立即/迟到），release记录并幂等应答0。
	 * 迟到应答用8秒（客户端rpc超时=max(timeoutMs+1000,5000)≥5秒，必超时先走）。
	 */
	private static final class ScriptedSemaphoreServer extends Service {
		final ConcurrentLinkedQueue<Object[]> releasedNames = new ConcurrentLinkedQueue<>(); // [name, permits]
		final CountDownLatch lateAnswerSent = new CountDownLatch(1);

		ScriptedSemaphoreServer(String name, int lateAcquireAnswer) {
			super(name);
			var acquireCount = new AtomicInteger();
			AddFactoryHandle(SemaphoreTryAcquire.TypeId_, new ProtocolFactoryHandle<>(SemaphoreTryAcquire::new, r -> {
				switch (acquireCount.incrementAndGet()) {
				case 1 -> r.SendResultCode(0); // 首次获取立即成功
				default -> Zeze.Util.TaskSpec.ofAction(() -> { // 后续获取迟到应答（成功0或失败2）
					r.SendResultCode(lateAcquireAnswer);
					lateAnswerSent.countDown();
				}).scheduleNow(8000);
				}
				return 0L;
			}, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(SemaphoreRelease.TypeId_, new ProtocolFactoryHandle<>(SemaphoreRelease::new, r -> {
				releasedNames.add(new Object[]{r.Argument.getLockName().getName(), r.Argument.getPermits()});
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

	/**
	 * 主回归：同线程持有1个许可后再次tryAcquire超时（服务端未发放、应答2迟到），
	 * 不得补发release——否则误释放先前持有的许可（容量虚增）。
	 */
	@Test
	public void testHeldPermitsNotReleasedByCompensation() throws Exception {
		Task.tryInitThreadPool();
		var server = new ScriptedSemaphoreServer("TestFnd873GuardSrv1", 2); // 迟到应答=未发放
		int port = listenPort(server);
		var client = new Service("TestFnd873GuardCli1");
		try {
			client.newClientSocket("127.0.0.1", port, null, null);
			await("client connected", 10_000, () -> client.GetSocket() != null);

			var threading = new Threading(client, 61);
			threading.RegisterProtocols(client);
			var semaphore = threading.openSemaphore("a6.fnd873.hold");
			try {
				Assertions.assertTrue(semaphore.tryAcquire(1, 0), "首次获取（立即成功应答）必须成功");
				Assertions.assertFalse(semaphore.tryAcquire(1, 0), "迟到未发放应答下客户端按未获取处理");
				// 等到迟到应答发出（8s）即证明5s超时路径已执行完毕——期间不得出现任何补偿release。
				Assertions.assertTrue(server.lateAnswerSent.await(10_000, java.util.concurrent.TimeUnit.MILLISECONDS),
						"迟到应答必须发出（超时路径已走完的确定性界标）");
				Assertions.assertTrue(server.releasedNames.isEmpty(),
						"持有中的超时补偿必须被本地计数门控拦下，不得误释放先前许可");

				semaphore.release(1); // 显式释放才是合法路径
				await("explicit release", 5_000, () -> !server.releasedNames.isEmpty());
				Assertions.assertEquals(1, server.releasedNames.peek()[1], "显式release必须携带permits");
			} finally {
				threading.close();
			}
		} finally {
			client.stop();
			server.stop();
		}
	}

	/**
	 * 护栏：release成功后本地计数归零，此后超时（服务端可能已发放、应答0迟到）仍必须补发
	 * ——FND7-64主场景（已发放未应答的悬挂）不被门控误伤。
	 */
	@Test
	public void testZeroHoldStillCompensatesAfterRelease() throws Exception {
		Task.tryInitThreadPool();
		var server = new ScriptedSemaphoreServer("TestFnd873GuardSrv2", 0); // 迟到应答=已发放
		int port = listenPort(server);
		var client = new Service("TestFnd873GuardCli2");
		try {
			client.newClientSocket("127.0.0.1", port, null, null);
			await("client connected", 10_000, () -> client.GetSocket() != null);

			var threading = new Threading(client, 62);
			threading.RegisterProtocols(client);
			var semaphore = threading.openSemaphore("a6.fnd873.zero");
			try {
				Assertions.assertTrue(semaphore.tryAcquire(1, 0), "首次获取（立即成功应答）必须成功");
				semaphore.release(1); // 本地计数归零
				await("explicit release", 5_000, () -> !server.releasedNames.isEmpty());
				server.releasedNames.clear();

				Assertions.assertFalse(semaphore.tryAcquire(2, 0), "迟到应答下客户端按未获取处理");
				await("compensating release", 6_500, () -> !server.releasedNames.isEmpty());
				var release = server.releasedNames.peek();
				Assertions.assertEquals("a6.fnd873.zero", release[0], "计数==0时超时必须补发release（FND7-64）");
				Assertions.assertEquals(2, release[1], "补发必须携带本次获取的permits");
			} finally {
				threading.close();
			}
		} finally {
			client.stop();
			server.stop();
		}
	}

	/**
	 * 护栏：真实ThreadingServer下常规获取/释放/跨线程互斥语义不受本地计数影响——
	 * 他人（不同SimulateThread）在持有满时正常失败（未超时、无补偿），释放后可再取。
	 */
	@Test
	public void testRealServerAcquireReleaseSemanticsUnchanged() throws Exception {
		Task.tryInitThreadPool();
		var serverService = new Service("TestFnd873RealSrv");
		var threadingServer = new ThreadingServer(serverService, new ServiceManagerServer.Conf());
		threadingServer.RegisterProtocols(serverService);
		int port = listenPort(serverService);
		var client = new Service("TestFnd873RealCli");
		try {
			client.newClientSocket("127.0.0.1", port, null, null);
			await("client connected", 10_000, () -> client.GetSocket() != null);

			var threading = new Threading(client, 63);
			threading.RegisterProtocols(client);
			try {
				var semaphore = threading.createSemaphore("a6.fnd873.real", 1);
				Assertions.assertTrue(semaphore.tryAcquire(1, 0), "空闲信号量tryAcquire必须成功");

				var acquired = new boolean[1];
				var other = new Thread(() -> acquired[0] = semaphore.tryAcquire(1, 100)); // 他人正常失败路径
				other.start();
				other.join(10_000);
				Assertions.assertFalse(acquired[0], "容量耗尽时他人tryAcquire必须失败（互斥语义）");

				semaphore.release(1);
				var reacquired = new boolean[1];
				var other2 = new Thread(() -> {
					reacquired[0] = semaphore.tryAcquire(1, 0);
					if (reacquired[0])
						semaphore.release(1);
				});
				other2.start();
				other2.join(10_000);
				Assertions.assertTrue(reacquired[0], "显式释放后他人必须可再获取（无残留占用）");
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
