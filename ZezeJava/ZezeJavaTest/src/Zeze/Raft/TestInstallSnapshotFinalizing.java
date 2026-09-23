package Zeze.Raft;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【raft-02】InstallSnapshot 收尾期间重装首块的竞态：done 块处理后（finalizing 条目
 * 未摘除）endReceiveInstallSnapshot 在 raft 锁外 await removeLogBeforeFuture（可达
 * 秒级）期间，leader 对 done 块的 AppendEntriesTimeout（默认2s）超时重发首块。
 * 修复结构：done 分支置 finalizing（所有权从"传输完成"延长到"收尾完成"），重装块
 * 应答 ResultCodeFinalizingConflict；条目文件唯一命名（.installing.{index}.{seq}，
 * 新安装永不复用旧安装路径，截断竞态结构上无对象）；条目由 endReceiveInstallSnapshot
 * 的 finally 同一性摘除；收尾提交前复核登记表仍持有本条目（被 gc 归属清理/shutdown
 * 撤销即丢弃应答冲突，不做任何重置）且文件尺寸==done时记录的应收总长（拦理论外改动）。
 * 暂停点确定性合成：removeLogBeforeFuture 是 public volatile，置未完成的
 * TaskCompletionSource 即可让 endReceiveInstallSnapshot 精确停在 raft 锁外 await。
 */
@Fast
public class TestInstallSnapshotFinalizing {
	private static final String raftName = "127.0.0.1:17680";
	private static final String dbHome = "TestInstallSnapshotFinalizing.raft";
	private static final String leaderId = "127.0.0.1:17681";

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17680" DbHome="TestInstallSnapshotFinalizing.raft">
					<node Host="127.0.0.1" Port="17680"/>
					<node Host="127.0.0.1" Port="17681"/>
					<node Host="127.0.0.1" Port="17682"/>
				</raft>
				""");
	}

	private static final class NoopStateMachine extends StateMachine {
		@Override
		public SnapshotResult snapshot(String path) {
			return new SnapshotResult();
		}

		@Override
		public void loadSnapshot(String path) {
		}
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

	private Raft newRaft() throws Exception {
		var raft = new Raft(new NoopStateMachine(), raftName, newRaftConfig());
		raft.getLogSequence().setWriteOptions(RocksDatabase.getDefaultWriteOptions());
		return raft;
	}

	private static Path installingPath(long lastIncludedIndex) {
		return Paths.get(dbHome, LogSequence.snapshotFileName + ".installing." + lastIncludedIndex);
	}

	private static InstallSnapshot newChunk(long term, String leader, long lastIncludedIndex,
											long offset, byte[] data, boolean done) {
		var r = new InstallSnapshot();
		r.Argument.setTerm(term);
		r.Argument.setLeaderId(leader);
		r.Argument.setLastIncludedIndex(lastIncludedIndex);
		r.Argument.setLastIncludedTerm(0);
		r.Argument.setOffset(offset);
		r.Argument.setData(new Binary(data));
		r.Argument.setDone(done);
		if (done)
			r.Argument.setLastIncludedLog(new Binary(new RaftLog(0, lastIncludedIndex, new HeartbeatLog()).encode()));
		return r;
	}

	// 在工作线程跑 done 块（其 endReceiveInstallSnapshot 阻塞在 raft 锁外 await 上），
	// 等到 finalizing 置位后返回；pause 即阻塞点，setResult 释放。
	private static final class DoneWorker {
		final InstallSnapshot rpc;
		final TaskCompletionSource<Boolean> pause = new TaskCompletionSource<>();
		private final Thread thread;
		private final AtomicReference<Throwable> error = new AtomicReference<>();

		DoneWorker(Raft raft, byte[] data) {
			rpc = newChunk(raft.getLogSequence().getTerm(), leaderId, 5, 0, data, true);
			thread = new Thread(() -> {
				try {
					raft.processInstallSnapshot(rpc);
				} catch (Throwable t) {
					error.set(t);
				}
			}, "TestInstallSnapshotFinalizing-done");
			thread.setDaemon(true);
		}

		void start(Raft raft) throws InterruptedException {
			raft.getLogSequence().removeLogBeforeFuture = pause;
			thread.start();
			// 等到 done 分支完成：finalizing 置位（此后线程阻塞在 endReceive 的锁外 await）
			var deadline = System.currentTimeMillis() + 10_000;
			while (true) {
				var entry = raft.receiveSnapshotting.get(5);
				if (entry != null && entry.finalizing)
					break;
				if (error.get() != null)
					throw new AssertionError("done worker failed", error.get());
				if (!thread.isAlive())
					throw new AssertionError("done worker exited before finalizing");
				if (System.currentTimeMillis() > deadline)
					throw new AssertionError("timeout waiting for finalizing entry");
				Thread.sleep(10);
			}
		}

		void releaseAndJoin() throws InterruptedException {
			pause.setResult(true);
			thread.join(10_000);
			assertFalse(thread.isAlive(), "done worker must finish after pause release");
			assertNull(error.get(), "done worker must not throw");
		}
	}

	// 核心竞态：收尾期间 leader 超时重装首块，必须应答 FinalizingConflict 且不截断文件；
	// 收尾完成后提交落地、条目摘除。
	@Test
	public void testFinalizingBlocksReinstallAndCommits() throws Exception {
		var raft = newRaft();
		try {
			var logSequence = raft.getLogSequence();
			var worker = new DoneWorker(raft, new byte[]{1, 2, 3, 4, 5});
			worker.start(raft);
			var entryPath = raft.receiveSnapshotting.get(5).path;

			// leader 对 done 块 2s 超时后重装：同 term/leader/边界的首块（offset=0）。
			var reinstall = newChunk(logSequence.getTerm(), leaderId, 5, 0, new byte[]{9, 9, 9}, false);
			assertEquals(Procedure.Success, raft.processInstallSnapshot(reinstall));
			assertEquals(InstallSnapshot.ResultCodeFinalizingConflict, reinstall.getResultCode(),
					"reinstall first chunk must be rejected while finalize in flight");
			assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, Files.readAllBytes(entryPath),
					"file being finalized must stay intact");
			assertTrue(raft.isReceivingSnapshot(), "finalizing entry occupies the registry");

			worker.releaseAndJoin();

			// 收尾成功落地 + finalizing 条目由 finally 摘除。
			assertFalse(raft.isReceivingSnapshot());
			assertTrue(raft.receiveSnapshotting.isEmpty());
			assertFalse(Files.exists(entryPath), ".installing must be consumed by Files.move");
			assertTrue(Files.exists(logSequence.genSnapshotPath(5)), "snapshot.dat.5 must be committed");
			assertEquals(5L, logSequence.getFirstIndex());
			assertEquals(5L, logSequence.getLastIndex());
			assertTrue(logSequence.logsAvailable, "logsAvailable must be restored in finally");
			assertEquals(Procedure.Success, worker.rpc.getResultCode(), "done chunk must be acked after finalize");
		} finally {
			raft.shutdown();
		}
	}

	// 所有权复核：收尾期间条目被撤销（gc 归属清理/shutdown cancelAll 的合成）——登记表
	// 不再持有本条目即不可信：丢弃文件应答冲突码，不做任何重置（旧代码会照常提交）。
	@Test
	public void testOwnershipRevokedDuringFinalizeDiscards() throws Exception {
		var raft = newRaft();
		try {
			var logSequence = raft.getLogSequence();
			var worker = new DoneWorker(raft, new byte[]{1, 2, 3, 4, 5});
			worker.start(raft);
			var entry = raft.receiveSnapshotting.get(5);
			raft.receiveSnapshotting.removeIdentity(5, entry); // 模拟收尾期间所有权被撤销

			worker.releaseAndJoin();

			assertEquals(InstallSnapshot.ResultCodeFinalizingConflict, worker.rpc.getResultCode(),
					"revoked ownership must abort commit and reply FinalizingConflict");
			assertFalse(Files.exists(entry.path), "file of revoked entry must be discarded");
			assertEquals(0L, logSequence.getLastIndex(), "logs must NOT be dropped/rebuilt");
			assertEquals(0L, logSequence.getFirstIndex());
			assertTrue(logSequence.logsAvailable);
			assertNull(raft.receiveSnapshotting.get(5), "entry must stay removed (identity remove no-op)");
		} finally {
			raft.shutdown();
		}
	}

	// 尺寸复核：收尾期间文件被理论外路径改动（磁盘异常/外部干预的合成）——提交前必须
	// 发现尺寸与done时记录的应收总长不符，丢弃文件应答冲突码，不做任何重置
	//（尺寸不符的文件一旦提交，loadSnapshot失败fatalKill且重启加载损坏快照起不来）。
	@Test
	public void testFinalizeSizeMismatchDiscards() throws Exception {
		var raft = newRaft();
		try {
			var logSequence = raft.getLogSequence();
			var worker = new DoneWorker(raft, new byte[]{1, 2, 3, 4, 5});
			worker.start(raft);
			var entry = raft.receiveSnapshotting.get(5);

			Files.write(entry.path, new byte[]{1}); // 模拟收尾期间文件被截断

			worker.releaseAndJoin();

			assertEquals(InstallSnapshot.ResultCodeFinalizingConflict, worker.rpc.getResultCode(),
					"size mismatch must abort commit and reply FinalizingConflict");
			assertFalse(Files.exists(entry.path), "truncated file must be discarded");
			assertEquals(0L, logSequence.getLastIndex(), "logs must NOT be dropped/rebuilt");
			assertEquals(0L, logSequence.getFirstIndex());
			assertTrue(logSequence.logsAvailable);
			assertTrue(raft.receiveSnapshotting.isEmpty(), "finalizing entry must be removed in finally");
		} finally {
			raft.shutdown();
		}
	}

	// gc 口径：finalizing 条目豁免空闲超时（仅告警）；归属失效（term 推进）仍清理。
	@Test
	public void testGcExemptsFinalizingIdleButRemovesStaleOwner() throws Exception {
		var raft = newRaft();
		try {
			var now = System.currentTimeMillis();
			var file = installingPath(5);
			Files.write(file, new byte[]{1, 2, 3});
			var entry = new ReceiveSnapshotting.Entry(file, new RandomAccessFile(file.toFile(), "rw"),
					0, leaderId, now - raft.receiveSnapshottingTimeout() - 1000);
			entry.markFinalizing();
			raft.receiveSnapshotting.put(5L, entry);

			raft.gcReceiveSnapshotting(now);

			assertSame(entry, raft.receiveSnapshotting.get(5),
					"idle timeout must not remove finalizing entry (gc action would recreate the truncation race)");
			assertTrue(Files.exists(file), "gc must not delete file being finalized");

			raft.lock(); // term 的写点全在 raft 锁内
			try {
				raft.getLogSequence().trySetTerm(1); // 新 leader 当选，term 推进
			} finally {
				raft.unlock();
			}
			raft.gcReceiveSnapshotting(now);

			assertNull(raft.receiveSnapshotting.get(5),
					"stale-owner finalizing entry must be removed (its endReceive will abort at term check)");
			assertFalse(Files.exists(file));
		} finally {
			raft.shutdown();
		}
	}

	// done 分支的 cleanupSmallerThan 必须跳过 finalizing 条目：
	// 其 .installing 文件正被收尾提交（Files.move 的源），删除会留下运行期日志空洞。
	@Test
	public void testCleanupSkipsFinalizing() throws Exception {
		var raft = newRaft();
		try {
			var logSequence = raft.getLogSequence();
			Files.createDirectories(Paths.get(dbHome));
			var file3 = installingPath(3);
			var file4 = installingPath(4);
			Files.write(file3, new byte[]{1});
			Files.write(file4, new byte[]{2});
			var finalizing3 = new ReceiveSnapshotting.Entry(file3, new RandomAccessFile(file3.toFile(), "rw"),
					0, leaderId, System.currentTimeMillis());
			finalizing3.markFinalizing();
			var normal4 = new ReceiveSnapshotting.Entry(file4, new RandomAccessFile(file4.toFile(), "rw"),
					0, leaderId, System.currentTimeMillis());
			raft.receiveSnapshotting.put(3L, finalizing3);
			raft.receiveSnapshotting.put(4L, normal4);

			// 生产唯一入口：done块收尾时清理更小边界残留（经processInstallSnapshot全流程）。
			var done5 = newChunk(logSequence.getTerm(), leaderId, 5, 0, new byte[]{5}, true);
			assertEquals(Procedure.Success, raft.processInstallSnapshot(done5));

			assertSame(finalizing3, raft.receiveSnapshotting.get(3), "finalizing entry must be left to endReceive's finally");
			assertTrue(Files.exists(file3), "file being finalized must not be deleted");
			assertNull(raft.receiveSnapshotting.get(4), "non-finalizing smaller entry must be cleaned");
			assertFalse(Files.exists(file4));
			assertTrue(Files.exists(logSequence.genSnapshotPath(5)), "done chunk must commit through the real flow");
		} finally {
			raft.shutdown();
		}
	}

	// 归属失效的 finalizing 条目（旧 term）：新 leader 的首块经 FND3-23 归属校验丢弃
	// （关句柄+删其文件——旧收尾会在 term 复核处放弃且不碰文件），随后按全新安装进行：
	// R5 唯一文件名，新装写自己的新文件，与旧文件无任何共享路径。
	@Test
	public void testStaleOwnerFirstChunkDiscardsFinalizingEntry() throws Exception {
		var raft = newRaft();
		try {
			var oldFile = installingPath(5);
			Files.write(oldFile, new byte[]{1, 2, 3});
			var entry = new ReceiveSnapshotting.Entry(oldFile, new RandomAccessFile(oldFile.toFile(), "rw"),
					0, leaderId, System.currentTimeMillis());
			entry.markFinalizing();
			raft.receiveSnapshotting.put(5L, entry);

			// 新 term 的重装首块（换主后 term 推进）。
			var reinstall = newChunk(1, "127.0.0.1:17682", 5, 0, new byte[]{7, 7}, false);
			assertEquals(Procedure.Success, raft.processInstallSnapshot(reinstall));
			assertEquals(Procedure.Success, reinstall.getResultCode(),
					"stale-owner reinstall must proceed as fresh install, not conflict");

			var fresh = raft.receiveSnapshotting.get(5);
			assertTrue(fresh != null && !fresh.finalizing, "fresh entry replaces stale finalizing one");
			assertEquals(1L, fresh.term);
			assertNotEquals(oldFile, fresh.path, "fresh install gets its own uniquely-named file (R5)");
			assertArrayEquals(new byte[]{7, 7}, Files.readAllBytes(fresh.path),
					"fresh install rewrites its own file from scratch");
			assertFalse(Files.exists(oldFile),
					"stale-owner discard deletes the old finalizing entry's file");
		} finally {
			raft.shutdown();
		}
	}
}
