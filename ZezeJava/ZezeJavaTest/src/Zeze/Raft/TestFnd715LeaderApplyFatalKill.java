package Zeze.Raft;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Changes;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Raft.RocksRaft.Record;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Raft.RocksRaft.Transaction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RocksRaft Transaction.leaderApply 的非Flush Throwable兜底（FND7-15）。
 * follower侧确立了"宁死不糊"原则：Rocks.followerApply对非FlushException的Throwable
 * 统一fatalKill。leader侧旧实现只catch FlushException——生成leaderApplyNoRecursive的
 * ClassCast/NPE、lastSavepoint的IllegalStateException等一路穿到后台apply线程的
 * uncaughtHandler（仅记日志），applyFuture在finally置null后同条目反复重入重抛：
 * lastApplied永久楔死且无fatalKill，leader持续复制提交并对外提供停在楔死点的
 * 过期读（静默落后），换主后需要InstallSnapshot追赶。
 * 修复：外层对齐follower侧catch(Throwable)→fatalKill。FlushException的pendingFlush
 * 重试语义不变。
 * 直接构造Rocks（不start server），用leaderApplyNoRecursive抛IllegalStateException的
 * bean注入确定性失败：旧代码下异常从leaderApply穿出（测试以未捕获异常失败），
 * 修复后fatalKill（注入测试钩子）且不再抛出，有区分度。
 */
@Fast
public class TestFnd715LeaderApplyFatalKill {
	private static final String raftName = "127.0.0.1:17690";
	private static final String dbHome = "TestFnd715LeaderApplyFatalKill.raft";
	private static final String templateName = "tFnd715Throw";

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17690" DbHome="TestFnd715LeaderApplyFatalKill.raft">
					<node Host="127.0.0.1" Port="17690"/>
					<node Host="127.0.0.1" Port="17691"/>
					<node Host="127.0.0.1" Port="17692"/>
				</raft>
				""");
	}

	// 托管int变量的最小bean：leaderApplyNoRecursive注入确定性失败（模拟生成代码缺陷/
	// 框架不变量破坏导致的非Flush Throwable）。
	public static final class BThrowBean extends Bean {
		private int _int;

		public int getInt() {
			if (!isManaged())
				return _int;
			var txn = Transaction.getCurrent();
			if (txn == null)
				return _int;
			var log = txn.getLog(objectId() + 1);
			if (log == null)
				return _int;
			return ((Zeze.Raft.RocksRaft.Log1.LogInt)log).value;
		}

		public void setInt(int value) {
			if (!isManaged()) {
				_int = value;
				return;
			}
			Transaction.getCurrent().putLog(new Zeze.Raft.RocksRaft.Log1.LogInt(this, 1, value));
		}

		@Override
		public Bean copy() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(_int);
		}

		@Override
		public void decode(IByteBuffer bb) {
			_int = bb.ReadInt();
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
		}

		@Override
		public void followerApply(Log log) {
			if (log.getVariableId() == 1)
				_int = ((Zeze.Raft.RocksRaft.Log1.LogInt)log).value;
		}

		@Override
		public void leaderApplyNoRecursive(Log vlog) {
			if (vlog.getVariableId() == 1)
				throw new IllegalStateException("injected leaderApplyNoRecursive failure (FND7-15)");
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
	public void testLeaderApplyThrowableFatalKills() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false)) {
			rocks.registerTableTemplate(templateName, Integer.class, BThrowBean.class);
			var raft = rocks.getRaft();
			var table = rocks.<Integer, BThrowBean>getTableTemplate(templateName).openTable(0);

			// 直写存储层做初始数据，事务内走Edit路径（日志的belong是bean本身，
			// leaderApply的日志迭代会调到bean的leaderApplyNoRecursive）。
			var seed = table.newValue();
			seed.setInt(10); // 未托管，直接修改
			var keyBB = ByteBuffer.Allocate();
			table.encodeKey(keyBB, 1);
			var valBB = ByteBuffer.Allocate();
			seed.encode(valBB);
			table.getRocksTable().put(keyBB.CopyIf(), valBB.CopyIf());

			// 捕获带修改的Changes（非leader：appendLog抛RaftRetry，过程按失败返回，
			// 但日志与accessedRecords已收集在事务对象上——即真实leader上apply前的现场）。
			final Transaction[] ts = new Transaction[1];
			var rc = rocks.newProcedure(() -> {
				ts[0] = Transaction.getCurrent();
				table.get(1).setInt(42);
				return 0L;
			}).call();
			assertEquals(Zeze.Transaction.Procedure.RaftRetry, rc); // not leader
			Changes changes = ts[0].getChanges();
			assertNotNull(changes);
			changes.encode(ByteBuffer.Allocate()); // 对齐真实路径：解析出Record.table

			var fatalled = new AtomicBoolean(false);
			raft.setFatalKillHookForTest(() -> fatalled.set(true));

			// 修复前：IllegalStateException从leaderApply穿出（真实路径上穿到后台apply
			// 线程仅记日志，lastApplied永久楔死）；修复后：fatalKill（测试钩子）且不再抛出。
			ts[0].leaderApply(changes, new RaftLog(1, 2, changes));

			assertTrue(fatalled.get(), "non-Flush Throwable in leaderApply must fatalKill (align with followerApply)");
			assertTrue(raft.isShutdown, "fatalKill marks isShutdown");
		}
	}
}
