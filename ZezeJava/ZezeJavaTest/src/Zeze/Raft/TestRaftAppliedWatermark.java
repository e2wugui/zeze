package Zeze.Raft;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.TestFlushRetryApply.BListBean;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 应用完成水位与状态数据、unique终态存根同一个WriteBatch原子提交。
 * <p>
 * 此前"应用日志N"分裂成三个事实：状态数据（状态机库批次）、unique终态存根（日志库
 * 独立put）、lastApplied（纯内存）——快照fence取内存值，存在"内容超前fence"窗口
 * （存根写失败后lastApplied停在N-1而存储已含N），靠pending推迟兜底。现在
 * Rocks.flush把终态存根与水位写进数据的同一个WriteBatch：一次commit即完整应用
 * 事实，任意时刻的物理拷贝内容与水位原子一致——快照fence直接读水位，FlushException
 * 补偿窗口内水位与内容同停于上一条，快照照常进行且fence准确；水位行不存在（升级后
 * 首次apply前）走legacy回退（lastApplied解码+pending推迟）。
 * <p>
 * headless构造Rocks（不start server、无网络），脚手架见RaftHeadlessSupport。
 */
@Fast
public class TestRaftAppliedWatermark {
	private static final int SERVER_ID = FastServerIds.TEST_RAFT_APPLIED_WATERMARK;
	private static final String raftName = RaftHeadlessSupport.raftName(SERVER_ID);
	private static final String dbHome = "TestRaftAppliedWatermark.raft";
	private static final String templateName = "tAppliedWatermark";
	private static final String clientId = "test.appliedWatermark";

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogList1<>(Integer.class));
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() {
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	/** apply成功=数据+终态存根+水位同批落盘；水位即快照fence；重试同请求命中终态存根。 */
	@Test
	public void testApplyCommitsWatermarkAndStubAtomically() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism,
				RaftHeadlessSupport.newRaftConfig(SERVER_ID, dbHome), new Config(), false)) {
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			RaftHeadlessSupport.seedStorage(table, 1, 10, 20);

			var changes = RaftHeadlessSupport.captureUniqueChanges(rocks, table, clientId,
					new Zeze.Net.Binary("fake.rpc.result.appliedWatermark".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
			var raftLog = new RaftLog(1, 1, changes);
			var callbackCount = new AtomicInteger();
			raftLog.setLeaderCallback((log, success) -> callbackCount.incrementAndGet());
			var logSequence = rocks.getRaft().getLogSequence();
			logSequence.saveLog(raftLog);
			RaftHeadlessSupport.leaderAppendLogsOf(logSequence).put(1L, raftLog);

			assertNull(rocks.readAppliedWatermark(), "apply前无水位行");

			logSequence.tryApply(raftLog, 1);
			assertEquals(1, logSequence.getLastApplied());
			assertEquals(List.of(10, 20, 30), RaftHeadlessSupport.readStorage(table, 1), "数据恰好应用一次");

			var watermark = rocks.readAppliedWatermark();
			assertNotNull(watermark, "apply成功必须落水位行");
			assertEquals(1L, watermark.index());
			assertEquals(1L, watermark.term());

			// 换主后同请求重发：双读命中状态机侧终态存根（已应用+携带结果）。
			var state = logSequence.tryGetRequestState(RaftHeadlessSupport.newRetriedRpc(changes, clientId));
			assertNotNull(state);
			Assertions.assertNotSame(UniqueRequestState.NOT_FOUND, state);
			assertTrue(state.isApplied(), "终态存根必须是已应用状态");
			Assertions.assertEquals(changes.getRpcResult(), state.getRpcResult(), "终态存根必须携带rpcResult");

			// 快照fence来自水位（不再解码日志、不受pending窗口影响）。
			var result = rocks.snapshot(Path.of(dbHome, "snapshot.t1.zip").toString());
			assertTrue(result.success);
			assertEquals(1L, result.lastIncludedIndex, "fence必须等于应用水位");
			assertEquals(1, callbackCount.get());
		}
	}

	/**
	 * 恢复整链自洽：快照fence来自水位，终态存根与水位随快照内容走，恢复后仍可查，
	 * 恢复基线上的后缀重放正常应用一次。水位路径不查阅pendingFlushApplies——
	 * 补偿窗口（批次未提交）内水位与内容同停于上一条已提交日志，快照自洽照常进行。
	 */
	@Test
	public void testSnapshotFenceConsistentAndRidesRestoreCycle() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism,
				RaftHeadlessSupport.newRaftConfig(SERVER_ID, dbHome), new Config(), false)) {
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			RaftHeadlessSupport.seedStorage(table, 1, 10, 20);

			var changes = RaftHeadlessSupport.captureUniqueChanges(rocks, table, clientId,
					new Zeze.Net.Binary("fake.rpc.result.appliedWatermark".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
			var raftLog = new RaftLog(1, 1, changes);
			var logSequence = rocks.getRaft().getLogSequence();
			logSequence.saveLog(raftLog);
			RaftHeadlessSupport.leaderAppendLogsOf(logSequence).put(1L, raftLog);
			logSequence.tryApply(raftLog, 1);
			assertEquals(List.of(10, 20, 30), RaftHeadlessSupport.readStorage(table, 1));

			var result = rocks.snapshot(Path.of(dbHome, "snapshot.t2.zip").toString());
			assertTrue(result.success, "水位路径：快照不推迟");
			assertEquals(1L, result.lastIncludedIndex, "fence=水位=内容边界，自洽");

			// 恢复整链：终态存根与水位随快照内容走，恢复后仍可查。
			rocks.loadSnapshot(logSequence.getCommittedSnapshotFile());
			assertEquals(List.of(10, 20, 30), RaftHeadlessSupport.readStorage(table, 1), "恢复后内容与fence一致");
			var watermark = rocks.readAppliedWatermark();
			assertNotNull(watermark, "水位行随快照内容走");
			assertEquals(1L, watermark.index());
			var state = logSequence.tryGetRequestState(RaftHeadlessSupport.newRetriedRpc(changes, clientId));
			assertNotNull(state);
			assertTrue(state.isApplied(), "终态存根随快照走，压缩/恢复后重发仍命中");

			// 恢复后按新边界重放条目2（模拟leader同步）：正常应用一次。
			// RaftLog构造顺序为(term, index, log)。
			var changes2 = RaftHeadlessSupport.captureChanges(rocks, table, 30);
			var raftLog2 = new RaftLog(1, 2, changes2);
			logSequence.saveLog(raftLog2);
			logSequence.tryApply(raftLog2, 1);
			assertEquals(List.of(10, 20, 30, 30), RaftHeadlessSupport.readStorage(table, 1), "恢复基线上的正常重放");
			assertEquals(2, logSequence.getLastApplied());
		}
	}

	/** legacy回退：无水位行（升级后首次apply前的窗口）时沿用旧fence路径，pending推迟仍生效。 */
	@SuppressWarnings("unchecked")
	@Test
	public void testLegacyDeferWhenWatermarkRowAbsent() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism,
				RaftHeadlessSupport.newRaftConfig(SERVER_ID, dbHome), new Config(), false)) {
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			RaftHeadlessSupport.seedStorage(table, 1, 10, 20); // 直写存储：无apply，无水位行（旧格式库的等价形态）

			assertNull(rocks.readAppliedWatermark(), "无水位行走legacy路径");
			// 注入pending登记（putPendingFlush包内不可达，反射置非空即可——checkpoint只查isEmpty）。
			var field = Rocks.class.getDeclaredField("pendingFlushApplies");
			field.setAccessible(true);
			((LongConcurrentHashMap<Object>)field.get(rocks)).put(1L, new Object()); // 内容可能超前lastApplied

			var result = rocks.snapshot(Path.of(dbHome, "snapshot.t3.zip").toString());
			assertFalse(result.success, "legacy路径：pending窗口内必须推迟（短期修复降级为兼容层）");
			assertFalse(java.nio.file.Files.exists(Path.of(dbHome, "snapshot.t3.zip")), "推迟不得产出快照文件");
			assertEquals(0, rocks.getRaft().getLogSequence().getFirstIndex(), "推迟不得提交");
		}
	}
}
