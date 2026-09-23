package Zeze.Raft;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * FND7-57回归：follower 侧同边界 InstallSnapshot 重装混拼。
 * 接收段对 offset==0 的块只在 bNewFile 时 setLength(0)——同 (term,leader,LastIncludedIndex)
 * 的残留条目重装时不清长度、按旧文件长度续写。leader 侧每次安装总是从 offset=0 全量重发
 * （InstallSnapshotState 新实例），若两次安装之间同边界快照重生成（snapshot.dat 消失触发
 * LogSequence.snapshot() 等）且新旧字节/长度不同：offset&lt;fileLength 且 newEnd≤fileLength 的
 * 块被跳写（残留旧字节）、或收尾后尾部残留旧字节，得到新旧混拼文件；done 提交后
 * loadSnapshot 失败 fatalKill，commitSnapshotNow 已 move 完成时重启加载损坏 snapshot.dat
 * 节点起不来。
 * 修复：offset==0 无条件 setLength(0)，重发块幂等重写，安装内容总是本次全量。
 * <p>
 * 复现：直接构造 Raft（不起 server，无网络），反射调用 processInstallSnapshot：
 * 先发长快照 offset=0 块（done=false）制造残留半截文件，再同边界短快照从 offset=0 重装至
 * done；记录型 StateMachine 捕获 loadSnapshot 实际读到的 snapshot.dat 字节，断言与新短快照
 * 完全一致。修复前实际读到的是旧长快照字节（残留未清），断言失败。
 */
@Fast
public class TestFnd757InstallSnapshotSameBoundaryRegen {
	private static final String raftName = "127.0.0.1:17670";
	private static final String dbHome = "TestFnd757InstallSnapshotSameBoundaryRegen.raft";

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17670" DbHome="TestFnd757InstallSnapshotSameBoundaryRegen.raft">
					<node Host="127.0.0.1" Port="17670"/>
					<node Host="127.0.0.1" Port="17671"/>
					<node Host="127.0.0.1" Port="17672"/>
				</raft>
				""");
	}

	// 记录型状态机：loadSnapshot 时捕获实际加载的字节（done 提交路径把它指向
	// commitSnapshotNow 刚 move 出来的 snapshot.dat）。
	static final class RecordingStateMachine extends StateMachine {
		byte[] loadedBytes;

		@Override
		public SnapshotResult snapshot(String path) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void loadSnapshot(String path) throws Exception {
			loadedBytes = Files.readAllBytes(Paths.get(path));
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

	// processInstallSnapshot raft-02 起为包内可见（测试直调合成rpc）。
	private static long processInstallSnapshot(Raft raft, InstallSnapshot r) throws Exception {
		return raft.processInstallSnapshot(r);
	}

	private static byte[] filled(int size, byte b) {
		var bytes = new byte[size];
		Arrays.fill(bytes, b);
		return bytes;
	}

	private static InstallSnapshot newChunk(long term, String leader, long offset, byte[] data, boolean done) {
		var r = new InstallSnapshot();
		r.Argument.setTerm(term);
		r.Argument.setLeaderId(leader);
		r.Argument.setLastIncludedIndex(5);
		r.Argument.setLastIncludedTerm(0);
		r.Argument.setOffset(offset);
		r.Argument.setData(new Binary(data));
		r.Argument.setDone(done);
		if (done)
			r.Argument.setLastIncludedLog(new Binary(new RaftLog(0, 5, new HeartbeatLog()).encode()));
		return r;
	}

	@Test
	public void testSameBoundaryReinstallReplacesResidue() throws Exception {
		var sm = new RecordingStateMachine();
		var raft = new Raft(sm, raftName, newRaftConfig());
		try {
			var logSequence = raft.getLogSequence();
			logSequence.setWriteOptions(RocksDatabase.getDefaultWriteOptions());
			var leader = "127.0.0.1:17671";
			long term = 1;

			// 第一次安装（长快照）中断：只收到 offset=0 块，done=false，条目与其唯一名
			// .installing 文件残留（R5：路径取自条目，不再按 index 推导固定名）。
			var longSnap = filled(1000, (byte)'A');
			var first = newChunk(term, leader, 0, longSnap, false);
			assertEquals(Procedure.Success, processInstallSnapshot(raft, first));
			var installing = raft.receiveSnapshotting.get(5).path;
			assertArrayEquals(longSnap, Files.readAllBytes(installing), "正控：中断后残留半截文件");

			// 同边界重装（短快照，leader 从 offset=0 全量重发）至 done：复用同条目，
			// offset==0 截断重写，最终 snapshot.dat 与新短快照字节完全一致（FND7-57）。
			var shortSnap = filled(400, (byte)'B');
			var second = newChunk(term, leader, 0, shortSnap, true);
			assertEquals(Procedure.Success, processInstallSnapshot(raft, second));

			assertNotNull(sm.loadedBytes, "done 提交路径必须到达 loadSnapshot");
			assertArrayEquals(shortSnap, sm.loadedBytes,
					"重装后 snapshot.dat 必须与新短快照字节完全一致，不得残留/混拼旧内容");
		} finally {
			raft.getLogSequence().close();
			raft.shutdown();
		}
	}
}
