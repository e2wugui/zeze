package harness;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import Zeze.Application;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class TestEnv {
	private static final @org.jetbrains.annotations.NotNull Logger logger = LogManager.getLogger(TestEnv.class);

	private TestEnv() {
	}

	/**
	 * 探测端口是否已有进程监听（本地、短超时）。
	 * 用于判断外部服务（ServiceManager/GlobalCacheManager）是否已手工启动。
	 */
	public static boolean portReachable(String host, int port) {
		try (var socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, port), 200);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 轮询等待 app 的 ServiceManager 订阅状态中出现全部给定 serverId 的服务注册。
	 * 替代“起完服务盲等固定时长”：provider 注册的订阅推送本身就是就绪信号，通常几百毫秒内满足，
	 * 慢机器上也不会 1 秒不够导致后续 RPC 超时连锁。identity 即 String.valueOf(serverId)。
	 * 带 60 秒超时兜底，超时抛异常并列出缺失项；轮询间隔 100ms。
	 * 使用 findServiceInfoByIdentity 而非 findNewestInfos：后者只取最高 version 桶，
	 * 混布版本集群里低版本服务器查不到。
	 */
	public static void waitServerRegistered(Application app, int... serverIds) throws InterruptedException {
		var missing = new HashSet<Integer>();
		for (var id : serverIds)
			missing.add(id);
		var deadline = System.currentTimeMillis() + 60_000;
		int polls = 0;
		while (!missing.isEmpty()) {
			Thread.sleep(100);
			for (var state : app.getServiceManager().getSubscribeStates().values()) {
				for (var it = missing.iterator(); it.hasNext(); ) {
					if (state.findServiceInfoByIdentity(String.valueOf(it.next())) != null)
						it.remove();
				}
				if (missing.isEmpty())
					break;
			}
			if (!missing.isEmpty()) {
				if (++polls % 50 == 0)
					logger.info("waitServerRegistered: missing={}", missing);
				if (System.currentTimeMillis() > deadline)
					throw new IllegalStateException("waitServerRegistered timeout(60s), missing=" + missing);
			}
		}
	}

	/**
	 * 连续区间便捷形式：等待 baseId..baseId+serverCount-1（含两端）全部注册，展开后委托给 varargs 版本。
	 * 注意不定名为重载：两个 int 的定参方法会静默捕获既有“两个离散 serverId”的 varargs 调用
	 * （如 ModuleRedirectRank 的 waitServerRegistered(zeze, 30, 31)），语义从“等 30/31”变成“等 30..60”。
	 */
	public static void waitServerRegisteredRange(Application app, int baseId, int serverCount) throws InterruptedException {
		if (serverCount < 1)
			throw new IllegalArgumentException("serverCount must >= 1: " + serverCount);
		var ids = new int[serverCount];
		for (int i = 0; i < serverCount; ++i)
			ids[i] = baseId + i;
		waitServerRegistered(app, ids);
	}
}
