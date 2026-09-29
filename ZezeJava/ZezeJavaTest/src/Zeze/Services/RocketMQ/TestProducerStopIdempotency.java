package Zeze.Services.RocketMQ;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
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
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Producer.stop() 幂等（FND30 rocketmq-01）：stop 里的 shutdown 族调用
 * （producer.shutdown/checkExecutor.shutdown）本身幂等，唯 liveInstances 递减无守卫——
 * 同一实例重复 stop 会重复递减静态计数且不可自愈，"同进程多活实例"构造告警判据
 * （liveInstances&gt;1，producerGroup 回查路由串台高危形态的进程内唯一防线）被静默瓦解：
 * p1 在线+p2 双停后计数下漂，p1 与新实例同时在线时不再告警。
 */
@Fast
@ResourceLock("rocketmq.producer.processSlot") // liveInstances 是进程级静态状态，与其它 Producer 测试串行
public class TestProducerStopIdempotency {
	// 两个独立 serverId+派生 url：多实例拓扑=多 Application（对齐 TestProducerMultipleInstancesShareProcess），
	// 同一 Application 双注册会撞 duplicate table。
	private static final int SERVER_ID = FastServerIds.TEST_PRODUCER_STOP_IDEMPOTENCY;
	private static final int SERVER_ID2 = SERVER_ID + 1;
	private static final String GROUP = "testStopIdempotent";

	private Application app;
	private Application app2;

	private static Application newApp(int serverId, String name) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("rocketmq_stop_idempotent_" + serverId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application(name, conf);
	}

	@BeforeEach
	public void setUp() throws Exception {
		app = newApp(SERVER_ID, "TestProducerStopIdempotent1");
		app2 = newApp(SERVER_ID2, "TestProducerStopIdempotent2");
	}

	@AfterEach
	public void tearDown() throws Exception {
		app.stop();
		app2.stop();
	}

	/**
	 * 重复 stop 不得重复递减活实例计数：p2 双停后 p1 仍在线，计数必须停在 before+1
	 * （修复前第二次 stop 再递减到 before+0，此后 p1+新实例双在线时构造点计数仅 before+1，
	 * liveInstances&gt;1 判据失效、告警静默丢失）。随后在 p1 在线时重建，验证告警判据恢复成立。
	 */
	@Test
	public void repeatedStopDoesNotDoubleDecrementLiveInstances() throws Exception {
		var before = liveInstances();
		var p1 = new Producer(app, GROUP, new ClientConfig());
		Producer p3 = null;
		try {
			var p2 = new Producer(app2, GROUP, new ClientConfig());
			assertEquals(before + 2, liveInstances(), "两实例构造后计数+2");
			assertDoesNotThrow(p2::stop, "首次 stop 必须成功");
			assertDoesNotThrow(p2::stop,
					"重复 stop 必须无害（shutdown 族幂等，集成层有充分理由假设 stop 幂等）");
			assertEquals(before + 1, liveInstances(),
					"p1 仍在线：重复 stop 不得重复递减 liveInstances（修复前下漂到 " + (before)
							+ "，多实例告警判据被静默瓦解）");
			p2.UnRegisterZezeTables(app2);
			// p1 在线时重建：构造点计数应达 before+2（&gt;before+1），warn 判据成立——修复前仅 before+1 不告警。
			p3 = assertDoesNotThrow(() -> new Producer(app2, GROUP, new ClientConfig()),
					"stop 幂等修复不得影响重建路径");
			assertEquals(before + 2, liveInstances(), "p1+p3 双在线：构造点计数使 liveInstances>1 告警判据成立");
		} finally {
			if (p3 != null) {
				p3.stop();
				p3.UnRegisterZezeTables(app2);
			}
			p1.stop();
			p1.UnRegisterZezeTables(app);
		}
		assertEquals(before, liveInstances(), "全部停机后计数归还基线");
	}

	// liveInstances 为 private static：读数走反射（告警判据的等价可观测量，不为测试放宽封装）。
	private static int liveInstances() throws Exception {
		Field field = Producer.class.getDeclaredField("liveInstances");
		field.setAccessible(true);
		return ((AtomicInteger)field.get(null)).get();
	}
}
