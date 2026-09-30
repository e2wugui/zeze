package Zeze.Dbh2;

import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Builtin.Dbh2.Master.BRegisterResult;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestManagerReregisterRetriesReadyFailure {
	private static final class RegistrationAgent extends MasterAgent {
		int registrations;
		boolean failReady;
		boolean readyAtMaster;

		RegistrationAgent() {
			super(new Config());
		}

		@Override
		public BRegisterResult.Data register(String host, int port, int bucketCount) {
			registrations++;
			readyAtMaster = false; // Master.Register replaces the entry with ready=false.
			return new BRegisterResult.Data();
		}

		@Override
		public void setDbh2Ready() {
			if (failReady)
				throw new IllegalStateException("SetDbh2Ready response failed");
			readyAtMaster = true;
		}

		@Override
		public void reportLoad(double load) {
			// The real master accepts load reports even when ready=false.
		}
	}

	@Test
	void monitorRetriesAfterAReadyManagerFailsToReregister(@TempDir Path tempDir) throws Exception {
		var config = tempDir.resolve("manager.xml");
		Files.writeString(config, "<zeze/>");
		var manager = new Dbh2Manager(tempDir.resolve("home").toString(), config.toString());
		var agent = new RegistrationAgent();
		var field = Dbh2Manager.class.getDeclaredField("masterAgent");
		field.setAccessible(true);
		field.set(manager, agent);
		try {
			manager.reRegister();
			assertEquals(true, agent.readyAtMaster);

			agent.failReady = true;
			manager.reRegister();
			assertEquals(false, agent.readyAtMaster);
			assertEquals(2, agent.registrations);

			agent.failReady = false;
			var monitor = Dbh2Manager.class.getDeclaredMethod("loadMonitor");
			monitor.setAccessible(true);
			monitor.invoke(manager);
			assertEquals(3, agent.registrations, "The monitor must retry the incomplete registration");
			assertEquals(true, agent.readyAtMaster, "The recovered manager must become eligible for buckets");
		} finally {
			manager.stop();
		}
	}
}
