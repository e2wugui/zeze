package UnitTest.Zeze.Component;

import java.util.concurrent.TimeUnit;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Component.Takeover;
import Zeze.Config;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import org.junit.jupiter.api.Assertions;

/**
 * Takeover测试公共设施：编程式Application（SM=disable、Memory库，Takeover不依赖SM）、
 * 租约伪造/读取、tryTransfer队列哨兵、Timer的受保护成员访问（死者定时器链伪造）。
 */
final class TakeoverTestEnv {
	private TakeoverTestEnv() {
	}

	// Application并发需要不同serverId：本地存储（zeze_cache_<serverId>等）每serverId一份，
	// @Fast类并行时共用默认0会撞锁（delete failed: zeze_cache_0）。从100起避开伪造死者id(777+)。
	private static final java.util.concurrent.atomic.AtomicInteger NextServerId =
			new java.util.concurrent.atomic.AtomicInteger(100);

	static Config newConf(String mode, long ttl, long scanPeriod) {
		var conf = new Config();
		conf.setServiceManager("disable");
		int serverId = NextServerId.getAndIncrement();
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf());
		// DatabaseMemory 的表存储是JVM级静态Map、按DatabaseUrl分桶：默认空url的桶被所有
		// Memory库App共享（含常驻的demo.App——zeze.xml即Memory+空url，其Takeover默认on，
		// 每30s walk共享租约表并对外来过期租约无声立碑）。每个测试App独占一个桶才能隔离。
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("takeover_test_" + serverId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf); // Memory库，独立url=独立存储
		conf.setTakeoverMode(mode);
		conf.setTakeoverTtl(ttl);
		conf.setTakeoverScanPeriod(scanPeriod);
		return conf;
	}

	/** Application不是AppBase，Timer需要AppBase——做个只带zeze的适配。 */
	static final class TestAppBase extends AppBase {
		private final Application zeze;

		TestAppBase(Application zeze) {
			this.zeze = zeze;
		}

		@Override
		public Application getZeze() {
			return zeze;
		}
	}

	/** 暴露AbstractTimer的protected表给测试伪造死者链（construct即RegisterZezeTables）。 */
	static final class AccessibleTimer extends Zeze.Component.Timer {
		AccessibleTimer(AppBase app) {
			super(app);
		}
	}

	// 与Takeover.tryTransfer同key投递哨兵，等待队列中排在前面的tryTransfer执行完。
	static void waitTryTransferQueue() {
		var done = new TaskCompletionSource<Void>();
		TaskSpec.ofAction(() -> done.setResult(null)).name("TakeoverTestEnv.sentinel")
				.executeOneByOne(Takeover.TryTransferOneByOneKey, Task.getOneByOne());
		done.get(5, TimeUnit.SECONDS);
	}

	static void forgeLease(Application app, int serverId, long epoch, long expireAt) {
		var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
			var lease = app.getTakeover().getTable().getOrAdd(serverId);
			lease.setEpoch(epoch);
			lease.setExpireAt(expireAt);
			return 0L;
		}, "TakeoverTestEnv.forgeLease@" + serverId)).call();
		Assertions.assertEquals(0L, rc);
	}

	// 行缺失与墓碑(expireAt=0)不可区分时返回{0,0}会造成假通过/假墓碑读数——这里大声失败。
	static long[] readLease(Application app, int serverId) {
		var out = new long[2];
		var exists = new boolean[1];
		var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
			var lease = app.getTakeover().getTable().get(serverId);
			if (lease != null) {
				exists[0] = true;
				out[0] = lease.getEpoch();
				out[1] = lease.getExpireAt();
			}
			return 0L;
		}, "TakeoverTestEnv.readLease@" + serverId)).call();
		Assertions.assertEquals(0L, rc);
		Assertions.assertTrue(exists[0], "租约行必须存在：serverId=" + serverId
				+ "（行缺失曾被误读为墓碑{0,0}）");
		return out;
	}
}
