package harness;

import java.util.HashSet;
import Zeze.Config;
import Zeze.Services.GlobalCacheManagerAsyncServer;
import Zeze.Services.ServiceManagerServer;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * 在测试 JVM 内自动启动两对基础服务（本 listener 即启动机制，无需手工运行任何外部进程）。通过 ServiceLoader 自动注册
 * （META-INF/services/org.junit.platform.launcher.LauncherSessionListener），
 * IDEA / gradle / ConsoleLauncher 三个入口统一生效。
 *
 * <ul>
 * <li>懒启动：会话开启时不启动（此时还不知道要跑哪些测试），注册 TestExecutionListener，
 *     等测试计划就绪（testPlanExecutionStarted，先于任何用例执行）后按标签决定——
 *     计划内存在非 fast 标签的测试才启动环境；纯 @Fast 计划（如 IDEA 单独运行单元测试）零环境开销；</li>
 * <li>第一对：ServiceManager(5001) + GlobalCacheManagerAsyncServer(5002)；</li>
 * <li>第二对：ServiceManager(5011, 独立 autokeys 目录) + GlobalCacheManagerAsyncServer(5012)，
 *     仅 Onz.TestOnz 需要（两个独立 zeze 集群）；GCM 支持多实例，与第一对互不影响；</li>
 * <li>端口已被占用（比如已有外部启动的同端口服务在跑）时不启动、直接复用，会话结束时也不负责关闭；</li>
 * <li>仅关闭自己启动的服务；同一 JVM 内多次执行测试计划只启动一次；</li>
 * <li>@Bench 的 A/B/C 事务场景同样依赖 SM/Global（见 build.gradle bench 任务），故免环境只认 fast 标签；</li>
 * <li>gradle test（@Fast 自包含测试）由 build.gradle 设 -Dzeze.test.env=off 显式跳过；
 *     懒启动机制下该属性退化为强制逃生门（设了就永不启动，与计划内容无关）。</li>
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
	private static boolean envStarted;

	@Override
	public void launcherSessionOpened(LauncherSession session) {
		if ("off".equalsIgnoreCase(System.getProperty(ENV_OFF_PROPERTY)))
			return;
		session.getLauncher().registerTestExecutionListeners(new TestExecutionListener() {
			@Override
			public void testPlanExecutionStarted(TestPlan testPlan) {
				if (!envStarted && hasNonFastTest(testPlan))
					startEnv();
			}
		});
	}

	// @Fast 的契约是自包含（不依赖 SM/GCM/外部数据库）；fast 之外的测试（含 @Bench）按约定需要环境。
	// 按 isTest() 粒度判断：类级标签会落到每个方法标识上，非 fast 类里的 fast 方法不会误触发。
	private static boolean hasNonFastTest(TestPlan testPlan) {
		var pending = testPlan.getRoots();
		while (!pending.isEmpty()) {
			var next = new HashSet<TestIdentifier>();
			for (var id : pending) {
				if (id.isTest() && id.getTags().stream().noneMatch(tag -> "fast".equals(tag.getName())))
					return true;
				next.addAll(testPlan.getChildren(id));
			}
			pending = next;
		}
		return false;
	}

	private static synchronized void startEnv() {
		if (envStarted)
			return;
		envStarted = true;
		try {
			Task.tryInitThreadPool();
			if (!TestEnv.portReachable("127.0.0.1", SERVICE_MANAGER_PORT)) {
				// 与 ServiceManagerServer.main 无参默认一致（Conf 由构造函数内部 parseCustomize，无需在此解析）
				var config = Config.load();
				serviceManager = new ServiceManagerServer(null, SERVICE_MANAGER_PORT, config);
			}
			if (!TestEnv.portReachable("127.0.0.1", GLOBAL_CACHE_MANAGER_PORT)) {
				GlobalCacheManagerAsyncServer.getInstance().start(null, GLOBAL_CACHE_MANAGER_PORT, null);
				globalCacheManagerStarted = true;
			}
			// 第二对（独立集群，仅 Onz.TestOnz 需要，同样由本 listener 自动启动）：
			// autokeys 目录必须与第一对分开（RocksDB 目录锁），故用 "autokeys2"。
			if (!TestEnv.portReachable("127.0.0.1", SERVICE_MANAGER_PORT_2)) {
				var config = Config.load();
				serviceManager2 = new ServiceManagerServer(null, SERVICE_MANAGER_PORT_2, config, "autokeys2");
			}
			if (!TestEnv.portReachable("127.0.0.1", GLOBAL_CACHE_MANAGER_PORT_2)) {
				globalCacheManager2 = new GlobalCacheManagerAsyncServer();
				globalCacheManager2.start(null, GLOBAL_CACHE_MANAGER_PORT_2, null);
				globalCacheManager2Started = true;
			}
		} catch (Exception e) {
			throw new IllegalStateException("启动测试环境失败：SM(5001)/GCM(5002) 或第二对 SM(5011)/GCM(5012)"
					+ "。如端口被无关进程占用，请释放端口后重试（环境由本 listener 自动启动，无需手工运行外部服务）", e);
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
