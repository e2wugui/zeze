package Zeze.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Net.ServiceConf;
import Zeze.Services.Log4jQuery.SessionAll;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.Task;

import harness.Fast;

/**
 * 全服视图会话成员集的减方向：注册表摘除（退服/缩容/SM remove通告）的成员必须在下一次
 * operate逐出会话。修复前alls只增不减——摘除成员的Connector已stop（onSmRemoved），
 * 每轮operate对其GetReadySocket立即失败、恒定warn，聚合结果持续缺台且以remain=false
 * 收尾；唯一成员形态下operate恒抛异常，该logName查询永久不可用直至调用方重建会话。
 * 逐出后成员回归（重部署/注册表抖动）由缺册补员重新入会，不叠加缓冲窗。
 * <p>
 * 真 LogService + 真 LogAgent + applyOnChanged 模拟 SM 推送（对齐
 * TestLog4jAllViewMemberReconcile）。
 */
@Fast
public class TestEvictsDeregisteredMember {
	private static final String LogName = "zeze.log";
	private static final LocalDateTime Base = LocalDateTime.of(2026, 9, 28, 10, 0);

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/** 摘除成员下一operate逐出（不再对其发起RPC）；回归后由缺册补员重新入会。 */
	@Test
	public void testDeregisteredMemberEvictedThenRejoins() throws Exception {
		int portA;
		int portB;
		try (var ssA = new ServerSocket(0); var ssB = new ServerSocket(0)) {
			portA = ssA.getLocalPort();
			portB = ssB.getLocalPort();
		}
		var dirA = Files.createTempDirectory("zeze-log4j-evict-a");
		var dirB = Files.createTempDirectory("zeze-log4j-evict-b");
		writeLogs(dirA, "a-", 19);
		writeLogs(dirB, "b-", 19);
		var serviceA = new LogService(newTestConfig(portA, dirA));
		var serviceB = new LogService(newTestConfig(portB, dirB));
		var logAgent = new LogAgent(newTestConfig(portA, dirA));
		try {
			serviceA.start();
			serviceB.start();
			logAgent.start();

			var idA = "LogService_a_127.0.0.1_" + portA;
			var idB = "LogService_b_127.0.0.1_" + portB;
			push(logAgent, idA, portA);
			push(logAgent, idB, portB);
			waitReady(logAgent, idA);
			waitReady(logAgent, idB);

			try (var sessionAll = logAgent.newSessionAll(LogName)) {
				assertEquals(java.util.Set.of(idA, idB), sessionAll.memberNames());
				assertNotNull(sessionAll.search(5, false, newSearchCondition()), "摘除前两台正常投递");

				// B退服：SM remove通告——Client.onSmRemoved停掉Connector并移出注册表。
				var remove = new BEditService();
				remove.getRemove().add(new BServiceInfo("Zeze.LogService", idB, 0, "127.0.0.1", portB));
				logAgent.applyOnChanged(remove);

				// 下一次operate必须逐出B（修复前B永久滞留alls，每轮恒失败恒warn）。
				assertNotNull(sessionAll.search(5, false, newSearchCondition()));
				assertEquals(java.util.Set.of(idA), sessionAll.memberNames(),
						"注册表已摘除的成员必须在下一operate逐出（修复前永久滞留）");

				// B回归（重部署）：缺册补员重新入会，查询恢复全服视图。
				push(logAgent, idB, portB);
				waitReady(logAgent, idB);
				assertNotNull(sessionAll.search(5, false, newSearchCondition()));
				assertTrue(sessionAll.memberNames().contains(idB), "回归成员必须由缺册补员重新入会");
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
	 * 唯一成员退服：修复前operate恒抛（rs空+firstFailure上抛），该logName查询永久不可用；
	 * 修复后逐出成员返回空结果（注册表空的"无查询目标"语义），成员回归后自愈恢复投递。
	 */
	@Test
	public void testUniqueMemberEvictionRecoversAfterRejoin() throws Exception {
		int portB;
		try (var ssB = new ServerSocket(0)) {
			portB = ssB.getLocalPort();
		}
		var dirB = Files.createTempDirectory("zeze-log4j-evict-only");
		writeLogs(dirB, "b-", 19);
		var serviceB = new LogService(newTestConfig(portB, dirB));
		var logAgent = new LogAgent(newTestConfig(portB, dirB));
		try {
			serviceB.start();
			logAgent.start();

			var idB = "LogService_b_127.0.0.1_" + portB;
			push(logAgent, idB, portB);
			waitReady(logAgent, idB);

			try (var sessionAll = logAgent.newSessionAll(LogName)) {
				assertEquals(java.util.Set.of(idB), sessionAll.memberNames());
				assertNotNull(sessionAll.search(5, false, newSearchCondition()));

				var remove = new BEditService();
				remove.getRemove().add(new BServiceInfo("Zeze.LogService", idB, 0, "127.0.0.1", portB));
				logAgent.applyOnChanged(remove);

				// 修复前：唯一成员失败+rs空→operate上抛，永久失败；修复后逐出返回空结果不抛。
				assertNotNull(sessionAll.search(5, false, newSearchCondition()),
						"唯一成员退服后不得恒抛（修复前rs空上抛firstFailure）");
				assertEquals(java.util.Set.of(), sessionAll.memberNames(), "退服成员逐出后会话为空");

				// 回归自愈：补员入会，查询恢复。
				push(logAgent, idB, portB);
				waitReady(logAgent, idB);
				assertNotNull(sessionAll.search(5, false, newSearchCondition()));
				assertEquals(java.util.Set.of(idB), sessionAll.memberNames(), "回归后必须恢复成员");
			}
		} finally {
			try {
				logAgent.stop();
			} finally {
				serviceB.stop();
				deleteBestEffort(dirB);
			}
		}
	}

	private static void writeLogs(Path dir, String tag, int count) throws IOException {
		var fmt = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
		var sb = new StringBuilder();
		for (var i = 0; i < count; ++i)
			sb.append(Base.plusSeconds(10L * i).format(fmt)).append(' ')
					.append("marker ").append(tag).append(i).append('\n');
		AtomicFileWriter.replace(dir.resolve(LogName), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	private static void push(LogAgent logAgent, String identity, int port) {
		var edit = new BEditService();
		edit.getAdd().add(new BServiceInfo("Zeze.LogService", identity, 0, "127.0.0.1", port));
		logAgent.applyOnChanged(edit);
	}

	private static void waitReady(LogAgent logAgent, String identity) throws InterruptedException {
		var deadline = System.currentTimeMillis() + 30_000;
		while (logAgent.__getLogServer(identity) == null || logAgent.__getLogServer(identity).TryGetReadySocket() == null) {
			if (System.currentTimeMillis() > deadline)
				throw new IllegalStateException("等待 LogService 连接就绪超时: " + identity);
			Thread.sleep(50);
		}
	}

	private static Zeze.Builtin.LogService.BCondition.Data newSearchCondition() {
		var cond = new Zeze.Builtin.LogService.BCondition.Data();
		cond.setBeginTime(Base.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
		cond.setEndTime(-1);
		cond.setContainsType(Zeze.Builtin.LogService.BCondition.ContainsAll);
		cond.setWords(new java.util.ArrayList<>(java.util.List.of("marker")));
		return cond;
	}

	private static Config newTestConfig(int logServicePort, Path logDir) throws Exception {
		var config = new Config();
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		var customize = doc.createElement("CustomizeConf");
		var logConfElem = doc.createElement("LogConf");
		logConfElem.setAttribute("LogActive", LogName);
		logConfElem.setAttribute("LogDir", logDir.toString());
		customize.appendChild(logConfElem);
		config.getCustomizes().put("LogServiceConf", customize);

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
