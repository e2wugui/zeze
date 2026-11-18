package Zeze;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestRaftServiceManagerConfiguration {
	@Test
	public void missingConfigurationIdentifiesRequiredNode() {
		var config = new Config();
		config.setServiceManager("raft");

		var error = assertThrows(IllegalStateException.class,
				() -> Application.createServiceManager(config, "TestRaftServiceManagerConfiguration"));
		assertTrue(error.getMessage().contains("ServiceManagerConf"));
	}
}
