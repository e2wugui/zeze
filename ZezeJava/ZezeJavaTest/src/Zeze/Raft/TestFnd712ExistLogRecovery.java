package Zeze.Raft;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Net.Binary;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LogSequence.endReceiveInstallSnapshot 的 ExistLog 分支恢复语义补全（FND7-12）。
 * 完整收尾路径中 commitSnapshotNow（含 saveFirstIndex 的 sync 写）失败后异常传出无
 * 应答，leader 超时按同边界 X 重装：本地 readLog(X) 命中上次半途 saveLog 的边界日志
 * 走 ExistLog 分支。旧实现只执行 commitSnapshotNow 即返回——内存 lastIndex/
 * commitIndex/lastApplied 与状态机仍停留在旧边界，而 firstIndex 已推进为 X：
 * tryApply 从 lastApplied+1 起 readLog 得 null，apply 永久楔死（backgroundApply 热循
 * 环抢 Raft 锁）；该节点日志完整可当选，当选后 SetLeaderReadyEvent 永远 apply 不到，
 * waitLeaderReady 恒超时，集群级死锁。
 * 修复：ExistLog 分支对齐完整路径——复位内存索引、setVoteFor 并 loadSnapshot；
 * 启动侧补 firstIndex 处边界日志缺失检测（半安装残留）——丢弃 logs 重置回无快照状态。
 * 两个用例分别覆盖运行期重装路径与崩溃重启路径，最小 StateMachine 假实现直接构造
 * Raft（不 start server，不占用端口），旧代码下断言失败，有区分度。
 */
@Fast
public class TestFnd712ExistLogRecovery {
	private static final String raftName = "127.0.0.1:17670";
	private static final String dbHome = "TestFnd712ExistLogRecovery.raft";

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17670" DbHome="TestFnd712ExistLogRecovery.raft">
					<node Host="127.0.0.1" Port="17670"/>
					<node Host="127.0.0.1" Port="17671"/>
					<node Host="127.0.0.1" Port="17672"/>
				</raft>
				""");
	}

	// 记录 loadSnapshot 调用的最小状态机。
	private static final class RecordingStateMachine extends StateMachine {
		final List<String> loadedPaths = new ArrayList<>();

		@Override
		public SnapshotResult snapshot(String path) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void loadSnapshot(String path) {
			loadedPaths.add(path);
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

	// 运行期路径：上次完整收尾在 commitSnapshotNow 处失败（如 saveFirstIndex 落盘
	// 失败），leader 按同边界 X=5 重装 → ExistLog 分支。修复后必须复位内存索引并
	// 装载快照；修复前 firstIndex=5 而 lastApplied 停留 0，apply 永久楔死。
	@Test
	public void testExistLogRetryResetsMemoryAndLoadsSnapshot() throws Exception {
		var raft = new Raft(new RecordingStateMachine(), raftName, newRaftConfig());
		try {
			var logSequence = raft.getLogSequence();
			logSequence.setWriteOptions(RocksDatabase.getDefaultWriteOptions());
			raft.setLeaderId("127.0.0.1:17671");

			// 模拟上次半途失败的收尾：边界日志 X=5 已 saveLog（term 0 与重装边界一致），
			// 内存索引与状态机停留在旧边界（构造初始值 0）。
			logSequence.saveLog(new RaftLog(0, 5, new HeartbeatLog()));

			var sm = (RecordingStateMachine)raft.getStateMachine();
			var r = new InstallSnapshot();
			r.Argument.setTerm(logSequence.getTerm()); // 初始 0，与当前一致
			r.Argument.setLeaderId("127.0.0.1:17671");
			r.Argument.setLastIncludedIndex(5);
			r.Argument.setLastIncludedTerm(0);
			r.Argument.setDone(true);
			r.Argument.setLastIncludedLog(new Binary(new RaftLog(0, 5, new HeartbeatLog()).encode()));

			// commitSnapshotNow 会把它 rename 成 snapshot.dat；内容无所谓（假状态机不解析）。
			var installingPath = Paths.get(dbHome, LogSequence.snapshotFileName + ".installing.5");
			Files.write(installingPath, new byte[]{1, 2, 3});

			logSequence.endReceiveInstallSnapshot(installingPath, r);

			assertEquals(5L, logSequence.getFirstIndex());
			assertEquals(5L, logSequence.getLastIndex(), "memory lastIndex must be reset to boundary");
			assertEquals(5L, logSequence.getCommitIndex(), "commitIndex must be reset to firstIndex");
			assertEquals(5L, logSequence.getLastApplied(),
					"lastApplied must be reset to firstIndex; stale value wedges apply forever");
			assertEquals("127.0.0.1:17671", logSequence.getVoteFor(), "vote of current term discarded (aligned with full path)");
			assertEquals(1, sm.loadedPaths.size(), "snapshot must be loaded into the state machine");
			assertEquals(logSequence.getSnapshotFullName(), sm.loadedPaths.get(0));
			assertTrue(logSequence.logsAvailable, "logsAvailable must be restored in finally");
		} finally {
			raft.shutdown();
		}
	}

	// 崩溃路径：完整收尾在边界日志 saveLog(X=5) 之后、saveFirstIndex 持久化之前崩溃，
	// 重启后 lastIndex(=5) >= firstIndex(=3)，单向检查不触发且 logs 在 (3,5) 区间有
	// 空洞。修复后启动检测边界日志缺失，丢弃半安装残留，重置回无快照状态；
	// 修复前 firstIndex=3、lastApplied=3 而 readLog(4)==null，apply 永久楔死。
	@Test
	public void testStartupHoleResetsToNoSnapshotState() throws Exception {
		{
			// 第一个实例制造半安装残留的持久化现场：
			// 提交 F=3 的旧快照（snapshot.dat + firstIndex=3 持久化），再留下边界日志 5。
			var raft = new Raft(new RecordingStateMachine(), raftName, newRaftConfig());
			try {
				var logSequence = raft.getLogSequence();
				logSequence.setWriteOptions(RocksDatabase.getDefaultWriteOptions());
				var installingPath = Paths.get(dbHome, LogSequence.snapshotFileName + ".installing.3");
				Files.write(installingPath, new byte[]{1, 2, 3});
				logSequence.commitSnapshot(installingPath.toString(), 3);
				logSequence.saveLog(new RaftLog(0, 5, new HeartbeatLog()));
			} finally {
				raft.shutdown();
			}
		}

		// 重启：Logs={5}，firstIndex=3 → 边界日志 readLog(3)==null。
		var raft2 = new Raft(new RecordingStateMachine(), raftName, newRaftConfig());
		try {
			var logSequence = raft2.getLogSequence();
			assertEquals(0L, logSequence.getFirstIndex(),
					"half-installed logs must be discarded, firstIndex reset to no-snapshot state");
			assertEquals(0L, logSequence.getLastIndex(), "logs re-seeded empty");
			assertEquals(0L, logSequence.getCommitIndex());
			assertEquals(0L, logSequence.getLastApplied(),
					"no hole: apply starts from 1 with consistent no-snapshot state");
		} finally {
			raft2.shutdown();
		}
	}
}
