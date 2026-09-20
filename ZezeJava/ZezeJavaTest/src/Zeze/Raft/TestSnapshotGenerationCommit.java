package Zeze.Raft;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RFD1-03：代际快照提交回归——活文件不可变+单点指针翻转。全场景状态构造、无生产
 * 钩子：即时提交/写-翻窗口（held候选启动判死）/翻-删窗口/过期防御/延时推一代。不起server。
 */
@Fast
public class TestSnapshotGenerationCommit {
	private static final String raftName = "127.0.0.1:26380";
	private static final String dbHome = "a3_TestSnapshotGenCommit.raft";

	private Raft raft;

	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:26380" DbHome="a3_TestSnapshotGenCommit.raft">
					<node Host="127.0.0.1" Port="26380"/>
					<node Host="127.0.0.1" Port="26381"/>
					<node Host="127.0.0.1" Port="26382"/>
				</raft>
				""");
	}

	private static Zeze.Raft.StateMachine newSm() {
		return new Zeze.Raft.StateMachine() {
			@Override
			public SnapshotResult snapshot(String path) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void loadSnapshot(String path) {
			}
		};
	}

	private Raft newRaft() throws Exception {
		return newRaft(false);
	}

	private Raft newRaft(boolean snapshotCommitDelayed) throws Exception {
		var conf = newRaftConfig();
		conf.setSnapshotCommitDelayed(snapshotCommitDelayed);
		raft = new Raft(newSm(), raftName, conf);
		raft.getLogSequence().setWriteOptions(RocksDatabase.getDefaultWriteOptions());
		return raft;
	}

	private void closeRaft() {
		if (raft != null) {
			try {
				raft.getLogSequence().close();
				raft.shutdown();
			} catch (Exception ignore) {
			}
			raft = null;
		}
	}

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() {
		closeRaft();
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	// 基础现场：logs=[0,5..12]、已提交gen(5)（firstIndex=5）。返回持有现场的LogSequence。
	private LogSequence forgeCommittedBase() throws Exception {
		var r = newRaft();
		var ls = r.getLogSequence();
		for (long i = 5; i <= 12; i++)
			ls.saveLog(new RaftLog(1, i, new HeartbeatLog()));
		ls.commitSnapshotNow(newStaging(5), 5);
		assertEquals(5, ls.getFirstIndex());
		return ls;
	}

	// 候选staging文件（tmp族命名，模拟状态机/接收侧写完的候选）。
	private static Path newStaging(long index) throws Exception {
		var staging = Paths.get(dbHome, LogSequence.snapshotFileName + "." + index + ".tmp");
		Files.write(staging, new byte[]{1, 2, 3});
		return staging;
	}

	// 即时提交：候选发布为gen、指针翻转、读者解析到gen。
	@Test
	public void testImmediateCommitPublishesGenAndFlips() throws Exception {
		var ls = forgeCommittedBase();
		ls.commitSnapshotNow(newStaging(8), 8);
		assertEquals(8, ls.getFirstIndex(), "指针必须翻转到新代");
		assertTrue(Files.exists(ls.genSnapshotPath(8)), "gen(8)必须就位");
		assertTrue(ls.getCommittedSnapshotFile().endsWith(
				LogSequence.snapshotFileName + ".8"), "读者必须解析到gen(8)");
	}

	// 写-翻窗口（延时held候选）：gen就位、指针未翻——重启按"无指针指认即垃圾"判死，
	// 旧代完整可用（对应崩溃穷举表的"发布后翻转前崩"行）。
	@Test
	public void testHeldCandidateSweptAtStartup() throws Exception {
		forgeCommittedBase();
		closeRaft();

		var r2 = newRaft(true); // 延时模式：候选只发布不翻指针
		var ls2 = r2.getLogSequence();
		ls2.commitSnapshot(newStaging(8).toString(), 8);
		assertEquals(5, ls2.getFirstIndex(), "延时模式候选不得翻指针");
		assertTrue(Files.exists(ls2.genSnapshotPath(8)), "held候选必须是已发布的gen文件");
		closeRaft();

		var r3 = newRaft();
		var ls3 = r3.getLogSequence();
		assertEquals(5, ls3.getFirstIndex(), "未提交候选崩溃后必须丢弃");
		assertFalse(Files.exists(ls3.genSnapshotPath(8)), "gen孤儿必须被启动清扫");
		assertTrue(Files.exists(ls3.genSnapshotPath(5)), "已提交旧代必须完好");
	}

	// 翻-删窗口：指针已翻、旧代未删——重启清扫旧代（幂等）。
	@Test
	public void testStaleGenSweptAtStartup() throws Exception {
		forgeCommittedBase();
		Files.write(Paths.get(dbHome, LogSequence.snapshotFileName + ".3"), new byte[]{9}); // 伪造删除中途残留的旧代
		closeRaft();

		var r = newRaft();
		var ls = r.getLogSequence();
		assertEquals(5, ls.getFirstIndex());
		assertFalse(Files.exists(ls.genSnapshotPath(3)), "index != firstIndex 的gen孤儿必须清扫");
		assertTrue(Files.exists(ls.genSnapshotPath(5)));
	}

	// 过期防御：候选M < firstIndex 到达——指针不动、候选丢弃（本地快照被更新的
	// InstallSnapshot超越的竞态防御）。
	@Test
	public void testStaleCandidateDiscarded() throws Exception {
		var ls = forgeCommittedBase();
		ls.commitSnapshotNow(newStaging(3), 3);
		assertEquals(5, ls.getFirstIndex(), "过期候选不得回退指针");
		assertFalse(Files.exists(ls.genSnapshotPath(3)), "过期候选必须丢弃");
		assertTrue(Files.exists(ls.genSnapshotPath(5)), "当前代不受影响");
	}

	// 延时运行期语义：held M后新候选N到达——M被提交（翻指针+清扫更旧），N成为held。
	@Test
	public void testDelayedCommitAdvancesOneGeneration() throws Exception {
		forgeCommittedBase();
		closeRaft();

		var r = newRaft(true);
		var ls = r.getLogSequence();
		ls.commitSnapshot(newStaging(7).toString(), 7); // held(7)
		assertEquals(5, ls.getFirstIndex());
		ls.commitSnapshot(newStaging(9).toString(), 9); // 提交held(7)，新held(9)
		assertEquals(7, ls.getFirstIndex(), "新候选到达必须提交上一代held");
		assertTrue(Files.exists(ls.genSnapshotPath(7)), "已提交held必须是gen(7)");
		assertTrue(Files.exists(ls.genSnapshotPath(9)), "新候选必须是held gen(9)");
		assertFalse(Files.exists(ls.genSnapshotPath(5)), "提交后旧代必须被清扫");
		assertTrue(ls.getCommittedSnapshotFile().endsWith(
				LogSequence.snapshotFileName + ".7"), "读者必须解析到gen(7)");
	}
}
