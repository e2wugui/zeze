package Zeze.Services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import Zeze.Builtin.LogService.BCondition;
import Zeze.Config;
import Zeze.Net.ServiceConf;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.Task;
import harness.Fast;

/**
 * S3-5 回归：onSmRemoved 摘除清理 logServers 死条目 + SessionAll 构造对死条目降级。
 * <p>
 * 起真实 LogService（本地端口）与 LogAgent（无 ServiceManager 配置，start 跳过订阅），
 * 用 applyOnChanged 直接模拟 SM 推送，绕开对外部 SM 进程的依赖（自包含）。
 * 修复前：onSmRemoved 不清理 logServers 表项，摘除后 newSessionAll 遍历到死条目
 * GetReadySocket 抛异常导致构造必失败，且先建成的存活会话无人 close（泄漏服务端句柄）。
 */
@Fast
public class TestLog4jSessionAllDegraded {
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testSmRemovedAndSessionAllDegrade() throws Exception {
		int port;
		try (var ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		var logDir = Files.createTempDirectory("zeze-log4j-sessionall-test");
		// LogService 与 LogAgent 各用独立 Config：一个 ServiceConf 只能绑定一个 Service，共享会冲突。
		var logService = new LogService(newTestConfig(port, logDir));
		var logAgent = new LogAgent(newTestConfig(port, logDir));
		try {
			logService.start();
			logAgent.start();

			var realIdentity = "LogService_test_127.0.0.1_" + port;
			var deadIdentity = "LogService_dead_127.0.0.1_1"; // 指向必然拒绝连接的端口

			var edit = new BEditService();
			edit.getAdd().add(new BServiceInfo("Zeze.LogService", realIdentity, 0, "127.0.0.1", port));
			edit.getAdd().add(new BServiceInfo("Zeze.LogService", deadIdentity, 0, "127.0.0.1", 1));
			logAgent.applyOnChanged(edit);

			assertTrue(logAgent.getLogServers().contains(realIdentity));
			assertTrue(logAgent.getLogServers().contains(deadIdentity));

			// 等真实服务握手就绪；死条目永远连不上，不等待它。
			var deadline = System.currentTimeMillis() + 10_000;
			while (logAgent.__getLogServer(realIdentity).TryGetReadySocket() == null) {
				if (System.currentTimeMillis() > deadline)
					throw new IllegalStateException("等待真实 LogService 连接就绪超时");
				Thread.sleep(50);
			}

			// 含死条目时查全服：单台失败必须降级跳过，构造与查询仍成功；
			// 修复前遍历到死条目即抛异常，先建成的真实会话无人close。
			try (var sessionAll = logAgent.newSessionAll("zeze.log")) {
				assertNotNull(sessionAll.search(3, false, newSearchCondition()));
			}

			// 模拟 SM 摘除推送：onSmRemoved 必须把 logServers 表项一并移除（修复前死条目残留）。
			var remove = new BEditService();
			remove.getRemove().add(new BServiceInfo("Zeze.LogService", realIdentity, 0, "127.0.0.1", port));
			remove.getRemove().add(new BServiceInfo("Zeze.LogService", deadIdentity, 0, "127.0.0.1", 1));
			logAgent.applyOnChanged(remove);
			assertFalse(logAgent.getLogServers().contains(realIdentity), "onSmRemoved 必须清理 logServers 表项");
			assertFalse(logAgent.getLogServers().contains(deadIdentity), "onSmRemoved 必须清理 logServers 表项");
			assertTrue(logAgent.getLogServers().isEmpty());

			// 摘除后查全服仍可用（空集返回空结果，不再对死条目超时失败）。
			try (var sessionAll = logAgent.newSessionAll("zeze.log")) {
				var r = sessionAll.search(3, false, newSearchCondition());
				assertNotNull(r);
				assertTrue(r.getLogs().isEmpty());
			}
		} finally {
			try {
				logAgent.stop();
			} finally {
				logService.stop();
				deleteBestEffort(logDir);
			}
		}
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

	// 尽力删除：留給系统临时目录清理，失败不干扰测试结果。
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
