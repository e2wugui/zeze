package Zeze.Arch;

import java.lang.reflect.Field;

import Zeze.Application;
import Zeze.Config;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A2-F1回归：setRelativeServiceReady用ps.appVersion查版本桶——direct会话的appVersion
 * 从未被设置（仅linkd侧announce路径写入），永远为0；配置AppVersion后按0查版本桶得到
 * null，setReady全部跳过（direct路由静默丢失）。修复：改用本机配置的
 * getAppMainVersion()查版本桶。
 * 环境为裸单测（FakeAgent不启动网络）：注入带版本桶的SubscribeState，断言
 * setRelativeServiceReady后localState被正确登记（修复前因查0桶而全部跳过）。
 */
@Fast
public class TestA2F1VersionBucketUsesConfigVersion {
	private static final String PREFIX = "A2F1#";
	private static final String SERVICE_NAME = PREFIX + 33;
	private static final int MID = 33;
	private static final String IDENTITY = "77";
	private static final String IP = "127.0.0.1";
	private static final int PORT = 45123;

	private Application app;
	private ProviderDirectService pds;
	private Agent.SubscribeState state;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(1);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("a2f1_memory");
		conf.getDatabaseConfMap().put("", dbConf);
		// 主版本1：getAppMainVersion()==1。会话侧appVersion为0（缺陷根因）。
		conf.setAppVersion(1L << 48);
		app = ProviderDirectTestSupport.newAppWithFakeAgent("TestA2F1", conf);

		pds = new ProviderDirectService("a2f1pds", app);
		pds.providerApp = new ProviderApp(app); // 轻量构造（仅zeze被本路径使用）
		setPrefix(pds.providerApp, PREFIX);

		// 注入订阅状态：版本桶1登记一个(ip,port)与本机direct一致的服务信息。
		state = new Agent.SubscribeState(new BSubscribeInfo(SERVICE_NAME, 1));
		state.onRegister(new BServiceInfo(SERVICE_NAME, IDENTITY, 1, IP, PORT));
		app.getServiceManager().getSubscribeStates().put(SERVICE_NAME, state);

		// 模块33已注册（ProviderApp的三map，public final）。
		pds.providerApp.modules.put(MID, new Zeze.Builtin.Provider.BModule.Data());
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (app != null && app.getStartState() != Application.StartState.eStopped)
			app.stop();
	}

	/** 版本桶查询必须用本机配置主版本（1），direct会话的appVersion(0)不得参与查询。 */
	@Test
	public void testVersionBucketQueriedByConfigMainVersion() {
		var ps = pds.newSession(Integer.parseInt(IDENTITY));
		pds.setRelativeServiceReady(ps, IP, PORT);

		var local = state.getLocalStates().get(IDENTITY);
		Assertions.assertNotNull(local,
				"配置AppVersion(主版本1)后setReady必须命中版本桶1（修复前用会话appVersion=0查询，全部跳过）");
		Assertions.assertEquals(ps.getSessionId(), ((ProviderModuleState)local).sessionId,
				"登记的localState必须属于当前direct会话");
		// 会话登记完成。
		Assertions.assertSame(ps, pds.providerByServerId.get(Integer.parseInt(IDENTITY)));
	}

	/** 版本桶不匹配（桶2，配置主版本1）时必须跳过——版本过滤语义本身不回归。 */
	@Test
	public void testMismatchedBucketStillSkipped() {
		var state2 = new Agent.SubscribeState(new BSubscribeInfo(PREFIX + 44, 1));
		state2.onRegister(new BServiceInfo(PREFIX + 44, "88", 2, IP, PORT)); // 桶2
		app.getServiceManager().getSubscribeStates().put(PREFIX + 44, state2);
		pds.providerApp.modules.put(44, new Zeze.Builtin.Provider.BModule.Data());

		var ps = pds.newSession(88);
		pds.setRelativeServiceReady(ps, IP, PORT);

		Assertions.assertNull(state2.getLocalStates().get("88"),
				"版本桶不匹配的服务不得setReady（版本过滤语义保留）");
	}

	/** 反射设置ProviderApp的final前缀字段（轻量构造器置null，direct路径需要非空前缀）。 */
	private static void setPrefix(ProviderApp providerApp, String prefix) {
		try {
			var f = ProviderApp.class.getDeclaredField("serverServiceNamePrefix");
			f.setAccessible(true);
			f.set(providerApp, prefix);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
