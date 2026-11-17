package Zeze.Arch;

import Zeze.Builtin.Provider.BModule;
import Zeze.Config;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestDirectReadinessAfterClose {
	@Test
	void closedSessionCannotPublishItsFirstReadiness() throws Exception {
		checkLateReadiness(false);
	}

	@Test
	void closedSessionCannotRepublishRemovedReadiness() throws Exception {
		checkLateReadiness(true);
	}

	private static void checkLateReadiness(boolean publishBeforeClose) throws Exception {
		Task.tryInitThreadPool();
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		// No application start/database: no shared server ID or in-memory database URL.
		var app = ProviderDirectTestSupport.newAppWithFakeAgent("DirectReadinessAfterClose", config);
		var direct = new ProviderDirectService("DirectReadinessAfterClose.direct", app);
		var links = new ProviderService("DirectReadinessAfterClose.links", app);
		var session = direct.newSession(2);
		try {
			var provider = new ProviderApp(app, new ProviderOnly(), links, "Readiness#",
					new ProviderDirect(), direct, "Readiness.linkd", new LoadConfig());
			provider.modules.put(42, new BModule.Data());
			var state = new Agent.SubscribeState(new BSubscribeInfo("Readiness#42", 0));
			state.onRegister(new BServiceInfo("Readiness#42", "2", 0, "127.0.0.1", 12345));
			requireNonNull(app.getServiceManager()).getSubscribeStates().put("Readiness#42", state);
			// A real socket close invokes the service callback; readiness can be delayed by
			// an already executing handshake/announce independently of this callback.
			var socket = direct.newServerSocket("127.0.0.1", 0, null);
			socket.setUserState(session);
			session.sessionId = socket.getSessionId();
			if (publishBeforeClose) {
				direct.setRelativeServiceReady(session, "127.0.0.1", 12345);
				assertSame(session, direct.providerByServerId.get(2));
				assertTrue(state.getLocalStates().containsKey("2"));
			}
			socket.close();
			assertNull(direct.providerByServerId.get(2));
			assertFalse(state.getLocalStates().containsKey("2"));
			direct.setRelativeServiceReady(session, "127.0.0.1", 12345);
			assertAll(
					() -> assertNull(direct.providerByServerId.get(2)),
					() -> assertFalse(direct.providerByLoadName.containsKey("127.0.0.1_12345")),
					() -> assertFalse(state.getLocalStates().containsKey("2")));
		} finally {
			direct.stop();
			links.stop();
			session.timeCounter.close();
			app.stop();
		}
	}
}
