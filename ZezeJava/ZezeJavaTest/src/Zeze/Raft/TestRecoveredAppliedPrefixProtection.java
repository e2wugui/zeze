package Zeze.Raft;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Raft.RocksRaft.TestFlushRetryApply.BListBean;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Extra;
import harness.Fast;
import harness.ForkedJvm;
import harness.ForkedJvmExtension;
import harness.FastServerIds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 水位恢复后的业务前缀仍然是已提交状态：旧快照不得回退它，冲突复制不得截断它。
 * 真实 Rocks 两段式重启，快照边界 1、磁盘水位 3。断言业务数据和日志的存活，
 * 不把 commitIndex 的具体实现值当成正确性的替代品。
 */
@Fast
@Extra
@ForkedJvm(timeoutSeconds = 45)
public class TestRecoveredAppliedPrefixProtection {
	private static final int SERVER_ID = FastServerIds.TEST_RECOVERED_APPLIED_PREFIX_PROTECTION;
	private static final String RAFT_NAME = RaftHeadlessSupport.raftName(SERVER_ID);
	private static final String TEMPLATE_NAME = "tRecoveredAppliedPrefixProtection";
	private static final List<Integer> APPLIED_DATA = List.of(10, 20, 30, 40, 50);

	@TempDir
	private Path dbHome;

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogList1<>(Integer.class));
	}

	private Rocks openRocks() throws Exception {
		return new Rocks(RAFT_NAME, RocksMode.Pessimism,
				RaftHeadlessSupport.newRaftConfig(SERVER_ID, dbHome.toString()), new Config(), false);
	}

	private static Table<Integer, BListBean> openTable(Rocks rocks) {
		rocks.registerTableTemplate(TEMPLATE_NAME, Integer.class, BListBean.class);
		return requireNonNull(rocks.<Integer, BListBean>getTableTemplate(TEMPLATE_NAME)).openTable(0);
	}

	/** 保存索引 2 的真实状态机快照，供稍后模拟延迟到达的 InstallSnapshot。 */
	private byte[] captureSnapshotWithoutCommit(Rocks rocks) throws Exception {
		var result = new StateMachine.SnapshotResult();
		var checkpoint = rocks.checkpoint(result);
		assertNotNull(checkpoint);
		assertEquals(2, result.lastIncludedIndex);
		var backup = dbHome.resolve("old-install-backup");
		var snapshot = dbHome.resolve("old-install.zip");
		try {
			Files.createDirectories(backup);
			RocksDatabase.backup(checkpoint, backup.toString());
			Rocks.createZipFromDirectory(backup.toString(), snapshot.toString());
			return Files.readAllBytes(snapshot);
		} finally {
			LogSequence.deletedDirectoryAndCheck(new File(checkpoint), 100);
		}
	}

	private byte[] prepareAppliedPrefix() throws Exception {
		return prepareAppliedPrefix(true);
	}

	private byte[] prepareAppliedPrefix(boolean applyThirdEntry) throws Exception {
		try (var rocks = openRocks()) {
			var table = openTable(rocks);
			RaftHeadlessSupport.seedStorage(table, 1, 10, 20);
			RaftHeadlessSupport.applyEntry(rocks, table, 1, 30);
			var snapshot = rocks.snapshot(dbHome.resolve("base.zip").toString());
			assertTrue(snapshot.success);
			assertEquals(1, snapshot.lastIncludedIndex);
			RaftHeadlessSupport.applyEntry(rocks, table, 2, 40);
			var oldSnapshot = captureSnapshotWithoutCommit(rocks);
			if (applyThirdEntry) {
				RaftHeadlessSupport.applyEntry(rocks, table, 3, 50);
				assertEquals(APPLIED_DATA, RaftHeadlessSupport.readStorage(table, 1));
				assertEquals(3, requireNonNull(rocks.readAppliedWatermark()).index());
			} else {
				assertEquals(List.of(10, 20, 30, 40), RaftHeadlessSupport.readStorage(table, 1));
				assertEquals(2, requireNonNull(rocks.readAppliedWatermark()).index());
			}
			return oldSnapshot;
		}
	}

	private static void assertRecoveredPrefix(Rocks rocks, Table<Integer, BListBean> table) throws Exception {
		var seq = requireNonNull(rocks.getRaft()).getLogSequence();
		assertEquals(1, seq.getFirstIndex(), "快照仍停在索引 1");
		assertEquals(3, seq.getLastApplied(), "构造器已从磁盘水位恢复到 3");
		assertEquals(APPLIED_DATA, RaftHeadlessSupport.readStorage(table, 1));
	}

	private static InstallSnapshot oldSnapshot(Raft raft, byte[] bytes) throws Exception {
		var r = new InstallSnapshot();
		r.Argument.setTerm(raft.getLogSequence().getTerm() + 1);
		r.Argument.setLeaderId(RaftHeadlessSupport.raftName(SERVER_ID + 1));
		r.Argument.setLastIncludedIndex(2);
		r.Argument.setLastIncludedTerm(1);
		r.Argument.setOffset(0);
		r.Argument.setData(new Binary(bytes));
		r.Argument.setDone(true);
		r.Argument.setLastIncludedLog(new Binary(requireNonNull(raft.getLogSequence().readLog(2)).encode()));
		return r;
	}

	/** 调用者持有 Raft 锁，让真实 AppendEntries 提交尾日志，后台 apply 暂时无法取得锁。 */
	private static void commitThirdEntryWithoutApplying(Raft raft, RaftLog third, long leaderTerm) throws Exception {
		var seq = raft.getLogSequence();
		raft.getRaftConfig().setBackgroundApplyCount(1);
		var append = new AppendEntries();
		append.Argument.setTerm(leaderTerm);
		append.Argument.setLeaderId(RaftHeadlessSupport.raftName(SERVER_ID + 1));
		append.Argument.setPrevLogIndex(2);
		append.Argument.setPrevLogTerm(requireNonNull(seq.readLog(2)).getTerm());
		append.Argument.setLeaderCommit(3);
		append.Argument.getEntries().add(new Binary(third.encode()));
		append.Argument.setLastEntryIndex(3);
		assertEquals(0, seq.followerOnAppendEntries(append));
		assertTrue(append.Result.getSuccess(), "真实复制已接收尾日志并推进提交位置");
		assertEquals(3, seq.getCommitIndex());
		assertEquals(2, seq.getLastApplied(), "外层 Raft 锁让后台 apply 保持在索引 2");
	}

	private static void assertCommittedTailSurvivesAndApplies(Rocks rocks, Table<Integer, BListBean> table) throws Exception {
		var seq = requireNonNull(rocks.getRaft()).getLogSequence();
		assertEquals(1, seq.getFirstIndex(), "忽略旧快照，不推进快照边界");
		assertEquals(3, seq.getLastIndex());
		assertEquals(3, seq.getCommitIndex());
		assertEquals(2, seq.getLastApplied());
		assertEquals(2, requireNonNull(rocks.readAppliedWatermark()).index());
		assertEquals(List.of(10, 20, 30, 40), RaftHeadlessSupport.readStorage(table, 1));
		var third = seq.readLog(3);
		assertNotNull(third, "已提交但尚未应用的日志 3 必须保留");
		seq.tryApply(third, 1);
		assertEquals(APPLIED_DATA, RaftHeadlessSupport.readStorage(table, 1), "保留的提交尾仍可正常应用");
		assertEquals(3, seq.getLastApplied());
		assertEquals(3, requireNonNull(rocks.readAppliedWatermark()).index());
	}

	@Test
	public void oldSnapshotAfterWatermarkRecoveryKeepsAppliedData() throws Exception {
		var oldSnapshot = prepareAppliedPrefix();
		try (var rocks = openRocks()) {
			var table = openTable(rocks);
			var raft = rocks.getRaft();
			assertRecoveredPrefix(rocks, table);
			var request = oldSnapshot(raft, oldSnapshot);
			assertDoesNotThrow(() -> raft.processInstallSnapshot(request));
			assertEquals(0, request.getResultCode(), "忽略旧快照应成功应答，leader 可继续复制");
			assertEquals(APPLIED_DATA, RaftHeadlessSupport.readStorage(table, 1),
					"边界 2 的延迟快照不得抹掉已落盘的索引 3 数据");
			assertEquals(3, raft.getLogSequence().getLastApplied());
			assertNotNull(raft.getLogSequence().readLog(3), "已应用日志前缀应保留");
			assertEquals(3, requireNonNull(rocks.readAppliedWatermark()).index());
		}
	}

	@Test
	public void snapshotFinalizationAfterApplyAdvanceKeepsDataAndCleansReceive() throws Exception {
		var snapshot = prepareAppliedPrefix(false);
		try (var rocks = openRocks()) {
			var table = openTable(rocks);
			var raft = rocks.getRaft();
			var seq = requireNonNull(raft).getLogSequence();
			assertEquals(1, seq.getFirstIndex());
			assertEquals(2, seq.getLastApplied(), "从水位 2 恢复，边界 2 尚可接收");
			assertEquals(List.of(10, 20, 30, 40), RaftHeadlessSupport.readStorage(table, 1));
			var split = snapshot.length / 2;
			assertTrue(split > 0);
			var first = oldSnapshot(requireNonNull(raft), Arrays.copyOfRange(snapshot, 0, split));
			first.Argument.setDone(false);
			assertEquals(0, raft.processInstallSnapshot(first));
			assertEquals(0, first.getResultCode());
			var receiving = raft.receiveSnapshotting.get(2);
			assertNotNull(receiving, "首块登记接收条目");
			assertTrue(Files.exists(receiving.path));
			assertEquals(split, Files.size(receiving.path), "首块已实际落盘");
			RaftHeadlessSupport.applyEntry(rocks, table, 3, 50);
			assertEquals(3, seq.getLastApplied());
			var last = oldSnapshot(raft, Arrays.copyOfRange(snapshot, split, snapshot.length));
			last.Argument.setTerm(first.Argument.getTerm());
			last.Argument.setOffset(split);
			// 模拟末块已通过初段检查后，apply 抢先推进到 3；直接驱动其锁外接收和收尾，
			// 确保验证的是 finalize 的二次检查，而不是初段的旧快照快速忽略。
			var done = raft.receiveSnapshotting.process(last);
			assertSame(receiving, done, "末块完成同一接收条目");
			assertTrue(requireNonNull(done).finalizing);
			assertEquals(0, seq.endReceiveInstallSnapshot(done, last), "过期收尾成功忽略");
			assertEquals(APPLIED_DATA, RaftHeadlessSupport.readStorage(table, 1));
			assertEquals(3, seq.getLastApplied());
			assertEquals(3, requireNonNull(rocks.readAppliedWatermark()).index());
			assertEquals(1, seq.getFirstIndex(), "不提交边界 2 的旧快照");
			assertNotNull(seq.readLog(3), "已应用日志 3 保留");
			assertNull(raft.receiveSnapshotting.get(2), "收尾清除接收登记");
			assertFalse(Files.exists(receiving.path), "收尾删除已关闭的接收文件");
		}
	}

	@Test
	public void staleFinalChunkAfterApplyAdvanceCleansReceive() throws Exception {
		var snapshot = prepareAppliedPrefix(false);
		try (var rocks = openRocks()) {
			var table = openTable(rocks);
			var raft = rocks.getRaft();
			var seq = requireNonNull(raft).getLogSequence();
			assertEquals(2, seq.getLastApplied());
			var split = snapshot.length / 2;
			var first = oldSnapshot(requireNonNull(raft), Arrays.copyOfRange(snapshot, 0, split));
			first.Argument.setDone(false);
			assertEquals(0, raft.processInstallSnapshot(first));
			assertEquals(0, first.getResultCode());
			var receiving = raft.receiveSnapshotting.get(2);
			assertNotNull(receiving, "首块在应用边界 2 登记并开始接收");
			assertEquals(split, Files.size(receiving.path));
			RaftHeadlessSupport.applyEntry(rocks, table, 3, 50);
			var last = oldSnapshot(raft, Arrays.copyOfRange(snapshot, split, snapshot.length));
			last.Argument.setTerm(first.Argument.getTerm());
			last.Argument.setOffset(split);
			// 与 finalize 交错测试不同，本例末块完整走 RPC 入口：快速忽略也必须收口
			// 首块留下的句柄/文件/登记，否则 isReceivingSnapshot 会阻止本地快照。
			assertDoesNotThrow(() -> raft.processInstallSnapshot(last));
			assertEquals(0, last.getResultCode(), "旧末块成功应答");
			assertEquals(APPLIED_DATA, RaftHeadlessSupport.readStorage(table, 1));
			assertEquals(3, seq.getLastApplied());
			assertEquals(3, requireNonNull(rocks.readAppliedWatermark()).index());
			assertEquals(1, seq.getFirstIndex());
			assertNotNull(seq.readLog(3));
			assertNull(raft.receiveSnapshotting.get(2), "旧末块必须清理首块留下的接收登记");
			assertFalse(Files.exists(receiving.path), "旧末块必须关闭并删除首块留下的接收文件");
		}
	}

	@Test
	public void oldSnapshotBeforeCommittedTailAppliesKeepsDataAndLog() throws Exception {
		var snapshot = prepareAppliedPrefix(false);
		try (var rocks = openRocks()) {
			var table = openTable(rocks);
			var raft = rocks.getRaft();
			var request = oldSnapshot(requireNonNull(raft), snapshot);
			var third = new RaftLog(1, 3, RaftHeadlessSupport.captureChanges(rocks, table, 50));
			raft.lock();
			try {
				commitThirdEntryWithoutApplying(raft, third, request.Argument.getTerm());
				assertDoesNotThrow(() -> raft.processInstallSnapshot(request));
				assertEquals(0, request.getResultCode(), "提交前缀内的旧快照在入口成功忽略");
				assertNull(raft.receiveSnapshotting.get(2), "入口忽略不得登记旧快照");
				assertCommittedTailSurvivesAndApplies(rocks, table);
			} finally {
				raft.unlock();
			}
		}
	}

	@Test
	public void snapshotFinalizationBeforeCommittedTailAppliesKeepsDataAndCleansReceive() throws Exception {
		var snapshot = prepareAppliedPrefix(false);
		try (var rocks = openRocks()) {
			var table = openTable(rocks);
			var raft = rocks.getRaft();
			var split = snapshot.length / 2;
			assertTrue(split > 0);
			var first = oldSnapshot(requireNonNull(raft), Arrays.copyOfRange(snapshot, 0, split));
			first.Argument.setDone(false);
			assertEquals(0, raft.processInstallSnapshot(first));
			assertEquals(0, first.getResultCode());
			var receiving = raft.receiveSnapshotting.get(2);
			assertNotNull(receiving);
			assertEquals(split, Files.size(receiving.path));
			var last = oldSnapshot(raft, Arrays.copyOfRange(snapshot, split, snapshot.length));
			last.Argument.setTerm(first.Argument.getTerm());
			last.Argument.setOffset(split);
			var third = new RaftLog(1, 3, RaftHeadlessSupport.captureChanges(rocks, table, 50));
			raft.lock();
			try {
				commitThirdEntryWithoutApplying(raft, third, first.Argument.getTerm());
				// 模拟末块已通过入口检查后提交位置前进，直接驱动最终安装前的二次检查。
				var done = raft.receiveSnapshotting.process(last);
				assertSame(receiving, done);
				assertTrue(requireNonNull(done).finalizing);
				assertEquals(0, assertDoesNotThrow(() -> raft.getLogSequence().endReceiveInstallSnapshot(done, last)),
						"提交前缀内的旧快照在最终安装前成功忽略");
				assertNull(raft.receiveSnapshotting.get(2), "过期收尾清理接收登记");
				assertFalse(Files.exists(receiving.path), "过期收尾清理接收文件");
				assertCommittedTailSurvivesAndApplies(rocks, table);
			} finally {
				raft.unlock();
			}
		}
	}

	@Test
	@ForkedJvm.ExpectHalt(value = "truncate committed entries", verifyAfterHalt = "verifyConflictingAppend")
	public void conflictingAppendAfterWatermarkRecoveryKeepsAppliedLogs() throws Exception {
		// halt后由另一子JVM重开这个固定于本次临时工作目录下的真实持久库。
		dbHome = Path.of("applied-prefix").toAbsolutePath();
		prepareAppliedPrefix();
		try (var rocks = openRocks()) {
			var table = openTable(rocks);
			var raft = rocks.getRaft();
			var seq = requireNonNull(raft).getLogSequence();
			assertRecoveredPrefix(rocks, table);
			var r = new AppendEntries();
			r.Argument.setTerm(seq.getTerm() + 2);
			r.Argument.setLeaderId(RaftHeadlessSupport.raftName(SERVER_ID + 1));
			r.Argument.setPrevLogIndex(1);
			r.Argument.setPrevLogTerm(1);
			r.Argument.setLeaderCommit(1);
			r.Argument.getEntries().add(new Binary(new RaftLog(r.Argument.getTerm(), 2, new HeartbeatLog()).encode()));
			r.Argument.setLastEntryIndex(2);
			requireNonNull(raft).addAtFatalKill(() -> {
				assertTrue(raft.isShutdown);
				assertEquals(1, requireNonNull(seq.readLog(2)).getTerm(), "已应用日志不得被新term覆盖");
				assertNotNull(seq.readLog(3), "已应用的后续日志不得被截断");
				assertEquals(3, seq.getLastApplied());
				ForkedJvmExtension.fatalCallbackPassed();
			});
			raft.lock();
			try {
				seq.followerOnAppendEntries(r);
				ForkedJvmExtension.unexpectedReturn();
			} finally {
				raft.unlock();
			}
		}
	}

	public static void verifyConflictingAppend(Path workingDirectory) throws Exception {
		var probe = new TestRecoveredAppliedPrefixProtection();
		probe.dbHome = workingDirectory.resolve("applied-prefix");
		probe.setUp();
		try (var rocks = probe.openRocks()) {
			var table = openTable(rocks);
			assertRecoveredPrefix(rocks, table);
			var seq = requireNonNull(rocks.getRaft()).getLogSequence();
			assertEquals(1, requireNonNull(seq.readLog(2)).getTerm(), "halt后已应用日志仍是原term");
			assertNotNull(seq.readLog(3), "halt后后续日志仍存在");
			assertEquals(3, requireNonNull(rocks.readAppliedWatermark()).index());
		}
	}
}
