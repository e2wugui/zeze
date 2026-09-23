package Zeze.Raft;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Raft.gcReceiveSnapshotting 的残留接收条目清理（FND3-23）。
 * leader 传输中途失联/换主后 done 永不到，旧实现的 receiveSnapshotting 条目+句柄
 * +.installing 文件永久残留，isReceivingSnapshot() 恒 true → LogSequence.snapshot()
 * 恒提前返回，本地快照与日志压缩永久停摆、磁盘无界增长。
 * 修复：条目绑定 (term, leaderId, lastActiveTime)，不变量=只为"当前 term 的当前
 * leader"的安装保留条目；onLowPrecisionTimer 周期驱动 gcReceiveSnapshotting 清理
 * term 不匹配 / leaderId 不匹配 / 空闲超时的条目；LogSequence 构造时清理磁盘残留的
 * .installing.* 文件（运行期 gc 只管理 map 内的条目，管不到崩溃残留的孤儿文件）。
 * 同包合成测试：直接操作 raft.receiveSnapshotting 并调用 gcReceiveSnapshotting，
 * 不启动 server、不占端口。
 */
@Fast
public class TestGcReceiveSnapshotting {
	private static final String raftName = "127.0.0.1:17670";
	private static final String dbHome = "TestGcReceiveSnapshotting.raft";

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17670" DbHome="TestGcReceiveSnapshotting.raft">
					<node Host="127.0.0.1" Port="17670"/>
					<node Host="127.0.0.1" Port="17671"/>
					<node Host="127.0.0.1" Port="17672"/>
				</raft>
				""");
	}

	private static final class NoopStateMachine extends StateMachine {
		@Override
		public SnapshotResult snapshot(String path) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void loadSnapshot(String path) {
			throw new UnsupportedOperationException();
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

	private ReceiveSnapshotting.Entry newEntry(Path file, long term, String leaderId,
											   long lastActiveTime) throws Exception {
		return new ReceiveSnapshotting.Entry(file, new RandomAccessFile(file.toFile(), "rw"),
				term, leaderId, lastActiveTime);
	}

	// 空闲超阈值（leader 失联）的条目必须被清理：条目移除、句柄关闭、文件删除。
	@Test
	public void testIdleTimeoutEntryRemoved() throws Exception {
		var raft = newRaft();
		try {
			var now = System.currentTimeMillis();
			var file = Paths.get(dbHome, LogSequence.snapshotFileName + ".installing.5");
			// 空闲时长超过派生阈值（AppendEntriesTimeout*4 + LeaderHeartbeatTimer*2）。
			var entry = newEntry(file, 0, "127.0.0.1:17671",
					now - raft.receiveSnapshottingTimeout() - 1000);
			raft.receiveSnapshotting.put(5L, entry);

			raft.gcReceiveSnapshotting(now);

			assertTrue(raft.receiveSnapshotting.isEmpty());
			assertFalse(raft.isReceivingSnapshot());
			assertFalse(entry.file.getFD().valid(), "file handle must be closed");
			assertFalse(Files.exists(file), ".installing file must be deleted");
		} finally {
			raft.shutdown();
		}
	}

	// 活跃（传输进行中）的条目必须原样保留：误杀会让合法安装永远传不完。
	@Test
	public void testActiveEntrySurvives() throws Exception {
		var raft = newRaft();
		try {
			var now = System.currentTimeMillis();
			var file = Paths.get(dbHome, LogSequence.snapshotFileName + ".installing.5");
			var entry = newEntry(file, 0, "127.0.0.1:17671", now); // 刚有块活动
			raft.receiveSnapshotting.put(5L, entry);

			raft.gcReceiveSnapshotting(now);

			assertSame(entry, raft.receiveSnapshotting.get(5L));
			assertTrue(raft.isReceivingSnapshot());
			assertTrue(entry.file.getFD().valid());
			assertTrue(Files.exists(file));
		} finally {
			raft.shutdown(); // registry.cancelAll 关闭残留句柄
		}
	}

	// 换主（term 推进）后，旧 term 的安装条目作废。
	@Test
	public void testTermAdvanceRemovesOldTermEntry() throws Exception {
		var raft = newRaft();
		try {
			var now = System.currentTimeMillis();
			var file = Paths.get(dbHome, LogSequence.snapshotFileName + ".installing.5");
			raft.receiveSnapshotting.put(5L, newEntry(file, 0, "127.0.0.1:17671", now)); // term=0

			raft.lock(); // term 的写点全在 raft 锁内（同 TestEndReceiveInstallSnapshotStaleTerm）
			try {
				raft.getLogSequence().trySetTerm(1); // 新 leader 当选，term 推进
			} finally {
				raft.unlock();
			}
			raft.gcReceiveSnapshotting(now);

			assertTrue(raft.receiveSnapshotting.isEmpty());
			assertFalse(raft.isReceivingSnapshot());
			assertFalse(Files.exists(file));
		} finally {
			raft.shutdown();
		}
	}

	// leaderId 归属不同（防御路径：正常换主伴随 term 推进，此处合成同 term 不一致）。
	@Test
	public void testLeaderMismatchRemovesEntry() throws Exception {
		var raft = newRaft();
		try {
			var now = System.currentTimeMillis();
			var file = Paths.get(dbHome, LogSequence.snapshotFileName + ".installing.5");
			raft.receiveSnapshotting.put(5L, newEntry(file, 0, "127.0.0.1:17671", now));
			raft.setLeaderId("127.0.0.1:17672");

			raft.gcReceiveSnapshotting(now);

			assertTrue(raft.receiveSnapshotting.isEmpty());
			assertFalse(Files.exists(file));
		} finally {
			raft.shutdown();
		}
	}

	// 启动清理：孤儿.installing删除；legacy .commit.delayed残留同样清扫
	//（代际化后该文件族不再产生，历史残留统一判死）。
	@Test
	public void testStartupCleansOrphanInstallingFiles() throws Exception {
		Files.createDirectories(Paths.get(dbHome));
		var orphan = Paths.get(dbHome, LogSequence.snapshotFileName + ".installing.7");
		Files.write(orphan, new byte[]{1, 2, 3});
		var delayed = Paths.get(dbHome, LogSequence.snapshotFileName + ".tmp.9.commit.delayed");
		Files.write(delayed, new byte[]{4, 5, 6});

		var raft = newRaft(); // LogSequence 构造时清理
		try {
			assertFalse(Files.exists(orphan), "orphan .installing file must be deleted on startup");
			assertFalse(Files.exists(delayed), "legacy .commit.delayed residue must be swept (hole 2 closed)");
		} finally {
			raft.shutdown();
		}
	}
}
