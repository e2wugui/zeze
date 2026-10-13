package Zeze.Services;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import Zeze.Builtin.LogService.BCondition;
import Zeze.Config;
import Zeze.Net.ServiceConf;
import Zeze.Services.Log4jQuery.SessionAll;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.Task;
import harness.Fast;

/**
 * 全服视图会话成员集维护（FND30 zokermanager-02）的自包含真身回归：
 * 真 LogService + 真 LogAgent + applyOnChanged 模拟 SM 推送（对齐
 * TestLog4jSessionAllDegraded）。
 * <p>
 * 修复前：SessionAll 构造期跳过的服务器（当时不可达）与构造后注册表上台的服务器
 * （SM 推送/扩容竞态）永不入会——alls 无补员入口（renewDeadMembers 候选只来自在册
 * 成员的失败），ZokerManager 侧快照/复用判定又取自注册表键集，键集稳定时缺员会话
 * 被持续复用，该台数据静默缺席、remain 提前 false。修复后：operate 入口缺册补员
 * （reconcileMissingMembers），失败记 per-member 退避时间戳（60s 窗内不重试）。
 * <p>
 * 退避表用反射直读（私有实现细节不扩 API；先例 TestIdleBindingSweep 反射驱动
 * sweepIdleBindings）。
 */
@Fast
@Extra
public class TestLog4jAllViewMemberReconcile {
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/** 构造期被跳过的服务器（当时端口无监听），恢复上线后下一次 operate 必须补入会话。 */
	@Test
	public void testSkippedServerRejoinsAfterRevive() throws Exception {
		int portA;
		int portB;
		try (var ssA = new ServerSocket(0); var ssB = new ServerSocket(0)) {
			portA = ssA.getLocalPort();
			portB = ssB.getLocalPort();
		}
		var dirA = Files.createTempDirectory("zeze-log4j-allview-a");
		var dirB = Files.createTempDirectory("zeze-log4j-allview-b");
		var serviceA = new LogService(newTestConfig(portA, dirA));
		LogService serviceB = null;
		var logAgent = new LogAgent(newTestConfig(portA, dirA));
		try {
			serviceA.start();
			logAgent.start();

			var idA = "LogService_a_127.0.0.1_" + portA;
			var idB = "LogService_b_127.0.0.1_" + portB;
			push(logAgent, idA, portA); // 真 A
			push(logAgent, idB, portB); // B 端口此刻无监听：构造期死条目
			waitReady(logAgent, idA);

			try (var sessionAll = logAgent.newSessionAll("zeze.log")) {
				assertEquals(Set.of(idA), sessionAll.memberNames(), "构造期：不可达的 B 被降级跳过");

				// B 恢复上线（起第二个真 LogService 于同端口）——修复前 B 永不入会。
				serviceB = new LogService(newTestConfig(portB, dirB));
				serviceB.start();
				waitReady(logAgent, idB);

				assertNotNull(sessionAll.search(3, false, newSearchCondition()));
				assertTrue(sessionAll.memberNames().contains(idB), "恢复后的 B 必须在下一 operate 补入会话（缺册补员）");
			}
		} finally {
			try {
				logAgent.stop();
			} finally {
				if (serviceB != null)
					serviceB.stop();
				try {
					serviceA.stop();
				} finally {
					deleteBestEffort(dirA);
					deleteBestEffort(dirB);
				}
			}
		}
	}

	/** 构造后注册表上台的服务器（扩容/SM 推送竞态形态）：下一 operate 补入。 */
	@Test
	public void testLateRegisteredServerJoinsOnNextOperate() throws Exception {
		int portA;
		int portB;
		try (var ssA = new ServerSocket(0); var ssB = new ServerSocket(0)) {
			portA = ssA.getLocalPort();
			portB = ssB.getLocalPort();
		}
		var dirA = Files.createTempDirectory("zeze-log4j-allview-a");
		var dirB = Files.createTempDirectory("zeze-log4j-allview-b");
		var serviceA = new LogService(newTestConfig(portA, dirA));
		var serviceB = new LogService(newTestConfig(portB, dirB));
		var logAgent = new LogAgent(newTestConfig(portA, dirA));
		try {
			serviceA.start();
			serviceB.start();
			logAgent.start();

			var idA = "LogService_a_127.0.0.1_" + portA;
			var idB = "LogService_b_127.0.0.1_" + portB;
			push(logAgent, idA, portA); // 构造时只推 A
			waitReady(logAgent, idA);

			try (var sessionAll = logAgent.newSessionAll("zeze.log")) {
				assertEquals(Set.of(idA), sessionAll.memberNames());

				push(logAgent, idB, portB); // 构造后推 B（已在监听）
				waitReady(logAgent, idB);

				assertNotNull(sessionAll.search(3, false, newSearchCondition()));
				assertTrue(sessionAll.memberNames().contains(idB), "上台后的 B 必须在下一 operate 补入会话");
			}
		} finally {
			try {
				logAgent.stop();
			} finally {
				try {
					serviceA.stop();
				} finally {
					serviceB.stop();
					deleteBestEffort(dirA);
					deleteBestEffort(dirB);
				}
			}
		}
	}

	/**
	 * 补员退避：B 死且保持死——连续两次 operate 只在第一次尝试补员（退避窗内不重试，
	 * 时间戳不刷新）；清退避表（等价退避窗过期）后再次 operate 必须重试（时间戳刷新）。
	 */
	@Test
	public void testReconcileBackoffSingleAttemptPerWindow() throws Exception {
		int portA;
		int portB;
		try (var ssA = new ServerSocket(0); var ssB = new ServerSocket(0)) {
			portA = ssA.getLocalPort();
			portB = ssB.getLocalPort();
		}
		var dirA = Files.createTempDirectory("zeze-log4j-allview-a");
		var serviceA = new LogService(newTestConfig(portA, dirA));
		var logAgent = new LogAgent(newTestConfig(portA, dirA));
		try {
			serviceA.start();
			logAgent.start();

			var idA = "LogService_a_127.0.0.1_" + portA;
			var idB = "LogService_b_127.0.0.1_" + portB;
			push(logAgent, idA, portA); // 真 A
			push(logAgent, idB, portB); // B 端口无监听：构造期跳过，保持死
			waitReady(logAgent, idA);

			try (var sessionAll = logAgent.newSessionAll("zeze.log")) {
				assertEquals(Set.of(idA), sessionAll.memberNames(), "构造期：不可达的 B 被降级跳过");

				assertNotNull(sessionAll.search(3, false, newSearchCondition()));
				var backoff = memberBackoffTableOf(sessionAll);
				assertTrue(backoff.containsKey(idB), "第一次 operate 必须尝试补员 B 并记录退避（修复前：从不尝试）");
				var firstStamp = backoff.get(idB);

				assertNotNull(sessionAll.search(3, false, newSearchCondition()));
				assertEquals(firstStamp, backoff.get(idB), "退避窗内不得重复尝试（失败时间戳不得刷新）");

				backoff.clear(); // 清退避表：等价退避窗过期
				assertNotNull(sessionAll.search(3, false, newSearchCondition()));
				assertNotEquals(firstStamp, backoff.get(idB), "清退避后必须再次尝试并记录新的失败时间戳");
			}
		} finally {
			try {
				logAgent.stop();
			} finally {
				serviceA.stop();
				deleteBestEffort(dirA);
			}
		}
	}

	private static void push(LogAgent logAgent, String identity, int port) {
		var edit = new BEditService();
		edit.getAdd().add(new BServiceInfo("Zeze.LogService", identity, 0, "127.0.0.1", port));
		logAgent.applyOnChanged(edit);
	}

	private static void waitReady(LogAgent logAgent, String identity) throws InterruptedException {
		var deadline = System.currentTimeMillis() + 30_000;
		while (logAgent.__getLogServer(identity).TryGetReadySocket() == null) {
			if (System.currentTimeMillis() > deadline)
				throw new IllegalStateException("等待 LogService 连接就绪超时: " + identity);
			Thread.sleep(50);
		}
	}

	/** 反射直读私有退避表：实现细节不扩 API（先例 TestIdleBindingSweep）。 */
	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, Long> memberBackoffTableOf(SessionAll sessionAll) throws Exception {
		var field = SessionAll.class.getDeclaredField("memberRetryBackoff");
		field.setAccessible(true);
		return (ConcurrentHashMap<String, Long>) field.get(sessionAll);
	}

	private static Config newTestConfig(int logServicePort, Path logDir) throws Exception {
		var config = new Config();
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		// LogServiceConf：注册 logName，服务端 NewSession 才能找到 logManager。
		var customize = doc.createElement("CustomizeConf");
		var logConfElem = doc.createElement("LogConf");
		logConfElem.setAttribute("LogActive", "zeze.log");
		logConfElem.setAttribute("LogDir", logDir.toString());
		customize.appendChild(logConfElem);
		config.getCustomizes().put("LogServiceConf", customize);

		// ServiceConf：LogService.Server 监听地址（构造尾部注册进 config）。
		var serviceConfElem = doc.createElement("ServiceConf");
		serviceConfElem.setAttribute("Name", "Zeze.LogService.Server");
		var acceptorElem = doc.createElement("Acceptor");
		acceptorElem.setAttribute("Ip", "127.0.0.1");
		acceptorElem.setAttribute("Port", String.valueOf(logServicePort));
		serviceConfElem.appendChild(acceptorElem);
		new ServiceConf(config, serviceConfElem);
		return config;
	}

	private static BCondition.Data newSearchCondition() {
		var cond = new BCondition.Data();
		cond.setBeginTime(-1);
		cond.setEndTime(-1);
		cond.setContainsType(BCondition.ContainsAll);
		cond.setWords(new ArrayList<>(List.of("nothing_will_match")));
		return cond;
	}

	// 尽力删除：留给系统临时目录清理，失败不干扰测试结果。
	private static void deleteBestEffort(Path dir) {
		try (var walk = Files.walk(dir)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (IOException e) {
					// ignore
				}
			});
		} catch (IOException e) {
			// ignore
		}
	}
}
