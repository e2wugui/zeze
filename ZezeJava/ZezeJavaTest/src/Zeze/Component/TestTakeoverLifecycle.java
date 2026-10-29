package Zeze.Component;

import java.nio.file.Path;
import Zeze.Application;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/**
 * 正常release/start生命周期与零epoch租约自愈。
 * 致命退出由TestTakeoverFence独立验证，不再模拟致命退出后重启同一对象。
 */
@Fast
@Timeout(45)
public class TestTakeoverLifecycle {

	/** 内存态记录scope：stamp只记epoch，transferAll恒0（本测试不触发搬运）。 */
	private static final class RecordingScope implements TakeoverScope {
		private final String name;
		private volatile long stampedEpoch;

		RecordingScope(String name) {
			this.name = name;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public void stamp(long epoch) {
			stampedEpoch = epoch;
		}

		@Override
		public long transferAll(int deadServerId, long deadEpoch) {
			return 0;
		}
	}

	@Test
	public void testRestartClearsScopedAndRefreshesGracePeriod() throws Exception {
		var conf = TakeoverTestEnv.newConf("on", 600_000, 600_000);
		var app = new Application("TestTakeoverLifecycleRestart", conf);
		try {
			app.start();
			var takeover = app.getTakeover();
			var myId = conf.getServerId();

			var scope1 = new RecordingScope("LifecycleScope1");
			takeover.addScope(scope1); // started：addScope内同步stamp
			Assertions.assertTrue(takeover.isScoped(scope1), "addScope后应完成stamp登记");
			var firstEpoch = takeover.getMyEpoch();
			Assertions.assertEquals(firstEpoch, scope1.stampedEpoch);

			// 正常release清登记，并刷新完整TTL宽限期；不伪造致命退出后的继续执行。
			var firstReleaseAt = System.currentTimeMillis();
			takeover.release();
			Assertions.assertFalse(takeover.isScoped(scope1), "release应清scoped登记（重启窗口防误杀）");
			var afterRelease = TakeoverTestEnv.readLease(app, myId);
			Assertions.assertEquals(firstEpoch, afterRelease[0]);
			Assertions.assertTrue(afterRelease[1] >= firstReleaseAt + 600_000, "正常停机应刷新完整TTL宽限期");

			var scope2 = new RecordingScope("LifecycleScope2");
			takeover.addScope(scope2); // 停止期间仅登记，重启后才stamp。
			Assertions.assertFalse(takeover.isScoped(scope2));
			Assertions.assertEquals(0L, scope2.stampedEpoch);
			takeover.start();
			var secondEpoch = takeover.getMyEpoch();
			Assertions.assertEquals(firstEpoch + 1, secondEpoch, "重启应claim新epoch");
			Assertions.assertTrue(takeover.isScoped(scope1), "重启后start应重新stamp");
			Assertions.assertTrue(takeover.isScoped(scope2));
			Assertions.assertEquals(secondEpoch, scope1.stampedEpoch, "已登记scope应重盖戳");
			Assertions.assertEquals(secondEpoch, scope2.stampedEpoch, "停止期间新增scope应补盖戳");

			// 使用明确不同的到期时间，验证第二次release确实执行刷新，避免同毫秒假通过。
			TakeoverTestEnv.forgeLease(app, myId, secondEpoch, System.currentTimeMillis() + 1_234_567);
			var beforeRelease = System.currentTimeMillis();
			takeover.release();
			var lease = TakeoverTestEnv.readLease(app, myId);
			Assertions.assertEquals(secondEpoch, lease[0], "宽限期刷新保留epoch");
			Assertions.assertTrue(lease[1] >= beforeRelease + 600_000,
					"重启后的正常停机应刷新完整TTL宽限期，lease=" + lease[1]);
			Assertions.assertTrue(lease[1] <= System.currentTimeMillis() + 600_000, "release应更新为当前宽限期");
			Assertions.assertFalse(takeover.isScoped(scope1));
			Assertions.assertFalse(takeover.isScoped(scope2));
		} finally {
			app.stop();
		}
	}

	@Test
	public void testStampScopeHealsZeroEpochRow(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path dir) throws Exception {
		TakeoverTestProcess.run(dir, TestTakeoverLifecycle.class, "zero-epoch-heal", null);
	}

	public static void main(String[] args) {
		try {
			if (!args[0].equals("zero-epoch-heal"))
				throw new IllegalArgumentException(args[0]);
			stampScopeHealsZeroEpochRow();
			TakeoverTestProcess.marker("PASS", args[0]);
			System.exit(0);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(2);
		}
	}

	private static void stampScopeHealsZeroEpochRow() throws Exception {
		var conf = TakeoverTestEnv.newConf("on", 600_000, 600_000);
		var app = new Application("TestTakeoverLifecycleHeal", conf);
		try {
			app.start();
			var takeover = app.getTakeover();
			var myId = conf.getServerId();
			TakeoverTestProcess.marker("READY", "zero-epoch-heal");

			// 伪造"行存在但epoch==0"的半清表残留（过期态）：renewOnce的自愈条件覆盖此形态，
			// stampScope旧行为却走lost致命退出——同一异常态两种结局，应统一为自愈。
			TakeoverTestEnv.forgeLease(app, myId, 0, System.currentTimeMillis() - 1_000);
			var scope = new RecordingScope("HealScope");
			takeover.addScope(scope);

			Assertions.assertTrue(takeover.isScoped(scope), "自愈后scope应完成stamp登记");
			Assertions.assertEquals(takeover.getMyEpoch(), scope.stampedEpoch, "stamp应写当前epoch");
			var lease = TakeoverTestEnv.readLease(app, myId);
			Assertions.assertEquals(takeover.getMyEpoch(), lease[0], "租约epoch应被自愈重写");
			Assertions.assertTrue(lease[1] > System.currentTimeMillis(), "租约应被自愈续期（非伪造的过期态）");
		} finally {
			app.stop();
		}
	}
}
