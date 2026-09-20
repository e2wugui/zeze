package Zeze.Arch;

import Zeze.Application;
import Zeze.Config;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Util.OutObject;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A2-F4回归：removeServer无条件providerByServerId.remove(serverId)——同serverId
 * 换地址滚动重启时，providerByServerId已指向新地址会话ps2，旧地址的remove误删ps2的
 * 注册，缺失持续到下次重连。修复：按旧会话条件删除（与OnSocketClose同口径）。
 * 场景：serverId=5换地址（10.0.0.1:100 → 10.0.0.2:200），新会话ps2已注册，
 * SM通告旧地址服务下线 → removeServer(旧地址) 不得删掉ps2。
 */
@Fast
public class TestA2F4RemoveServerConditionalRemove {
	private static final String SERVICE_NAME = "A2F4#21";
	private static final int SERVER_ID = 5;
	private static final String OLD_IP = "10.0.0.1";
	private static final int OLD_PORT = 100;
	private static final String NEW_IP = "10.0.0.2";
	private static final int NEW_PORT = 200;

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
		dbConf.setDatabaseUrl("a2f4_memory");
		conf.getDatabaseConfMap().put("", dbConf);
		app = ProviderDirectTestSupport.newAppWithFakeAgent("TestA2F4", conf);

		pds = new ProviderDirectService("a2f4pds", app);
		pds.providerApp = new ProviderApp(app); // 轻量构造：removeServer仅使用providerApp.zeze

		state = new Agent.SubscribeState(new BSubscribeInfo(SERVICE_NAME, 0));
		app.getServiceManager().getSubscribeStates().put(SERVICE_NAME, state);
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (app != null && app.getStartState() != Application.StartState.eStopped)
			app.stop();
	}

	/** 同serverId换地址滚动重启：removeServer(旧地址)不得误删新地址会话的注册。 */
	@Test
	public void testRemoveOldAddressKeepsNewSessionRegistration() {
		// 旧会话ps1（注册在旧loadName下）与新会话ps2（接管serverId注册）并存。
		var ps1 = pds.newSession(SERVER_ID);
		ps1.serverLoadIp = OLD_IP;
		ps1.serverLoadPort = OLD_PORT;
		var ps2 = pds.newSession(SERVER_ID);
		ps2.serverLoadIp = NEW_IP;
		ps2.serverLoadPort = NEW_PORT;
		pds.providerByLoadName.put(ps1.getServerLoadName(), ps1);
		pds.providerByLoadName.put(ps2.getServerLoadName(), ps2);
		pds.providerByServerId.put(SERVER_ID, ps2); // 滚动重启后已指向新会话

		// 旧地址connector存在（removeServer只处理已配置connector的地址）。
		var out = new OutObject<Zeze.Net.Connector>();
		pds.getConfig().tryGetOrAddConnector(OLD_IP, OLD_PORT, true, out);

		pds.removeServer(oldAddressInfo());

		// 修复前：无条件providerByServerId.remove(5)误删ps2 → 路由缺失直到下次重连。
		Assertions.assertSame(ps2, pds.providerByServerId.get(SERVER_ID),
				"removeServer(旧地址)不得误删同serverId新会话的注册（A2-F4条件删除）");
		// 旧loadName的注册随旧地址下线而清理。
		Assertions.assertNull(pds.providerByLoadName.get(OLD_IP + '_' + OLD_PORT),
				"旧地址的loadName注册必须被清理");
		Assertions.assertSame(ps2, pds.providerByLoadName.get(NEW_IP + '_' + NEW_PORT),
				"新地址的loadName注册不受影响");
	}

	/** 旧会话仍在任（serverId注册就是它自己）：removeServer按条件正常清空两表。 */
	@Test
	public void testRemoveCurrentSessionCleansBothMaps() {
		var ps1 = pds.newSession(SERVER_ID);
		ps1.serverLoadIp = OLD_IP;
		ps1.serverLoadPort = OLD_PORT;
		pds.providerByLoadName.put(ps1.getServerLoadName(), ps1);
		pds.providerByServerId.put(SERVER_ID, ps1);

		var out = new OutObject<Zeze.Net.Connector>();
		pds.getConfig().tryGetOrAddConnector(OLD_IP, OLD_PORT, true, out);

		pds.removeServer(oldAddressInfo());

		Assertions.assertNull(pds.providerByServerId.get(SERVER_ID), "旧会话仍任时serverId注册正常清理");
		Assertions.assertNull(pds.providerByLoadName.get(OLD_IP + '_' + OLD_PORT), "loadName注册正常清理");
	}

	private static BServiceInfo oldAddressInfo() {
		return new BServiceInfo(SERVICE_NAME, String.valueOf(SERVER_ID), 0, OLD_IP, OLD_PORT);
	}
}
