package UnitTest.Zeze.Trans;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Procedure;
import Zeze.Util.TaskSpec;
import demo.Module1.Table3;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND3-01 回归：Transaction.perform 异常路径（throwRedo 抛 GoBackZeze）丢失
 * alwaysReleaseLockWhenRedo→RedoAndReleaseLock 升级。
 *
 * 触发链（单线程可确定性构造，无需真实并发窗口）：
 * 1. procP 第1轮访问A后挂起，主线程提交对A的冲突修改（时间戳前移），procP 修改A正常返回；
 *    perform→lockAndCheck 发现冲突返回 Redo，但按"冲突也继续加锁"保留A的写锁。
 * 2. 第2轮（持锁重入）首次走到 selectCopy(B)→setAlwaysReleaseLockWhenRedo 发现 holdLocks
 *    非空→throwRedo("Redo: AlwaysReleaseLock")。修复前 catch 的 case Redo 直接赋 Redo，
 *    回不到 try 内的升级点：每轮都在 selectCopy 处立即抛出（不释放锁、不 sleep），
 *    持锁空转到 256 次后 TooManyTry。
 * 3. 修复后该 Redo 升级为 RedoAndReleaseLock：释放全部锁并 sleep 后重试，
 *    第3轮 selectCopy 不再抛出，事务正常提交。
 */
@Fast
public class TestAlwaysReleaseLockWhenRedo {
	// 独立serverId隔离本地zeze_cache目录与其他@Fast测试（T2组：700起；本类750起）。
	private static final AtomicInteger nextServerId = new AtomicInteger(750);

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(nextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("l4_always_release_test_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestAlwaysReleaseLockWhenRedo@" + conf.getServerId(), conf);
	}

	@Test
	public void testExceptionPathRedoUpgradesToReleaseLock() throws Exception {
		var app = newApp();
		var table = new Table3();
		app.addTable("", table);
		app.start();
		try {
			var keyA = 881001L; // 冲突记录：第1轮加锁保留
			var keyB = 881002L; // selectCopy 目标：第2轮起首次访问
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				table.getOrAdd(keyA).setInt_1(100);
				table.getOrAdd(keyB).setInt_1(200);
				return Procedure.Success;
			}, "prepare").call());

			var round = new AtomicInteger();
			var firstRoundLoaded = new CountDownLatch(1); // procP第1轮已加载A
			var conflictCommitted = new CountDownLatch(1); // 主线程冲突修改已提交

			Future<Long> procP = TaskSpec.ofProcedure(app.newProcedure(() -> {
				int r = round.incrementAndGet();
				var v = table.getOrAdd(keyA); // 每轮先访问A，第1轮在此取时间戳快照
				if (r == 1) {
					firstRoundLoaded.countDown();
					// 等主线程提交冲突修改；本轮不碰selectCopy，让alwaysReleaseLockWhenRedo保持false
					if (!conflictCommitted.await(30, TimeUnit.SECONDS))
						return Procedure.Exception;
					v.setInt_1(1);
					return Procedure.Success;
				}
				// 第2轮起带锁走到这里：selectCopy发现持锁→throwRedo（"Redo: AlwaysReleaseLock"）
				var b = table.selectCopy(keyB);
				Assertions.assertNotNull(b);
				Assertions.assertEquals(200, b.getInt_1());
				v.setInt_1(2);
				return Procedure.Success;
			}, "procP")).submitNow();

			Assertions.assertTrue(firstRoundLoaded.await(30, TimeUnit.SECONDS), "procP第1轮未启动");
			// 冲突修改：procP第1轮lockAndCheck将因时间戳不一致返回Redo并保留A的写锁
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				table.getOrAdd(keyA).setInt_1(999);
				return Procedure.Success;
			}, "procQ").call());
			conflictCommitted.countDown();

			// 修复前：持锁在selectCopy处每轮立即抛出，空转256次→TooManyTry；
			// 修复后：升级RedoAndReleaseLock，释放锁sleep重试，第3轮提交成功。
			Assertions.assertEquals(Procedure.Success, (long)procP.get(), "rounds=" + round.get());
			Assertions.assertTrue(round.get() <= 8, "redo轮数异常，疑似持锁空转: rounds=" + round.get());

			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				Assertions.assertEquals(2, table.getOrAdd(keyA).getInt_1());
				return Procedure.Success;
			}, "verify").call());
		} finally {
			app.stop();
		}
	}
}
