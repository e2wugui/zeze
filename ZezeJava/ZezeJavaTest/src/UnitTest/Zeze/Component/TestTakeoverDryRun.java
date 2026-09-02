package UnitTest.Zeze.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Component.Takeover;
import Zeze.Component.TakeoverScope;
import Zeze.Config;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Takeover dry-run回归（步骤①）：伪造死者租约，tryTransfer只记日志：
 * 不调用scope.transferAll、不立墓碑、不动未过期租约。旧机制零扰动。
 */
@Fast
public class TestTakeoverDryRun {

	// 与Takeover.tryTransfer同key投递哨兵，等待队列中排在前面的tryTransfer执行完。
	private static void waitTryTransferQueue() throws Exception {
		var done = new TaskCompletionSource<Void>();
		TaskSpec.ofAction(() -> done.setResult(null)).name("TestTakeoverDryRun.sentinel")
				.executeOneByOne(Takeover.TryTransferOneByOneKey, Task.getOneByOne());
		done.get(5, TimeUnit.SECONDS);
	}

	private static void forgeLease(Application app, int serverId, long epoch, long expireAt) {
		var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
			var lease = app.getTakeover().getTable().getOrAdd(serverId);
			lease.setEpoch(epoch);
			lease.setExpireAt(expireAt);
			return 0L;
		}, "TestTakeoverDryRun.forgeLease")).call();
		Assertions.assertEquals(0L, rc);
	}

	private static long[] readLease(Application app, int serverId) {
		var out = new long[2];
		var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
			var lease = app.getTakeover().getTable().get(serverId);
			if (lease != null) {
				out[0] = lease.getEpoch();
				out[1] = lease.getExpireAt();
			}
			return 0L;
		}, "TestTakeoverDryRun.readLease")).call();
		Assertions.assertEquals(0L, rc);
		return out;
	}

	@Test
	public void testDryRun() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setDefaultTableConf(new Config.TableConf());
		conf.getDatabaseConfMap().putIfAbsent("", new Config.DatabaseConf()); // 默认Memory库
		conf.setTakeoverMode("dryrun");
		conf.setTakeoverTtl(600_000);
		conf.setTakeoverScanPeriod(600_000); // 不依赖周期扫描，手动tryTransfer
		var app = new Application("TestTakeoverDryRun", conf);
		try {
			app.start();
			var takeover = app.getTakeover();

			var transferCalls = new AtomicInteger();
			var stamps = new AtomicInteger();
			takeover.addScope(new TakeoverScope() {
				@Override
				public String name() {
					return "FakeScope";
				}

				@Override
				public void stamp(long epoch) {
					stamps.incrementAndGet(); // dryrun下不应被调用
				}

				@Override
				public long transferAll(int deadServerId, long deadEpoch) {
					transferCalls.incrementAndGet();
					return 0;
				}
			});

			// 伪造死者777：过期租约 + 一个数据行不影响断言（dryrun不触碰任何数据）。
			var expiredAt = System.currentTimeMillis() - 1_000;
			forgeLease(app, 777, 5, expiredAt);
			takeover.tryTransfer(777);
			waitTryTransferQueue();

			var lease777 = readLease(app, 777);
			Assertions.assertEquals(5L, lease777[0], "dryrun不得改epoch");
			Assertions.assertEquals(expiredAt, lease777[1], "dryrun不得立墓碑");
			Assertions.assertEquals(0, transferCalls.get(), "dryrun不得调用transferAll");
			Assertions.assertEquals(0, stamps.get(), "dryrun不得stamp");

			// 未过期租约：无任何动作（幂等重试由真实接管路径安排，dryrun不动租约行）。
			var liveExpireAt = System.currentTimeMillis() + 60_000;
			forgeLease(app, 888, 7, liveExpireAt);
			takeover.tryTransfer(888);
			waitTryTransferQueue();

			var lease888 = readLease(app, 888);
			Assertions.assertEquals(7L, lease888[0]);
			Assertions.assertEquals(liveExpireAt, lease888[1]);
			Assertions.assertEquals(0, transferCalls.get());

			// 自身serverId：直接跳过。
			takeover.tryTransfer(conf.getServerId());
			waitTryTransferQueue();
			Assertions.assertEquals(0, transferCalls.get());
		} finally {
			app.stop();
		}
	}
}
