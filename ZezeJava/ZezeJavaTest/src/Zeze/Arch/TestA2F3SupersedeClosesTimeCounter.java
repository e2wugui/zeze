package Zeze.Arch;

import java.lang.reflect.Field;
import java.util.concurrent.Future;

import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A2-F3回归：setRelativeServiceReady的supersede分支只踢socket（真会话），
 * 本机合成会话（sessionId=0、无连接可踢）的TimeCounter每秒discard周期任务
 * 永无取消点——随模块数/SM重订阅无界累积泄漏。修复：替换前后对旧会话统一
 * old.timeCounter.close()（真会话随后OnSocketClose的close幂等，双关无害）。
 * 断言：同loadName新会话接管后，旧会话的discardTimer已取消、新会话的仍在运行。
 */
@Fast
public class TestA2F3SupersedeClosesTimeCounter {
	private static final String IP = "127.0.0.1";
	private static final int PORT = 45201;

	private Application app;
	private ProviderDirectService pds;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(1);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("a2f3_memory");
		conf.getDatabaseConfMap().put("", dbConf);
		app = ProviderDirectTestSupport.newAppWithFakeAgent("TestA2F3", conf);
		// 订阅状态为空：setRelativeServiceReady的订阅循环不执行，直达supersede逻辑。
		pds = new ProviderDirectService("a2f3pds", app);
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (app != null && app.getStartState() != Application.StartState.eStopped)
			app.stop();
	}

	/** 本机合成会话被接管：旧会话的TimeCounter周期任务必须在替换点被取消。 */
	@Test
	public void testSupersedeClosesOldSyntheticSessionTimer() {
		var s1 = pds.newSession(11); // 合成会话：sessionId=0，无连接可踢
		pds.setRelativeServiceReady(s1, IP, PORT);
		Assertions.assertFalse(isCancelled(s1),
				"测试前提：首次注册的会话TimeCounter周期任务在运行");

		var s2 = pds.newSession(11); // 同loadName接管
		pds.setRelativeServiceReady(s2, IP, PORT);

		Assertions.assertSame(s2, pds.providerByLoadName.get(s2.getServerLoadName()),
				"新会话必须接管loadName注册");
		Assertions.assertTrue(isCancelled(s1),
				"被替换的旧合成会话（无OnSocketClose清理点）必须在supersede点关闭TimeCounter（A2-F3）");
		Assertions.assertFalse(isCancelled(s2), "现任会话的TimeCounter必须仍在运行");
	}

	/** 未被替换的会话不受影响；不同loadName的注册不构成supersede。 */
	@Test
	public void testIndependentSessionUntouched() {
		var s1 = pds.newSession(12);
		pds.setRelativeServiceReady(s1, IP, PORT);
		var s2 = pds.newSession(13); // 不同ip/port：不同loadName
		pds.setRelativeServiceReady(s2, IP, PORT + 1);

		Assertions.assertFalse(isCancelled(s1), "不同loadName的注册不构成supersede，不得关闭");
		Assertions.assertFalse(isCancelled(s2), "新会话的TimeCounter在运行");
	}

	/** 重复注册同一会话实例（本机连接多次set）：不构成supersede，不误关自己的timer。 */
	@Test
	public void testSameSessionReRegisterUntouched() {
		var s1 = pds.newSession(14);
		pds.setRelativeServiceReady(s1, IP, PORT);
		pds.setRelativeServiceReady(s1, IP, PORT); // old == ps：幂等返回
		Assertions.assertFalse(isCancelled(s1), "同一会话重复setReady不得关闭自己的TimeCounter");
	}

	private static boolean isCancelled(ProviderSession session) {
		try {
			Field f = Zeze.Util.TimeCounter.class.getDeclaredField("discardTimer");
			f.setAccessible(true);
			var timer = f.get(session.timeCounter);
			return timer != null && ((Future<?>)timer).isCancelled();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
