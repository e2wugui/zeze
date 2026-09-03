package Zeze.Raft;

import java.io.File;
import java.lang.reflect.Method;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LogSequence.waitLogFateDetermined（FND-R2-2）。
 * appendLog 超时/取消后条目仍留在日志中（lastIndex 不回退），命运未定：
 * 可能稍后被提交应用，也可能被新 leader 截断。此时调用方（RocksRaft
 * Transaction.perform）即将按失败返回并在 finally 释放悲观锁——若在条目
 * 应用前放锁，后续同 key 事务会基于应用前的旧值计算并提交，随后本条目
 * 又被应用，造成丢失更新。修复：超时抛 RaftRetryException 前先等待条目
 * 命运确定（已应用或已删除），使调用方在窗口期内继续持锁。
 * 这里直接构造 Rocks（不 start server，无网络与选举流量），用反射写入一条
 * 未决日志，验证等待方法的四条契约：已应用立即返回 / 已删除立即返回 /
 * 未决时等待且有界（超时放弃）/ shutdown 立即返回。
 * 全链路（perform 持锁跨超时窗口 + 网络延迟多数派确认）属集群竞态序列，
 * 由提交信息中的推演核查覆盖。
 */
@Fast
public class TestAppendLogWaitFate {
	private static final String raftName = "127.0.0.1:17630";
	private static final String dbHome = "TestAppendLogWaitFate.raft";

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17630" DbHome="TestAppendLogWaitFate.raft">
					<node Host="127.0.0.1" Port="17630"/>
					<node Host="127.0.0.1" Port="17631"/>
					<node Host="127.0.0.1" Port="17632"/>
				</raft>
				""");
	}

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() {
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	// 反射调用私有的 saveLog 写入一条未决日志（不推进 commitIndex/lastApplied）。
	private static void savePendingLog(LogSequence logSequence, long term, long index) throws Exception {
		Method saveLog = LogSequence.class.getDeclaredMethod("saveLog", RaftLog.class);
		saveLog.setAccessible(true);
		try {
			saveLog.invoke(logSequence, new RaftLog(term, index, new HeartbeatLog()));
		} catch (java.lang.reflect.InvocationTargetException e) {
			throw (Exception)e.getCause();
		}
	}

	private static long elapsedMs(long beginNano) {
		return (System.nanoTime() - beginNano) / 1_000_000L;
	}

	// 已应用（lastApplied >= index）：立即返回。初始库只有 index=0 的日志，lastApplied=0。
	@Test
	public void testAppliedReturnsImmediately() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false)) {
			var logSequence = rocks.getRaft().getLogSequence();
			assertTrue(logSequence.getLastApplied() >= 0);

			var begin = System.nanoTime();
			logSequence.waitLogFateDetermined(0, 60_000);
			assertTrue(elapsedMs(begin) < 2000, "applied entry must not wait");
		}
	}

	// 已删除/不存在（readLog == null）：立即返回。截断后的条目同样走这条路径。
	@Test
	public void testMissingReturnsImmediately() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false)) {
			var logSequence = rocks.getRaft().getLogSequence();

			var begin = System.nanoTime();
			logSequence.waitLogFateDetermined(99, 60_000);
			assertTrue(elapsedMs(begin) < 2000, "missing entry must not wait");
		}
	}

	// 未决（存在且未应用）：一直等待到超时才返回——不能提前放弃（否则悲观锁提前释放），
	// 也不能永久等待（集群选不出leader时业务线程会无限挂住）。
	@Test
	public void testPendingWaitsUntilBoundedTimeout() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false)) {
			var logSequence = rocks.getRaft().getLogSequence();
			var pendingIndex = logSequence.getLastIndex() + 1; // saveLog 不推进 lastIndex 字段
			savePendingLog(logSequence, 1, pendingIndex);

			var begin = System.nanoTime();
			logSequence.waitLogFateDetermined(pendingIndex, 500);
			var elapsed = elapsedMs(begin);
			assertTrue(elapsed >= 490, "pending entry must wait, elapsed=" + elapsed);
			assertTrue(elapsed < 5000, "wait must be bounded, elapsed=" + elapsed);
		}
	}

	// 未决但 raft 已 shutdown：立即返回（进程关闭时不再等待）。
	@Test
	public void testShutdownReturnsWhilePending() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false)) {
			var raft = rocks.getRaft();
			var logSequence = raft.getLogSequence();
			var pendingIndex = logSequence.getLastIndex() + 1; // saveLog 不推进 lastIndex 字段
			savePendingLog(logSequence, 1, pendingIndex);

			raft.isShutdown = true;
			try {
				var begin = System.nanoTime();
				logSequence.waitLogFateDetermined(pendingIndex, 60_000);
				assertTrue(elapsedMs(begin) < 2000, "shutdown must not wait");
			} finally {
				raft.isShutdown = false; // 恢复，让 close() 正常走关闭流程
			}
		}
	}
}
