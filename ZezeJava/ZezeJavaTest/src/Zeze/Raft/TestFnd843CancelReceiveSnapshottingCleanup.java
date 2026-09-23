package Zeze.Raft;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;

import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND8-43回归：cancelAllReceiveSnapshotting只关句柄不删.installing文件——clear后
 * gcReceiveSnapshotting以map为准看不到已移除条目，运行期无任何文件清理路径
 *（启动清理仅进程重启执行），"接收途中换主/shutdown"反复发生时磁盘按快照大小
 * 缓慢泄漏（不损正确性）。
 * 修复：关句柄的同时对每个条目调用discard（关句柄+删文件，失败仅告警，与gc路径
 * 同口径）。
 * 纯单元：合成receiveSnapshotting残留条目（registry包内可见），直接调用
 * cancelAll（R4抽取前经反射调Raft私有方法，抽取后为登记表包内方法），不起server。
 */
@Fast
public class TestFnd843CancelReceiveSnapshottingCleanup {
	private static final String dbHome = "a3_TestFnd843CancelCleanup.raft";

	private Raft raft;
	private RandomAccessFile file;

	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:26385" DbHome="a3_TestFnd843CancelCleanup.raft">
					<node Host="127.0.0.1" Port="26385"/>
					<node Host="127.0.0.1" Port="26386"/>
					<node Host="127.0.0.1" Port="26387"/>
				</raft>
				""");
	}

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (file != null && file.getFD().valid())
			file.close();
		if (raft != null) {
			try {
				raft.getLogSequence().close();
				raft.shutdown();
			} catch (Exception ignore) {
			}
		}
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	// 取消接收必须同时删除.installing残留文件（修复前只关句柄，磁盘泄漏）。
	@Test
	public void testCancelDeletesInstallingFiles() throws Exception {
		var sm = new StateMachine() {
			@Override
			public SnapshotResult snapshot(String path) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void loadSnapshot(String path) {
			}
		};
		raft = new Raft(sm, "127.0.0.1:26385", newRaftConfig());

		// 合成两个接收中条目及其.installing文件。
		var path5 = Paths.get(dbHome, LogSequence.snapshotFileName + ".installing.5");
		var path7 = Paths.get(dbHome, LogSequence.snapshotFileName + ".installing.7");
		Files.createDirectories(Paths.get(dbHome));
		Files.writeString(path5, "a3partial5");
		Files.writeString(path7, "a3partial7");
		file = new RandomAccessFile(path5.toFile(), "rw");
		var file7 = new RandomAccessFile(path7.toFile(), "rw");
		raft.receiveSnapshotting.put(5L, new ReceiveSnapshotting.Entry(path5, file, 0, "leader", 0));
		raft.receiveSnapshotting.put(7L, new ReceiveSnapshotting.Entry(path7, file7, 0, "leader", 0));

		raft.receiveSnapshotting.cancelAll();

		assertTrue(raft.receiveSnapshotting.isEmpty(), "条目全部清除");
		assertFalse(file.getFD().valid(), "句柄必须关闭");
		assertFalse(file7.getFD().valid(), "句柄必须关闭");
		assertFalse(Files.exists(path5), ".installing残留文件必须删除（修复前泄漏）");
		assertFalse(Files.exists(path7), ".installing残留文件必须删除");
	}
}
