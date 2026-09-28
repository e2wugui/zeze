package Zeze.Dbh2;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Config;
import Zeze.Dbh2.Dbh2AgentManager;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Dbh2.Master.MasterTable;
import java.util.concurrent.atomic.AtomicInteger;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND21 GA-C02回归：startRefreshMasterTable任务体异常必须复位refreshMasterTableTask槽位。
 * bug：置null在reload之后且无异常保护——reload内getBuckets对master短暂不可达即抛
 * RuntimeException（拒绝风暴期间高发时刻），scheduleNow任务异常被Task框架吞掉记日志，
 * 置null永不执行；此后门槛if(null!=refreshMasterTableTask)对一个早已异常完结的Future
 * 永久成立，之后所有PrepareBatch拒绝触发的刷新成为no-op直到进程重启（路由缓存陈旧、
 * 每笔多付一轮refused→redirect，死桶agent不再回收）。
 * 修复=置null移入finally（reload失败记warn，下次拒绝重新触发，自愈）。
 * 钉住三件事：失败后槽位必须回到null；失败后再次触发必须真的重新执行reload（自愈）；
 * 成功路径行为不变（reload成功后槽位同样回到null）。
 * serverId 880段为本用例预留。形态：桩MasterAgent.getBuckets按脚本抛异常/回数据，
 * 反射观测私有volatile槽位（不启真实master，@Fast无全局状态）。
 */
@Fast
public class TestFnd21GAC02RefreshMasterTableResetsOnFailure {

	// 脚本化桩master：前failTimes次getBuckets抛异常（模拟master短暂不可达），之后返回空表。
	private static final class ScriptedMasterAgent extends MasterAgent {
		final AtomicInteger getBucketsCalls = new AtomicInteger();
		volatile int failTimes;

		ScriptedMasterAgent(int failTimes) {
			super(new Config());
			this.failTimes = failTimes;
		}

		@Override
		public MasterTable.Data getBuckets(String database, String table) {
			if (getBucketsCalls.incrementAndGet() <= failTimes)
				throw new RuntimeException("injected getBuckets failure (master unreachable)");
			return new MasterTable.Data();
		}

		@Override
		public void stop() {
			// 未start的服务：跳过service.stop()。
		}
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, MasterAgent> getMasterAgentMap(Dbh2AgentManager manager) throws Exception {
		Field field = Dbh2AgentManager.class.getDeclaredField("masterAgent");
		field.setAccessible(true);
		return (ConcurrentHashMap<String, MasterAgent>)field.get(manager);
	}

	private static Future<?> getRefreshTask(Dbh2AgentManager manager) throws Exception {
		Field field = Dbh2AgentManager.class.getDeclaredField("refreshMasterTableTask");
		field.setAccessible(true);
		return (Future<?>)field.get(manager);
	}

	// BooleanSupplier不容checked异常：反射读取包一层。
	private static boolean isRefreshTaskNull(Dbh2AgentManager manager) {
		try {
			return getRefreshTask(manager) == null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void waitUntil(java.util.function.BooleanSupplier condition, String message, long timeoutMs)
			throws InterruptedException {
		var deadline = System.currentTimeMillis() + timeoutMs;
		while (!condition.getAsBoolean()) {
			if (System.currentTimeMillis() > deadline)
				Assertions.fail(message);
			//noinspection BusyWait
			Thread.sleep(20);
		}
	}

	@Test
	public void testSlotResetsAfterFailedReloadAndSelfHeals(@TempDir Path tempDir) throws Exception {
		Zeze.Util.Task.tryInitThreadPool(); // scheduleNow任务需要任务线程池
		// 远程提交模式配置：Dbh2AgentManager构造不创建本地Commit/CommitRocks（无磁盘副作用）。
		var config = Config.load(Fnd19GADStubSupport.writeRemoteCommitConfig(tempDir).toString());
		var manager = new Dbh2AgentManager(new Fnd19GADStubSupport.NullServiceAgent(), config, 880);
		try {
			var stub = new ScriptedMasterAgent(1); // 首次reload失败，第二次成功
			getMasterAgentMap(manager).put("127.0.0.1_1", stub);

			// 第一次触发：任务已调度（槽位非null），200ms后任务体执行，reload抛异常。
			manager.startRefreshMasterTable("127.0.0.1_1", "db1", "t1");
			Assertions.assertNotNull(getRefreshTask(manager), "调度后槽位必须非null");
			waitUntil(() -> stub.getBucketsCalls.get() >= 1, "刷新任务必须实际执行reload", 10_000);

			// bug时：任务体异常完结，置null被跳过，槽位对已完结Future永久非null。
			waitUntil(() -> isRefreshTaskNull(manager),
					"reload失败后槽位必须复位null（bug：异常完结的Future永久占据门槛，刷新从此no-op）", 10_000);

			// 失败自愈：master恢复后再次触发，必须真的重新调度并执行reload（脚本第二次返回数据）。
			manager.startRefreshMasterTable("127.0.0.1_1", "db1", "t1");
			waitUntil(() -> stub.getBucketsCalls.get() >= 2, "复位后再次触发必须重新执行reload（bug：第二次触发被no-op）", 10_000);
			waitUntil(() -> isRefreshTaskNull(manager), "成功的reload同样必须复位槽位", 10_000);
		} finally {
			manager.stop();
		}
	}
}
