package Zeze.Services.RocketMQ;

import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import harness.FastServerIds;
import org.apache.rocketmq.client.ClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Producer 同 JVM 多实例拓扑（rocketmq-02 修订后契约）：多 app 同进程
 * （Infinite.Simulate 五 app 形态）各自构造 Producer 是合法拓扑——仅构造未 start 的
 * 实例是惰性的，进程内单例硬拒绝会把该拓扑一刀切死（终验 Simulate 级联 139 败实锤）。
 * 多活实例的回查路由串台风险由构造计数 warn 保留可观测性，部署契约见类 javadoc。
 */
@Fast
@ResourceLock("rocketmq.producer.processSlot") // 与 TestProducerTxnSendRejectsEnvTransaction 串行（共享进程级形态）
public class TestProducerMultipleInstancesShareProcess {
	// 两个独立 serverId+派生 url：多实例拓扑=多 Application（Simulate 同构），
	// 同一 Application 双注册会撞 duplicate table（既有的表级防重）。
	private static final int SERVER_ID = FastServerIds.TEST_PRODUCER_MULTIPLE_INSTANCES_SHARE_PROCESS;
	private static final int SERVER_ID2 = SERVER_ID + 1;

	private Application app;
	private Application app2;

	private static Application newApp(int serverId, String name) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("rocketmq_multi_instance_" + serverId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application(name, conf);
	}

	@BeforeEach
	public void setUp() throws Exception {
		app = newApp(SERVER_ID, "TestProducerMultiInstance1");
		app2 = newApp(SERVER_ID2, "TestProducerMultiInstance2");
	}

	@AfterEach
	public void tearDown() throws Exception {
		app.stop();
		app2.stop();
	}

	/** 多 app 同进程各自构造 Producer 必须共存（Simulate 拓扑回归钉）；stop 后可重建。 */
	@Test
	public void multipleAppsConstructProducersCoexist() {
		var p1 = new Producer(app, "testMultiInstance", new ClientConfig());
		try {
			var p2 = assertDoesNotThrow(() -> new Producer(app2, "testMultiInstance", new ClientConfig()),
					"多 app 同进程拓扑必须可共存（单例硬拒绝曾使 Simulate 级联 139 败）");
			p2.stop(); // stop 自带 tSent 反注册（成对收口）
		} finally {
			p1.stop();
		}
		// stop 计数归还后重建
		var p3 = assertDoesNotThrow(() -> new Producer(app, "testMultiInstance", new ClientConfig()),
				"stop 后必须可重建（demo App stop/start 重启路径）");
		p3.stop();
	}

	/** 构造中途失败（表重复注册）归还计数，不阻塞后续重建：失败触发形态=旧实例仍在线
	 * （其 tSent 注册未随 stop 反注册前，同 app 第二个构造在 RegisterZezeTables 处失败）；
	 * 修复后 stop 自带反注册，失败释放计数后 stop→重建在同 app 上直接可行。 */
	@Test
	public void failedConstructorReleasesCount() {
		var p1 = new Producer(app, "testFailedCtor", new ClientConfig());
		// p1 不 stop：表仍注册中，第二次构造在 RegisterZezeTables 处失败（duplicate table）。
		var ex = assertThrows(IllegalStateException.class,
				() -> new Producer(app, "testFailedCtor", new ClientConfig()));
		assertTrue(ex.getMessage().contains("duplicate table"),
				"本用例的失败点应是表重复注册，实际: " + ex.getMessage());
		p1.stop();
		var p3 = assertDoesNotThrow(() -> new Producer(app, "testFailedCtor", new ClientConfig()),
				"构造中途失败必须归还计数，不得阻塞后续重建");
		p3.stop();
	}
}
