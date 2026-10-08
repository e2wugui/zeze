package Zeze.Services.RocketMQ;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import Zeze.Application;
import Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult;
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
 * databaseType 即 Memory，漏配数据库的应用静默把 tSent 落进内存库——tSent 数据随
 * 进程重启灭失，重启前"本地已提交+COMMIT 应答丢失"的半消息回查恒 UNKNOW，broker
 * 回查次数耗尽后丢弃（违背"仅当事务成功才发送"）。修复前构造器与 start() 对落库
 * 形态零校验。校验落在 start()（生产入口；@Fast 测试不调 start()，内存库夹具不受
 * 影响），联调/demo 形态经系统属性显式豁免。
 * 另锚定该形态的真实可用面：memory 库的表仍建 storage（TableX.open 只对表级
 * kind="memory" 置 storage=null），TableMemory 的分页 walk 正常工作——旧契约
 * 声明"每日清理 walk 必抛错停摆、表无界增长"不成立（真实且唯一的危害是重启
 * 灭失），勿混淆"库类型 Memory"与"表 kind=memory"两类语义。
 */
@Fast
@ResourceLock("rocketmq.producer.processSlot") // 构造 Producer 占用进程级静态计数，与其它 Producer 测试串行
@Extra
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

	/** memory 形态在 start() 显式拒绝（修复前静默接受，重启灭失回查证据的风险零告警）。 */
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

	/**
	 * memory 库形态的可用面锚点：tSent 未声明 kind="memory"，TableX.open 为它创建
	 * 真实 storage（包装 DatabaseMemory.TableMemory），walk 正常分页遍历——本锚点
	 * 机械化否证"该形态下每日清理 walk 必抛错停摆"的旧契约声明（真实危害是重启
	 * 灭失，见类 javadoc）。walk 读落库面，先 runOnce 把提交记录 flush 到 storage
	 * （每日清理的7天保留窗远大于任何 checkpoint 周期，生产语义不受影响）。若未来
	 * tSent 改为 kind="memory"（storage=null，walk 抛 IllegalStateException），
	 * 部署契约与豁免开关的风险声明需随之重新评估。
	 */
	@Test
	public void memoryDatabaseTSentWalkStillWorks() throws Exception {
		var producer = new Producer(app, "testMemWalk", new ClientConfig());
		try {
			app.start(); // 打开 tSent（storage 就位）——与 start() 门禁不同，walk 需要表已打开
			var keyPrefix = "walkAnchor";
			var rc = app.newProcedure(() -> {
				for (var i = 0; i < 3; ++i)
					producer._tSent.insert(keyPrefix + i,
							new BTransactionMessageResult(false, System.currentTimeMillis()));
				return 0L;
			}, "TestProducerStartRejectsMemoryTSent.walkAnchor").call();
			assertEquals(0L, rc, "预插 tSent 行必须成功");
			app.getCheckpoint().runOnce(); // 提交记录 flush 到 storage（walk 的数据面）
			var keys = new ArrayList<String>();
			var lastKey = producer._tSent.walk(null, 100, (key, value) -> {
				if (key.startsWith(keyPrefix))
					keys.add(key);
				return true; // 继续
			});
			assertEquals(3, keys.size(), "memory 库上 tSent.walk 必须正常遍历"
					+ "（storage 非空，TableMemory 分页实现）");
			assertNotNull(lastKey, "非空表 walk 必须返回最后键");
		} finally {
			producer.stop();
		}
	}
}
