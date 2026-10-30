package Zeze.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Config;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRaftRegistrationConfirmation {
	private static void checkFailedDuplicateAdds(boolean existing) throws Exception {
		Task.tryInitThreadPool();
		Path xml = Files.createTempFile("registration-confirmation", ".xml");
		Files.writeString(xml, """
				<raft Name="registration-confirmation">
					<node Host="127.0.0.1" Port="19621"/>
					<node Host="127.0.0.1" Port="19622"/>
				</raft>
				""");
		try {
			var config = Config.load();
			assertNotNull(config.getServiceManagerConf());
			config.getServiceManagerConf().setRaftXml(xml.toString());
			config.getServiceManagerConf().setSessionName("registration-confirmation");
			try (var agent = new ServiceManagerAgentWithRaft(config)) {
				var login = ServiceManagerAgentWithRaft.class.getDeclaredField("loginFuture");
				login.setAccessible(true);
				@SuppressWarnings("unchecked")
				var ready = (TaskCompletionSource<Boolean>)login.get(agent);
				ready.setResult(true);
				var transport = ServiceManagerAgentWithRaft.class.getDeclaredField("raftClient");
				transport.setAccessible(true);
				((Zeze.Raft.Agent)transport.get(agent)).stop();

				var field = ServiceManagerAgentWithRaft.class.getDeclaredField("registers");
				field.setAccessible(true);
				@SuppressWarnings("unchecked")
				var registers = (ConcurrentHashMap<BServiceInfo, BServiceInfo>)field.get(agent);
				var original = new BServiceInfo("service", "1", 1, "old", 1);
				if (existing)
					registers.put(original, original);
				var edit = new BEditService();
				edit.getAdd().add(new BServiceInfo("service", "1", 2, "new-a", 2));
				edit.getAdd().add(new BServiceInfo("service", "1", 3, "new-b", 3));
				assertThrows(Exception.class, () -> agent.editService(edit));
				if (existing) {
					assertEquals(1, registers.size());
					assertSame(original, registers.get(original), "失败批次必须保留原始已确认注册");
				} else
					assertTrue(registers.isEmpty(), "失败批次不得留下中间注册供重连重放");
			}
		} finally {
			Files.deleteIfExists(xml);
		}
	}

	@Test
	void failedRepeatedAddsDoNotCreateRegistration() throws Exception {
		checkFailedDuplicateAdds(false);
	}

	@Test
	void failedRepeatedAddsPreserveConfirmedRegistration() throws Exception {
		checkFailedDuplicateAdds(true);
	}
}
