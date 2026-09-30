package Zeze.Arch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest;
import Zeze.Config;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.Procedure;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRedirectAllMissingSubscription {

	@Test
	public void unsubscribedModuleCompletesEveryHashWithProviderNotExist() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		var app = ProviderDirectTestSupport.newAppWithFakeAgent("RedirectAllMissingSubscription", config);
		var providerApp = new ProviderApp(app);
		var service = new ProviderDirectService("RedirectAllMissingSubscription.direct", app);
		service.providerApp = providerApp;
		setField(providerApp, "providerDirectService", service);
		var distribute = new ProviderDistribute(app, new LoadConfig(), service, 0);
		setField(providerApp, "distribute", distribute);
		var direct = new ProviderDirect();
		direct.providerApp = providerApp;
		direct.RegisterProtocols(service);

		var prefix = "MissingSubscription#";
		var serviceName = prefix + direct.getId();
		var info = new BServiceInfo(serviceName, "99", 0, "127.0.0.1", 1);
		var state = new Agent.SubscribeState(new BSubscribeInfo(serviceName, 1));
		state.onRegister(info);
		app.getServiceManager().getSubscribeStates().put(serviceName, state);
		distribute.addServer(info);
		// SM退订只移除订阅表；一致性哈希环的节点仍在，不能靠空环提前返回掩盖问题。
		app.getServiceManager().getSubscribeStates().remove(serviceName);
		assertNotNull(distribute.getConsistentHash(serviceName));

		var context = new RedirectAllContext<RedirectResult>(3, binary -> new RedirectResult());
		long sessionId = service.addManualContextWithTimeout(context, 30_000);
		var completed = new CountDownLatch(1);
		context.getFuture().OnAllDone(done -> completed.countDown());
		var request = new ModuleRedirectAllRequest();
		request.Argument.setServiceNamePrefix(prefix);
		request.Argument.setModuleId(direct.getId());
		request.Argument.setHashCodeConcurrentLevel(3);
		request.Argument.setSessionId(sessionId);
		request.Argument.setMethodFullName("MissingSubscription.redirectAll");
		try {
			assertDoesNotThrow(() -> app.redirect.redirectAll(direct, request, context),
					"退订后的缺失服务必须按hash返回失败，不能解引用null订阅态");
			assertTrue(completed.await(5, TimeUnit.SECONDS), "所有hash必须立即收尾，无需等待上下文超时");
			assertEquals(3, context.getAllResults().size());
			for (int hash = 0; hash < 3; hash++) {
				var result = context.getAllResults().get(hash);
				assertNotNull(result);
				assertEquals(Procedure.ProviderNotExist, result.getResultCode());
			}
			assertNull(service.tryGetManualContext(sessionId));
		} finally {
			service.tryRemoveManualContext(sessionId);
			app.stop();
		}
	}

	private static void setField(Object owner, String name, Object value) throws Exception {
		var field = ProviderApp.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(owner, value);
	}
}
