package Zeze.Game;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Game.Rank.BValueLong;
import Zeze.Config;
import Zeze.Transaction.Procedure;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 进程级榜单缓存的发布必须是"本轮构建值+本轮提交戳"成对出现：
 * 事务A构建后未提交期间，事务B重建（含读己之写）后回滚，A随后提交——
 * B回滚的值不得被A的提交盖章为新鲜快照提供。
 * 修复前value在事务内立即写入共享缓存对象，A提交只盖BuildTime，
 * 结果缓存里是B的回滚值+A的新鲜度，未提交的进榜被当作已提交排名提供。
 * 修复后本轮结果只经局部快照返回给调用者，value+time一起延迟到提交后发布。
 */
@Fast
public class TestRankCacheCrossTransactionPollution {

	private static final int ServerId = FastServerIds.TEST_RANK_CACHE_CROSS_TRANSACTION;

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(ServerId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("rank_cross_tx_" + ServerId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestRankCacheCrossTx", conf);
	}

	private static Rank newRank(Application zeze) {
		var app = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		new ProviderApp(zeze);
		var rank = new Rank(app);
		rank.Initialize(app);
		return rank;
	}

	@Test
	public void rolledBackValueNotStampedByOtherTransactionCommit() throws Exception {
		var app = newApp();
		var rank = newRank(app);
		rank.setFuncConcurrentLevel(t -> 1);
		rank.setFuncRankSize(t -> 4);
		app.start();
		try {
			var key = Rank.newRankKey(1, 888L);
			var builtA = new CountDownLatch(1);
			var rolledB = new CountDownLatch(1);
			var rcA = new AtomicLong(Long.MIN_VALUE);
			var a = Thread.ofPlatform().start(() -> rcA.set(app.newProcedure(() -> {
				rank.getRankTotal(key); // A构建空表快照，未提交
				builtA.countDown();
				rolledB.await();
				return 0L; // B回滚后才提交
			}, "buildA").call()));
			assertTrue(builtA.await(10, java.util.concurrent.TimeUnit.SECONDS), "A必须先完成构建");

			// B：进榜+重建缓存（含读己之写），返回LogicError强制回滚
			assertEquals(Procedure.LogicError, app.newProcedure(() -> {
				rank.updateRank(77, key, 77L, new BValueLong(9999));
				rank.getRankTotal(key);
				return Procedure.LogicError;
			}, "rollbackB").call(), "B必须以回滚收场");

			rolledB.countDown();
			a.join(10_000);
			assertEquals(0L, rcA.get(), "A必须成功提交");

			var seen = new long[]{Long.MIN_VALUE, Long.MIN_VALUE};
			assertEquals(0L, app.newProcedure(() -> {
				seen[0] = rank.getRankPosition(key, 77L); // 走缓存
				seen[1] = rank.getRankDirect(key).getRankList().size(); // 已提交直读
				return 0L;
			}, "verify").call());
			assertEquals(0L, seen[1], "已提交视图必须为空（B已回滚）");
			assertEquals(-1L, seen[0], "缓存不得提供B回滚的进榜（修复前B的value被A的提交盖章为新鲜）");
		} finally {
			try {
				app.stop();
			} catch (Exception ignored) {
			}
		}
	}
}
