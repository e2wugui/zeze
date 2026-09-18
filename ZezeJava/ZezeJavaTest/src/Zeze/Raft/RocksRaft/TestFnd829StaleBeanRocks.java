package Zeze.Raft.RocksRaft;

import java.io.File;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RaftLog;
import Zeze.Raft.RocksRaft.Log1.LogInt;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.FuncLong;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND8-29回归（Rocks镜像）：LogMap2/LogSortedMap2.encode 的 changed 过滤缺身份校验。
 * put覆盖后旧bean不detach（仍managed、mapKey不清），跨事务陈旧引用的修改会被collect，
 * key级三条过滤（putted/removed/存在性）拦不住，changedWithKey 携带陈旧日志，
 * follower 侧应用到覆盖后的新值上，状态机静默分歧。
 * 修复：第三条过滤强化为 c.getThis()==getValue().get(pkey)。
 * <p>
 * 完整序列（TestMap2FollowerApplyMapKey 范式，不start server、无网络）：
 * put装入itemA并应用（txn1）→ put覆盖为itemB并应用（txn2，itemA引用仍managed）→
 * 陈旧引用itemA.setI(9)（txn3）：encode→decode 后 changedWithKey 必须为空、
 * followerApply 后存储的 itemB 值不变；正控：编辑当前值（txn4）正常携带、正常应用。
 */
@Fast
public class TestFnd829StaleBeanRocks {
	private static final String raftName = "127.0.0.1:26365";
	private static final String dbHome = "a3_TestFnd829StaleBeanRocks.raft";
	private static final String mapTemplateName = "tMap2Stale";
	private static final String sortedTemplateName = "tSortedMap2Stale";

	static {
		// follower 路径 Changes.decode -> LogBean.decode -> Log.create(typeId) 需要 factory。
		Rocks.registerLog(() -> new LogMap2<>(Long.class, BItem.class));
		Rocks.registerLog(() -> new LogSortedMap2<>(Long.class, BItem.class));
	}

	/** map2 动态 value bean：mapKey 由容器维护（参照 TestMap2FollowerApplyMapKey 的 BItem）。 */
	public static final class BItem extends Bean {
		private transient Object _mapKey;

		@Override
		public Object mapKey() {
			return _mapKey;
		}

		@Override
		public void mapKey(Object mapKey) {
			_mapKey = mapKey;
		}

		private int _i;

		public int getI() {
			if (isManaged()) {
				var t = Transaction.getCurrent();
				if (t == null)
					return _i;
				var log = t.getLog(objectId() + 1);
				if (log == null)
					return _i;
				return ((LogInt)log).value;
			}
			return _i;
		}

		public void setI(int value) {
			if (isManaged())
				Transaction.getCurrent().putLog(new LogInt(this, 1, value));
			else
				_i = value;
		}

		@Override
		public Bean copy() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(getI());
		}

		@Override
		public void decode(IByteBuffer bb) {
			setI(bb.ReadInt());
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
		}

		@Override
		public void followerApply(Log log) {
			var vars = ((LogBean)log).getVariables();
			if (vars == null)
				return;
			for (var it = vars.iterator(); it.moveToNext(); ) {
				var vlog = it.value();
				if (vlog.getVariableId() == 1)
					_i = ((LogInt)vlog).value;
			}
		}

		@Override
		public void leaderApplyNoRecursive(Log vlog) {
			if (vlog.getVariableId() == 1)
				_i = ((LogInt)vlog).value;
		}
	}

	/** 记录值bean基类：CollMapXxx 的 encode/decode/followerApply 样板。 */
	/** 记录值 bean：含 CollMap2&lt;Long, BItem&gt;。 */
	public static final class BMapBean extends Bean {
		private final CollMap2<Long, BItem> _map;

		public BMapBean() {
			_map = new CollMap2<>(Long.class, BItem.class);
			_map.variableId(1);
		}

		public CollMap2<Long, BItem> getMap() {
			return _map;
		}

		@Override
		public Bean copy() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void encode(ByteBuffer bb) {
			_map.encode(bb);
		}

		@Override
		public void decode(IByteBuffer bb) {
			_map.decode(bb);
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
			_map.initRootInfo(root, this);
		}

		@Override
		public void followerApply(Log log) {
			var vars = ((LogBean)log).getVariables();
			if (vars == null)
				return;
			for (var it = vars.iterator(); it.moveToNext(); ) {
				var vlog = it.value();
				if (vlog.getVariableId() == 1)
					_map.followerApply(vlog);
			}
		}

		@Override
		public void leaderApplyNoRecursive(Log vlog) {
			if (vlog.getVariableId() == 1)
				_map.leaderApplyNoRecursive(vlog);
		}
	}

	/** 记录值 bean：含 CollSortedMap2&lt;Long, BItem&gt;（sorted 镜像）。 */
	public static final class BSortedMapBean extends Bean {
		private final CollSortedMap2<Long, BItem> _map;

		public BSortedMapBean() {
			_map = new CollSortedMap2<>(Long.class, BItem.class);
			_map.variableId(1);
		}

		public CollSortedMap2<Long, BItem> getMap() {
			return _map;
		}

		@Override
		public Bean copy() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void encode(ByteBuffer bb) {
			_map.encode(bb);
		}

		@Override
		public void decode(IByteBuffer bb) {
			_map.decode(bb);
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
			_map.initRootInfo(root, this);
		}

		@Override
		public void followerApply(Log log) {
			var vars = ((LogBean)log).getVariables();
			if (vars == null)
				return;
			for (var it = vars.iterator(); it.moveToNext(); ) {
				var vlog = it.value();
				if (vlog.getVariableId() == 1)
					_map.followerApply(vlog);
			}
		}

		@Override
		public void leaderApplyNoRecursive(Log vlog) {
			if (vlog.getVariableId() == 1)
				_map.leaderApplyNoRecursive(vlog);
		}
	}

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:26365" DbHome="a3_TestFnd829StaleBeanRocks.raft">
					<node Host="127.0.0.1" Port="26365"/>
					<node Host="127.0.0.1" Port="26366"/>
					<node Host="127.0.0.1" Port="26367"/>
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

	// 在过程中执行修改并捕获收集到的 Changes（appendLog因非leader抛RaftRetry，
	// 过程按失败返回；Changes已在_final_commit_里收集完成）。
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

	// 直写存储层做初始数据。
	private static <K, V extends Bean> void seedEmptyRecord(Table<K, V> table, K key) throws Exception {
		var seed = table.newValue();
		var keyBB = ByteBuffer.Allocate();
		table.encodeKey(keyBB, key);
		var valBB = ByteBuffer.Allocate();
		seed.encode(valBB);
		table.getRocksTable().put(keyBB.CopyIf(), valBB.CopyIf());
	}

	// 读存储层的最终值（不经缓存）。
	private static int readStorageItemI(Table<Integer, BMapBean> table, int key, long itemKey)
			throws Exception {
		var keyBB = ByteBuffer.Allocate();
		table.encodeKey(keyBB, key);
		var bytes = table.getRocksTable().get(keyBB.CopyIf());
		assertNotNull(bytes);
		var value = table.newValue();
		value.decode(ByteBuffer.Wrap(bytes));
		var item = value.getMap().get(itemKey);
		assertNotNull(item);
		return item.getI();
	}

	private static int readStorageSortedItemI(Table<Integer, BSortedMapBean> table, int key, long itemKey)
			throws Exception {
		var keyBB = ByteBuffer.Allocate();
		table.encodeKey(keyBB, key);
		var bytes = table.getRocksTable().get(keyBB.CopyIf());
		assertNotNull(bytes);
		var value = table.newValue();
		value.decode(ByteBuffer.Wrap(bytes));
		var item = value.getMap().get(itemKey);
		assertNotNull(item);
		return item.getI();
	}

	// encode -> 全新 decode（follower收到复制日志的状态）。
	private static Changes encodeDecode(Rocks rocks, Changes changes) {
		var bb = ByteBuffer.Allocate();
		changes.encode(bb);
		var decoded = new Changes(rocks);
		decoded.decode(ByteBuffer.Wrap(bb.CopyIf()));
		return decoded;
	}

	// 从 Changes 里找到指定类型的 map 日志。
	@SuppressWarnings("unchecked")
	private static <T extends Log> T findLog(Changes changes, Class<T> logClass) {
		for (var r : changes.getRecords().values())
			for (var logBean : r.getLogBean()) {
				var vars = logBean.getVariables();
				if (vars == null)
					continue;
				for (var it = vars.iterator(); it.moveToNext(); )
					if (logClass.isInstance(it.value()))
						return (T)it.value();
			}
		return null;
	}

	@Test
	public void testMap2StaleRefEditFiltered() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false)) {
			rocks.registerTableTemplate(mapTemplateName, Integer.class, BMapBean.class);
			var table = rocks.<Integer, BMapBean>getTableTemplate(mapTemplateName).openTable(0);
			seedEmptyRecord(table, 1);

			// txn1：装入itemA并应用（capture会回滚本地修改，应用后cache里是
			// decode装载的受管itemA实例——留作陈旧引用）。
			var put1 = captureChanges(rocks, () -> {
				table.getOrAdd(1).getMap().put(1L, new BItem());
				return 0L;
			});
			rocks.followerApply(encodeDecode(rocks, put1), new RaftLog(1, 1, put1));
			var itemA = ((BMapBean)table.getLruCache().get(1).getValue()).getMap().get(1L);
			assertNotNull(itemA);

			// txn2：put覆盖为itemB并应用（put不detach：itemA仍managed、mapKey仍是1L）。
			var put2 = captureChanges(rocks, () -> {
				table.getOrAdd(1).getMap().put(1L, new BItem());
				return 0L;
			});
			rocks.followerApply(encodeDecode(rocks, put2), new RaftLog(1, 2, put2));

			// txn3：陈旧引用itemA.setI(9)——必须被collect但不得进changedWithKey
			// （getOrAdd访问记录，模拟真实场景从map取得引用后编辑）。
			var staleChanges = captureChanges(rocks, () -> {
				table.getOrAdd(1);
				itemA.setI(9);
				return 0L;
			});
			var followerStale = encodeDecode(rocks, staleChanges);
			var staleMapLog = findLog(followerStale, LogMap2.class);
			assertNotNull(staleMapLog, "陈旧引用修改必须被collect（否则测试无效）");
			assertFalse(staleMapLog.getChangedWithKey().containsKey(1L),
					"陈旧引用的changed必须被身份校验过滤");
			rocks.followerApply(followerStale, new RaftLog(1, 3, staleChanges));
			assertEquals(0, readStorageItemI(table, 1, 1L), "follower存储的当前值不得被陈旧日志污染");

			// 正控：编辑当前值正常携带、正常应用。
			var editChanges = captureChanges(rocks, () -> {
				table.getOrAdd(1).getMap().get(1L).setI(42);
				return 0L;
			});
			var followerEdit = encodeDecode(rocks, editChanges);
			var editMapLog = findLog(followerEdit, LogMap2.class);
			assertNotNull(editMapLog);
			assertTrue(editMapLog.getChangedWithKey().containsKey(1L), "当前值的changed不得误杀");
			rocks.followerApply(followerEdit, new RaftLog(1, 4, editChanges));
			assertEquals(42, readStorageItemI(table, 1, 1L));
		}
	}

	@Test
	public void testSortedMap2StaleRefEditFiltered() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false)) {
			rocks.registerTableTemplate(sortedTemplateName, Integer.class, BSortedMapBean.class);
			var table = rocks.<Integer, BSortedMapBean>getTableTemplate(sortedTemplateName).openTable(0);
			seedEmptyRecord(table, 1);

			var put1 = captureChanges(rocks, () -> {
				table.getOrAdd(1).getMap().put(1L, new BItem());
				return 0L;
			});
			rocks.followerApply(encodeDecode(rocks, put1), new RaftLog(1, 1, put1));
			var itemA = ((BSortedMapBean)table.getLruCache().get(1).getValue()).getMap().get(1L);
			assertNotNull(itemA);

			var put2 = captureChanges(rocks, () -> {
				table.getOrAdd(1).getMap().put(1L, new BItem());
				return 0L;
			});
			rocks.followerApply(encodeDecode(rocks, put2), new RaftLog(1, 2, put2));

			var staleChanges = captureChanges(rocks, () -> {
				table.getOrAdd(1);
				itemA.setI(9);
				return 0L;
			});
			var followerStale = encodeDecode(rocks, staleChanges);
			var staleMapLog = findLog(followerStale, LogSortedMap2.class);
			assertNotNull(staleMapLog, "陈旧引用修改必须被collect（否则测试无效）");
			assertFalse(staleMapLog.getChangedWithKey().containsKey(1L),
					"陈旧引用的changed必须被身份校验过滤");
			rocks.followerApply(followerStale, new RaftLog(1, 3, staleChanges));
			assertEquals(0, readStorageSortedItemI(table, 1, 1L), "follower存储的当前值不得被陈旧日志污染");

			var editChanges = captureChanges(rocks, () -> {
				table.getOrAdd(1).getMap().get(1L).setI(42);
				return 0L;
			});
			var followerEdit = encodeDecode(rocks, editChanges);
			var editMapLog = findLog(followerEdit, LogSortedMap2.class);
			assertNotNull(editMapLog);
			assertTrue(editMapLog.getChangedWithKey().containsKey(1L), "当前值的changed不得误杀");
			rocks.followerApply(followerEdit, new RaftLog(1, 4, editChanges));
			assertEquals(42, readStorageSortedItemI(table, 1, 1L));
		}
	}
}
