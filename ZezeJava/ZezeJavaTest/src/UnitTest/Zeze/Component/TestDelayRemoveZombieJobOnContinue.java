package UnitTest.Zeze.Component;

import Zeze.Application;
import Zeze.Builtin.DelayRemove.BJob;
import Zeze.Builtin.DelayRemove.tJobs;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * FND5-21存量治理回归：addJob入口校验只防新增，continueJobs装载期负责清除修复前
 * 已持久化的僵尸行（未注册handleName）——否则走"startJob的NPE被任务框架吞→行不
 * 清理→每次启动重试再失败"的死循环。直接向_tJobs注入僵尸行（绕过addJob，模拟
 * 修复前存量数据），断言continueJobs后僵尸行被删除且合法Job不受影响。
 * @Isolated：与TestDelayRemoveAddJobBeforeStart同因（boot Application的测试并发，
 * 全局表注册互斥），独占运行。
 */
@Fast
@Isolated
public class TestDelayRemoveZombieJobOnContinue {

	// _tJobs是AbstractDelayRemove的protected字段，测试经反射注入存量行（同TestLateProtocolAfterClose口径）。
	@SuppressWarnings("unchecked")
	private static tJobs getJobsTable(Zeze.Component.DelayRemove dr) throws Exception {
		var field = Zeze.Component.AbstractDelayRemove.class.getDeclaredField("_tJobs");
		field.setAccessible(true);
		return (tJobs)field.get(dr);
	}

	@Test
	public void testZombieJobDiscardedOnContinue() throws Exception {
		Task.tryInitThreadPool();
		var conf = TakeoverTestEnv.newConf("dryrun", 600_000, 600_000);
		var app = new Application("TestDelayRemoveZombie", conf);
		try {
			app.start();
			var dr = app.getDelayRemove();
			var serverId = app.getConfig().getServerId();
			var tJobs = getJobsTable(dr);
			dr.register("UnitTest.FND5_21.Legit", (delayRemove, jobId, jobState) -> {
			});

			// 合法Job：走正规addJob（行持久化）。
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				dr.addJob("UnitTest.FND5_21.Legit", EmptyBean.instance);
				return Procedure.Success;
			}, "addLegitJob").call());
			Assertions.assertEquals(1, app.newProcedure(dr::jobCount, "countAfterAdd").call());

			// 注入僵尸行：绕过addJob直接写_tJobs（模拟修复前已持久化的未注册handleName条目）。
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var bJob = new BJob();
				bJob.setJobHandleName("UnitTest.FND5_21.Zombie");
				var bb = ByteBuffer.Allocate(16);
				EmptyBean.instance.encode(bb);
				bJob.setJobState(new Binary(bb));
				tJobs.getOrAdd(serverId).getJobs().put("injectedZombieJob", bJob);
				return Procedure.Success;
			}, "injectZombie").call());
			Assertions.assertEquals(2, app.newProcedure(dr::jobCount, "countAfterInject").call());

			// 装载期治理：僵尸行被删除，合法Job不受影响。
			dr.continueJobs();
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				Assertions.assertNull(tJobs.get(serverId).getJobs().get("injectedZombieJob"),
						"僵尸Job行必须被continueJobs删除（FND5-21存量治理）");
				return Procedure.Success;
			}, "verifyAfterContinue").call());
			Assertions.assertEquals(1, app.newProcedure(dr::jobCount, "countAfterContinue").call(),
					"合法Job行不受治理影响");
		} finally {
			try {
				app.stop();
			} catch (Throwable ignored) {
				// ignored
			}
		}
	}
}
