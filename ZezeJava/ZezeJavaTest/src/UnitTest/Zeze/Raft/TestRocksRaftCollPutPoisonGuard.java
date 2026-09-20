package UnitTest.Zeze.Raft;

import java.io.File;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.CollList2;
import Zeze.Raft.RocksRaft.CollMap2;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Raft.RocksRaft.Record;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.HasManagedException;
import Zeze.Util.Task;

/**
 * RR1-F1/RR1-F2 回归：RocksRaft容器托管分支的"先改写bean状态、后验界/后挂接"顺序缺陷。
 * 普通字段写（mapKey/initRootInfo设置的归属）不受事务回滚保护，异常路径毒化的bean
 * 被调用方复用时携带脏归属——复用抛HasManagedException，后续编辑被encode期静默剔除。
 * <p>
 * RR1-F1：CollMap2/CollSortedMap2.put在managed分支必须initRootInfo成功后再mapKey
 * （对齐经典PMap2）；HasManagedException路径不得改写既有mapKey。
 * RR1-F2：CollList2.set/add(int,V)必须在initRootInfo挂接前验界（对齐经典PList2 FND7-06）；
 * 越界IOOBE不得留下携带脏归属的bean。
 * 不start server：断言在事务内完成且返回非0（不触发appendLog），无网络与选举。
 */
@Fast
public class TestRocksRaftCollPutPoisonGuard {
	private static final String raftName = "127.0.0.1:17751";
	private static final String dbHome = "TestRocksRaftCollPoison.raft";

	private Rocks rocks;

	/** RR1-F1的value bean：mapKey可观测。 */
	public static final class BLeaf extends Bean {
		public int i;
		private transient Object mapKey_;

		@Override
		public Object mapKey() {
			return mapKey_;
		}

		@Override
		public void mapKey(Object mapKey) {
			mapKey_ = mapKey;
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(i);
		}

		@Override
		public void decode(IByteBuffer bb) {
			i = bb.ReadInt();
		}

		@Override
		public Bean copy() {
			var c = new BLeaf();
			c.i = i;
			return c;
		}

		@Override
		public void followerApply(Log log) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void leaderApplyNoRecursive(Log log) {
		}
	}

	/** RR1-F1的holder bean：CollMap2&lt;Integer,BLeaf&gt;（variableId=1）。 */
	public static final class BMapHolder extends Bean {
		public final CollMap2<Integer, BLeaf> map2;

		public BMapHolder() {
			map2 = new CollMap2<>(Integer.class, BLeaf.class);
			map2.variableId(1);
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
			map2.initRootInfo(root, this);
		}

		@Override
		public void encode(ByteBuffer bb) {
			map2.encode(bb);
		}

		@Override
		public void decode(IByteBuffer bb) {
			map2.decode(bb);
		}

		@Override
		public Bean copy() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void followerApply(Log log) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void leaderApplyNoRecursive(Log log) {
		}
	}

	/** RR1-F2的holder bean：CollList2&lt;BLeaf&gt;（variableId=1）。 */
	public static final class BListHolder extends Bean {
		public final CollList2<BLeaf> list2;

		public BListHolder() {
			list2 = new CollList2<>(BLeaf.class);
			list2.variableId(1);
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
			list2.initRootInfo(root, this);
		}

		@Override
		public void encode(ByteBuffer bb) {
			list2.encode(bb);
		}

		@Override
		public void decode(IByteBuffer bb) {
			list2.decode(bb);
		}

		@Override
		public Bean copy() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void followerApply(Log log) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void leaderApplyNoRecursive(Log log) {
		}
	}

	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17751" DbHome="TestRocksRaftCollPoison.raft">
					<node Host="127.0.0.1" Port="17751"/>
					<node Host="127.0.0.1" Port="17752"/>
					<node Host="127.0.0.1" Port="17753"/>
				</raft>
				""");
	}

	@BeforeEach
	public void setUp() throws Exception {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
		rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false);
	}

	@AfterEach
	public void tearDown() throws Exception {
		rocks.close();
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	/**
	 * RR1-F1：向第二个map放入已托管bean时initRootInfo抛HasManagedException，
	 * 异常路径不得毒化bean的mapKey（原值77保持，修复前被改写为99）。
	 */
	@Test
	public void testMapPutHasManagedKeepsMapKey() throws Exception {
		rocks.registerTableTemplate("TestRocksRaftCollPoison.map", Integer.class, BMapHolder.class);
		Table<Integer, BMapHolder> table = rocks.<Integer, BMapHolder>getTableTemplate(
				"TestRocksRaftCollPoison.map").openTable(0);

		var rc = rocks.newProcedure(() -> {
			var h1 = new BMapHolder();
			table.put(1, h1); // h1（含map2）托管
			var leaf = new BLeaf();
			h1.map2.put(77, leaf); // leaf托管，mapKey=77

			var h2 = new BMapHolder();
			table.put(2, h2);
			Assertions.assertThrows(HasManagedException.class, () -> h2.map2.put(99, leaf),
					"已托管bean放入另一map必须抛HasManagedException");
			Assertions.assertEquals(77, leaf.mapKey(),
					"HasManagedException路径不得毒化mapKey（修复前被改写为99，后续编辑被encode静默剔除）");
			return -1L;
		}).call();
		Assertions.assertEquals(-1L, rc);
	}

	/**
	 * RR1-F2：set越界IOOBE必须在挂接bean之前抛出——异常后bean不得携带脏归属
	 * （修复前可继续放入其他容器；被毒化的bean复用抛HasManagedException）。
	 */
	@Test
	public void testListSetOutOfBoundsLeavesBeanClean() throws Exception {
		rocks.registerTableTemplate("TestRocksRaftCollPoison.list", Integer.class, BListHolder.class);
		Table<Integer, BListHolder> table = rocks.<Integer, BListHolder>getTableTemplate(
				"TestRocksRaftCollPoison.list").openTable(0);

		var rc = rocks.newProcedure(() -> {
			var h1 = new BListHolder();
			table.put(1, h1); // h1（含list2）托管
			h1.list2.add(new BLeaf()); // size=1

			var leaf = new BLeaf();
			Assertions.assertThrows(IndexOutOfBoundsException.class, () -> h1.list2.set(5, leaf),
					"越界set必须抛IOOBE");
			Assertions.assertFalse(leaf.isManaged(),
					"越界IOOBE不得留下携带脏归属的bean（initRootInfo必须在验界之后）");

			// 干净bean可继续正常使用：放入另一容器的托管list不抛HasManagedException
			var h2 = new BListHolder();
			table.put(2, h2);
			h2.list2.add(leaf);
			Assertions.assertTrue(leaf.isManaged(), "正常挂接后bean应托管");
			return -1L;
		}).call();
		Assertions.assertEquals(-1L, rc);
	}

	/**
	 * RR1-F2：add(int,V)越界（合法域0&lt;=index&lt;=size）同set，先验界后挂接。
	 */
	@Test
	public void testListAddIndexOutOfBoundsLeavesBeanClean() throws Exception {
		rocks.registerTableTemplate("TestRocksRaftCollPoison.list", Integer.class, BListHolder.class);
		Table<Integer, BListHolder> table = rocks.<Integer, BListHolder>getTableTemplate(
				"TestRocksRaftCollPoison.list").openTable(0);

		var rc = rocks.newProcedure(() -> {
			var h1 = new BListHolder();
			table.put(1, h1);
			h1.list2.add(new BLeaf()); // size=1

			var leaf = new BLeaf();
			Assertions.assertThrows(IndexOutOfBoundsException.class, () -> h1.list2.add(3, leaf),
					"index>size的add必须抛IOOBE");
			Assertions.assertFalse(leaf.isManaged(), "越界add不得挂接bean");

			// 边界值index==size合法（尾部追加），行为不变
			h1.list2.add(1, leaf);
			Assertions.assertEquals(2, h1.list2.size());
			return -1L;
		}).call();
		Assertions.assertEquals(-1L, rc);
	}
}
