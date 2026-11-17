package Zeze.Services;

import Zeze.Config;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestGcmInitializesConfigurationBeforeListening {
	@Test
	void synchronousConfigurationFailurePrecedesListening() throws Exception {
		Task.tryInitThreadPool();
		var constructor = GlobalCacheManagerServer.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		var server = constructor.newInstance();
		server.getGcmConfig().initialCapacity = 31;
		server.getGcmConfig().serverReleaseTimeout = Integer.MAX_VALUE;
		// 无法表示的安全期限必须在任何监听尝试之前拒绝；非法端口用于检测是否先进入了 bind。
		assertThrows(ArithmeticException.class, () -> server.start(null, 70000, new Config()));
		assertClosed(server);
		server.stop();
	}

	@Test
	void raftConfigurationFailurePrecedesOpeningStorage() throws Exception {
		var config = new Config();
		var element = javax.xml.parsers.DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument().createElement("GlobalCacheManager");
		element.setAttribute("ServerReleaseTimeout", String.valueOf(Integer.MAX_VALUE));
		config.getCustomizes().put("GlobalCacheManager", element);
		// 不提供 Raft 配置：若先构造 Rocks，将尝试加载磁盘配置并以另一原因失败。
		assertThrows(ArithmeticException.class,
				() -> new GlobalCacheManagerWithRaft("unconfigured", null, config));
	}

	@Test
	void asynchronousConfigurationFailurePrecedesListening() throws Exception {
		Task.tryInitThreadPool();
		var server = new GlobalCacheManagerAsyncServer();
		server.getGcmConfig().initialCapacity = 31;
		server.getGcmConfig().serverReleaseTimeout = Integer.MAX_VALUE;
		assertThrows(ArithmeticException.class, () -> server.start(null, 70000, new Config()));
		assertClosed(server);
		server.stop();
	}

	private static void assertClosed(Object server) throws Exception {
		var open = server.getClass().getDeclaredField("open");
		open.setAccessible(true);
		assertFalse(open.getBoolean(server));
	}
}
