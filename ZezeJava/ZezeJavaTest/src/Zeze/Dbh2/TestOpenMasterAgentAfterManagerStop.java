package Zeze.Dbh2;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 管理器停止后必须拒绝新建MasterAgent（对齐openBucket的stopped门禁）：
 * stop()持管理器锁清理全部MasterAgent后本类不可重启，此后openMasterAgent若不加门禁，
 * 会在锁外构造并登记一个无人回收的MasterAgent（连接器+自动重连常驻直至进程退出）。
 * 钉住：stop后openMasterAgent立即以IllegalStateException失败，且不向masterAgent表复活任何条目。
 */
@Fast
public class TestOpenMasterAgentAfterManagerStop {

	@SuppressWarnings("unchecked")
	private static int registeredMasterAgents(Dbh2AgentManager manager) throws Exception {
		Field field = Dbh2AgentManager.class.getDeclaredField("masterAgent");
		field.setAccessible(true);
		var masterAgent = (ConcurrentHashMap<String, MasterAgent>)field.get(manager);
		return masterAgent.size();
	}

	@Test
	public void testOpenMasterAgentRejectedAfterStop(@TempDir Path tempDir) throws Exception {
		var manager = new Dbh2AgentManager(new Dbh2AgentStubSupport.NullServiceAgent(),
				Config.load(Dbh2AgentStubSupport.writeRemoteCommitConfig(tempDir).toString()), 832);
		try {
			manager.stop();

			// 门禁必须快速失败：不构造MasterAgent、不等待连接（bug时会构造并阻塞到READY_TIMEOUT
			// 后以连接失败异常上抛，且期间已建立连接器与重连任务）。
			var ex = Assertions.assertThrows(IllegalStateException.class,
					() -> manager.openMasterAgent("127.0.0.1_1"),
					"stopped管理器的openMasterAgent必须立即拒绝（对齐openBucket门禁）");
			Assertions.assertTrue(ex.getMessage().contains("stopped"));

			// 不得向已清空的masterAgent表复活条目。
			Assertions.assertEquals(0, registeredMasterAgents(manager),
					"stop后masterAgent表必须保持为空，不得登记新建的agent");
		} finally {
			// 兼容bug形态下的失败清理：断言失败（未抛IllegalStateException）时openMasterAgent
			// 可能已把构造半途失败的异常上抛，manager本身保持可stop。
			manager.stop();
		}
	}
}
