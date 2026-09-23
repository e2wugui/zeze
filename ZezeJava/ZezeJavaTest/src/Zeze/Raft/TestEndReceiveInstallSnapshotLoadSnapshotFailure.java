package Zeze.Raft;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

import Zeze.Net.Binary;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LogSequence.endReceiveInstallSnapshot 的 loadSnapshot 失败必须 fatalKill（FND3-21
 * 失败原子性半）。失败时日志已 drop、firstIndex 已持久化推进、内存 lastApplied 已是
 * 新边界，而状态机仍是旧内容；旧实现无 catch 直接传出，leader 超时重试会走 ExistLog
 * 分支只 commitSnapshotNow 不再 loadSnapshot 且应答成功，follower 永久脏状态
 * （静默分歧），仅进程重启可恢复。
 * 修复：catch 后 fatalKill，把静默分歧变成显性 crash，重启即从 snapshot.zip（已被
 * commitSnapshotNow 替换为新边界内容）恢复自愈。
 * 本测试用最小 StateMachine 假实现（loadSnapshot 确定性抛异常）直接构造 Raft，
 * 并注入 fatalKill 钩子（真实路径会 LogManager.shutdown + Runtime.halt，杀死测试
 * JVM）：断言进入 fatal 且异常继续向上抛；firstIndex 已推进的断言同时证明
 * "为什么必须 fatal 而不能继续运行"。
 */
@Fast
public class TestEndReceiveInstallSnapshotLoadSnapshotFailure {
	private static final String raftName = "127.0.0.1:17660";
	private static final String dbHome = "TestEndReceiveInstallSnapshotLoadSnapshotFailure.raft";

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17660" DbHome="TestEndReceiveInstallSnapshotLoadSnapshotFailure.raft">
					<node Host="127.0.0.1" Port="17660"/>
					<node Host="127.0.0.1" Port="17661"/>
					<node Host="127.0.0.1" Port="17662"/>
				</raft>
				""");
	}

	private static final class FailLoadSnapshotStateMachine extends StateMachine {
		@Override
		public SnapshotResult snapshot(String path) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void loadSnapshot(String path) throws Exception {
			throw new IOException("injected loadSnapshot failure");
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

	@Test
	public void testLoadSnapshotFailureFatalKills() throws Exception {
		var fatalled = new AtomicBoolean(false);
		var raft = new Raft(new FailLoadSnapshotStateMachine(), raftName, newRaftConfig());
		try {
			var logSequence = raft.getLogSequence();
			logSequence.setWriteOptions(RocksDatabase.getDefaultWriteOptions());
			raft.setFatalKillHookForTest(() -> fatalled.set(true));

			var r = new InstallSnapshot();
			r.Argument.setTerm(logSequence.getTerm()); // 初始 0
			r.Argument.setLeaderId("127.0.0.1:17661");
			r.Argument.setLastIncludedIndex(5);
			r.Argument.setLastIncludedTerm(0);
			r.Argument.setDone(true);
			r.Argument.setLastIncludedLog(new Binary(new RaftLog(0, 5, new HeartbeatLog()).encode()));
			// 等价于 processInstallSnapshot 锁内已记录 leaderId（Raft.java:446）。
			raft.setLeaderId(r.Argument.getLeaderId());

			// commitSnapshotNow 会把它 rename 成 snapshot.dat；内容无所谓，
			// loadSnapshot 已被覆写为直接抛异常。
			var installingPath = Paths.get(dbHome, LogSequence.snapshotFileName + ".installing.5");
			Files.write(installingPath, new byte[]{1, 2, 3});
			// raft-02 起 done 分支保留 finalizing 条目（应收长度=文件长度），收尾侧据此校验。
			var raf = new RandomAccessFile(installingPath.toFile(), "rw");
			raf.close(); // 生产流程 done 分支已关句柄（move 前句柄必须是关闭态）
			var entry = new Raft.ReceiveSnapshotEntry(raf, r.Argument.getTerm(),
					r.Argument.getLeaderId(), System.currentTimeMillis());
			entry.receivedLength = 3;
			entry.finalizing = true;
			raft.receiveSnapshotting.put(5L, entry);

			var ex = assertThrows(IOException.class,
					() -> logSequence.endReceiveInstallSnapshot(installingPath, r),
					"exception must propagate after fatalKill for the procedure to fail visibly");
			assertEquals("injected loadSnapshot failure", ex.getMessage());
			assertTrue(fatalled.get(), "loadSnapshot failure must reach fatalKill");
			assertTrue(raft.isShutdown, "fatalKill marks isShutdown");
			assertEquals(5L, logSequence.getFirstIndex(),
					"boundary already persisted before loadSnapshot: continuing would diverge silently");
			assertTrue(logSequence.logsAvailable, "logsAvailable must be restored in finally");
		} finally {
			// fatalKill 已置 isShutdown，Raft.shutdown 会早退而跳过 logSequence.close；
			// 测试 JVM 内手动补关，避免 RocksDB 句柄残留导致目录删不掉。
			raft.getLogSequence().close();
			raft.shutdown();
		}
	}
}
