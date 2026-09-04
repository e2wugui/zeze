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
import harness.Fast;
import org.jetbrains.annotations.NotNull;

/**
 * FND2-S2-4：TCP版 allocateTidCacheFuture 镜像 f561f8e41（128版）的自愈——
 * 上一次分配异常完成时 get() 抛出且发生在 lastTidCacheFuture（唯一写入点）替换之前，
 * 异常传播出去导致毒化状态永久保留（与128版同构；当前TCP版为死代码，属修复不完整收敛）。
 * <p>
 * allocateAsync 返回 false（模拟发送失败）即可确定性构造毒化future，无需网络。
 */
@Fast
public class TestTidCacheFutureTcpSelfHeal {
	private static final String GLOBAL_NAME = "UnitTest.FND2_S2_4.TcpSelfHeal";

	@Test
	public void testTcpPoisonSelfHeal() {
		var agent = new NoSendAgent();
		var f1 = agent.allocateTidCacheFuture(GLOBAL_NAME);
		Assertions.assertTrue(f1.isCompletedExceptionally()); // allocateAsync发送失败→毒化。

		// 修复前：这里抛CompletionException，且lastTidCacheFuture永不被替换（毒化永久保留）。
		var f2 = Assertions.assertDoesNotThrow(() -> agent.allocateTidCacheFuture(GLOBAL_NAME));
		Assertions.assertNotSame(f1, f2);
		Assertions.assertSame(f2, agent.getLastTidCacheFuture());
	}

	@Test
	public void testTcpPoisonSelfHealRepeatable() {
		var agent = new NoSendAgent();
		for (var i = 0; i < 3; i++) {
			var poisoned = agent.allocateTidCacheFuture(GLOBAL_NAME);
			Assertions.assertTrue(poisoned.isCompletedExceptionally());
			var next = Assertions.assertDoesNotThrow(() -> agent.allocateTidCacheFuture(GLOBAL_NAME));
			Assertions.assertNotSame(poisoned, next);
			Assertions.assertSame(next, agent.getLastTidCacheFuture());
		}
	}

	/** allocateAsync恒返回false：future立即异常完成（毒化），全程无需网络。 */
	private static final class NoSendAgent extends AbstractAgent {
		@Override
		protected void allocate(@NotNull AutoKey autoKey, int pool) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected boolean allocateAsync(@NotNull String globalName, int allocCount,
										@NotNull ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>> callback) {
			return false; // 模拟发送失败。
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
			// nothing
		}
	}
}
