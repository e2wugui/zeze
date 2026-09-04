package UnitTest.Zeze.Services;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import Zeze.Component.Threading;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Services.ServiceManager.BAllocateIdArgument;
import Zeze.Services.ServiceManager.BAllocateIdResult;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Services.ServiceManager.BSubscribeArgument;
import Zeze.Services.ServiceManager.BUnSubscribeArgument;
import Zeze.Services.ServiceManager.Id128UdpClient;
import Zeze.Services.ServiceManager.Id128UdpServer;
import harness.Fast;
import org.jetbrains.annotations.NotNull;

/**
 * FND2-S2-6：lastTid128CacheFuture 按globalName存取。原agent级单槽不区分名字：
 * 对A分配后再对B调getUsableTid128CacheFuture(B)会返回A的future（单槽最后写入者），
 * B的消费者从A的号段取号——服务端对B独立计数发段，两个客户端可在B空间拿到重叠tid（静默发错号）。
 * <p>
 * 真实loopback Id128UdpServer保证udp.send总是成功（future保持pending），但不启动
 * Id128UdpClient工作线程（应答无人处理），future完成状态完全由测试手动控制，确定性构造。
 */
@Fast
public class TestTid128CacheFuturePerName {
	private static final String NAME_A = "UnitTest.FND2_S2_6.PerName.A";
	private static final String NAME_B = "UnitTest.FND2_S2_6.PerName.B";
	private static Id128UdpServer server;
	private static TestAgent agent;

	@BeforeAll
	public static void setUp() throws Exception {
		server = new Id128UdpServer();
		server.start();
		agent = new TestAgent();
		agent.setTid128UdpClient(new Id128UdpClient(agent, "127.0.0.1", server.getLocalPort(),
				new AtomicLong()::incrementAndGet)); // 不调用start()：工作线程不启动，future保持pending。
	}

	@AfterAll
	public static void tearDown() throws Exception {
		agent.client.stop();
		server.stop();
	}

	// 不能把pending的future留给下一个测试：allocateTid128CacheFuture内部会get()等待上一次分配完成。
	@AfterEach
	public void completePendingFutures() {
		for (var name : new String[]{NAME_A, NAME_B}) {
			var last = agent.getLastTid128CacheFuture(name);
			if (last != null && !last.isDone())
				Assertions.assertTrue(last.setException(new TimeoutException("test cleanup")));
		}
	}

	@Test
	public void testPerNameIsolation() {
		var fa = agent.allocateTid128CacheFuture(NAME_A); // pending
		var fb = agent.allocateTid128CacheFuture(NAME_B);
		Assertions.assertNotSame(fa, fb);

		// 修复前单槽：getUsable(NAME_A)返回最后写入的B的future，跨名号段串用。
		Assertions.assertSame(fa, agent.getUsableTid128CacheFuture(NAME_A));
		Assertions.assertSame(fb, agent.getUsableTid128CacheFuture(NAME_B));
		Assertions.assertSame(fa, agent.getLastTid128CacheFuture(NAME_A));
		Assertions.assertSame(fb, agent.getLastTid128CacheFuture(NAME_B));
		// 无参兼容镜像：最近一次分配（NAME_B的fb）。
		Assertions.assertSame(fb, agent.getLastTid128CacheFuture());

		// A毒化只影响A：B的pending future原样返回（不触发多余分配、不被牵连）。
		Assertions.assertTrue(fa.setException(new TimeoutException("simulate udp timeout")));
		var fb2 = Assertions.assertDoesNotThrow(() -> agent.getUsableTid128CacheFuture(NAME_B));
		Assertions.assertSame(fb, fb2);
		// A按名字自愈重新分配，新的future仍是A名下的。
		var fa2 = Assertions.assertDoesNotThrow(() -> agent.getUsableTid128CacheFuture(NAME_A));
		Assertions.assertNotSame(fa, fa2);
		Assertions.assertSame(fa2, agent.getLastTid128CacheFuture(NAME_A));
		Assertions.assertSame(fa2, agent.getLastTid128CacheFuture());
	}

	/** 只实现被测路径需要的行为，其余入口不可用。 */
	private static final class TestAgent extends AbstractAgent {
		private Id128UdpClient client;

		void setTid128UdpClient(Id128UdpClient client) {
			this.client = client;
			this.tid128UdpClient = client; // protected字段，子类内可访问。
		}

		@Override
		protected void allocate(@NotNull AutoKey autoKey, int pool) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected boolean allocateAsync(@NotNull String globalName, int allocCount,
										@NotNull ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>> callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void start() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void waitReady() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void editService(@NotNull BEditService arg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull CompletableFuture<List<SubscribeState>> subscribeServicesAsync(@NotNull BSubscribeArgument info) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void unSubscribeService(@NotNull BUnSubscribeArgument arg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean setServerLoad(@NotNull BServerLoad load) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull Threading getThreading() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() {
			throw new UnsupportedOperationException();
		}
	}
}
