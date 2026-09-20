package Zeze.Arch;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import Zeze.Application;
import Zeze.Config;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Services.ServiceManager.BUnSubscribeArgument;
import Zeze.Component.Threading;
import org.jetbrains.annotations.NotNull;

/**
 * ProviderDirectService单测公共脚手架：disable模式构造Application（不建真实SM、
 * 无网络副作用），再反射注入全no-op的FakeAgent——setRelativeServiceReady/removeServer
 * 只需要getSubscribeStates()的登记能力。Application为final无法继承，故用反射设置
 * final的serviceManager字段（非静态final字段setAccessible后可写）。
 */
final class ProviderDirectTestSupport {
	private ProviderDirectTestSupport() {
	}

	static Application newAppWithFakeAgent(String name, Config conf) throws Exception {
		var app = new Application(name, conf);
		var f = Application.class.getDeclaredField("serviceManager");
		f.setAccessible(true);
		f.set(app, new FakeAgent());
		return app;
	}

	/** 全no-op的AbstractAgent：仅提供getSubscribeStates的登记能力。 */
	static class FakeAgent extends AbstractAgent {
		@Override
		protected void allocate(@NotNull AutoKey autoKey, int pool) {
		}

		@Override
		protected boolean allocateAsync(@NotNull String globalName, int allocCount,
										@NotNull Zeze.Net.ProtocolHandle<
												Zeze.Net.Rpc<Zeze.Services.ServiceManager.BAllocateIdArgument,
														Zeze.Services.ServiceManager.BAllocateIdResult>> callback) {
			return false;
		}

		@Override
		public void start() {
		}

		@Override
		public void waitReady() {
		}

		@Override
		public void editService(@NotNull BEditService arg) {
		}

		@Override
		public @NotNull CompletableFuture<List<Agent.SubscribeState>> subscribeServicesAsync(
				@NotNull Zeze.Services.ServiceManager.BSubscribeArgument arg) {
			return CompletableFuture.completedFuture(List.of());
		}

		@Override
		public void unSubscribeService(@NotNull BUnSubscribeArgument arg) {
		}

		@Override
		public boolean setServerLoad(@NotNull BServerLoad load) {
			return false;
		}

		@Override
		public @NotNull Threading getThreading() {
			throw new UnsupportedOperationException("not used in these tests");
		}

		@Override
		public void close() {
		}
	}
}
