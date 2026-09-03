package UnitTest.Zeze.Services;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Assertions;
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
import Zeze.Services.ServiceManagerAgentWithRaft;
import harness.Fast;
import org.jetbrains.annotations.NotNull;

/**
 * FND-S2-2：History + raft版ServiceManager组合下tid128UdpClient恒为null
 * （ServiceManagerAgentWithRaft构造器todo未初始化），冷写事务_check_预热、finalCommit
 * （经getUsableTid128CacheFuture）、Tid128Cache.next三个入口全部裸NPE，finalCommit路径
 * 还会halt(543543)，无任何防呆。
 * <p>
 * 修复：(1) 组合在构造器即fail-fast报错（明确信息，而非运行期NPE/halt）；
 * (2) allocateTid128CacheFuture对null的tid128UdpClient抛带明确信息的IllegalStateException
 * （防御深度：绕过构造检查的组合失败形态不再是裸NPE）。
 */
@Fast
public class TestRaftSmHistoryFailFast {
	@Test
	public void testRaftAgentWithHistoryFailsFast() {
		var config = new Zeze.Config();
		config.setHistory("UnitTest.FND_S2_2.History");
		// 修复前：构造成功（todo注释跳过初始化），毒组合存活到运行期以NPE/halt形态失败。
		var ex = Assertions.assertThrows(IllegalStateException.class,
				() -> new ServiceManagerAgentWithRaft(config));
		Assertions.assertTrue(ex.getMessage().contains("History"),
				"报错必须明确指出History与raft版SM的组合不支持: " + ex.getMessage());
	}

	@Test
	public void testAllocateTid128FutureNullClientThrowsExplicit() {
		var agent = new NullTid128Agent();
		// 修复前：这里抛裸NullPointerException（tid128UdpClient.allocateFuture）。
		var ex = Assertions.assertThrows(IllegalStateException.class,
				() -> agent.allocateTid128CacheFuture("UnitTest.FND_S2_2.Null"));
		Assertions.assertTrue(ex.getMessage().contains("tid128UdpClient"),
				"报错必须明确指出tid128UdpClient不可用: " + ex.getMessage());
	}

	/** 不初始化tid128UdpClient的最小Agent（模拟raft版组合的运行期形态）。 */
	private static final class NullTid128Agent extends AbstractAgent {
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
