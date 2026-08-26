package harness;

import Zeze.Config;
import Zeze.Services.GlobalCacheManagerAsyncServer;
import Zeze.Services.ServiceManagerServer;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * 在测试 JVM 内启动两对基础服务，取代手工运行 test/*.bat。通过 ServiceLoader 自动注册
 * （META-INF/services/org.junit.platform.launcher.LauncherSessionListener），
 * IDEA / gradle / ConsoleLauncher 三个入口统一生效。
 *
 * <ul>
 * <li>第一对：ServiceManager(5001) + GlobalCacheManagerAsyncServer(5002)；</li>
 * <li>第二对：ServiceManager(5011, 独立 autokeys 目录) + GlobalCacheManagerAsyncServer(5012)，
 *     仅 Onz.TestOnz 需要（两个独立 zeze 集群）；GCM 支持多实例，与第一对互不影响；</li>
 * <li>端口已被占用（比如手工 bat 已在跑）时不启动、直接复用，会话结束时也不负责关闭；</li>
 * <li>仅关闭自己启动的服务；</li>
 * <li>gradle test（@Fast 自包含测试）不需要环境，由 build.gradle 设 -Dzeze.test.env=off 跳过。</li>
 * </ul>
 */
public class TestEnvLauncherListener implements LauncherSessionListener {
	public static final String ENV_OFF_PROPERTY = "zeze.test.env";
	public static final int SERVICE_MANAGER_PORT = 5001;
	public static final int GLOBAL_CACHE_MANAGER_PORT = 5002;
	public static final int SERVICE_MANAGER_PORT_2 = 5011;
	public static final int GLOBAL_CACHE_MANAGER_PORT_2 = 5012;

	private static final Logger logger = LogManager.getLogger(TestEnvLauncherListener.class);

	private static ServiceManagerServer serviceManager;
	private static boolean globalCacheManagerStarted;
	private static ServiceManagerServer serviceManager2;
	private static GlobalCacheManagerAsyncServer globalCacheManager2;
	private static boolean globalCacheManager2Started;

	@Override
	public void launcherSessionOpened(LauncherSession session) {
		if ("off".equalsIgnoreCase(System.getProperty(ENV_OFF_PROPERTY)))
			return;
		try {
			Task.tryInitThreadPool();
			if (!TestEnv.portReachable("127.0.0.1", SERVICE_MANAGER_PORT)) {
				// 与 ServiceManagerServer.main 无参默认一致
				var conf = new ServiceManagerServer.Conf();
				var config = Config.load();
				config.parseCustomize(conf);
				serviceManager = new ServiceManagerServer(null, SERVICE_MANAGER_PORT, config);
			}
			if (!TestEnv.portReachable("127.0.0.1", GLOBAL_CACHE_MANAGER_PORT)) {
				GlobalCacheManagerAsyncServer.getInstance().start(null, GLOBAL_CACHE_MANAGER_PORT, null);
				globalCacheManagerStarted = true;
			}
			// 第二对（与 test/service & global.another.bat 等价）：
			// autokeys 目录必须与第一对分开（RocksDB 目录锁），故用 "autokeys2"。
			if (!TestEnv.portReachable("127.0.0.1", SERVICE_MANAGER_PORT_2)) {
				var conf = new ServiceManagerServer.Conf();
				var config = Config.load();
				config.parseCustomize(conf);
				serviceManager2 = new ServiceManagerServer(null, SERVICE_MANAGER_PORT_2, config, "autokeys2");
			}
			if (!TestEnv.portReachable("127.0.0.1", GLOBAL_CACHE_MANAGER_PORT_2)) {
				globalCacheManager2 = new GlobalCacheManagerAsyncServer();
				globalCacheManager2.start(null, GLOBAL_CACHE_MANAGER_PORT_2, null);
				globalCacheManager2Started = true;
			}
		} catch (Exception e) {
			throw new IllegalStateException("启动测试环境失败：SM(5001)/GCM(5002) 或第二对 SM(5011)/GCM(5012)"
					+ "。如端口被无关进程占用，请释放端口或手工运行 test/service & global.bat 后重试", e);
		}
	}

	@Override
	public void launcherSessionClosed(LauncherSession session) {
		if (globalCacheManagerStarted) {
			globalCacheManagerStarted = false;
			try {
				GlobalCacheManagerAsyncServer.getInstance().stop();
			} catch (Exception e) {
				logger.error("close GlobalCacheManagerAsyncServer failed", e);
			}
		}
		if (globalCacheManager2Started) {
			globalCacheManager2Started = false;
			try {
				globalCacheManager2.stop();
			} catch (Exception e) {
				logger.error("close GlobalCacheManagerAsyncServer(5012) failed", e);
			}
			globalCacheManager2 = null;
		}
		if (serviceManager != null) {
			var sm = serviceManager;
			serviceManager = null;
			try {
				sm.close();
			} catch (Exception e) {
				logger.error("close ServiceManagerServer failed", e);
			}
		}
		if (serviceManager2 != null) {
			var sm = serviceManager2;
			serviceManager2 = null;
			try {
				sm.close();
			} catch (Exception e) {
				logger.error("close ServiceManagerServer(5011) failed", e);
			}
		}
	}
}
