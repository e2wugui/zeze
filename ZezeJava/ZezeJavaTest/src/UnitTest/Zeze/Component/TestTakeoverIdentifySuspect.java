package UnitTest.Zeze.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManagerServer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 步骤②提示通道回归：Agent连接时上报Identify（SM把serverId记在会话上），
 * 断线时SM立即向其余会话广播Suspect（不再走旧的600s延迟通知）。
 * 进程内SM（动态空闲端口+独立autokeys目录），两个Agent（serverId=11/12）。
 */
@Fast
public class TestTakeoverIdentifySuspect {

	@Test
	public void testSuspectBroadcastOnClose() throws Exception {
		// 本类单跑（--tests过滤）时没有其他类先行初始化全局线程池：SM构造里ZezeCounter.tryInit
		// →PerfCounter调度直接抛"scheduled pool is null"。以前多类同跑被别人的tryInitThreadPool
		// 掩护，从未单跑暴露过。
		Task.tryInitThreadPool();
		// 固定端口（@Fast固定端口独占契约）：SM要在同端口号绑TCP(Acceptor)+UDP(Id128UdpServer)。
		// 曾用动态探测（TCP先选UDP后验/UDP先选TCP后验两版）都在负载下32连败：WinNAT/Hyper-V
		// 按协议分别保留动态段内的大块端口（块长可上百），跟随任一分配器的游标走进对侧协议的
		// 保留块即连续失败。26xxx段在动态端口范围之外（see TestMQ的选段注释），本测试用26110
		// （已避开TestMQ系26000-26003、26100-26102）。
		final int port = 26110;
		// autokeys目录放在已被gitignore的autokeys/下，避免污染仓库；RocksDB需要父目录存在。
		Files.createDirectories(Path.of("autokeys"));

		var sm = new ServiceManagerServer(null, port, new Config(), "autokeys/takeover-e2e");
		Agent agent1 = null;
		Agent agent2 = null;
		try {
			agent1 = newAgent(port, 11);
			agent2 = newAgent(port, 12);
			agent1.start();
			agent2.start();
			waitReadyWithRetry(agent1);
			waitReadyWithRetry(agent2);

			var suspected = new LinkedBlockingQueue<Integer>();
			agent2.setOnSuspect(suspected::add);

			// 确定性等Identify在SM侧生效：直接轮询SM会话的identifyServerId==11。
			// 旧同步点editService往返不足：Connector.WaitReady只等TCP连接，客户端Identify要等
			// 握手末包到达才由onConnected发出，紧随waitReady的editService可在线上先于Identify
			// 到达（30轮压测2026-09-19轮11实证：SM处理了空edit并应答Success，但断线RST掐掉了
			// 在途Identify，会话identifyServerId=-1，onClose按契约不广播Suspect）。
			var serverField = ServiceManagerServer.class.getDeclaredField("server");
			serverField.setAccessible(true);
			var netServer = serverField.get(sm);
			var idField = ServiceManagerServer.Session.class.getDeclaredField("identifyServerId");
			idField.setAccessible(true);
			var idSeen = new boolean[1];
			var deadline = System.currentTimeMillis() + 10_000;
			while (!idSeen[0]) {
				Assertions.assertTrue(System.currentTimeMillis() < deadline, "10s内Identify未在SM侧生效");
				//noinspection BusyWait
				Thread.sleep(10);
				((Zeze.Net.Service)netServer).foreach(so -> {
					if (so.getUserState() instanceof ServiceManagerServer.Session session
							&& (int)idField.get(session) == 11)
						idSeen[0] = true;
				});
			}

			// agent1正常关闭：连接断开→SM onClose→Suspect(serverId=11)广播→agent2回调。
			agent1.stop();
			agent1 = null;

			// 30s：单向异步通知无确定性同步点可用，预算必须容忍满负载派发延迟尖峰
			//（5s版在30轮压测轮1复发——套件最冷启动时刻的链路尖峰，非顺序竞态：
			// editService同步点已保证serverId先于断线记入会话）。AcquireKick 10→30s同判例。
			var got = suspected.poll(30, TimeUnit.SECONDS);
			Assertions.assertNotNull(got, "断线后应收到Suspect广播");
			Assertions.assertEquals(11, got, "Suspect应携带Identify上报的serverId");
		} finally {
			if (agent1 != null)
				agent1.stop();
			if (agent2 != null)
				agent2.stop();
			sm.close();
		}
	}

	// WaitReady固定5s预算在满负载下不足（test30-3 round17实证5.2s超时抛TimeoutException）：
	// 有界重试（同第八轮SessionCloseRetry/Peer.connect先例）；连接器自动重连，重试安全。
	// 超时经Task.forceThrow sneaky-throw受检TimeoutException，编译期不可见，只能catch Exception再判型。
	private static void waitReadyWithRetry(Agent agent) throws Exception {
		for (int attempt = 1; ; ++attempt) {
			try {
				agent.waitReady();
				return;
			} catch (Exception e) {
				if (!(e instanceof java.util.concurrent.TimeoutException) || attempt >= 6)
					throw e;
				//noinspection BusyWait
				Thread.sleep(200);
			}
		}
	}

	private static Agent newAgent(int port, int serverId) throws Exception {
		var conf = new Config();
		conf.setServerId(serverId);
		var agent = new Agent(conf);
		agent.getClient().getConfig().addConnector(new Connector("127.0.0.1", port));
		return agent;
	}
}
