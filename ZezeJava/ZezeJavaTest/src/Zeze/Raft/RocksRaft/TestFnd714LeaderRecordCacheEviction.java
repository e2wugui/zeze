package Zeze.Raft.RocksRaft;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RocksRaft Table 记录缓存的LRU驱逐在用保护（FND7-14）。
 * leader业务事务从table.get()拿到缓存Record引用后原位修改其bean，提交时
 * Transaction.leaderApply经事务捕获的ar.origin应用并flush。原实现生成代码注册表模板
 * 不设lruTryRemoveCallback，cleanNow走无回调分支无条件remove：容量压力下驱逐事务
 * 正在使用的记录A后，同key再访问从storage装载出旧值的新记录C——后续读经过期值；
 * 再修改提交则已提交更新被C的旧值全量覆盖静默丢失。
 * 修复：Table总是安装带在用保护的驱逐回调（在用拒绝；确无在用时r.mutex内置
 * removed=true激活getOrLoad重试环并pair-remove），getOrLoad在r.mutex内登记在用，
 * Transaction.perform收尾与Rocks.followerApply的flush成功后释放。
 * 直接构造Rocks（不start server）用受控调用序列复现：修复前无回调注册
 * （getTryRemoveCallback()==null）且驱逐无保护，断言失败，有区分度。
 */
@Fast
public class TestFnd714LeaderRecordCacheEviction {
	private static final String raftName = "127.0.0.1:17680";
	private static final String dbHome = "TestFnd714LeaderRecordCacheEviction.raft";
	private static final String templateName = "tFnd714Evict";

	// 含 CollList1 的最小bean（对齐TestFlushRetryApply的载体：增量日志按索引追加）。
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
				<raft Name="127.0.0.1:17680" DbHome="TestFnd714LeaderRecordCacheEviction.raft">
					<node Host="127.0.0.1" Port="17680"/>
					<node Host="127.0.0.1" Port="17681"/>
					<node Host="127.0.0.1" Port="17682"/>
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

	// 直写存储层做初始数据（绕过事务，模拟上一次已提交落盘的状态）。
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

	// 在过程中执行只读访问并返回结果（只读不产生changes，非leader也成功返回0）。
	private static Object[] readInProcedure(Rocks rocks, FuncLong func) throws Exception {
		var out = new Object[2];
		var rc = rocks.newProcedure(() -> {
			out[0] = func.call();
			return 0L;
		}).call();
		out[1] = rc;
		return out;
	}

	// 事务访问中的记录不可被驱逐；事务结束（perform收尾释放）后才可驱逐，
	// 驱逐后下次访问经慢路径从storage重装载新记录。
	@Test
	public void testInUseRecordNotEvicted() throws Exception {
		try (var rocks = newRocks()) {
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			seedStorage(table, 1, 10);

			// 过程内：记录被访问（在用），驱逐回调必须拒绝。
			var rc = rocks.newProcedure(() -> {
				assertNotNull(table.get(1));
				var record = table.getLruCache().get(1);
				assertNotNull(record);
				var cb = table.getLruCache().getTryRemoveCallback();
				assertNotNull(cb, "FND7-14: protective tryRemoveCallback must be installed (generated templates register none)");
				assertFalse(cb.test(1, record), "record referenced by an in-flight transaction must NOT be evicted");
				assertNotNull(table.getLruCache().get(1), "record must stay in cache while in use");
				return 0L;
			}).call();
			assertEquals(0L, rc);

			// 过程结束（perform收尾释放）：驱逐放行，pair-remove生效。
			var cb = table.getLruCache().getTryRemoveCallback();
			var record = table.getLruCache().get(1);
			assertNotNull(record);
			assertTrue(cb.test(1, record), "released record must be evictable");
			assertNull(table.getLruCache().get(1), "evicted record must be pair-removed from cache");

			// 下次访问经慢路径重装载：新记录、值与storage一致。
			var out = readInProcedure(rocks, () -> {
				var v = table.get(1);
				assertNotNull(v);
				assertNotSame(record, table.getLruCache().get(1), "next access must load a fresh record");
				var items = new ArrayList<Integer>();
				for (var i : v.getList())
					items.add(i);
				assertEquals(List.of(10), items);
				return 0L;
			});
			assertEquals(0L, out[1]);
		}
	}

	// 已提交更新不得因驱逐丢失：事务结束释放后驱逐origin记录，迟到leaderApply经
	// origin应用并flush（库状态正确），后续访问从storage重装载到提交后的值——
	// 修复前无保护：驱逐发生在事务中间，flush后缓存里留下装载于驱逐与flush之间的
	// 旧值记录C，已提交更新被静默覆盖丢失。
	@Test
	public void testEvictionCannotStrandStaleRecordAcrossApply() throws Exception {
		try (var rocks = newRocks()) {
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			seedStorage(table, 1, 10);

			// 捕获带修改的Changes（非leader：appendLog抛RaftRetry，过程按失败返回，
			// 但日志与accessedRecords已收集在事务对象上——即真实leader上提交前的现场）。
			final Transaction[] ts = new Transaction[1];
			var rc = rocks.newProcedure(() -> {
				ts[0] = Transaction.getCurrent();
				table.getOrAdd(1).getList().add(30); // 原位修改缓存记录：[10] -> [10,30]
				return 0L;
			}).call();
			assertEquals(Zeze.Transaction.Procedure.RaftRetry, rc); // not leader
			var changes = ts[0].getChanges();
			assertNotNull(changes);
			changes.encode(ByteBuffer.Allocate()); // 对齐真实路径：解析出Record.table

			// 过程已返回（在用保护已随perform收尾释放）：此时驱逐是合法的。
			var cb = table.getLruCache().getTryRemoveCallback();
			assertNotNull(cb);
			var origin = table.getLruCache().get(1);
			assertNotNull(origin);
			assertTrue(cb.test(1, origin), "after perform released, eviction is allowed");
			assertNull(table.getLruCache().get(1));

			// 迟到的leader侧应用（真实方法）：经origin记录应用并flush。
			ts[0].leaderApply(changes, new RaftLog(1, 2, changes));
			assertEquals(List.of(10, 30), readStorage(table, 1), "committed update must reach storage");

			// 后续访问：缓存从storage重装载到提交后的值，无过期读。
			var out = readInProcedure(rocks, () -> {
				var items = new ArrayList<Integer>();
				for (var i : table.get(1).getList())
					items.add(i);
				assertEquals(List.of(10, 30), items);
				return 0L;
			});
			assertEquals(0L, out[1]);
			assertEquals(List.of(10, 30), readStorage(table, 1));
		}
	}

	// 【FND7-14残余】leader侧flush失败进入pendingFlush补偿后，perform收尾会释放业务
	// 访问计数——补偿期间记录必须由补偿登记继续持有在用：否则迟到flush重试窗口内
	// 同key重装载从storage拿到flush前旧值，后续在该旧基线上的修改提交会整值覆盖
	// 已应用未flush的更新（丢失更新）。补偿被消费（重试flush成功，leader/follower
	// 两条pending分支）或过期丢弃（takePendingFlush的term不匹配）时释放。
	@Test
	public void testPendingFlushHoldsAccessUntilConsumed() throws Exception {
		try (var rocks = newRocks()) {
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			seedStorage(table, 1, 10);

			final Transaction[] ts = new Transaction[1];
			var rc = rocks.newProcedure(() -> {
				ts[0] = Transaction.getCurrent();
				table.getOrAdd(1).getList().add(30);
				return 0L;
			}).call();
			assertEquals(Zeze.Transaction.Procedure.RaftRetry, rc);
			var changes = ts[0].getChanges();
			assertNotNull(changes);
			changes.encode(ByteBuffer.Allocate()); // 对齐真实路径：解析出Record.table

			// 过程返回后（perform已释放业务计数）无补偿登记：可驱逐（基线对照）。
			var cb = table.getLruCache().getTryRemoveCallback();
			assertNotNull(cb);
			var record = table.getLruCache().get(1);
			assertNotNull(record);
			assertTrue(cb.test(1, record), "no pending registration: evictable");
			assertFalse(record.isAccessed());

			// 重装载恢复现场（上一行已驱逐旧记录）。
			var out = readInProcedure(rocks, () -> {
				assertNotNull(table.get(1));
				return 0L;
			});
			assertEquals(0L, out[1]);
			var record2 = table.getLruCache().get(1);
			assertNotNull(record2);
			assertNotSame(record, record2);

			// 补偿登记即持有在用：驱逐必须拒绝（修复前putPendingFlush不计数，
			// 迟到flush窗口内记录可被驱逐，同key重装载读到storage旧值）。
			rocks.putPendingFlush(3, 1, List.of(record2));
			assertTrue(record2.isAccessed(), "pendingFlush registration must hold the record in-use");
			assertFalse(cb.test(1, record2), "record under pendingFlush compensation must NOT be evicted");
			assertNotNull(table.getLruCache().get(1));

			// 过期丢弃（term不匹配）释放。
			assertNull(rocks.takePendingFlush(3, 2));
			assertFalse(record2.isAccessed(), "term-mismatch discard must release the hold");
			assertTrue(cb.test(1, record2));
			assertNull(table.getLruCache().get(1));

			// 消费路径（leader重试flush成功）也释放：重装载后再登记，走真实leaderApply
			// 的pending分支（只重试flush），成功后释放在用保护。
			var out2 = readInProcedure(rocks, () -> {
				assertNotNull(table.get(1));
				return 0L;
			});
			assertEquals(0L, out2[1]);
			var record3 = table.getLruCache().get(1);
			assertNotNull(record3);
			rocks.putPendingFlush(4, 1, List.of(record3));
			assertTrue(record3.isAccessed());
			ts[0].leaderApply(changes, new RaftLog(1, 4, changes));
			assertFalse(record3.isAccessed(), "leader retry-flush success must release the hold");
			assertTrue(cb.test(1, record3));
			assertNull(table.getLruCache().get(1));
		}
	}

	// followerApply装载的记录在flush成功后释放在用保护（后续可驱逐）。
	@Test
	public void testFollowerApplyReleasesAccess() throws Exception {
		try (var rocks = newRocks()) {
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			seedStorage(table, 1, 10);

			final Transaction[] ts = new Transaction[1];
			var rc = rocks.newProcedure(() -> {
				ts[0] = Transaction.getCurrent();
				table.getOrAdd(1).getList().add(30);
				return 0L;
			}).call();
			assertEquals(Zeze.Transaction.Procedure.RaftRetry, rc);
			var changes = ts[0].getChanges();
			assertNotNull(changes);
			changes.encode(ByteBuffer.Allocate());

			// 丢弃事务侧的在用登记（perform已释放），再用follower路径装载并应用：
			// followerApply装载的记录在flush前受保护、成功后释放。
			var cb = table.getLruCache().getTryRemoveCallback();
			assertNotNull(cb);
			var origin = table.getLruCache().get(1);
			assertNotNull(origin);
			assertTrue(cb.test(1, origin)); // perform已释放，先驱逐

			rocks.followerApply(changes, new RaftLog(1, 2, changes));
			assertEquals(List.of(10, 30), readStorage(table, 1));

			// followerApply应用完成：它装载的当前缓存记录已释放，可再次驱逐。
			var loaded = table.getLruCache().get(1);
			assertNotNull(loaded, "followerApply must have loaded the record into cache");
			assertTrue(cb.test(1, loaded), "record loaded by followerApply must be released after flush");
			assertNull(table.getLruCache().get(1));
		}
	}
}
