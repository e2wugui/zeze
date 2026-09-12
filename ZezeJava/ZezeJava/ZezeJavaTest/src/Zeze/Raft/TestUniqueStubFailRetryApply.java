package Zeze.Raft;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDBException;

import Zeze.Config;
import Zeze.Raft.RocksRaft.Changes;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Raft.RocksRaft.TestFlushRetryApply.BListBean;
import Zeze.Raft.RocksRaft.Transaction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FND3-22：tryApply对"apply已完整成功（内存变更+flush提交）后、unique存根写失败"的补偿。
 * <p>
 * tryApply对每个条目依次：①内存变更②flush③unique存根写④lastApplied推进⑤invokeCallback。
 * ③是一次RocksDB put，失败时异常传出、④⑤不执行，且此前无任何"已应用"标记（FND-R2-4的
 * pendingFlush只在FlushException时记录）——重试从日志库解码重放：list的OP_ADD按索引追加
 * 等非幂等增量在已应用状态上双重应用、随后落盘（状态机静默分歧）；且原始raftLog已从
 * leaderAppendLogs摘除、存根写失败不放回（只有FlushException才放回），重试用解码的新
 * 对象invokeCallback，挂在原对象上的回调永不触发——等待appendLog的业务线程等满超时
 * 拿到RaftRetry假失败（条目实际已提交）。
 * <p>
 * 可达面核实（报告与复核的"受损侧"判断均不准确，以代码为准）：Changes.encode/decode
 * 未序列化Log基类的unique/createTime（HeartbeatLog等子类都调super，Changes漏调——潜在
 * 日志格式缺口，改格式属兼容红线，仅记档），解码出的日志恒为requestId==0，③只对
 * leaderAppendLogs里的原始对象（活leader自己的唯一请求）执行——本缺陷的窗口仅在活
 * leader一侧，follower/重放侧不经过③。修复对两侧对称生效，未来格式补齐后follower侧
 * 自动获得同一补偿。
 * <p>
 * 修复：apply成功后、存根写前登记"已应用"补偿（空记录集，复用pendingFlush表：重试经
 * takePendingFlush命中→no-op flush短路，不重放增量）；存根写失败补回标记+放回原始
 * raftLog（对齐FlushException分支）；lastApplied推进后清除标记。
 * <p>
 * headless构造Rocks（不start server、无网络），复用TestFlushRetryApply的非幂等CollList1
 * 载体（LogList1&lt;Integer&gt;需自注册解码工厂——生成代码场景由生成的注册代码负责）。
 * 修复前红：重试经解码重放OP_ADD，存储读出[10,20,30,30]双重应用、回调计数0（丢失）；
 * 修复后绿：恰好应用一次、原始回调恰好触发一次。
 */
@Fast
public class TestUniqueStubFailRetryApply {
	private static final String raftName = "127.0.0.1:17660";
	private static final String dbHome = "TestUniqueStubFailRetry.raft";
	private static final String templateName = "tUniqueStubRetry";

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17660" DbHome="TestUniqueStubFailRetry.raft">
					<node Host="127.0.0.1" Port="17660"/>
					<node Host="127.0.0.1" Port="17661"/>
					<node Host="127.0.0.1" Port="17662"/>
				</raft>
				""");
	}

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		// CollList1的增量日志解码工厂：生成代码场景由生成的注册代码负责（如
		// AbstractGlobalCacheManagerWithRaft注册LogSet1<Integer>），自建表模板需自注册。
		// 修复前的重试路径经readLog解码，需要它能解码。
		Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogList1<>(Integer.class));
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() {
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	// 收集带unique请求编号的增量Changes：appendLog因非leader抛RaftRetry，过程按失败返回；
	// Changes已在_final_commit_里收集完成（保存在事务对象上）。补encode步骤解析Record.table
	// （对齐TestFlushRetryApply.captureChanges）。
	private static Changes captureUniqueChanges(Rocks rocks, Table<Integer, BListBean> table) throws Exception {
		final Transaction[] ts = new Transaction[1];
		var rc = rocks.newProcedure(() -> {
			ts[0] = Transaction.getCurrent();
			table.getOrAdd(1).getList().add(30); // LogList1 OP_ADD：重放不幂等，双重应用的载体
			return 0L;
		}).call();
		assertEquals(Zeze.Transaction.Procedure.RaftRetry, rc); // not leader
		var changes = ts[0].getChanges();
		assertNotNull(changes);
		// requestId>0触发tryApply的unique存根写路径（FND3-22窗口的载体）。
		changes.getUnique().setRequestId(1);
		changes.getUnique().setClientId("test.fnd3_22");
		changes.setCreateTime(System.currentTimeMillis());
		changes.encode(ByteBuffer.Allocate());
		return changes;
	}

	// 直写存储层做初始数据（绕过事务，模拟前序条目已成功应用落盘的状态）。
	private static void seedStorage(Table<Integer, BListBean> table, int key, Integer... items) throws Exception {
		var seed = table.newValue();
		for (var item : items)
			seed.getList().add(item); // 未托管，直接修改
		var keyBB = ByteBuffer.Allocate();
		table.encodeKey(keyBB, key);
		var valBB = ByteBuffer.Allocate();
		seed.encode(valBB);
		table.getRocksTable().put(keyBB.CopyIf(), valBB.CopyIf());
	}

	// 读存储层的最终值（不经缓存，验证flush真的落盘）。
	private static List<Integer> readStorage(Table<Integer, BListBean> table, int key) throws Exception {
		var keyBB = ByteBuffer.Allocate();
		table.encodeKey(keyBB, key);
		var bytes = table.getRocksTable().get(keyBB.CopyIf());
		var out = new ArrayList<Integer>();
		if (bytes != null) {
			var value = table.newValue();
			value.decode(ByteBuffer.Wrap(bytes));
			for (var item : value.getList())
				out.add(item);
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private static LongConcurrentHashMap<RaftLog> leaderAppendLogsOf(LogSequence logSequence) throws Exception {
		var field = LogSequence.class.getDeclaredField("leaderAppendLogs");
		field.setAccessible(true);
		return (LongConcurrentHashMap<RaftLog>)field.get(logSequence);
	}

	@Test
	public void testStubWriteFailRetryNoReplayNoCallbackLoss() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false)) {
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			seedStorage(table, 1, 10, 20); // 初始 [10,20]

			var changes = captureUniqueChanges(rocks, table); // list.add(30) -> OP_ADD
			var raftLog = new RaftLog(1, 1, changes); // term=1, index=1
			var callbackCount = new AtomicInteger();
			raftLog.setLeaderCallback((log, success) -> callbackCount.incrementAndGet());

			var logSequence = rocks.getRaft().getLogSequence();
			logSequence.saveLog(raftLog);
			// 模拟appendLog对leader请求的登记：tryApply优先从leaderAppendLogs取原始对象
			//（携带Transaction、unique与leaderCallback）。
			leaderAppendLogsOf(logSequence).put(1L, raftLog);

			// 1. 首次应用：原始对象->leaderApply（内存+flush落盘[10,20,30]），unique存根写
			//    被注入失败一次：lastApplied不推进、回调不触发。
			logSequence.testHookBeforeUniqueApply = () -> {
				throw new RocksDBException("test inject: unique stub apply fail once");
			};
			assertThrows(RocksDBException.class, () -> logSequence.tryApply(raftLog, 1));
			assertEquals(0, logSequence.getLastApplied(), "lastApplied不得推进");
			assertEquals(List.of(10, 20, 30), readStorage(table, 1), "首次apply的flush已落盘");
			assertEquals(0, callbackCount.get(), "收尾未完成，回调不得触发");

			// 2. 重试（钩子一次性已自动清空）：
			//    断言①不得重放增量（修复前：原始对象不放回->解码重放OP_ADD->[10,20,30,30]）；
			//    断言②必须经原始raftLog收尾并回调（修复前：解码新对象回调丢失，计数0——
			//    等待的业务线程等满超时拿到RaftRetry假失败）。
			logSequence.tryApply(raftLog, 1);
			assertEquals(List.of(10, 20, 30), readStorage(table, 1), "重试必须跳过增量重放（恰好应用一次）");
			assertEquals(1, logSequence.getLastApplied(), "重试后lastApplied推进");
			assertEquals(1, callbackCount.get(), "原始raftLog的回调必须恰好触发一次");
		}
	}
}
