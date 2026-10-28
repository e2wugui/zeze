package Zeze.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Threading.KeepAlive;
import Zeze.Builtin.Threading.SemaphoreRelease;
import Zeze.Builtin.Threading.SemaphoreTryAcquire;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestThreadingSemaphoreServiceDomain {
	private static final String NAME = "semaphore-service-domain";

	private static Service server() {
		var server = new Service("SemaphoreServiceDomainServer");
		server.AddFactoryHandle(SemaphoreTryAcquire.TypeId_, new Service.ProtocolFactoryHandle<>(
				SemaphoreTryAcquire::new, rpc -> { rpc.SendResultCode(0); return 0; },
				TransactionLevel.None, DispatchMode.Direct));
		server.AddFactoryHandle(SemaphoreRelease.TypeId_, new Service.ProtocolFactoryHandle<>(
				SemaphoreRelease::new, rpc -> { rpc.SendResultCode(0); return 0; },
				TransactionLevel.None, DispatchMode.Direct));
		server.AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(
				KeepAlive::new, rpc -> 0, TransactionLevel.None, DispatchMode.Direct));
		return server;
	}

	private static Service client(Service server) throws Exception {
		var listener = (TcpSocket)server.newServerSocket(new InetSocketAddress("127.0.0.1", 0), null);
		int port = listener.getLocalInet().getPort();
		return client(port);
	}

	private static Service client(int port) throws Exception {
		var client = new Service("SemaphoreServiceDomainClient");
		client.getConfig().addConnector(new Connector("127.0.0.1", port, false));
		client.newClientSocket("127.0.0.1", port, null, null);
		long deadline = System.nanoTime() + 10_000_000_000L;
		while (client.GetSocket() == null) {
			assertTrue(System.nanoTime() < deadline);
			Thread.sleep(10);
		}
		return client;
	}

	@Test
	void remoteServicesHaveIndependentHoldsAndLocalWrappersShare() throws Exception {
		Task.tryInitThreadPool();
		var firstServer = server();
		var secondServer = server();
		var firstClient = client(firstServer);
		var secondClient = client(secondServer);
		var firstPort = new int[1];
		firstClient.getConfig().forEachConnector(connector -> firstPort[0] = connector.getPort());
		var aliasClient = client(firstPort[0]);
		var first = new Threading(firstClient, 17);
		var sameService = new Threading(aliasClient, 17);
		var second = new Threading(secondClient, 17);
		first.RegisterProtocols(firstClient);
		sameService.RegisterProtocols(aliasClient);
		second.RegisterProtocols(secondClient);
		var a = first.openSemaphore(NAME);
		var alias = sameService.openSemaphore(NAME);
		var b = second.openSemaphore(NAME);
		try {
			assertTrue(a.tryAcquire(2, 0));
			assertTrue(b.tryAcquire(3, 0));
			assertEquals(List.of(2, 3), holds(), "两个远端服务的相同线程/名字不能合并本地计数");
			assertTrue(alias.tryAcquire(1, 0));
			assertEquals(List.of(3, 3), holds(), "同Service包装必须共享持有计数");
			alias.release(1);
			a.release(2);
			assertEquals(List.of(3), holds(), "释放只影响对应远端域");
			b.release(3);
			assertTrue(holds().isEmpty());
		} finally {
			first.close();
			sameService.close();
			second.close();
			firstClient.stop();
			aliasClient.stop();
			secondClient.stop();
			firstServer.stop();
			secondServer.stop();
		}
	}

	@SuppressWarnings("unchecked")
	private static List<Integer> holds() throws Exception {
		var field = Threading.class.getDeclaredField("semaphoreLocalHolds");
		field.setAccessible(true);
		var map = (Map<?, AtomicInteger>)field.get(null);
		return map.entrySet().stream().filter(e -> e.getKey().toString().contains(NAME))
				.map(e -> e.getValue().get()).sorted().toList();
	}
}
