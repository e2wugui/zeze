package Zeze.Arch;

import java.util.ArrayList;
import Zeze.Application;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRedirectAllLocalResultSnapshot {

	@Test
	void delayedLocalDispatchRetainsFlushedAndRemainingResults() throws Exception {
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		var app = ProviderDirectTestSupport.newAppWithFakeAgent("RedirectAllLocalSnapshot", config);
		var service = new DeferredService(app);
		var providerApp = new ProviderApp(app);
		service.providerApp = providerApp;
		var serviceField = ProviderApp.class.getDeclaredField("providerDirectService");
		serviceField.setAccessible(true);
		serviceField.set(providerApp, service);
		var direct = new ProviderDirect();
		direct.providerApp = providerApp;
		direct.RegisterProtocols(service);
		var context = new RedirectAllContext<>(2,
			bytes -> new SizedResult(requireNonNull(bytes).size()));
		long sessionId = service.addManualContextWithTimeout(context, 30_000);
		try {
			String method = "LocalSnapshot:collect";
			requireNonNull(app.redirect).handles.put(method, new RedirectHandle(TransactionLevel.None,
					(hash, params) -> RedirectAllFuture.result(new SizedResult(hash == 0 ? 0x10_0001 : 3)),
					result -> new Binary(new byte[((SizedResult)result).size])));
			var request = new ModuleRedirectAllRequest();
			request.Argument.setMethodFullName(method);
			request.Argument.setSessionId(sessionId);
			request.Argument.setHashCodeConcurrentLevel(2);
			request.Argument.getHashCodes().add(0);
			request.Argument.getHashCodes().add(1);
			assertEquals(Procedure.Success, direct.ProcessModuleRedirectAllRequest(request));
			assertEquals(2, service.pending.size(), "one size flush and one remaining batch");
			// 模拟结果任务晚于发送方处理下一批执行，避免依赖线程调度概率。
			for (var result : service.pending)
				direct.ProcessModuleRedirectAllResult(result);
			assertEquals(2, context.getAllResults().size(), "both local hash results must survive batching");
			assertTrue(context.isCompleted(), "local collection must complete without waiting for timeout");
			assertEquals(0x10_0001, requireNonNull(context.getAllResults().get(0)).size);
			assertEquals(3, requireNonNull(context.getAllResults().get(1)).size);
			assertEquals(Procedure.Success, requireNonNull(context.getAllResults().get(0)).getResultCode());
		} finally {
			service.tryRemoveManualContext(sessionId);
			service.stop();
			app.stop();
		}
	}

	private static final class SizedResult extends RedirectResult {
		final int size;

		SizedResult(int size) {
			this.size = size;
		}
	}

	private static final class DeferredService extends ProviderDirectService {
		final ArrayList<ModuleRedirectAllResult> pending = new ArrayList<>();

		DeferredService(Application app) {
			super("RedirectAllLocalSnapshot.direct", app);
		}

		@Override
		public void dispatchProtocol(Protocol<?> protocol, ProtocolFactoryHandle<?> factoryHandle) {
			pending.add(assertInstanceOf(ModuleRedirectAllResult.class, protocol));
		}
	}
}
