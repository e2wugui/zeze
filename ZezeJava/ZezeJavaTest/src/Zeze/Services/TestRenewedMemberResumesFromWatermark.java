package Zeze.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Builtin.LogService.BCondition;
import Zeze.Builtin.LogService.BResult;
import Zeze.Config;
import Zeze.Net.ServiceConf;
import Zeze.Services.Log4jQuery.SessionAll;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.Task;

import harness.Fast;

/**
 * 会话级死亡成员（服务端闲置回收/进程重启致sid失效）重建后的续投递契约：新服务端会话
 * 必须从该成员已投递水位续扫，不得从查询下界整段重扫——死亡前已通过此前各页返回的日志
 * 跨页重复投递（SessionAll.merge原样归并无去重），聚合流出现同一台服务器的重复日志块，
 * 违反模块反复承诺的"不丢不重"。
 * <p>
 * 真 LogService + 真 LogAgent + applyOnChanged 模拟 SM 推送（对齐
 * TestLog4jAllViewMemberReconcile）；成员死亡用服务端惰性清理的生触路径：
 * SessionIdleTimeoutMillis=2s，闲置后对该连接发起一次 NewSession（此处为丢弃用的
 * scratch会话）即触发 cleanIdleLogSessions 回收原会话，下一页 getLogSession==null
 * 返回会话级错误，SessionAll 归入 deadMembers 重建。
 */
@Fast
public class TestRenewedMemberResumesFromWatermark {
	private static final String LogName = "zeze.log";
	private static final LocalDateTime Base = LocalDateTime.of(2026, 9, 28, 10, 0);

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testRenewedMemberContinuesFromDeliveredWatermark() throws Exception {
		int portA;
		int portB;
		try (var ssA = new ServerSocket(0); var ssB = new ServerSocket(0)) {
			portA = ssA.getLocalPort();
			portB = ssB.getLocalPort();
		}
		var dirA = Files.createTempDirectory("zeze-log4j-renew-a");
		var dirB = Files.createTempDirectory("zeze-log4j-renew-b");
		// A写19条（翻不完），B写10条：每条间隔10s（与索引10s建点对齐，seek定位精确）。
		writeLogs(dirA, "a-", 19);
		writeLogs(dirB, "b-", 10);
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
				var condition = newSearchCondition();
				// 第1页：A投递a-0..a-4，B投递b-0..b-4（B已投递水位=b-4的时间）。
				var page1 = sessionAll.search(5, false, condition);
				assertEquals(10, page1.getLogs().size(), "两成员各投递一页");
				assertEquals(5, countTag(page1, "a-"));
				assertEquals(5, countTag(page1, "b-"));

				// B的服务端会话闲置超时被惰性清理：休眠超阈值后对B连接发一次NewSession触发回收。
				Thread.sleep(3_000);
				try (var scratch = logAgent.newSession(idB, LogName)) {
					// scratch仅为触发清理，立即关闭。
				}

				// 第2页：B原sid会话级错误→死亡成员→立即重建（本页按降级语义缺B）；
				// A正常续投a-5..a-9。
				var page2 = sessionAll.search(5, false, condition);
				assertEquals(5, page2.getLogs().size(), "本页按降级语义缺B，A照常投递");
				assertEquals(5, countTag(page2, "a-"));

				// 第3页：重建的B会话必须从已投递水位续扫——返回b-4..b-8（b-4为水位边界同时间
				// 条的少量重复），不得从查询下界重扫整段返回b-0..b-4（b-0..b-3跨页重复投递）。
				var page3 = sessionAll.search(5, false, condition);
				assertEquals(10, page3.getLogs().size(), "重建后恢复全服视图");
				assertEquals(5, countTag(page3, "a-"));
				var bLogs = countTag(page3, "b-");
				assertEquals(5, bLogs, "B续投一页");
				assertFalse(hasTag(page3, "b-0"), "已投递的b-0不得重复投递（修复前从查询下界整段重扫）");
				assertFalse(hasTag(page3, "b-1"), "已投递的b-1不得重复投递");
				assertFalse(hasTag(page3, "b-2"), "已投递的b-2不得重复投递");
				assertFalse(hasTag(page3, "b-3"), "已投递的b-3不得重复投递");
				assertTrue(hasTag(page3, "b-8"), "必须从水位续扫推进到后续数据（b-4..b-8）");
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

	private static void writeLogs(Path dir, String tag, int count) throws IOException {
		var fmt = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
		var sb = new StringBuilder();
		for (var i = 0; i < count; ++i)
			sb.append(Base.plusSeconds(10L * i).format(fmt)).append(' ')
					.append("marker ").append(tag).append(i).append('\n');
		AtomicFileWriter.replace(dir.resolve(LogName), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	private static int countTag(BResult.Data result, String tag) {
		var n = 0;
		for (var log : result.getLogs())
			if (log.getLog().contains(tag))
				++n;
		return n;
	}

	private static boolean hasTag(BResult.Data result, String tag) {
		return countTag(result, tag) > 0;
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

	private static BCondition.Data newSearchCondition() {
		var cond = new BCondition.Data();
		cond.setBeginTime(millis(Base));
		cond.setEndTime(-1);
		cond.setContainsType(BCondition.ContainsAll);
		cond.setWords(new ArrayList<>(List.of("marker")));
		return cond;
	}

	private static long millis(LocalDateTime time) {
		return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	private static Config newTestConfig(int logServicePort, Path logDir) throws Exception {
		var config = new Config();
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		var customize = doc.createElement("CustomizeConf");
		// 2s闲置即回收：休眠+一次NewSession即确定性地触发服务端惰性清理（成员死亡的生触路径）。
		customize.setAttribute("SessionIdleTimeoutMillis", "2000");
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
