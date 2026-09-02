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
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		conf.getDatabaseConfMap().putIfAbsent("", new Config.DatabaseConf()); // 默认Memory库
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
	static void waitTryTransferQueue() throws Exception {
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

	static long[] readLease(Application app, int serverId) {
		var out = new long[2];
		var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
			var lease = app.getTakeover().getTable().get(serverId);
			if (lease != null) {
				out[0] = lease.getEpoch();
				out[1] = lease.getExpireAt();
			}
			return 0L;
		}, "TakeoverTestEnv.readLease@" + serverId)).call();
		Assertions.assertEquals(0L, rc);
		return out;
	}
}
