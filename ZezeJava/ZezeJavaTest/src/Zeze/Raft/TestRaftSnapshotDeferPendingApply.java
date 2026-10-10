package Zeze.Raft;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * unique存根写失败留下的pending窗口内，快照不得把"内容超前"标成lastApplied索引。
 * <p>
 * tryApply对unique条目：①内存变更②flush③unique存根写④lastApplied推进。③失败时按
 * markApplied补偿登记（pendingFlushApplies）后抛出，lastApplied停在N-1而存储已含日志N
 * 的数据。修复前checkpoint只取lastApplied做lastIncludedIndex再物理复制状态库，生成的
 * 快照"内容包含N、声明N-1"；恢复重放后缀N时非幂等增量（list按索引追加）双重应用。
 * 修复后checkpoint持raft锁检查pendingFlushApplies非空即返回null推迟本次快照：不产出
 * 快照文件、不commitSnapshot（firstIndex不推进），apply重试收尾后的trySnapshot再次触发。
 * <p>
 * headless构造Rocks（不start server、无网络），复用TestUniqueStubFailRetryApply的
 * 非幂等CollList1载体。修复前红：pending窗口内snapshot返回success=true并产出
 * lastIncludedIndex=0、内容[10,20,30]的快照且firstIndex推进；修复后绿：推迟、重试收尾后
 * 正常快照且fence与内容一致。
 */
@Fast
public class TestRaftSnapshotDeferPendingApply {
	private static final String raftName = "127.0.0.1:17660";
	private static final String dbHome = "TestRaftSnapshotDeferPending.raft";
	private static final String templateName = "tSnapshotDeferPending";

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17660" DbHome="TestRaftSnapshotDeferPending.raft">
					<node Host="127.0.0.1" Port="17660"/>
					<node Host="127.0.0.1" Port="17661"/>
					<node Host="127.0.0.1" Port="17662"/>
				</raft>
				""");
	}

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		// CollList1的增量日志解码工厂：生成代码场景由生成的注册代码负责，自建表模板需自注册。
		Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogList1<>(Integer.class));
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() {
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	// 收集带unique请求编号的增量Changes（对齐TestUniqueStubFailRetryApply.captureUniqueChanges）。
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
		changes.getUnique().setRequestId(1);
		changes.getUnique().setClientId("test.snapshotDeferPending");
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
	public void testSnapshotDeferredWhilePendingApplyUnfinished() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false)) {
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			seedStorage(table, 1, 10, 20);

			var changes = captureUniqueChanges(rocks, table);
			var raftLog = new RaftLog(1, 1, changes);
			var callbackCount = new AtomicInteger();
			raftLog.setLeaderCallback((log, success) -> callbackCount.incrementAndGet());
			var logSequence = rocks.getRaft().getLogSequence();
			logSequence.saveLog(raftLog);
			// 模拟appendLog对leader请求的登记：tryApply优先从leaderAppendLogs取原始对象。
			leaderAppendLogsOf(logSequence).put(1L, raftLog);

			// 1. 首次应用：原始对象->leaderApply（内存+flush落盘[10,20,30]），存根写注入失败。
			//    pending窗口打开：存储内容超前，lastApplied=0。
			logSequence.testHookBeforeUniqueApply = () -> {
				throw new RocksDBException("test inject: unique stub apply fail once");
			};
			assertThrows(RocksDBException.class, () -> logSequence.tryApply(raftLog, 1));
			assertEquals(0, logSequence.getLastApplied(), "lastApplied不得推进");
			assertEquals(List.of(10, 20, 30), readStorage(table, 1), "首次apply的flush已落盘，存储超前于lastApplied");

			// 2. pending窗口内快照：必须推迟。修复前：产出lastIncludedIndex=0、内容[10,20,30]
			//    的快照并提交（恢复重放日志1->[10,20,30,30]双重应用的种子）。
			var deferredPath = Path.of(dbHome, "snapshot.deferred.zip");
			var deferred = rocks.snapshot(deferredPath.toString());
			assertFalse(deferred.success, "pending窗口内的快照必须推迟");
			assertFalse(Files.exists(deferredPath), "推迟不得产出快照文件");
			assertEquals(0, logSequence.getFirstIndex(), "推迟不得提交（firstIndex不推进）");

			// 3. 重试收尾：数据恰好应用一次（修复的重试补偿，见TestUniqueStubFailRetryApply），
			//    pending清空、lastApplied推进。
			logSequence.tryApply(raftLog, 1);
			assertEquals(1, logSequence.getLastApplied(), "重试后lastApplied推进");
			assertEquals(List.of(10, 20, 30), readStorage(table, 1), "重试必须跳过增量重放");
			assertEquals(1, callbackCount.get(), "原始raftLog的回调恰好触发一次");

			// 4. 收尾后快照：正常生成并提交，fence与内容一致（默认立即提交模式，firstIndex推进）。
			var finalPath = Path.of(dbHome, "snapshot.final.zip");
			var done = rocks.snapshot(finalPath.toString());
			assertTrue(done.success, "pending清空后快照必须正常执行");
			assertEquals(1, done.lastIncludedIndex, "快照边界必须是已收尾的lastApplied");
			assertEquals(1, logSequence.getFirstIndex(), "提交推进firstIndex到快照边界");
		}
	}
}
