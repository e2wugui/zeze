package Zeze.Raft.RocksRaft;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RaftLog;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.FuncLong;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND7-14联动（R3-R桶D①）：takePendingFlush 的 term 不匹配丢弃分支驱逐污染记录。
 * 旧条目（index=i, term=T）在 leaderApply/followerApply 完成"先改内存"后 flush 失败，
 * 进入补偿（putPendingFlush）；随后同 index 被新 term 条目复用（旧条目被截断）。
 * 丢弃过期补偿只释放了在用计数，被截断条目应用过的内存 bean 仍留在缓存中：
 * 新条目（新 leader 在干净基线上生成的增量 Edit）经 getOrLoad 命中污染记录，
 * 叠加在旧条目残迹上应用并 flush——双重应用被提交复制，leader/follower 静默分歧。
 * 修复：丢弃分支对污染记录做 isAccessed 保护下的 setRemoved+pair-remove（不经使用方
 * 回调，理由见 Record.evictPolluted 的决策注释），后续 getOrLoad 从 storage 重载
 * 截断前的干净基线（flush 失败时 storage 仍是已提交状态），新条目恰好应用一次。
 * 直接构造 Rocks（不 start server）复现：flush 失败（补偿登记）→ 新 term 截断 →
 * 新条目 Edit 重放，断言无双重应用；修复前（无驱逐）污染 bean 上叠加，断言失败。
 */
@Fast
public class TestFnd714PollutedRecordEviction {
	private static final String raftName = "127.0.0.1:17690";
	private static final String dbHome = "TestFnd714PollutedRecordEviction.raft";
	private static final String templateName = "tFnd714Polluted";

	// 含 CollList1 的最小bean（对齐TestFlushRetryApply的载体：增量日志OP_ADD按索引追加，
	// 是污染叠加最直接的观测面）。
	public static final class BListBean extends Bean {
		private final CollList1<Integer> _list;

		public BListBean() {
			_list = new CollList1<>(Integer.class);
			_list.variableId(1);
		}

		public CollList1<Integer> getList() {
			return _list;
		}

		@Override
		public Bean copy() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void encode(ByteBuffer bb) {
			_list.encode(bb);
		}

		@Override
		public void decode(Zeze.Serialize.IByteBuffer bb) {
			_list.decode(bb);
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
			_list.initRootInfo(root, this);
		}

		@Override
		public void followerApply(Log log) {
			var vars = ((LogBean)log).getVariables();
			if (vars == null)
				return;
			for (var it = vars.iterator(); it.moveToNext(); ) {
				var vlog = it.value();
				if (vlog.getVariableId() == 1)
					_list.followerApply(vlog);
			}
		}

		@Override
		public void leaderApplyNoRecursive(Log vlog) {
			if (vlog.getVariableId() == 1)
				_list.leaderApplyNoRecursive(vlog);
		}
	}

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17690" DbHome="TestFnd714PollutedRecordEviction.raft">
					<node Host="127.0.0.1" Port="17690"/>
					<node Host="127.0.0.1" Port="17691"/>
					<node Host="127.0.0.1" Port="17692"/>
				</raft>
				""");
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

	private static Rocks newRocks() throws Exception {
		var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false);
		rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
		return rocks;
	}

	// 在过程中执行修改并捕获收集到的 Changes：appendLog 因非leader抛 RaftRetry，过程按
	// 失败返回；业务编辑已原位落在缓存记录上（Savepoint.rollback不还原内存态），
	// Changes 已收集完成——即真实leader上"leaderApply已改内存、flush失败"的现场。
	private static Changes captureChanges(Rocks rocks, FuncLong func) throws Exception {
		final Transaction[] ts = new Transaction[1];
		var rc = rocks.newProcedure(() -> {
			ts[0] = Transaction.getCurrent();
			return func.call();
		}).call();
		assertEquals(Zeze.Transaction.Procedure.RaftRetry, rc); // not leader
		var changes = ts[0].getChanges();
		assertNotNull(changes);
		return changes;
	}

	// 直写存储层做初始数据（绕过事务，模拟截断前已提交落盘的状态）。
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

	// flush失败→补偿→新term截断→新条目Edit重放：污染记录必须被驱逐，新条目在
	// storage重载的干净基线上恰好应用一次（无双重应用）。
	@Test
	public void testTermMismatchDiscardEvictsPollutedRecord() throws Exception {
		try (var rocks = newRocks()) {
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			seedStorage(table, 1, 10, 20); // 截断前已提交基线 [10,20]

			// 旧条目（index=2, term=1）：Edit add(30)。leaderApply 已把缓存 bean 改到
			// [10,20,30]（业务编辑原位落在缓存记录上），flush 失败进入补偿持有。
			var oldEntryChanges = captureChanges(rocks, () -> {
				table.getOrAdd(1).getList().add(30);
				return 0L;
			});
			oldEntryChanges.encode(ByteBuffer.Allocate()); // 对齐真实路径：解析出Record.table
			var polluted = table.getLruCache().get(1);
			assertNotNull(polluted);
			rocks.putPendingFlush(2, 1, List.of(polluted));
			assertTrue(polluted.isAccessed(), "compensation must hold the polluted record in-use");
			assertEquals(List.of(10, 20), readStorage(table, 1),
					"flush failed: storage must stay at the committed baseline");

			// 新条目（index=2, term=2）由新 leader 在干净基线上生成：Edit add(99)。
			// 增量日志（OP_ADD）只含本次编辑、与基线无关，在本节点污染现场构造不影响
			// 其语义；构造过程给缓存 bean 额外留下的幻影会被随后的驱逐重置，不影响
			// 断言区分度（修复前最终是 [10,20,30,99,99]：旧条目残迹+新条目双重）。
			var newEntryChanges = captureChanges(rocks, () -> {
				table.getOrAdd(1).getList().add(99);
				return 0L;
			});
			newEntryChanges.encode(ByteBuffer.Allocate());

			// 新 term 条目复用 index=2：takePendingFlush term 不匹配丢弃旧补偿并驱逐
			// 污染记录，新条目的 Edit 经 getOrLoad 从 storage 重载干净基线 [10,20] 应用。
			rocks.followerApply(newEntryChanges, new RaftLog(2, 2, newEntryChanges));

			assertEquals(List.of(10, 20, 99), readStorage(table, 1),
					"no double-apply: truncated entry's edit must not survive the replay");
			assertFalse(polluted.isAccessed(), "discard must have released the compensation hold");
			assertNotSame(polluted, table.getLruCache().get(1),
					"cache must hold a fresh record reloaded from the clean storage baseline");
		}
	}

	// 记录在用（并发业务事务）时驱逐必须放弃：不强制摘除（会重演FND7-14丢失更新：
	// 在用方提交经origin应用flush后，与驱逐后重装载的记录互相整值覆盖）、也不得
	// 只置removed不pair-remove（getOrLoad重试环会拿同一条目自旋）。
	@Test
	public void testEvictionSkippedWhileInUse() throws Exception {
		try (var rocks = newRocks()) {
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			seedStorage(table, 1, 10);

			final Record<?>[] holder = new Record<?>[1];
			var rc = rocks.newProcedure(() -> {
				assertNotNull(table.get(1)); // 业务访问：beginAccess持有
				var record = table.getLruCache().get(1);
				assertNotNull(record);
				holder[0] = record;
				rocks.putPendingFlush(3, 1, List.of(record)); // 补偿持有（模拟flush失败）
				// 新term截断丢弃：业务仍在用——驱逐必须放弃，记录留在缓存且不置removed。
				assertNull(rocks.takePendingFlush(3, 2));
				assertTrue(record.isAccessed(), "business hold must survive the discard");
				assertSame(record, table.getLruCache().get(1), "in-use polluted record must stay cached this round");
				assertFalse(record.getRemoved(), "must not set removed without pair-remove (getOrLoad spin)");
				return 0L;
			}).call();
			assertEquals(0L, rc);

			// 过程结束（业务持有释放）：本轮驱逐窗口已过，污染记录留在缓存（残余；
			// 该窗口内并发读方的可见性由"无同key并发隔离"契约覆盖，后续容量驱逐/
			// 重装载兜底）。本用例锁定的是"在用时不强制摘除、不自旋"的保护语义。
			var record = holder[0];
			assertNotNull(record);
			assertFalse(record.isAccessed());
			assertSame(record, table.getLruCache().get(1));
		}
	}
}
