package Zeze.Raft.RocksRaft;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RaftLog;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND8-39 回归：flush补偿换手的"先endAccess后putPendingFlush"归零间隙——间隙内
 * 记录可被LRU驱逐，同key重装载从storage拿到flush前旧值，增量日志应用在旧基线上
 * 即节点静默永久分歧。修复为转移语义：putPendingFlush增加addReference参数，
 * follower首失败/重试再失败与leader重试再失败三个换手点登记不增不减（调用方的
 * endAccess删除），计数全程≥1；leader首失败保留加计语义（业务计数在appendLog
 * 等待中，+1与业务超时释放配对）。消费端配对不变（不泄漏）。
 * 注入方式：向storage的batchPool预投毒Batch（armed时commit抛RocksDBException，
 * flush包装为FlushException；未armed时写真实库），确定性驱动真实followerApply/
 * leaderApply的失败与重试路径，断言补偿全周期（首失败→重试再失败→重试成功）
 * 在用保护不断档、最终无泄漏。归零间隙本身只有两条语句宽，无法确定性观测，
 * 由转移语义的结构性保证（不再有endAccess语句）+ 本用例的全周期持有断言承担。
 */
@Fast
public class TestFnd839PendingFlushTransfer {
	private static final String raftName = "127.0.0.1:27680";
	private static final String dbHome = "a2_TestFnd839PendingFlushTransfer.raft";
	private static final String templateName = "tFnd839Transfer";

	// 含 CollList1 的最小bean（对齐TestFnd714LeaderRecordCacheEviction的载体）。
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

	/** 受控失败Batch：armed>0时commit抛RocksDBException（flush包装为FlushException）并自减。 */
	private static final class FailingBatch extends RocksDatabase.Batch {
		private final AtomicInteger failArmed;

		FailingBatch(@NotNull RocksDatabase outer, @NotNull AtomicInteger failArmed) {
			outer.super();
			this.failArmed = failArmed;
		}

		@Override
		public void commit(@NotNull WriteOptions options) throws RocksDBException {
			if (failArmed.getAndUpdate(x -> x > 0 ? x - 1 : 0) > 0)
				throw new RocksDBException("FND8-39 simulated flush fail");
			super.commit(options); // enclosing为真实storage：未armed时正常写真实库
		}

		@Override
		public void close() {
			// 不回池也不关句柄：毒批一次性消费，后续flush借用正常Batch（句柄随测试进程回收）。
		}
	}

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:27680" DbHome="a2_TestFnd839PendingFlushTransfer.raft">
					<node Host="127.0.0.1" Port="27680"/>
					<node Host="127.0.0.1" Port="27681"/>
					<node Host="127.0.0.1" Port="27682"/>
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
		return new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false);
	}

	// 向storage的batchPool预投毒count个Batch（armed总开关共享），下次count次flush可控失败。
	private static void armFlush(Rocks rocks, AtomicInteger failArmed, int count) throws Exception {
		var storageField = Rocks.class.getDeclaredField("storage");
		storageField.setAccessible(true);
		var db = (RocksDatabase)storageField.get(rocks);
		var poolField = RocksDatabase.class.getDeclaredField("batchPool");
		poolField.setAccessible(true);
		db.lock();
		try {
			@SuppressWarnings("unchecked")
			var pool = (ArrayList<RocksDatabase.Batch>)poolField.get(db);
			if (pool == null) {
				pool = new ArrayList<>();
				poolField.set(db, pool);
			}
			for (int i = 0; i < count; i++)
				pool.add(new FailingBatch(db, failArmed));
		} finally {
			db.unlock();
		}
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

	// 捕获带修改的Changes（非leader：appendLog抛RaftRetry，日志与accessedRecords留在事务上）。
	private static Transaction captureChanges(Rocks rocks, Table<Integer, BListBean> table, int key) throws Exception {
		final Transaction[] ts = new Transaction[1];
		var rc = rocks.newProcedure(() -> {
			ts[0] = Transaction.getCurrent();
			table.getOrAdd(key).getList().add(30); // [10] -> [10,30]
			return 0L;
		}).call();
		assertEquals(Zeze.Transaction.Procedure.RaftRetry, rc); // not leader
		assertNotNull(ts[0]);
		var changes = ts[0].getChanges();
		assertNotNull(changes);
		changes.encode(ByteBuffer.Allocate()); // 对齐真实路径：解析出Record.table
		return ts[0];
	}

	/** follower首失败（转移语义）：flush抛出后记录必须仍在用（驱逐拒绝），重试成功后配对释放。 */
	@Test
	public void testFollowerFirstFailTransferKeepsHold() throws Exception {
		try (var rocks = newRocks()) {
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			seedStorage(table, 1, 10);
			var ts = captureChanges(rocks, table, 1);
			var changes = ts.getChanges();

			// 事务结束（perform已释放业务计数）：先驱逐，让followerApply走装载路径持有计数。
			var cb = table.getLruCache().getTryRemoveCallback();
			assertNotNull(cb);
			assertTrue(cb.test(1, table.getLruCache().get(1)), "no hold: evictable");
			assertNull(table.getLruCache().get(1));

			var failArmed = new AtomicInteger(1);
			armFlush(rocks, failArmed, 1);
			assertThrows(Rocks.FlushException.class, () -> rocks.followerApply(changes, new RaftLog(1, 2, changes)));

			// 转移语义：装载计数随失败转入补偿登记，全程未归零——记录仍在缓存、驱逐仍拒绝。
			var record = table.getLruCache().get(1);
			assertNotNull(record, "in-use record must stay in cache across failed flush");
			assertTrue(record.isAccessed(), "transfer registration must keep the load hold");
			assertFalse(cb.test(1, record), "record under compensation must NOT be evicted");

			// 重试成功：消费配对释放，不泄漏。
			rocks.followerApply(changes, new RaftLog(1, 2, changes));
			assertFalse(record.isAccessed(), "retry-flush success must release the hold (paired, no leak)");
			assertEquals(List.of(10, 30), readStorage(table, 1));
		}
	}

	/** follower重试再失败（转移换手点）：补偿持有随消费原样移入新登记，仍不断档、仍不泄漏。 */
	@Test
	public void testFollowerRetryReFailTransferNoGapNoLeak() throws Exception {
		try (var rocks = newRocks()) {
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			seedStorage(table, 1, 10);
			var ts = captureChanges(rocks, table, 1);
			var changes = ts.getChanges();

			var cb = table.getLruCache().getTryRemoveCallback();
			assertNotNull(cb);
			assertTrue(cb.test(1, table.getLruCache().get(1)));
			assertNull(table.getLruCache().get(1));

			// 两个毒Batch：首失败 + 重试再失败。
			var failArmed = new AtomicInteger(2);
			armFlush(rocks, failArmed, 2);
			assertThrows(Rocks.FlushException.class, () -> rocks.followerApply(changes, new RaftLog(1, 2, changes)));
			assertThrows(Rocks.FlushException.class, () -> rocks.followerApply(changes, new RaftLog(1, 2, changes)),
					"重试路径必须仍是FlushException（只重试flush，不重放增量日志）");

			// 重试再失败的换手点：持有原样转移（计数不增不减），不得归零。
			var record = table.getLruCache().get(1);
			assertNotNull(record);
			assertTrue(record.isAccessed(), "re-registration must transfer (not drop) the hold");
			assertFalse(cb.test(1, record), "record under re-registered compensation must NOT be evicted");

			rocks.followerApply(changes, new RaftLog(1, 2, changes)); // 第三次：成功
			assertFalse(record.isAccessed(), "multi-cycle compensation must pair exactly (no leak)");
			assertEquals(List.of(10, 30), readStorage(table, 1));
			assertTrue(cb.test(1, record), "released record must be evictable again");
		}
	}

	/**
	 * leader侧：首失败保留加计语义（业务计数+补偿计数=2，业务超时释放后仍剩1）；
	 * 重试再失败走转移语义（剩1不动）；重试成功归零。三个换手点的配对一次覆盖。
	 */
	@Test
	public void testLeaderFirstFailAddRefAndRetryReFailTransfer() throws Exception {
		try (var rocks = newRocks()) {
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			seedStorage(table, 1, 10);
			var ts = captureChanges(rocks, table, 1);
			var changes = ts.getChanges();

			var cb = table.getLruCache().getTryRemoveCallback();
			assertNotNull(cb);
			var origin = table.getLruCache().get(1);
			assertNotNull(origin);

			// 模拟业务线程仍在appendLog等待的业务持有（leader首失败时它尚未释放）。
			origin.beginAccess();

			var failArmed = new AtomicInteger(1);
			armFlush(rocks, failArmed, 1);
			assertThrows(Rocks.FlushException.class, () -> ts.leaderApply(changes, new RaftLog(1, 2, changes)));
			assertTrue(origin.isAccessed(), "add-ref registration must add the compensation hold");

			// 业务超时释放（perform finally）：补偿持有仍在，驱逐拒绝。
			origin.endAccess();
			assertTrue(origin.isAccessed(), "compensation hold must survive business release");
			assertFalse(cb.test(1, origin));

			// 重试再失败：转移语义（不增不减），仍持有。
			failArmed.set(1);
			armFlush(rocks, failArmed, 1);
			assertThrows(Rocks.FlushException.class, () -> ts.leaderApply(changes, new RaftLog(1, 2, changes)));
			assertTrue(origin.isAccessed(), "retry re-fail must transfer the hold (never zero)");
			assertFalse(cb.test(1, origin));

			// 重试成功：配对归零，落库正确。
			ts.leaderApply(changes, new RaftLog(1, 2, changes));
			assertFalse(origin.isAccessed(), "final success must release exactly once");
			assertEquals(List.of(10, 30), readStorage(table, 1));
		}
	}

	/** 过期丢弃（term不匹配）在转移语义下同样配对释放并驱逐污染记录。 */
	@Test
	public void testTransferHoldReleasedOnTermMismatch() throws Exception {
		try (var rocks = newRocks()) {
			rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			seedStorage(table, 1, 10);
			var ts = captureChanges(rocks, table, 1);
			var changes = ts.getChanges();

			var cb = table.getLruCache().getTryRemoveCallback();
			assertNotNull(cb);
			assertTrue(cb.test(1, table.getLruCache().get(1)));
			assertNull(table.getLruCache().get(1));

			var failArmed = new AtomicInteger(1);
			armFlush(rocks, failArmed, 1);
			assertThrows(Rocks.FlushException.class, () -> rocks.followerApply(changes, new RaftLog(1, 2, changes)));
			var record = table.getLruCache().get(1);
			assertNotNull(record);
			assertTrue(record.isAccessed());

			// 同index(=2)被新term复用：过期丢弃释放转移持有并驱逐污染记录。
			assertNull(rocks.takePendingFlush(2, 3));
			assertFalse(record.isAccessed(), "term-mismatch discard must release the transferred hold");
			assertNull(table.getLruCache().get(1), "term-mismatch discard must evict the polluted record");
		}
	}
}
