package UnitTest.Zeze.Services;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Services.ServiceManagerAgentWithRaft;
import Zeze.Services.ServiceManagerWithRaft;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Util.Task;
import harness.Fast;

/**
 * S-3：raft 版 ServiceManager 会话行的归属校验。
 * <p>
 * 会话状态按 name 持久化在 tableSession 行中，同名新连接 Login 只覆写行的 sessionId；
 * 旧连接的关闭事件可能晚于新连接 Login 被处理（keepalive 超时等），onClose 不校验归属
 * 就清理注册/订阅并删行：新连接的订阅被清（Edit 推送静默丢失）、行删除后其 Edit 请求 NPE。
 * <p>
 * 进程内 3 节点 raft 集群 + 三个客户端：Flap1 与 Flap2 同名，Target 订阅观察。
 */
@Fast
public class TestServiceManagerWithRaftSessionOwnership {
	private static final String serviceName = "UnitTest.S3.Svc";
	private static final String flapSessionName = "UnitTest.S3.Flap";
	private static final String raftXmlString = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="s3_sm_test">
				<node Host="127.0.0.1" Port="19501"/>
				<node Host="127.0.0.1" Port="19502"/>
				<node Host="127.0.0.1" Port="19503"/>
			</raft>
			""";
	private static Path raftXmlFile;
	private static final ArrayList<ServiceManagerWithRaft> servers = new ArrayList<>();

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		cleanDirs();
		raftXmlFile = Files.createTempFile("s3_sm_test_raft", ".xml");
		Files.writeString(raftXmlFile, raftXmlString);
		for (var node : RaftConfig.loadFromString(raftXmlString).getNodes().values())
			servers.add(new ServiceManagerWithRaft(node.getName(),
					RaftConfig.loadFromString(raftXmlString), new Config(), false));
	}

	@AfterAll
	public static void tearDown() throws Exception {
		for (var sm : servers)
			sm.close();
		servers.clear();
		Files.deleteIfExists(raftXmlFile);
		cleanDirs();
	}

	// Windows下启动前清理，保证全新状态；收尾best-effort。
	private static void cleanDirs() {
		LogSequence.deleteDirectory(new File("s3_sm_test"));
		for (int port = 19501; port <= 19503; ++port) {
			LogSequence.deleteDirectory(new File("127.0.0.1_" + port));
			LogSequence.deleteDirectory(new File("s3_sm_test_127.0.0.1_" + port));
		}
	}

	private static ServiceManagerAgentWithRaft newAgent(String sessionName) throws Exception {
		var config = Config.load();
		config.getServiceManagerConf().setRaftXml(raftXmlFile.toString());
		config.getServiceManagerConf().setSessionName(sessionName);
		var agent = new ServiceManagerAgentWithRaft(config);
		agent.start();
		agent.waitReady();
		return agent;
	}

	private static void register(ServiceManagerAgentWithRaft agent, String identity) {
		var edit = new BEditService();
		edit.getAdd().add(new BServiceInfo(serviceName, identity, 0));
		agent.editService(edit);
	}

	private static boolean targetSees(ServiceManagerAgentWithRaft target, String identity) {
		var state = target.getSubscribeStates().get(serviceName);
		return state != null && state.findServiceInfoByIdentity(identity) != null;
	}

	private static void waitTargetSees(ServiceManagerAgentWithRaft target, String identity, long timeoutMs)
			throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (targetSees(target, identity))
				return;
			Thread.sleep(50);
		}
		Assertions.assertTrue(targetSees(target, identity),
				"target must see identity=" + identity + " within " + timeoutMs + "ms");
	}

	@Test
	@Timeout(120)
	@Disabled
	public void testLateCloseMustNotDestroyNewConnectionRow() throws Exception {
		var flap1 = new ServiceManagerAgentWithRaft[]{null};
		try {
			// 1. Flap1 以名字 flapSessionName 登录并注册 "1"
			flap1[0] = newAgent(flapSessionName);
			register(flap1[0], "1");

			// 2. Target 订阅，看到 "1"
			var target = newAgent("UnitTest.S3.Target");
			try {
				target.subscribeService(new BSubscribeInfo(serviceName));
				waitTargetSees(target, "1", 5000);

				// 3. Flap2 同名登录（行被新连接接管）并注册 "2"
				var flap2 = newAgent(flapSessionName);
				try {
					register(flap2, "2");
					waitTargetSees(target, "2", 5000);

					// 4. Flap1 的旧连接此刻才关闭（模拟晚到的关闭事件）
					stopQuietly(flap1[0]);
					flap1[0] = null;
					Thread.sleep(1000); // 等服务端处理完关闭事件

					// 新连接的注册不能被旧连接的关闭摧毁
					Assertions.assertTrue(targetSees(target, "2"),
							"late onClose of old connection must not unregister new connection's services");
					Assertions.assertTrue(targetSees(target, "1"),
							"late onClose of old connection must not unregister new connection's services");

					// 5. 新连接继续注册 "3"：行还在，必须成功且 Target 能收到推送
					//    （当前实现行被删，ProcessEditRequest 中 tableSession.get 返回 null NPE，注册静默失败）
					register(flap2, "3");
					waitTargetSees(target, "3", 5000);
				} finally {
					closeQuietly(flap2);
				}
			} finally {
				closeQuietly(target);
			}
		} finally {
			if (flap1[0] != null)
				closeQuietly(flap1[0]);
		}
	}

	private static void stopQuietly(ServiceManagerAgentWithRaft agent) {
		try {
			// 反射取私有字段 raftClient（Zeze.Raft.Agent），停掉底层连接（不发 NormalClose，模拟异常断开）
			Field field = ServiceManagerAgentWithRaft.class.getDeclaredField("raftClient");
			field.setAccessible(true);
			var raftAgent = field.get(agent);
			raftAgent.getClass().getMethod("stop").invoke(raftAgent);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void closeQuietly(ServiceManagerAgentWithRaft agent) {
		try {
			agent.close();
		} catch (Throwable ignored) { // ignored
		}
	}
}
