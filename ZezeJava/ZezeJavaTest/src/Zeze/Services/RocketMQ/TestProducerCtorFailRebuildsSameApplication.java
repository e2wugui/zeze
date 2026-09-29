package Zeze.Services.RocketMQ;

import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import harness.FastServerIds;
import org.apache.rocketmq.client.ClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Producer 构造失败回滚（构造侧半边）：构造器把 RegisterZezeTables 放在 try 块第2行，
 * 失败补偿只覆盖 liveInstances 计数——注册之后、initialized=true 之前的失败（如
 * copyRoutingIdentity 对问题 clientConfig 的异常）使 tSent 残留 Application 注册表且
 * 半构造对象不可达（无人再为它调 stop()/反注册），同一 Application 重建必撞
 * duplicate table（addTable 表 id 查重），本 app 的事务消息能力不可恢复。
 * 修复：!initialized 分支对注册成功过的构造补对称反注册（与 stop 侧同一配对不变量）；
 * 失败点在 RegisterZezeTables 自身（注册未发生）时不得反注册注册者的活表。
 */
@Fast
@ResourceLock("rocketmq.producer.processSlot") // 与其他构造 Producer 的用例串行（共享进程级 liveInstances 计数）
public class TestProducerCtorFailRebuildsSameApplication {
	// 独立serverId+派生url：本用例只触登记路径不start（无缓存目录），派生url防Memory桶互撞。
	private static final int SERVER_ID = FastServerIds.TEST_PRODUCER_CTOR_FAIL_REBUILDS;

	private Application app;

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("rocketmq_ctor_rollback_" + SERVER_ID);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestProducerCtorRollback", conf);
	}

	@BeforeEach
	public void setUp() throws Exception {
		app = newApp();
	}

	@AfterEach
	public void tearDown() throws Exception {
		app.stop();
	}

	/** 注册后失败（copyRoutingIdentity首字段抛出）必须反注册tSent：同app重建不得撞
	 * duplicate table（修复前：只归还计数，表注册泄漏，重建永久阻断）。 */
	@Test
	public void ctorFailAfterRegisterRollsBackForRebuild() {
		// 注入点：copyRoutingIdentity在RegisterZezeTables之后调用，且首个读取就是getNamesrvAddr。
		var boom = new ClientConfig() {
			@Override
			public String getNamesrvAddr() {
				throw new RuntimeException("injected ctor failure after register");
			}
		};
		assertThrows(RuntimeException.class, () -> new Producer(app, "testCtorRollback", boom),
				"注入的构造失败必须原样传播");
		var p = assertDoesNotThrow(() -> new Producer(app, "testCtorRollback", new ClientConfig()),
				"构造失败必须反注册tSent（与计数归还对称的生命周期配对），同app重建不得撞duplicate table");
		p.stop();
		// 回滚后stop侧路径同样不受污染：stop→重建仍可行。
		var p2 = assertDoesNotThrow(() -> new Producer(app, "testCtorRollback", new ClientConfig()),
				"回滚干净后stop→重建路径不回归");
		p2.stop();
	}

	/** 对照（防矫枉过正）：失败点在RegisterZezeTables自身（duplicate table，注册未发生）时，
	 * 失败构造的回滚不得摘掉注册者的活表——否则在线Producer的tSent被静默移出注册表。 */
	@Test
	public void ctorFailAtRegisterKeepsExistingTable() {
		var p1 = new Producer(app, "testCtorFailAtReg", new ClientConfig());
		try {
			assertThrows(IllegalStateException.class,
					() -> new Producer(app, "testCtorFailAtReg", new ClientConfig()),
					"在线实例的表仍注册中，第二次构造应在RegisterZezeTables处失败");
			assertNotNull(app.getTable(p1._tSent.getId()),
					"注册未发生的失败构造不得反注册在线实例的tSent（removeTable按id/name删）");
		} finally {
			p1.stop();
		}
		var p3 = Assertions.assertDoesNotThrow(() -> new Producer(app, "testCtorFailAtReg", new ClientConfig()),
				"失败释放计数后重建可行（既有契约不回归）");
		p3.stop();
	}
}
