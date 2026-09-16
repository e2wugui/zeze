package UnitTest.Zeze;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import Zeze.Application;
import Zeze.Config;
import Zeze.Onz.Onz;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FND7-56 回归：Application.stop各拆卸步骤无异常隔离——任一步抛出（Service.stop关
 * 连接的IO异常、join中断forceThrow等）即从stop逃逸，其后所有步骤被跳过：数据库未关、
 * InUse未清、LocalRocksCache目录未删，startState永驻eStopping。
 * 修复：每步骤经stopStep包装——异常只记日志继续，终态必达eStopped。
 * 测试注入stop()必抛异常的Onz（第一个拆卸步骤），验证：
 * (1)stop不再抛出；(2)终态eStopped；(3)其后步骤未被跳过（zeze_cache_目录被删除）。
 */
@Fast
public class TestFnd756StopStepIsolation {
	// 独立serverId+url：@Fast类并行时避免本地RocksCache目录互撞（对齐TestCheckpointRunThreadSentinel）。
	private static final int SERVER_ID = 7560;

	private Application app;

	/** 构造轻量（无ServiceConf时不建Service），仅stop()模拟IO等步骤失败。 */
	static final class BrokenOnz extends Onz {
		BrokenOnz(Application zeze) {
			super(zeze);
		}

		@Override
		public void stop() throws Exception {
			throw new RuntimeException("Fnd756 simulated stop failure");
		}
	}

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("fnd7_56_stop_isolation_" + SERVER_ID);
		conf.getDatabaseConfMap().put("", dbConf);
		app = new Application("TestFnd756StopStepIsolation", conf);
		app.start();
	}

	@AfterEach
	public void tearDown() {
		if (app.getStartState() != Application.StartState.eStopped) {
			try {
				app.stop(); // 未修复时stop抛异常滞留eStopping：兜底再停一次清理
			} catch (Exception ignored) {
				// 旧代码遗留半停状态时尽力而为
			}
		}
	}

	@Test
	public void testStopCompletesWhenFirstStepThrows() throws Exception {
		// start()创建的本地RocksCache目录（stop中段才删除），作为"后续步骤未被跳过"的哨兵。
		var cacheDir = new File("zeze_cache_" + SERVER_ID);
		assertTrue(cacheDir.exists(), "前置：启动后本地RocksCache目录存在");

		// 注入stop()必抛异常的onz——拆卸链的第一步（反射置字段，Onz构造轻量）。
		var field = Application.class.getDeclaredField("onz");
		field.setAccessible(true);
		field.set(app, new BrokenOnz(app));

		assertDoesNotThrow(app::stop, "任一步骤失败不得从stop逃逸（FND7-56）");
		assertEquals(Application.StartState.eStopped, app.getStartState(),
				"终态必达eStopped，不得滞留eStopping");
		assertFalse(cacheDir.exists(), "失败步骤之后的收尾（LocalRocksCache关闭+目录删除）不得被跳过");
	}
}
