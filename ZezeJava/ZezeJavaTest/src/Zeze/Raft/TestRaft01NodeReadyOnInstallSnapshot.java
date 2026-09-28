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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND12 raft-01回归（D1修复·通路二）：InstallSnapshot 收尾（快照装载成功）置 NodeReady。
 * 缺陷：全新节点追赶走 InstallSnapshot 全量安装时，收尾只做边界复位 + loadSnapshot，
 * 不触碰 nodeReady；集群空闲后该节点收到恒定 leaderCommit 永不就绪，ready 与 never-ready
 * 多数派选举永久互拒（见 TestRaft01NodeReadyOnCommitAdvance）。
 * 修复：快照是多数派提交产物，装载成功即持有已提交状态，两个收尾分支（全新安装、
 * 同边界 ExistLog 重装）loadSnapshot 成功后 trySetNodeReady。
 * <p>
 * 复现：headless 构造 Raft（不起 server，无网络），反射可见性内直调 processInstallSnapshot：
 * 单块 done=true 快照走完整安装分支；预置同边界日志走 ExistLog 分支。修复前两个分支
 * 收尾后 NodeReady 均为 false，断言失败。
 */
@Fast
public class TestRaft01NodeReadyOnInstallSnapshot {
	private static final String raftName = "127.0.0.1:17773";
	private static final String dbHome = "TestRaft01NodeReadyOnInstallSnapshot.raft";

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17773" DbHome="TestRaft01NodeReadyOnInstallSnapshot.raft">
					<node Host="127.0.0.1" Port="17773"/>
					<node Host="127.0.0.1" Port="17774"/>
					<node Host="127.0.0.1" Port="17775"/>
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

	private Raft raft;

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() throws Exception {
		raft.getLogSequence().close();
		raft.shutdown();
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

	private static InstallSnapshot newDoneChunk(long term, String leader) {
		var r = new InstallSnapshot();
		r.Argument.setTerm(term);
		r.Argument.setLeaderId(leader);
		r.Argument.setLastIncludedIndex(5);
		r.Argument.setLastIncludedTerm(0);
		r.Argument.setOffset(0);
		r.Argument.setData(new Binary(filled(400, (byte)'B')));
		r.Argument.setDone(true);
		r.Argument.setLastIncludedLog(new Binary(new RaftLog(0, 5, new HeartbeatLog()).encode()));
		return r;
	}

	@Test
	public void testFullInstallSetsNodeReady() throws Exception {
		var sm = new RecordingStateMachine();
		raft = new Raft(sm, raftName, newRaftConfig());
		var logSequence = raft.getLogSequence();
		logSequence.setWriteOptions(RocksDatabase.getDefaultWriteOptions());
		assertFalse(logSequence.getNodeReady(), "正控：全新节点初始为 never-ready");

		// 本地无边界日志（仅有哨兵）：走"Discard the entire log"完整安装分支。
		assertEquals(Procedure.Success, processInstallSnapshot(raft, newDoneChunk(1, "127.0.0.1:17774")));

		assertNotNull(sm.loadedBytes, "正控：收尾路径到达 loadSnapshot");
		assertArrayEquals(filled(400, (byte)'B'), sm.loadedBytes);
		assertTrue(logSequence.getNodeReady(),
				"快照是多数派提交产物，装载成功即持有已提交状态，必须就绪（FND12 raft-01）");
	}

	@Test
	public void testExistLogReinstallSetsNodeReady() throws Exception {
		var sm = new RecordingStateMachine();
		raft = new Raft(sm, raftName, newRaftConfig());
		var logSequence = raft.getLogSequence();
		logSequence.setWriteOptions(RocksDatabase.getDefaultWriteOptions());
		assertFalse(logSequence.getNodeReady(), "正控：全新节点初始为 never-ready");

		// 预置同边界（term=0, index=5）本地日志：走 ExistLog 重装分支
		// （上次同边界收尾中途失败后的重装恢复路径）。
		logSequence.saveLog(new RaftLog(0, 5, new HeartbeatLog()));

		assertEquals(Procedure.Success, processInstallSnapshot(raft, newDoneChunk(1, "127.0.0.1:17774")));

		assertNotNull(sm.loadedBytes, "正控：收尾路径到达 loadSnapshot");
		assertTrue(logSequence.getNodeReady(),
				"ExistLog 重装分支同样在装载成功后置 ready（FND12 raft-01）");
	}
}
