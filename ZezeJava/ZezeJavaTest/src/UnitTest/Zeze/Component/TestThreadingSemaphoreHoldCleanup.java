package UnitTest.Zeze.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Threading.KeepAlive;
import Zeze.Builtin.Threading.SemaphoreRelease;
import Zeze.Builtin.Threading.SemaphoreTryAcquire;
import Zeze.Component.Threading;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;

/**
 * CP1-F6 回归（P3）：semaphoreLocalHolds（静态map）原先只增不减——acquire成功
 * computeIfAbsent+addAndGet，release仅把计数夹到0不删条目——动态/虚拟线程场景按
 * (serverId,threadId,name)无界滞留。
 * 修复：release归零后semaphoreLocalHolds.remove(key, holder)（两参条件删）。
 * 测试：真实Threading客户端+最小模拟服务端（acquire/release立即成功应答），断言
 * acquire后计数条目存在、release后条目被删除（修复前：值0的条目永久滞留）。
 */
@Fast
public class TestThreadingSemaphoreHoldCleanup {

	/** 立即成功应答的最小服务端：acquire应答0，release记录后幂等应答0。 */
	private static final class GrantServer extends Service {
		GrantServer() {
			super("TestCp1f6GrantSrv");
			AddFactoryHandle(SemaphoreTryAcquire.TypeId_, new ProtocolFactoryHandle<>(SemaphoreTryAcquire::new,
					r -> {
						r.SendResultCode(0);
						return 0L;
					}, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(SemaphoreRelease.TypeId_, new ProtocolFactoryHandle<>(SemaphoreRelease::new,
					r -> {
						r.SendResultCode(0);
						return 0L;
					}, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(KeepAlive.TypeId_, new ProtocolFactoryHandle<>(KeepAlive::new,
					r -> 0L, TransactionLevel.None, DispatchMode.Direct));
		}
	}

	private static int listenPort(Service service) throws Exception {
		var listener = (TcpSocket)service.newServerSocket(new InetSocketAddress("127.0.0.1", 0), null);
		var local = listener.getLocalInet();
		assertTrue(local != null);
		return local.getPort();
	}

	@SuppressWarnings("unchecked")
	private static int holdEntryCount() throws Exception {
		Field field = Threading.class.getDeclaredField("semaphoreLocalHolds");
		field.setAccessible(true);
		var map = (ConcurrentHashMap<String, AtomicInteger>)field.get(null);
		int nonzero = 0;
		for (Map.Entry<String, AtomicInteger> e : map.entrySet())
			if (e.getKey().contains("cp1f6.hold.semaphore"))
				nonzero++;
		return nonzero;
	}

	private static int holdCount(String namePart) throws Exception {
		Field field = Threading.class.getDeclaredField("semaphoreLocalHolds");
		field.setAccessible(true);
		var map = (ConcurrentHashMap<String, AtomicInteger>)field.get(null);
		for (Map.Entry<String, AtomicInteger> e : map.entrySet())
			if (e.getKey().contains(namePart))
				return e.getValue().get();
		return -1;
	}

	@Test
	public void testHoldEntryRemovedAfterReleaseToZero() throws Exception {
		Task.tryInitThreadPool();
		var before = holdEntryCount();
		var server = new GrantServer();
		int port = listenPort(server);
		var client = new Service("TestCp1f6Cli");
		try {
			client.newClientSocket("127.0.0.1", port, null, null);
			var threading = new Threading(client, 3);
			threading.RegisterProtocols(client);
			var semaphore = threading.openSemaphore("cp1f6.hold.semaphore");
			try {
				assertTrue(semaphore.tryAcquire(2, 5_000), "模拟服务端总是授予");
				assertEquals(2, holdCount("cp1f6.hold.semaphore"), "acquire成功后本地持有计数必须登记");
				assertEquals(before + 1, holdEntryCount(), "acquire后新增恰好一个条目");

				semaphore.release(2);
				// 核心（红断言）：归零后条目必须删除——修复前值0条目永久滞留（无界积累）
				assertEquals(-1, holdCount("cp1f6.hold.semaphore"),
						"release归零后持有条目必须删除（修复前静态map只增不减）");
				assertEquals(before, holdEntryCount(), "release后不得残留本信号量的条目");
			} finally {
				threading.close();
			}
		} finally {
			client.stop();
			server.stop();
		}
	}
}
