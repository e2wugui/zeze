package UnitTest.Zeze.Component;

import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Component.TakeoverScope;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Takeover生命周期边界回归：
 * B——同进程stop→start重启：release清scoped登记（stale登记会在【新claim生效、重盖戳前】
 *    窗口放行写路径→读到旧epoch被fence误杀）+复位fenceFatal（stale值会让重启后的
 *    下一次release错误跳过正常停机宽限期刷新）；
 * C——stampScope对"epoch==0的租约行"与renewOnce同款自愈重写（旧行为走lost致命退出）。
 * 均可红绿双向：B/C修复前对应断言失败（fatal走注入计数，不真退出）。
 */
@Fast
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
	public void test1_RestartClearsStaleScopedAndFenceFatal() throws Exception {
		var conf = TakeoverTestEnv.newConf("on", 600_000, 600_000);
		var app = new Application("TestTakeoverLifecycleRestart", conf);
		var fatalCount = new AtomicInteger();
		try {
			app.start();
			var takeover = app.getTakeover();
			var myId = conf.getServerId();

			var scope1 = new RecordingScope("LifecycleScope1");
			takeover.addScope(scope1); // started：addScope内同步stamp
			Assertions.assertTrue(takeover.isScoped(scope1), "addScope后应完成stamp登记");

			// 制造"本生命周期被接管"：租约改成别人的epoch（fresh，不触发扫描），再注册新scope
			// → stampScope看到lost → fenceFailed（fatal走注入计数，不退出），fenceFatal=true。
			TakeoverTestEnv.forgeLease(app, myId, takeover.getMyEpoch() + 7, System.currentTimeMillis() + 600_000);
			takeover.setFatalAction(fatalCount::incrementAndGet);
			var scope2 = new RecordingScope("LifecycleScope2");
			takeover.addScope(scope2);
			Assertions.assertEquals(1, fatalCount.get(), "lost应触发一次fenceFailed（注入计数）");

			// 第一次release：被接管的一生不动租约（不立碑、不刷新宽限期）；但scoped登记要清、fenceFatal要复位。
			var fencedLease = TakeoverTestEnv.readLease(app, myId);
			takeover.release();
			Assertions.assertFalse(takeover.isScoped(scope1), "release应清scoped登记（重启窗口防误杀）");
			Assertions.assertFalse(takeover.isScoped(scope2), "未完成stamp的scope同样不得残留登记");
			var afterFenced = TakeoverTestEnv.readLease(app, myId);
			Assertions.assertEquals(fencedLease[0], afterFenced[0], "被接管的一生release不得动新owner的epoch");
			Assertions.assertEquals(fencedLease[1], afterFenced[1], "被接管的一生release不得动新owner的expireAt（含到期时刻）");

			// 重启：claim新epoch（foreign+1）并重盖戳——一切恢复正常。
			takeover.start();
			Assertions.assertTrue(takeover.isScoped(scope1), "重启后start应重新stamp");
			Assertions.assertTrue(takeover.isScoped(scope2));

			// 第二次release（重启后的一生，未被接管）：fenceFatal已复位 → 应刷新正常停机宽限期。
			// 【红】stale fenceFatal未复位时会跳过刷新（旧语义写墓碑expireAt=0同样非宽限期），
			// 宽限期下界断言失败。
			var beforeRelease = System.currentTimeMillis();
			var myEpoch2 = takeover.getMyEpoch();
			takeover.release();
			var lease = TakeoverTestEnv.readLease(app, myId);
			Assertions.assertEquals(myEpoch2, lease[0], "宽限期刷新保留epoch");
			Assertions.assertTrue(lease[1] >= beforeRelease + 600_000,
					"重启后的正常停机应刷新完整TTL宽限期（fenceFatal已随上一生命周期复位），lease=" + lease[1]);
			Assertions.assertEquals(1, fatalCount.get(), "重启的一生不应再有fence");
		} finally {
			safeStop(app);
		}
	}

	@Test
	public void test2_StampScopeHealsZeroEpochRow() throws Exception {
		var conf = TakeoverTestEnv.newConf("on", 600_000, 600_000);
		var app = new Application("TestTakeoverLifecycleHeal", conf);
		var fatalCount = new AtomicInteger();
		try {
			app.start();
			var takeover = app.getTakeover();
			var myId = conf.getServerId();
			takeover.setFatalAction(fatalCount::incrementAndGet); // 先注入：旧行为此处会致命退出

			// 伪造"行存在但epoch==0"的半清表残留（过期态）：renewOnce的自愈条件覆盖此形态，
			// stampScope旧行为却走lost致命退出——同一异常态两种结局，应统一为自愈。
			TakeoverTestEnv.forgeLease(app, myId, 0, System.currentTimeMillis() - 1_000);
			var scope = new RecordingScope("HealScope");
			takeover.addScope(scope);

			Assertions.assertEquals(0, fatalCount.get(), "epoch==0行应自愈而非致命退出");
			Assertions.assertTrue(takeover.isScoped(scope), "自愈后scope应完成stamp登记");
			Assertions.assertEquals(takeover.getMyEpoch(), scope.stampedEpoch, "stamp应写当前epoch");
			var lease = TakeoverTestEnv.readLease(app, myId);
			Assertions.assertEquals(takeover.getMyEpoch(), lease[0], "租约epoch应被自愈重写");
			Assertions.assertTrue(lease[1] > System.currentTimeMillis(), "租约应被自愈续期（非伪造的过期态）");
		} finally {
			safeStop(app);
		}
	}

	/** 恢复默认fatal动作前先停App：测试注入的计数器在停机竞态下不误退出进程（stop后模块引用被置空，先捕获）。 */
	private static void safeStop(Application app) throws Exception {
		var takeover = app.getTakeover();
		app.stop();
		if (takeover != null)
			takeover.setFatalAction(null);
	}
}
