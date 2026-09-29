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
 * Producer 进程内单例登记：事务回查表 tSent 是随本进程 Application 注册的本地表，
 * 而 broker 的事务回查按 producerGroup 从组内任一存活通道中选一个发出——同 JVM
 * 第二个 Producer 实例会让回查路由到无 tSent 行的实例恒答 UNKNOW，半消息被回查
 * 次数耗尽丢弃（本地事务已提交而消息灭失）。第二实例构造必须立即抛错并点明原因；
 * stop() 释放占位后可重建；构造中途失败必须归还占位（不阻塞后续重建）。
 */
@Fast
@ResourceLock("rocketmq.producer.processSlot") // 进程内单例占位是静态状态，与 TestProducerTxnSendRejectsEnvTransaction 串行
public class TestProducerSingleInstancePerProcess {
	// 独立serverId+派生url：本类只构造 Application（注册表用），不 start、不建缓存目录。
	private static final int SERVER_ID = FastServerIds.TEST_PRODUCER_SINGLE_INSTANCE_PER_PROCESS;

	private Application app;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("rocketmq_single_instance_" + SERVER_ID);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		app = new Application("TestProducerSingleInstancePerProcess", conf);
	}

	@AfterEach
	public void tearDown() throws Exception {
		app.stop();
	}

	/** 同 JVM 第二实例构造即抛（带 tSent 回查路由灭失的原因）；stop 释放占位后可重建。 */
	@Test
	public void secondConstructThrowsAndStopReleasesSlot() {
		var p1 = new Producer(app, "testSingleInstance", new ClientConfig());
		try {
			var ex = assertThrows(IllegalStateException.class,
					() -> new Producer(app, "testSingleInstance", new ClientConfig()),
					"同 JVM 第二个 Producer 实例构造必须立即抛错（修复前仅报 duplicate table，回查路由灭失的原因不可见）");
			assertTrue(ex.getMessage().contains("tSent"),
					"报错须点明 tSent 是进程本地表、多实例回查路由灭失的原因，实际: " + ex.getMessage());
		} finally {
			p1.stop();
			p1.UnRegisterZezeTables(app);
		}
		// stop 已释放占位（表注册也已解除）：可重建
		var p2 = assertDoesNotThrow(() -> new Producer(app, "testSingleInstance", new ClientConfig()),
				"stop 释放占位后必须可重建（demo App stop/start 重启路径）");
		p2.stop();
		p2.UnRegisterZezeTables(app);
	}

	/** 构造中途失败（占位之后、初始化未完成）必须归还占位，不阻塞后续重建。 */
	@Test
	public void failedConstructorReleasesSlot() {
		var p1 = new Producer(app, "testFailedCtor", new ClientConfig());
		p1.stop();
		// 故意保留表注册：重建在占位检查之后、RegisterZezeTables 处失败（duplicate table），
		// 证明抛错点已越过单例占位（占位被失败构造释放过），而非单例错。
		var ex = assertThrows(IllegalStateException.class,
				() -> new Producer(app, "testFailedCtor", new ClientConfig()));
		assertTrue(ex.getMessage().contains("duplicate table"),
				"本用例的失败点应是表重复注册（占位之后），实际: " + ex.getMessage());
		p1.UnRegisterZezeTables(app);
		// 失败构造归还了占位：解除表注册后重建成功
		var p3 = assertDoesNotThrow(() -> new Producer(app, "testFailedCtor", new ClientConfig()),
				"构造中途失败必须归还占位，不得永久阻塞重建");
		p3.stop();
		p3.UnRegisterZezeTables(app);
	}
}
