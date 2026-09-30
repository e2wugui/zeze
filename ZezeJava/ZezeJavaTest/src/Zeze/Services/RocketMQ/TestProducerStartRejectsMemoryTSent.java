package Zeze.Services.RocketMQ;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import harness.FastServerIds;
import org.apache.rocketmq.client.ClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * tSent 落 memory 库必须显式拒绝（部署契约）：默认 Config.DatabaseConf 的
 * databaseType 即 Memory，漏配数据库的应用静默把 tSent 落进内存库——该形态下模块
 * 两项自述保障双双静默失效：1) start() 无条件注册的每日清理对无 storage 的表
 * walk 必抛 IllegalStateException 被吞（表无界增长，恰是清理注释宣称要防的）；
 * 2) tSent 行随进程重启灭失，重启前"本地已提交+COMMIT 应答丢失"的半消息回查恒
 * UNKNOW，broker 回查次数耗尽后丢弃（违背"仅当事务成功才发送"）。修复前构造器
 * 与 start() 对落库形态零校验。校验落在 start()（生产入口；@Fast 测试不调
 * start()，内存库夹具不受影响），联调/demo 形态经系统属性显式豁免。
 */
@Fast
@ResourceLock("rocketmq.producer.processSlot") // 构造 Producer 占用进程级静态计数，与其它 Producer 测试串行
public class TestProducerStartRejectsMemoryTSent {
	private static final int SERVER_ID = FastServerIds.TEST_PRODUCER_START_REJECTS_MEMORY_TSENT;
	// 与 Producer.TSENT_ALLOW_MEMORY_PROPERTY 同名（private常量，测试用字面量，先例：tSentKeepTimeMillis）
	private static final String ALLOW_MEMORY_PROPERTY = "RocketMQ.Producer.tSentAllowMemory";

	private Application app;

	private static Application newApp(int serverId, String name) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("rocketmq_mem_tsent_" + serverId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application(name, conf);
	}

	@BeforeEach
	public void setUp() throws Exception {
		app = newApp(SERVER_ID, "TestProducerStartRejectsMemoryTSent");
	}

	@AfterEach
	public void tearDown() throws Exception {
		app.stop();
	}

	/** memory 形态在 start() 显式拒绝（修复前静默接受，定时器照常注册、每日清理抛错停摆无感）。 */
	@Test
	public void startRejectsMemoryTSentDatabase() {
		var producer = new Producer(app, "testMemReject", new ClientConfig());
		try {
			var ex = assertThrows(IllegalStateException.class, producer::start,
					"tSent落memory库必须在start()显式拒绝（修复前静默接受）");
			assertTrue(ex.getMessage().contains("tSent"), "异常须指向tSent部署契约: " + ex.getMessage());
		} finally {
			producer.stop();
		}
	}

	/** 显式豁免开关（联调/demo形态自担风险）后不得以本契约拒绝；客户端离线start的
	 * 环境性错误（无namesrv等）不属于本校验对象，不作断言。 */
	@Test
	public void explicitOptInBypassesForInteropForms() {
		System.setProperty(ALLOW_MEMORY_PROPERTY, "true");
		var producer = new Producer(app, "testMemOptIn", new ClientConfig());
		try {
			try {
				producer.start();
			} catch (IllegalStateException e) {
				fail("显式豁免后不得以tSent契约拒绝: " + e.getMessage());
			} catch (Exception e) {
				// 客户端侧环境错误：校验已放行，不属本校验对象
			}
			assertDoesNotThrow(producer::stop, "豁免形态的stop路径不受影响");
		} finally {
			producer.stop();
			System.clearProperty(ALLOW_MEMORY_PROPERTY);
		}
	}
}
