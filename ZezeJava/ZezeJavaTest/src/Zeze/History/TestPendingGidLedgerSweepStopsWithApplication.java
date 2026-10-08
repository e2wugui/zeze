package Zeze.History;

import harness.Extra;
import java.lang.reflect.Field;
import Zeze.Application;
import Zeze.Config;
import Zeze.Util.DaemonTimer;
import Zeze.Util.Id128;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * PendingGidLedger 对账守护随 Application 生命周期收口：账本在首个 gid 登记时惰性
 * 启动 60s 周期的 DaemonTimer（HistoryPendingGidSweep@owner），该定时链经
 * finishRound→rescheduleLocked 无条件自续——Application.stop 逐一收编周期守护
 * （achillesHeelDaemon/delayRemove/safeBatch/timer/checkpoint…）独缺此项时，
 * 每个启用 history 且发生过登记的实例都在进程内遗留一条永久自续的定时链（含
 * 账本对象图，随实例数无界累积；长测试与循环创建/停止 Application 的进程形态
 * 线性堆积全局调度池任务）。修复：PendingGidLedger.stop（幂等，stop 后 register
 * 重新拉起，对齐 DaemonTimer 重启语义）在 Application.stop 的终检点之后收编。
 */
@Fast
@Extra
public class TestPendingGidLedgerSweepStopsWithApplication {

	// 独立号段+派生url（每用例独立url隔离DatabaseMemory静态桶的归属标记残留）。
	private static final int SERVER_ID = FastServerIds.TEST_PENDING_GID_LEDGER_SWEEP_STOPS_WITH_APP;

	private static Application newApp(int serverId, String url, String historyName) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setTakeoverMode("off");
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl(url);
		conf.getDatabaseConfMap().put("", dbConf);
		conf.setHistory(historyName);
		return new Application("TestPendingGidLedgerSweepStops", conf);
	}

	/** 账本对账守护的关门态（反射读私有 sweepDaemon——不为主流程增设只读访问面）。 */
	private static boolean sweepDaemonShutdown(Application app) throws Exception {
		Field field = PendingGidLedger.class.getDeclaredField("sweepDaemon");
		field.setAccessible(true);
		var timer = (DaemonTimer) field.get(app.getPendingGidLedger());
		return timer.isShutdown();
	}

	private static void quietStop(Application app) {
		if (app == null)
			return;
		try {
			app.stop();
		} catch (Throwable e) {
			// 收尾异常不掩盖断言。
		}
	}

	/** Application.stop 后守护链必须停止（修复前：仍存活——定时链永久自续）。 */
	@Test
	public void testSweepDaemonStopsAfterApplicationStop() throws Exception {
		var url = "history_ledger_sweep_stop_" + SERVER_ID;
		var app = newApp(SERVER_ID, url, "TestLedgerSweepStops");
		try {
			app.start();
			// 首笔登记（Transaction.finalCommit beforeApply 的同一入口）惰性启动守护。
			app.getPendingGidLedger().register(new Id128(System.nanoTime(), 1), System.nanoTime());
			Assertions.assertFalse(sweepDaemonShutdown(app), "前置：register 后守护链已启动（未关门）");
		} finally {
			quietStop(app);
		}
		Assertions.assertTrue(sweepDaemonShutdown(app),
				"Application.stop 后对账守护必须停止（修复前定时链永久自续，随实例数无界累积）");
	}

	/** 未启动守护的账本（无登记）stop 同样安全：幂等关门，无异常无副作用。 */
	@Test
	public void testStopWithoutRegisterIsIdempotentNoop() throws Exception {
		var url = "history_ledger_sweep_noop_" + (SERVER_ID + 1);
		var app = newApp(SERVER_ID + 1, url, "TestLedgerSweepNoop");
		try {
			app.start();
			Assertions.assertTrue(sweepDaemonShutdown(app), "前置：无登记守护未启动（构造即关门态）");
		} finally {
			quietStop(app);
		}
		Assertions.assertTrue(sweepDaemonShutdown(app), "未启动的守护 stop 后保持关门态（幂等无副作用）");
	}
}
