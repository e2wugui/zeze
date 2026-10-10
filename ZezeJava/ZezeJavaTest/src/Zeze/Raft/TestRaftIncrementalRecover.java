package Zeze.Raft;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.TestFlushRetryApply.BListBean;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 水位增量恢复：重启跳过loadSnapshot的O(库大小)解压+restore拷贝，仅重放水位之后。
 * <p>
 * 状态机库内容由应用水位原子自描述（水位与数据、终态存根同一个WriteBatch），重启时
 * 水位有效（存在于[firstIndex, lastIndex]且term与日志一致）则lastApplied直接定位到
 * 水位——盘上前缀内容存活，Raft后缀重放机制以lastApplied为起点，(firstIndex, 水位]
 * 不会被再次应用。此前每次重启无条件loadSnapshot：解压zip+RocksDB整库restore拷贝，
 * 成本O(库大小)与后缀长度无关。校验失败（无水位行的旧格式库等）返回false回退全量
 * 恢复。headless两段式：run1造"业务快照fence=1+水位=3"的存活状态，close后同DbHome
 * 重开模拟重启（run2构造器走真实Raft.recover接线）。脚手架见RaftHeadlessSupport。
 */
@Fast
public class TestRaftIncrementalRecover {
	private static final int SERVER_ID = FastServerIds.TEST_RAFT_INCREMENTAL_RECOVER;
	private static final String raftName = RaftHeadlessSupport.raftName(SERVER_ID);
	private static final String dbHome = "TestRaftIncrementalRecover.raft";
	private static final String templateName = "tIncrementalRecover";

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

	@Test
	public void testRecoverSkipsRestoreAndReplaysOnlyAboveWatermark() throws Exception {
		// run1：业务条目1后应用心跳2，水位停在1，快照边界必须允许是业务日志。
		// 重启的构造期还没有注册表模板，边界检查不得解码业务内容。
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism,
				RaftHeadlessSupport.newRaftConfig(SERVER_ID, dbHome), new Config(), false)) {
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			RaftHeadlessSupport.seedStorage(table, 1, 10, 20);

			RaftHeadlessSupport.applyEntry(rocks, table, 1, 30);
			var seq = rocks.getRaft().getLogSequence();
			var heartbeat = new RaftLog(1, 2, new HeartbeatLog());
			seq.saveLog(heartbeat);
			seq.tryApply(heartbeat, 1);
			assertEquals(2, seq.getLastApplied());
			var snap = rocks.snapshot(java.nio.file.Path.of(dbHome, "snapshot.run1.zip").toString());
			assertTrue(snap.success);
			assertEquals(1, snap.lastIncludedIndex, "业务日志作为快照边界");
			assertEquals(1, seq.getFirstIndex());

			RaftHeadlessSupport.applyEntry(rocks, table, 3, 50);
			assertEquals(List.of(10, 20, 30, 50), RaftHeadlessSupport.readStorage(table, 1));
			assertEquals(3L, rocks.readAppliedWatermark().index());
		}

		// run2：同DbHome重开=重启。水位有效必须增量恢复：跳过restore、lastApplied定位到
		// 水位（legacy为firstIndex=1并整库重放）、盘上前缀内容存活。
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism,
				RaftHeadlessSupport.newRaftConfig(SERVER_ID, dbHome), new Config(), false)) {
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			var logSequence = rocks.getRaft().getLogSequence();
			assertEquals(1, logSequence.getFirstIndex());
			assertEquals(3, logSequence.getLastIndex());
			assertTrue(new File(logSequence.getCommittedSnapshotFile()).isFile(), "已提交快照存在");

			assertEquals(3, logSequence.getLastApplied(), "构造器必须直接定位到水位");

			// 只重放水位之后：条目4在存活前缀上正常应用一次（前缀不被重放）。
			RaftHeadlessSupport.applyEntry(rocks, table, 4, 60);
			assertEquals(List.of(10, 20, 30, 50, 60), RaftHeadlessSupport.readStorage(table, 1), "前缀存活+新条目恰好一次");
			assertEquals(4, logSequence.getLastApplied());
			assertEquals(4L, rocks.readAppliedWatermark().index());
		}
	}

	@Test
	public void testNoWatermarkFallsBackToFullRecover() throws Exception {
		// 无apply、无水位行（升级后首次重启/旧格式库的等价形态）：必须返回false走全量恢复。
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism,
				RaftHeadlessSupport.newRaftConfig(SERVER_ID, dbHome), new Config(), false)) {
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			var logSequence = rocks.getRaft().getLogSequence();
			assertFalse(logSequence.tryRecoverFromWatermark(), "无水位行必须回退全量恢复");
			assertEquals(0, logSequence.getLastApplied());
		}
	}

	private void saveSnapshotAndApplySuffix() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism,
				RaftHeadlessSupport.newRaftConfig(SERVER_ID, dbHome), new Config(), false)) {
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			RaftHeadlessSupport.seedStorage(table, 1, 10, 20);
			assertTrue(rocks.snapshot(Path.of(dbHome, "snapshot.base.zip").toString()).success);
			RaftHeadlessSupport.applyEntry(rocks, table, 1, 30);
			assertEquals(List.of(10, 20, 30), RaftHeadlessSupport.readStorage(table, 1));
		}
	}

	private void assertRestartRestoresSnapshot() throws Exception {
		// 捕获构造中的Raft，构造失败时也释放已打开的日志库句柄。
		var startingRaft = new AtomicReference<Raft>();
		try {
			var rocks = new Rocks(raftName, RocksMode.Pessimism,
					RaftHeadlessSupport.newRaftConfig(SERVER_ID, dbHome), new Config(), false,
					(raft, name, config) -> {
						startingRaft.set(raft);
						return new Server(raft, name, config);
					}, new TaskOneByOneByKey());
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			var seq = rocks.getRaft().getLogSequence();
			assertEquals(0, seq.getFirstIndex());
			assertEquals(0, seq.getLastApplied(), "全量恢复回到快照边界");
			assertNull(rocks.readAppliedWatermark(), "损坏水位不得留在恢复后的库中");
			assertEquals(List.of(10, 20), RaftHeadlessSupport.readStorage(table, 1));
			// 原日志后缀仍可在快照基线上重放，非幂等增量恰好应用一次。
			seq.tryApply(seq.readLog(1), 1);
			assertEquals(List.of(10, 20, 30), RaftHeadlessSupport.readStorage(table, 1));
		} finally {
			var raft = startingRaft.get();
			if (raft != null)
				((Rocks)raft.getStateMachine()).close();
		}
	}

	@Test
	public void testCorruptLocalDatabaseRestoresCommittedSnapshot() throws Exception {
		saveSnapshotAndApplySuffix();
		Files.writeString(Path.of(dbHome, "statemachine", "CURRENT"), "missing-manifest");
		assertRestartRestoresSnapshot();
	}

	@Test
	public void testMalformedWatermarkRestoresCommittedSnapshot() throws Exception {
		saveSnapshotAndApplySuffix();
		try (var db = new RocksDatabase(Path.of(dbHome, "statemachine").toString())) {
			db.getOrAddTable("Zeze.Raft.RocksRaft.AppliedWatermark").put(new byte[]{0}, new byte[]{127});
		}
		assertRestartRestoresSnapshot();
	}
}
