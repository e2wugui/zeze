package Zeze.Component;

import java.lang.reflect.Field;

import Zeze.Builtin.Threading.KeepAlive;
import Zeze.Net.Service;
import Zeze.Services.ServiceManagerServer;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FND5-23 回归：ThreadingServer.ProcessKeepAlive对lastAppSerial !=
 * p.Argument即release该serverId全部资源——serial无版本单调性约束。
 * 同serverId双实例（实例重建窗口内旧keepAliveTask在途、或错误配置双
 * 客户端）时两个appSerialId交替到达，10秒一轮互解，正常持锁者被持续
 * 强制释放。修复：appSerialId为PersistentAtomicLong单调递增——更高的
 * serial才接管（release旧资源），更低的serial视为旧实例迟到，忽略。
 * harness同TestThreadingKeepAliveTakeover（包内直调rpc处理器）。
 */
@Fast
public class TestThreadingSerialMonotonic {

	private static final int SERVER_ID = 943;

	private ThreadingServer server;

	@BeforeEach
	public void setup() {
		Zeze.Util.Task.tryInitThreadPool();
		server = new ThreadingServer(new Service("TestThreadingSerialMonotonic"), new ServiceManagerServer.Conf());
	}

	@AfterEach
	public void cleanup() {
		server.close();
	}

	@Test
	public void testOldSerialIgnoredNewTakesOver() throws Exception {
		keepAlive(5); // first：登记
		keepAlive(3); // 旧实例迟到（更低的serial）
		Assertions.assertEquals(5L, lastAppSerialId(),
				"旧serial迟到不得覆盖lastAppSerial/触发release（FND5-23）");
		keepAlive(7); // 新实例（更高的serial）：接管
		Assertions.assertEquals(7L, lastAppSerialId(),
				"更高的serial必须接管（release旧资源并覆盖）");
	}

	private void keepAlive(long appSerialId) throws Exception {
		var ka = new KeepAlive();
		ka.Argument.setServerId(SERVER_ID);
		ka.Argument.setAppSerialId(appSerialId);
		Assertions.assertEquals(0L, server.ProcessKeepAlive(ka));
	}

	@SuppressWarnings("unchecked")
	private long lastAppSerialId() throws Exception {
		Field mapField = ThreadingServer.class.getDeclaredField("simulateThreadsByServerId");
		mapField.setAccessible(true);
		var map = (java.util.concurrent.ConcurrentHashMap<Integer, ?>)mapField.get(server);
		var threads = map.get(SERVER_ID);
		Assertions.assertNotNull(threads);
		Field serialField = threads.getClass().getDeclaredField("lastAppSerial");
		serialField.setAccessible(true);
		var serial = serialField.get(threads);
		Assertions.assertNotNull(serial);
		return ((Zeze.Builtin.Threading.BKeepAlive.Data)serial).getAppSerialId();
	}
}
