package Zeze.Services;

import static java.util.Objects.requireNonNull;

import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Util.Task;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * ServiceManagerServer 停机（close/stop）时必须先置 closed 闸门再关闭客户端连接，
 * 最后取消并清空 suspectTiers：
 * 1. 停机期间不得因批量 OnSocketClose 广播虚假 Suspect；
 * 2. 停机后 suspectTiers 必须为空且既有升档定时器全部被取消（不泄漏 900s 定时器）。
 */
@Fast
public class TestServiceManagerShutdownSuspectCleanup {

	private static int probeDualStackPort(InetAddress loop) throws Exception {
		for (int attempt = 0; attempt < 64; ++attempt) {
			int port;
			try (var tcp = new ServerSocket(0, 1, loop)) {
				port = tcp.getLocalPort();
			}
			try (var udp = new DatagramSocket(new InetSocketAddress(loop, port));
					var tcp = new ServerSocket(port, 1, loop)) {
				return port;
			} catch (BindException ignored) {
			}
		}
		throw new IllegalStateException("failed to probe free dual-stack loopback port");
	}

	private static Agent newAgent(int port, int serverId, long appMainVersion) throws Exception {
		var conf = new Config();
		conf.setServerId(serverId);
		conf.setAppVersion(appMainVersion << 48);
		var agent = new Agent(conf);
		agent.getClient().getConfig().addConnector(new Connector("127.0.0.1", port));
		agent.start();
		agent.waitReady();
		return agent;
	}

	private static void waitIdentified(ServiceManagerServer sm, int... serverIds) throws Exception {
		var serverField = ServiceManagerServer.class.getDeclaredField("server");
		serverField.setAccessible(true);
		var netServer = (Zeze.Net.Service)requireNonNull(serverField.get(sm));
		var idField = ServiceManagerServer.Session.class.getDeclaredField("identifyServerId");
		idField.setAccessible(true);
		long deadline = System.currentTimeMillis() + 10_000;
		while (true) {
			var seen = new boolean[serverIds.length];
			netServer.foreach(so -> {
				if (so.getUserState() instanceof ServiceManagerServer.Session session) {
					int sid = idField.getInt(session);
					for (int i = 0; i < serverIds.length; ++i)
						if (sid == serverIds[i])
							seen[i] = true;
				}
			});
			boolean all = true;
			for (boolean b : seen)
				all &= b;
			if (all)
				return;
			Assertions.assertTrue(System.currentTimeMillis() < deadline, "timeout waiting Identify");
			Thread.sleep(20);
		}
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<Integer, Object> suspectTiersOf(ServiceManagerServer sm) throws Exception {
		var field = ServiceManagerServer.class.getDeclaredField("suspectTiers");
		field.setAccessible(true);
		return (ConcurrentHashMap<Integer, Object>)requireNonNull(field.get(sm));
	}

	private static void stopQuietly(@Nullable Agent... agents) {
		for (var a : agents) {
			if (a != null) {
				try {
					a.stop();
				} catch (Exception ignored) {
				}
			}
		}
	}

	private static Config configWithDbHome(Path tempDir) throws Exception {
		var conf = new Config();
		var doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		var el = doc.createElement("CustomizeConf");
		el.setAttribute("DbHome", tempDir.toString());
		conf.getCustomizes().put("Zeze.Services.ServiceManager", el);
		return conf;
	}

	@Test
	@Timeout(60)
	public void testCloseDoesNotBroadcastSuspectOrLeakTiers(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var loop = InetAddress.getLoopbackAddress();
		int port = probeDualStackPort(loop);
		var sm = new ServiceManagerServer(loop, port, configWithDbHome(tempDir), "autokeys");
		Agent a1 = null, a2 = null;
		try {
			a1 = newAgent(port, 101, 10);
			a2 = newAgent(port, 102, 10);
			waitIdentified(sm, 101, 102);

			var suspects = new ConcurrentLinkedQueue<Integer>();
			a1.setOnSuspect(suspects::add);
			a2.setOnSuspect(suspects::add);

			sm.close();
			Thread.sleep(200);

			Assertions.assertTrue(suspects.isEmpty(),
					"planned close must not broadcast Suspect, got=" + suspects);
			Assertions.assertTrue(suspectTiersOf(sm).isEmpty(),
					"suspectTiers must remain empty after close");
		} finally {
			stopQuietly(a1, a2);
			sm.close();
		}
	}

	@Test
	@Timeout(60)
	public void testStopCancelsExistingTierAndSuppressesShutdownSuspect(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var loop = InetAddress.getLoopbackAddress();
		int port = probeDualStackPort(loop);
		var sm = new ServiceManagerServer(loop, port, configWithDbHome(tempDir), "autokeys");
		Agent a1 = null, a2 = null, a3 = null;
		try {
			a1 = newAgent(port, 201, 10);
			a2 = newAgent(port, 202, 10);
			a3 = newAgent(port, 203, 10);
			waitIdentified(sm, 201, 202, 203);

			var preShutdownSuspect = new java.util.concurrent.LinkedBlockingQueue<Integer>();
			a1.setOnSuspect(preShutdownSuspect::add);

			// 先断开 a3 制造一条活跃的 900s 升档定时器
			a3.stop();
			a3 = null;
			Assertions.assertEquals(203, preShutdownSuspect.poll(10, TimeUnit.SECONDS),
					"disconnecting a3 before shutdown must create a suspect tier");

			var tiers = suspectTiersOf(sm);
			var existingTier = requireNonNull(tiers.get(203), "suspect tier for 203 must exist");
			var timerField = existingTier.getClass().getDeclaredField("timer");
			timerField.setAccessible(true);
			var existingTimer = (Future<?>)requireNonNull(timerField.get(existingTier));
			Assertions.assertFalse(existingTimer.isCancelled());

			var shutdownSuspects = new ConcurrentLinkedQueue<Integer>();
			a1.setOnSuspect(shutdownSuspects::add);
			a2.setOnSuspect(shutdownSuspects::add);

			// 直接调用 stop()（非 close()）同样必须取消既有定时器且不得在停机断连时重插/广播 Suspect
			sm.stop();
			Thread.sleep(200);

			Assertions.assertTrue(existingTimer.isCancelled(), "existing tier timer must be cancelled on stop()");
			Assertions.assertTrue(tiers.isEmpty(), "suspectTiers must be empty after stop()");
			Assertions.assertTrue(shutdownSuspects.isEmpty(),
					"stop() must not broadcast Suspect for closing clients, got=" + shutdownSuspects);
		} finally {
			stopQuietly(a1, a2, a3);
			sm.close();
		}
	}
}
