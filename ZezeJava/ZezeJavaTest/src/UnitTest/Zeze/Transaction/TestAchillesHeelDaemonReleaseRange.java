package UnitTest.Zeze.Transaction;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Application;
import Zeze.Config;
import Zeze.Services.Daemon;
import Zeze.Transaction.AchillesHeelDaemon;
import Zeze.Transaction.GlobalAgentBase;

/**
 * FND2-T1-1: ProcessDaemon 收到 Release 命令后直接 agents[r.globalIndex] 取 agent，
 * 无 [0, agents.length) 范围校验；Release 的解码（Daemon.Release.decode）接受任意 int。
 * 本地任意进程向 ProcessDaemon 的 loopback UDP 端口发送一个命令号=3、globalIndex 越界的
 * 报文，AIOOBE 逃逸到外层 catch (Throwable) → halt(321321)，单包杀死整个进程。
 * 修复后：越界包记 error 丢弃，处理体整体 try-catch 兜底，线程必须持续存活。
 */
@Fast
public class TestAchillesHeelDaemonReleaseRange {
	private Application app;
	private AchillesHeelDaemon daemon;
	private DatagramSocket peer; // 假装daemon进程，接住Register（不消费，仅保证端口可达）
	private String oldPort;

	private void setup() throws Exception {
		peer = new DatagramSocket(0, InetAddress.getLoopbackAddress());
		oldPort = System.setProperty(Daemon.propertyNamePort, String.valueOf(peer.getLocalPort()));
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true); // 仅需要守护线程骨架，不需要数据库
		config.setServerId(7311);
		app = new Application("TestAchillesHeelDaemonReleaseRange", config);
		var agent = new GlobalAgentBase(app) {
			@Override
			protected void cancelPending() {
			}

			@Override
			public void keepAlive() {
			}

			@Override
			public void startRelease(Zeze.Application zeze, Runnable endAction) {
				// 测试App未start（checkpoint为null），真Releaser会在checkpointRun上NPE；
				// 本测试只验证daemon侧Release分支的防御，startRelease语义不属于这里。
			}
		};
		daemon = new AchillesHeelDaemon(app, new GlobalAgentBase[]{agent});
		daemon.start();
	}

	@AfterEach
	void tearDown() throws Exception {
		if (daemon != null)
			daemon.stopAndJoin();
		if (oldPort != null)
			System.setProperty(Daemon.propertyNamePort, oldPort);
		else
			System.clearProperty(Daemon.propertyNamePort);
		if (peer != null)
			peer.close();
	}

	@Test
	public void testReleaseOutOfRangePoisonPacket() throws Exception {
		setup();

		var pdField = AchillesHeelDaemon.class.getDeclaredField("pd");
		pdField.setAccessible(true);
		var pd = (Thread)pdField.get(daemon);
		Assertions.assertTrue(pd.isAlive());

		var sockField = pd.getClass().getDeclaredField("udpSocket");
		sockField.setAccessible(true);
		var pdPort = ((DatagramSocket)sockField.get(pd)).getLocalPort();
		var pdAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), pdPort);

		// 越界index：负数、>=agents.length(=1)、极值。未修复时任一个都触发AIOOBE→halt(321321)，
		// 测试进程直接死亡；修复后记error丢弃。receiveCommand的soTimeout=200ms，多等几轮确保全部处理。
		int[] poisons = {-1, 1, 100, Integer.MAX_VALUE, Integer.MIN_VALUE};
		try (var sender = new DatagramSocket()) {
			for (var index : poisons)
				Daemon.sendCommand(sender, pdAddress, new Daemon.Release(index));
		}
		for (int i = 0; i < 10; i++) {
			Thread.sleep(200);
			Assertions.assertTrue(pd.isAlive(), "ProcessDaemon 处理越界 Release 包后必须存活");
		}

		// 合法index（0）不被防御误伤：仍被正常处理（处理体走完后线程存活）。
		try (var sender = new DatagramSocket()) {
			Daemon.sendCommand(sender, pdAddress, new Daemon.Release(0));
		}
		for (int i = 0; i < 10; i++) {
			Thread.sleep(200);
			Assertions.assertTrue(pd.isAlive(), "ProcessDaemon 处理合法 Release 包后必须存活");
		}
	}
}
