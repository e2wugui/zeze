package Zeze.Transaction;

import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestGlobalAgentLateInitialization {
	@Test
	void loginResponseAfterDaemonDetachedStillUpdatesConfig() throws Exception {
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		var app = new Application("GlobalAgentLateInitialization", config);
		var agent = new GlobalAgentBase(app) {
			@Override
			protected void cancelPending() {
			}

			@Override
			public void keepAlive() {
			}
		};
		agent.initialize(100, 200, 300);
		assertEquals(300, agent.getConfig().serverReleaseTimeout);
		assertEquals(100, agent.getConfig().keepAliveTimeout);
	}
}
