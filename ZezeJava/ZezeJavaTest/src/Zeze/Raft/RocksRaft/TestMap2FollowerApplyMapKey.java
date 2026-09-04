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
 * CollMap2.followerApply 安装 putted bean 必须补设 mapKey（FND2-R2-1）。
 * leader 侧 put/putAll 与 decode 装载路径都设置 mapKey，唯独 followerApply 漏设：
 * follower 期安装的条目在本节点 failover 当选新 leader 后被编辑时，
 * LogMap2.encode 用 bean.mapKey() 作为 changed 条目的 key——null 会让 Long key
 * 拆箱 NPE（该记录的写事务在新 leader 上永久失败），String key 则被
 * WriteString(null) 编码成 ""（follower 静默丢编辑，状态机分歧）。
 * 这里直接构造 Rocks（不 start server，无网络与选举流量；3节点仅是Raft构造的
 * 配置要求），用"leader collect+encode -> follower decode+followerApply 安装 ->
 * failover（同进程模拟，条目仍在 lruCache）-> 新 leader 编辑+encode"的完整
 * 序列锁定行为：
 * 1. follower 安装后的 bucket 的 mapKey 必须等于 map key（修复前为 null）；
 * 2. 新 leader 编辑该条目后 Changes.encode 必须成功（修复前 Long key 拆箱 NPE）；
 * 3. 编码出的日志经 decode 后 changedWithKey 携带正确的 key，followerApply
 *    能命中条目应用修改并落盘（修复前 key 丢失，编辑被静默丢弃）。
 */
@Fast
public class TestMap2FollowerApplyMapKey {
	private static final String raftName = "127.0.0.1:17660";
	private static final String dbHome = "TestMap2FollowerApplyMapKey.raft";
	private static final String templateName = "tMap2MapKey";

	static {
		// follower 路径 Changes.decode -> LogBean.decode -> Log.create(typeId) 需要 factory，
		// typeId 与 CollMap2 构造时计算的一致（参照 RocksRaft/Test.java 的配对注册）。
		Rocks.registerLog(() -> new LogMap2<>(Long.class, BItem.class));
	}

	/** map2 动态 value bean：mapKey 由容器维护（参照 RocksRaft/Test.java 的 Bean1）。 */
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

	/** 记录值 bean：含 CollMap2&lt;Long, BItem&gt;（map2 表，Long key 触发 NPE 分支）。 */
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

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17660" DbHome="TestMap2FollowerApplyMapKey.raft">
					<node Host="127.0.0.1" Port="17660"/>
					<node Host="127.0.0.1" Port="17661"/>
					<node Host="127.0.0.1" Port="17662"/>
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
		rocks.registerTableTemplate(templateName, Integer.class, BMapBean.class);
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
	private static void seedEmptyRecord(Table<Integer, BMapBean> table, int key) throws Exception {
		var seed = table.newValue();
		var keyBB = ByteBuffer.Allocate();
		table.encodeKey(keyBB, key);
		var valBB = ByteBuffer.Allocate();
		seed.encode(valBB);
		table.getRocksTable().put(keyBB.CopyIf(), valBB.CopyIf());
	}

	// 读存储层的最终值（不经缓存，验证flush真的落盘）。
	private static int readStorageItemI(Table<Integer, BMapBean> table, int key, long itemKey) throws Exception {
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

	@SuppressWarnings("unchecked")
	private static LogMap2<Long, BItem> findLogMap2(Changes changes) {
		for (var r : changes.getRecords().values())
			for (var logBean : r.getLogBean()) {
				var vars = logBean.getVariables();
				if (vars == null)
					continue;
				for (var it = vars.iterator(); it.moveToNext(); )
					if (it.value().getVariableId() == 1)
						return (LogMap2<Long, BItem>)it.value();
			}
		return null;
	}

	@Test
	public void testFailoverEditFollowerInstalledEntry() throws Exception {
		try (var rocks = newRocks()) {
			var table = rocks.<Integer, BMapBean>getTableTemplate(templateName).openTable(0);
			seedEmptyRecord(table, 1);

			// 1.【旧leader】事务 put map 条目（put() 在 leader 侧设置 bucket 的 mapKey），
			//   记录已存在 -> Changes.Record=Edit，日志携带 LogMap2.putted。
			var putChanges = captureChanges(rocks, () -> {
				table.getOrAdd(1).getMap().put(1L, new BItem());
				return 0L;
			});
			var putBB = ByteBuffer.Allocate();
			putChanges.encode(putBB);

			// 2.【follower】从编码字节解码出全新 Changes（putted 的 bucket 由
			//   valueFactory 新建、无人设置 mapKey——正是 follower 收到复制日志的
			//   状态），followerApply 安装进内存 map 并落盘。
			var followerPut = new Changes(rocks);
			followerPut.decode(ByteBuffer.Wrap(putBB.CopyIf()));
			rocks.followerApply(followerPut, new RaftLog(1, 1, followerPut));

			// 安装后的 bucket 必须携带 mapKey（修复前：null——failover 后编辑即 NPE/丢key）。
			var installed = ((BMapBean)table.getLruCache().get(1).getValue()).getMap().get(1L);
			assertNotNull(installed);
			assertEquals(1L, installed.mapKey());

			// 3.【failover：原 follower 当选新 leader，条目仍在 lruCache】编辑该条目
			//   并编码日志：LogMap2.encode 用 bucket.mapKey() 作为 changed 条目的 key。
			var editChanges = captureChanges(rocks, () -> {
				table.getOrAdd(1).getMap().get(1L).setI(42);
				return 0L;
			});
			var editBB = ByteBuffer.Allocate();
			editChanges.encode(editBB); // 修复前：Long key 拆箱 NPE，该记录的写事务永久失败

			// 4. 新 leader 的编辑复制到 follower：changedWithKey 必须携带正确的 key，
			//    followerApply 能命中条目应用修改并落盘。
			var followerEdit = new Changes(rocks);
			followerEdit.decode(ByteBuffer.Wrap(editBB.CopyIf()));
			var mapLog = findLogMap2(followerEdit);
			assertNotNull(mapLog);
			assertFalse(mapLog.getChangedWithKey().containsKey(null));
			assertTrue(mapLog.getChangedWithKey().containsKey(1L));
			rocks.followerApply(followerEdit, new RaftLog(1, 2, followerEdit));
			assertEquals(42, readStorageItemI(table, 1, 1L)); // 修复前：编辑静默丢失，读到 0
		}
	}
}
