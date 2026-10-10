package harness;

import java.net.InetSocketAddress;
import java.net.Socket;
import Zeze.Config;
import Zeze.Services.GlobalCacheManagerAsyncServer;
import Zeze.Services.ServiceManagerServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * v1.6.3 回植（bench/backport-v1.6.3 分支专用）：在 benchCore JVM 内自动启动测试环境
 * ——ServiceManager(5001) + GlobalCacheManagerAsyncServer(5002)。
 * 机制：Jupiter 扩展自动发现（META-INF/services/org.junit.jupiter.api.extension.Extension
 * + build.gradle 设 junit.jupiter.extensions.autodetection.enabled=true），beforeAll 幂等启动。
 * 2.x master 用的是 LauncherSessionListener（gradle 9.7 集成下 ServiceLoader 发现正常），
 * 本分支 gradle 8.14 实证发现不了 LauncherSessionListener，改用 Jupiter 原生机制等价替代。
 * 端口被占用即复用不重建；关闭走 JVM shutdown hook。
 */
public class TestEnvExtension implements BeforeAllCallback {
	public static final int SERVICE_MANAGER_PORT = 5001;
	public static final int GLOBAL_CACHE_MANAGER_PORT = 5002;

	private static final Logger logger = LogManager.getLogger(TestEnvExtension.class);

	private static ServiceManagerServer serviceManager;
	private static GlobalCacheManagerAsyncServer globalCacheManager;
	private static boolean envStarted;

	@Override
	public void beforeAll(ExtensionContext context) {
		ensureStarted();
	}

	public static synchronized void ensureStarted() {
		if (envStarted)
			return;
		envStarted = true;
		try {
			Zeze.Util.Task.tryInitThreadPool(); // v1.6.3 起 SM 前 Thread 池必须先就绪（PerfCounter.init 走 scheduled 池）
			if (!portReachable("127.0.0.1", SERVICE_MANAGER_PORT))
				serviceManager = new ServiceManagerServer(null, SERVICE_MANAGER_PORT, Config.load());
			if (!portReachable("127.0.0.1", GLOBAL_CACHE_MANAGER_PORT)) {
				globalCacheManager = GlobalCacheManagerAsyncServer.getInstance(); // v1.6.3 单例（无参构造 private）
				globalCacheManager.start(null, GLOBAL_CACHE_MANAGER_PORT, null);
			}
		} catch (Exception e) {
			throw new IllegalStateException("启动 v1.6.3 基准环境失败：SM(5001)/GCM(5002)。"
					+ "如端口被无关进程占用，请释放端口后重试", e);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(TestEnvExtension::stopEnv, "v163-bench-env-stop"));
	}

	private static void stopEnv() {
		if (globalCacheManager != null) {
			try {
				globalCacheManager.stop();
			} catch (Exception e) {
				logger.error("close GlobalCacheManagerAsyncServer failed", e);
			}
		}
		if (serviceManager != null) {
			try {
				serviceManager.close();
			} catch (Exception e) {
				logger.error("close ServiceManagerServer failed", e);
			}
		}
	}

	private static boolean portReachable(String host, int port) {
		try (var socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, port), 200);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
