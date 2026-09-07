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
 * LogMap2.encode 必须过滤"编辑时已不在最终map"的changed条目（第三条过滤，
 * 对齐 Transaction.Collections.LogMap2.buildChangedWithKey）。
 * remove()不detach被删bean，应用编辑已删bean的陈旧引用仍会经parent链collect进
 * LogMap2.changed，其key来自更早事务（本事务putted/removed都拦不住）；不过滤则
 * 编码出的日志在follower侧changed必取到null——CollMap2.followerApply该分支为
 * fatal+fatalKill，陈旧引用误用会直接杀节点。
 * 完整序列：put安装 -> remove删除 -> 编辑陈旧引用 -> encode：
 * 1. 陈旧编辑确实被collect（changed非空，证明场景真实存在）；
 * 2. 修复后changedWithKey不携带已消失key（修复前编码进日志）；
 * 3. 该日志在follower上followerApply安全（修复前走到fatal分支halt整个JVM）。
 */
@Fast
public class TestMap2EncodeStaleChangedKey {
	private static final String raftName = "127.0.0.1:17770";
	private static final String dbHome = "TestMap2EncodeStaleChangedKey.raft";
	private static final String templateName = "tMap2StaleKey";

	static {
		// follower 路径 Changes.decode -> LogBean.decode -> Log.create(typeId) 需要 factory，
		// typeId 与 CollMap2 构造时计算的一致（参照 RocksRaft/Test.java 的配对注册）。
		Rocks.registerLog(() -> new LogMap2<>(Long.class, BStaleItem.class));
	}

	/** map2 动态 value bean：mapKey 由容器维护（参照 TestMap2FollowerApplyMapKey.BItem）。 */
	public static final class BStaleItem extends Bean {
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

	/** 记录值 bean：含 CollMap2<Long, BStaleItem>。 */
	public static final class BStaleMapBean extends Bean {
		private final CollMap2<Long, BStaleItem> _map;

		public BStaleMapBean() {
			_map = new CollMap2<>(Long.class, BStaleItem.class);
			_map.variableId(1);
		}

		public CollMap2<Long, BStaleItem> getMap() {
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
				<raft Name="127.0.0.1:17770" DbHome="TestMap2EncodeStaleChangedKey.raft">
					<node Host="127.0.0.1" Port="17770"/>
					<node Host="127.0.0.1" Port="17771"/>
					<node Host="127.0.0.1" Port="17772"/>
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
		rocks.registerTableTemplate(templateName, Integer.class, BStaleMapBean.class);
		return rocks;
	}

	// 在过程中执行修改并捕获收集到的 Changes：appendLog 因非leader抛 RaftRetry，
	// 过程按失败返回；Changes 已在 _final_commit_ 里收集完成（保存在事务对象上）。
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

	// 直写存储层做初始数据（绕过事务，让后续修改走 Edit 状态携带 LogMap2.putted）。
	private static void seedEmptyRecord(Table<Integer, BStaleMapBean> table, int key) throws Exception {
		var seed = table.newValue();
		var keyBB = ByteBuffer.Allocate();
		table.encodeKey(keyBB, key);
		var valBB = ByteBuffer.Allocate();
		seed.encode(valBB);
		table.getRocksTable().put(keyBB.CopyIf(), valBB.CopyIf());
	}

	@SuppressWarnings("unchecked")
	private static LogMap2<Long, BStaleItem> findLogMap2(Changes changes) {
		for (var r : changes.getRecords().values())
			for (var logBean : r.getLogBean()) {
				var vars = logBean.getVariables();
				if (vars == null)
					continue;
				for (var it = vars.iterator(); it.moveToNext(); )
					if (it.value().getVariableId() == 1)
						return (LogMap2<Long, BStaleItem>)it.value();
			}
		return null;
	}

	@Test
	public void testStaleBeanEditFilteredAtEncode() throws Exception {
		try (var rocks = newRocks()) {
			var table = rocks.<Integer, BStaleMapBean>getTableTemplate(templateName).openTable(0);
			seedEmptyRecord(table, 1);

			// 1. put安装条目（followerApply路径），持有bean引用。
			var putChanges = captureChanges(rocks, () -> {
				table.getOrAdd(1).getMap().put(1L, new BStaleItem());
				return 0L;
			});
			var putBB = ByteBuffer.Allocate();
			putChanges.encode(putBB);
			var followerPut = new Changes(rocks);
			followerPut.decode(ByteBuffer.Wrap(putBB.CopyIf()));
			rocks.followerApply(followerPut, new RaftLog(1, 1, followerPut));
			var staleItem = ((BStaleMapBean)table.getLruCache().get(1).getValue()).getMap().get(1L);
			assertNotNull(staleItem);

			// 2. 事务删除该条目并应用到follower：map里已无key 1L，但staleItem的
			//    parent/rootInfo/mapKey仍在（remove不detach）。
			var removeChanges = captureChanges(rocks, () -> {
				table.getOrAdd(1).getMap().remove(1L);
				return 0L;
			});
			var removeBB = ByteBuffer.Allocate();
			removeChanges.encode(removeBB);
			var followerRemove = new Changes(rocks);
			followerRemove.decode(ByteBuffer.Wrap(removeBB.CopyIf()));
			rocks.followerApply(followerRemove, new RaftLog(1, 2, followerRemove));
			assertTrue(((BStaleMapBean)table.getLruCache().get(1).getValue()).getMap().get(1L) == null);

			// 3.【陈旧引用编辑】事务里编辑已删bean：经parent链collect进LogMap2.changed
			//    （changed非空证明场景真实），但encode时key 1L已不在最终map。
			var staleChanges = captureChanges(rocks, () -> {
				table.getOrAdd(1); // 访问记录，对齐真实事务形态
				staleItem.setI(42);
				return 0L;
			});
			var capturedLog = findLogMap2(staleChanges);
			assertNotNull(capturedLog);
			assertTrue(capturedLog.getChanged().size() > 0, "stale edit must be collected"); // 场景成立的前提

			// 4. encode->decode：changedWithKey不得携带已消失的key（修复前containsKey(1L)）。
			var staleBB = ByteBuffer.Allocate();
			staleChanges.encode(staleBB);
			var followerStale = new Changes(rocks);
			followerStale.decode(ByteBuffer.Wrap(staleBB.CopyIf()));
			var decodedLog = findLogMap2(followerStale);
			assertNotNull(decodedLog);
			assertFalse(decodedLog.getChangedWithKey().containsKey(1L));

			// 5. 该日志在follower上应用必须安全：不得走到CollMap2.followerApply的
			//    null分支（fatalKill会halt整个JVM）。
			rocks.followerApply(followerStale, new RaftLog(1, 3, followerStale));
		}
	}
}
