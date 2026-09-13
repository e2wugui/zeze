package UnitTest.Zeze.Services;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.EditService;
import Zeze.Services.ServiceManagerServer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-29：Critical协议经oneByOne池执行，可能晚于OnSocketClose的会话清理到达。
 * 注册报文与RST几乎同时到达（发送后立刻崩溃）是常态触发。
 * 用确定性交错坐实：服务端socket关闭且清理完成后，迟到的EditService
 * 必须被拒绝，不得把死会话注册写回serviceStates（幽灵服务地址）。
 */
@Fast
public class TestLateProtocolAfterClose {

	@Test
	public void testLateEditServiceAfterClose() throws Exception {
		Task.tryInitThreadPool();
		// 固定端口契约与选段说明见TestTakeoverIdentifySuspect；26111避开其26110。
		final int port = 26111;
		Files.createDirectories(Path.of("autokeys"));

		var sm = new ServiceManagerServer(null, port, new Config(), "autokeys/fnd5-29");
		Agent agent = null;
		try {
			agent = newAgent(port);
			agent.start();
			agent.waitReady();

			var server = (Service)field(sm, "server");
			var serverSide = server.GetSocket(); // 当前唯一连接的服务端会话
			Assertions.assertNotNull(serverSide, "agent应已建立服务端会话");

			var arg = new BEditService();
			arg.getAdd().add(new BServiceInfo("Fnd5LateGhost", "7", 0, "127.0.0.1", 1234));
			var late = new EditService(arg);
			late.setSender(serverSide);

			// 关闭服务端socket：OnSocketClose清理（此刻session.registers为空）——
			// 等价于“报文已解码入队、close先于池任务执行”的交错后态。
			serverSide.close();
			Assertions.assertTrue(waitUntil(() -> server.GetSocket(serverSide.getSessionId()) == null),
					"socketMap应已摘除该会话");

			// 迟到协议处理（等价于oneByOne池任务在close之后执行）
			Method process = ServiceManagerServer.class.getDeclaredMethod("processEditService", EditService.class);
			process.setAccessible(true);
			process.invoke(sm, late);

			@SuppressWarnings("unchecked")
			var serviceStates = (ConcurrentHashMap<String, ?>)field(sm, "serviceStates");
			Assertions.assertFalse(serviceStates.containsKey("Fnd5LateGhost"),
					"迟到EditService不得在会话清理后落盘死注册");
		} finally {
			if (agent != null)
				agent.stop();
			sm.close();
		}
	}

	private static Agent newAgent(int port) throws Exception {
		var agent = new Agent(new Config());
		agent.getClient().getConfig().addConnector(new Connector("127.0.0.1", port));
		return agent;
	}

	private static Object field(Object obj, String name) throws Exception {
		Field f = obj.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static boolean waitUntil(java.util.function.BooleanSupplier cond) throws InterruptedException {
		for (var i = 0; i < 100 && !cond.getAsBoolean(); i++)
			Thread.sleep(20);
		return cond.getAsBoolean();
	}
}
