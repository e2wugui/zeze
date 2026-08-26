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
	 * 慢机器上也不会出现 1 秒不够导致后续 RPC 超时连锁。identity 即 String.valueOf(serverId)。
	 * 带 60 秒超时兜底，超时抛异常并列出缺失项；轮询间隔 100ms。
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
				var infos = state.findNewestInfos();
				if (infos == null)
					continue;
				for (var it = missing.iterator(); it.hasNext(); ) {
					if (infos.findServiceInfoByIdentity(String.valueOf(it.next())) != null)
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
}
