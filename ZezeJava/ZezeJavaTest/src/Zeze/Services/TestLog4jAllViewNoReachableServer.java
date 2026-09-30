package Zeze.Services;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import Zeze.Config;
import Zeze.Net.ServiceConf;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.Task;
import Zeze.log.FileSessionManager;
import harness.Fast;

/**
 * 全服视图"注册表非空但日志服务器全部不可达"的 resolve 收口直测
 * （FND30 zokermanager-01）。
 * <p>
 * 起真 LogAgent（无 ServiceManager 配置，start 跳过订阅）+ applyOnChanged 模拟
 * SM 推送（自包含，对齐 TestLog4jSessionAllDegraded），注册表推两个拒连端口的
 * 死条目——注册表非空但全部不可达（进程刚死/分区/重启窗口，SM 租约未过期）。
 * 修复前：newSessionAll 构造器逐台跳过后返回 0 成员会话，resolve 空成功返回且
 * 绑定入库（快照=注册表键集），此后键集不漂移即恒复用空结果；修复后：按会话
 * 实际成员集校验，0 成员抛 IllegalStateException（消息含注册数与成员数）、
 * 不入库（毒化绑定不存在）。
 * <p>
 * SessionAll 直调层面维持"空集构造返回空结果"的既有降级语义
 * （TestLog4jSessionAllDegraded 覆盖），本校验只收 FileSessionManager.resolve。
 */
@Fast
public class TestLog4jAllViewNoReachableServer {
	/** 会话身份条件指纹样本（值任意，比对按值等价）。 */
	private static final String COND = "search|-1|-1|1|[error]|";

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testResolveRejectsAllUnreachableAndNotCached() throws Exception {
		int port;
		try (var ss = new ServerSocket(0)) {
			port = ss.getLocalPort();
		}
		var logDir = Files.createTempDirectory("zeze-log4j-allview-unreachable");
		// 本测试不起 LogService：两个死条目都指向必然拒绝连接的端口。
		var logAgent = new LogAgent(newTestConfig(port, logDir));
		try {
			logAgent.start();

			var deadA = "LogService_deada_127.0.0.1_1";
			var deadB = "LogService_deadb_127.0.0.1_2";
			var edit = new BEditService();
			edit.getAdd().add(new BServiceInfo("Zeze.LogService", deadA, 0, "127.0.0.1", 1));
			edit.getAdd().add(new BServiceInfo("Zeze.LogService", deadB, 0, "127.0.0.1", 2));
			logAgent.applyOnChanged(edit);
			assertTrue(logAgent.getLogServers().size() == 2, "摆盘：注册表非空（两个死条目）");

			// 源 IP 用独立网段，避免与并行车道的其他 FileSessionManager 直测互踩静态表。
			var client = new InetSocketAddress(InetAddress.getByName("10.200.30.40"), 45678);
			var ex = assertThrows(IllegalStateException.class, () -> FileSessionManager.resolve(
					logAgent, client, false, true, null, "zeze.log", COND),
					"注册表非空但全部不可达：resolve 必须显式失败而非返回 0 成员会话");
			assertTrue(ex.getMessage().contains("no reachable log server for all-servers view"),
					"错误须指向全部不可达：\n" + ex.getMessage());
			// 未入库即抛：毒化绑定不存在（修复前 0 成员会话入库，键集不漂移即恒复用空成功）。
			assertNull(FileSessionManager.get(client), "0 成员会话不得留下绑定缓存");
		} finally {
			logAgent.stop();
			deleteBestEffort(logDir);
		}
	}

	private static Config newTestConfig(int logServicePort, Path logDir) throws Exception {
		var config = new Config();
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		// LogServiceConf：注册 logName（对齐 TestLog4jSessionAllDegraded 的配置形态；本测试
		// 无服务端，节点只为让 Agent 侧 LogServiceConf 解析路径与生产一致）。
		var customize = doc.createElement("CustomizeConf");
		var logConfElem = doc.createElement("LogConf");
		logConfElem.setAttribute("LogActive", "zeze.log");
		logConfElem.setAttribute("LogDir", logDir.toString());
		customize.appendChild(logConfElem);
		config.getCustomizes().put("LogServiceConf", customize);

		// ServiceConf：LogService.Server 监听地址（构造尾部注册进 config，本测试不绑定）。
		var serviceConfElem = doc.createElement("ServiceConf");
		serviceConfElem.setAttribute("Name", "Zeze.LogService.Server");
		var acceptorElem = doc.createElement("Acceptor");
		acceptorElem.setAttribute("Ip", "127.0.0.1");
		acceptorElem.setAttribute("Port", String.valueOf(logServicePort));
		serviceConfElem.appendChild(acceptorElem);
		new ServiceConf(config, serviceConfElem);
		return config;
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
