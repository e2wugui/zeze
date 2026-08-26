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
 * 在测试 JVM 内启动 ServiceManager(5001) 和 GlobalCacheManagerAsyncServer(5002)，
 * 取代手工运行 test/service & global.bat。通过 ServiceLoader 自动注册
 * （META-INF/services/org.junit.platform.launcher.LauncherSessionListener），
 * IDEA / gradle / ConsoleLauncher 三个入口统一生效。
 *
 * <ul>
 * <li>端口已被占用（比如手工 bat 已在跑）时不启动、直接复用，会话结束时也不负责关闭；</li>
 * <li>仅关闭自己启动的服务；</li>
 * <li>第二对服务(5011/5012)不在这里启动：GlobalCacheManagerAsyncServer 是进程内单例，
 *     同 JVM 起不了第二个，需要它的只有 Onz.TestOnz（用端口可达性 Assumption 跳过）；</li>
 * <li>gradle test（@Fast 自包含测试）不需要环境，由 build.gradle 设 -Dzeze.test.env=off 跳过。</li>
 * </ul>
 */
public class TestEnvLauncherListener implements LauncherSessionListener {
	public static final String ENV_OFF_PROPERTY = "zeze.test.env";
	public static final int SERVICE_MANAGER_PORT = 5001;
	public static final int GLOBAL_CACHE_MANAGER_PORT = 5002;

	private static final Logger logger = LogManager.getLogger(TestEnvLauncherListener.class);

	private static ServiceManagerServer serviceManager;
	private static boolean globalCacheManagerStarted;

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
		} catch (Exception e) {
			throw new IllegalStateException("启动测试环境失败：SM(127.0.0.1:" + SERVICE_MANAGER_PORT
					+ ")/GCM(127.0.0.1:" + GLOBAL_CACHE_MANAGER_PORT
					+ ")。如端口被无关进程占用，请释放端口或手工运行 test/service & global.bat 后重试", e);
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
		if (serviceManager != null) {
			var sm = serviceManager;
			serviceManager = null;
			try {
				sm.close();
			} catch (Exception e) {
				logger.error("close ServiceManagerServer failed", e);
			}
		}
	}
}
