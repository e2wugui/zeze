package Zeze.Raft;

import java.io.File;
import java.lang.reflect.Field;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Raft.RocksRaft.Changes;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.TestFlushRetryApply.BListBean;
import Zeze.Raft.RocksRaft.Transaction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;

/**
 * FND16 raft-01 红绿钉板：appendLog 的 unique 存根与日志必须同生共死（WriteBatch 原子提交）。
 * <p>
 * 修复前两笔独立 sync 写：存根落盘后、日志落盘前失败/崩溃留孤儿存根（!isApplied 且
 * 日志不存在——apply 遍历日志、removeLog 先 readLog（null 即跳过）都触达不了它），
 * 同号重发命中 DuplicateRequest 不可服务直到按天过期（默认7天）。
 * <p>
 * 用例1（正常路径回归）：appendLog 成功后存根（!isApplied）与日志同在。
 * 用例2（原子性·红绿双向的核心断言）：经 testHookBetweenStubAndLog 在存根写之后、
 * 日志写/提交之前注入失败——修复前=存根已独立落盘留孤儿（红）；合批后=组装失败
 * try-with-resources 丢弃未提交 batch，两笔同弃，重发路径 getRequestState 为 null（绿）。
 * <p>
 * headless 构造 Rocks（不 start server、无网络，对齐 TestUniqueStubFailRetryApply）。
 */
@Fast
public class TestAppendLogAtomicBatch {
	private static final String raftName = "127.0.0.1:17680";
	private static final String dbHome = "TestFnd16Raft01Atomic.raft";
	private static final String templateName = "tFnd16Raft01";

	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17680" DbHome="TestFnd16Raft01Atomic.raft">
					<node Host="127.0.0.1" Port="17680"/>
					<node Host="127.0.0.1" Port="17681"/>
					<node Host="127.0.0.1" Port="17682"/>
				</raft>
				""");
	}

	@BeforeEach
	public void setUp() throws Exception {
		Task.tryInitThreadPool();
		Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogList1<>(Integer.class));
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() {
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	// 收集带unique的Changes（对齐TestUniqueStubFailRetryApply.captureUniqueChanges）：
	// 事务内需有真实表写才触发appendLog的leader前置检查（RaftRetry），空事务直接成功。
	private static Changes newUniqueChanges(Rocks rocks, Zeze.Raft.RocksRaft.Table<Integer, BListBean> table)
			throws Exception {
		final Transaction[] ts = new Transaction[1];
		var rc = rocks.newProcedure(() -> {
			ts[0] = Transaction.getCurrent();
			table.getOrAdd(1).getList().add(30);
			return 0L;
		}).call();
		Assertions.assertEquals(Zeze.Transaction.Procedure.RaftRetry, rc); // not leader，仅收集
		var changes = ts[0].getChanges();
		Assertions.assertNotNull(changes);
		changes.getUnique().setRequestId(42);
		changes.getUnique().setClientId("test.fnd16.raft01");
		changes.setCreateTime(System.currentTimeMillis());
		changes.encode(ByteBuffer.Allocate());
		return changes;
	}

	private static void becomeLeader(Raft raft) throws Exception {
		// headless无选举：反射置Leader（对齐TestServerNonRaftRpcProtocol先例）。
		Field stateField = Raft.class.getDeclaredField("state");
		stateField.setAccessible(true);
		stateField.set(raft, Raft.RaftState.Leader);
	}

	@Test
	public void testStubAndLogCommitTogether() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Zeze.Config(), false)) {
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			var raft = rocks.getRaft();
			var logSequence = raft.getLogSequence();
			logSequence.setWriteOptions(Zeze.Util.RocksDatabase.getDefaultWriteOptions());

			// 先收集（not leader纯收集：事务RaftRetry不触发appendLog），后置Leader——
			// leader态下的收集procedure会真实走appendLog占掉index（广播失败但日志已留）。
			var changes = newUniqueChanges(rocks, table);
			var changes2 = newUniqueChanges(rocks, table);
			changes2.getUnique().setRequestId(42);
			changes2.getUnique().setClientId("test.fnd16.raft01");
			changes2.setCreateTime(changes.getCreateTime());
			changes2.encode(ByteBuffer.Allocate());
			becomeLeader(raft);
			var result = logSequence.appendLog(changes, null);

			Assertions.assertEquals(1L, result.index, "首条日志index=1");
			Assertions.assertNotNull(logSequence.readLog(1L), "日志必须落盘");
			// 行为级断言（存根在册）：同unique重发必须撞存根查重。
			var ex = Assertions.assertThrows(Exception.class,
					() -> logSequence.appendLog(changes2, null),
					"同unique重发必须撞存根查重（RaftRetryException Duplicate Request Found）");
			var cause = ex instanceof RuntimeException ? ex : ex.getCause();
			Assertions.assertTrue(String.valueOf(cause).contains("Duplicate Request Found"),
					"查重命中：实际=" + cause);
		}
	}

	@Test
	public void testFailureBetweenStubAndLogLeavesNoOrphan() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Zeze.Config(), false)) {
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			var raft = rocks.getRaft();
			var logSequence = raft.getLogSequence();
			logSequence.setWriteOptions(Zeze.Util.RocksDatabase.getDefaultWriteOptions());

			// 先收集（not leader），后置Leader——becomeLeader后的newProcedure会真实走
			// appendLog（广播失败但日志已留，见LogSequence lastIndex注释），抢先消费一次性hook。
			var changes = newUniqueChanges(rocks, table);
			var changes2 = newUniqueChanges(rocks, table);
			changes2.getUnique().setRequestId(42);
			changes2.getUnique().setClientId("test.fnd16.raft01");
			changes2.setCreateTime(changes.getCreateTime());
			changes2.encode(ByteBuffer.Allocate());
			becomeLeader(raft);
			// 一次性注入：存根写之后、日志写/提交之前失败。
			logSequence.testHookBetweenStubAndLog = () -> {
				throw new RuntimeException("test inject: fail between stub and log");
			};
			Assertions.assertThrows(Exception.class, () -> logSequence.appendLog(changes, null),
					"注入失败必须传播出appendLog");

			// 原子性断言：孤儿存根不得存在（修复前=存根已独立落盘）。
			// 行为级证明：同unique重发必须不被Duplicate拦截（NOT_FOUND→可正常执行）。
			var result2 = logSequence.appendLog(changes2, null);
			Assertions.assertEquals(1L, result2.index,
					"组装失败后两笔同弃：重发不得被孤儿存根拦（index仍为1=首条）");
			Assertions.assertNotNull(logSequence.readLog(1L), "重发的日志正常落盘");
		}
	}

	// 危害对照（自含红证，修复前后均成立）：人工构造孤儿存根（只写存根不写日志——
	// 即修复前"两笔独立写中间失败"的持久化结果）——同unique重发必须被Duplicate拦截，
	// 证明孤儿存根的危害机制真实；用例2证明合批后该窗口不再产生。
	@Test
	public void testOrphanStubBlocksResend() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Zeze.Config(), false)) {
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			var raft = rocks.getRaft();
			var logSequence = raft.getLogSequence();
			logSequence.setWriteOptions(Zeze.Util.RocksDatabase.getDefaultWriteOptions());

			var changes = newUniqueChanges(rocks, table);
			becomeLeader(raft);
			// 人工孤儿：只save存根（单写形态，等价修复前第一笔独立落盘），不写日志。
			var raftLog = new RaftLog(logSequence.getTerm(), logSequence.getLastIndex() + 1, changes);
			var saveMethod = LogSequence.class.getDeclaredClasses()[0]; // UniqueRequestSet为内部类，经openUniqueRequests构造
			var openMethod = LogSequence.class.getDeclaredMethod("openUniqueRequests", long.class);
			openMethod.setAccessible(true);
			var uniqueSet = openMethod.invoke(logSequence, changes.getCreateTime());
			var saveSingle = uniqueSet.getClass().getDeclaredMethod("save", RaftLog.class);
			saveSingle.setAccessible(true);
			saveSingle.invoke(uniqueSet, raftLog);

			var ex = Assertions.assertThrows(Exception.class,
					() -> logSequence.appendLog(changes, null),
					"孤儿存根必须拦住同unique重发（Duplicate Request Found）——危害机制真实");
			var cause = ex instanceof RuntimeException ? ex : ex.getCause();
			Assertions.assertTrue(String.valueOf(cause).contains("Duplicate Request Found"),
					"实际=" + cause);
		}
	}
}
