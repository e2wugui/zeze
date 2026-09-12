package UnitTest.Zeze.Raft;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * FND4-24：嵌套存储过程put/remove回滚后，记录的put意图必须随savepoint丢弃。
 * 粘性putLog字段的旧实现回滚不清除：外层get读到已回滚的值、getOrAdd用空bean覆盖原记录、
 * 提交收集(Changes.Record.collect)把被回滚的put当作最终意图提交。
 * 修复后put意图唯一事实源是savepoint日志栈：嵌套回滚丢弃日志，读取自动回落。
 * 这里不start server：全部断言在事务内完成且外层返回非0（不触发appendLog），无网络与选举。
 */
@Fast
public class TestNestRollbackPutLog {
	private static final String raftName = "127.0.0.1:17731";
	private static final String dbHome = "TestNestRollbackPutLog.raft";

	private Rocks rocks;

	public static final class BValue extends Bean {
		public int i;

		@Override
		protected void initChildrenRootInfo(Zeze.Raft.RocksRaft.Record.RootInfo root) {
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
			var c = new BValue();
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

	@BeforeEach
	public void before() throws Exception {
		Zeze.Raft.LogSequence.deletedDirectoryAndCheck(new java.io.File(dbHome), 100);
		var raftConfig = RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17731" DbHome="TestNestRollbackPutLog.raft">
					<node Host="127.0.0.1" Port="17731"/>
					<node Host="127.0.0.1" Port="17732"/>
					<node Host="127.0.0.1" Port="17733"/>
				</raft>
				""");
		Zeze.Util.Task.tryInitThreadPool();
		rocks = new Rocks(raftName, RocksMode.Pessimism, raftConfig, new Config(), false);
	}

	@AfterEach
	public void after() throws Exception {
		rocks.close();
		Zeze.Raft.LogSequence.deleteDirectory(new java.io.File(dbHome)); // best-effort
	}

	private Table<Integer, BValue> table() {
		return rocks.<Integer, BValue>getTableTemplate("TestNestRollbackPutLog.table").openTable(0);
	}

	/** 嵌套put回滚后，外层get必须返回null（记录不存在），不得读到已回滚的值。 */
	@Test
	public void testNestPutRollbackThenGet() throws Exception {
		rocks.registerTableTemplate("TestNestRollbackPutLog.table", Integer.class, BValue.class);
		var table = table();

		var rc = rocks.newProcedure(() -> {
			rocks.newProcedure(() -> {
				var v = new BValue();
				v.i = 2;
				table.put(1, v);
				return -1L; // 嵌套失败回滚：put意图应随savepoint丢弃
			}).call();

			assertNull(table.get(1), "nested put rolled back: outer get must see record absent");
			return -1L; // 外层也回滚：不触发appendLog
		}).call();
		assertEquals(-1L, rc);
	}

	/** 外层put后嵌套put覆盖再回滚，外层get必须仍看到外层的值。 */
	@Test
	public void testNestOverwriteRollbackKeepsOuterPut() throws Exception {
		rocks.registerTableTemplate("TestNestRollbackPutLog.table", Integer.class, BValue.class);
		var table = table();

		var rc = rocks.newProcedure(() -> {
			var outer = new BValue();
			outer.i = 1;
			table.put(1, outer);

			rocks.newProcedure(() -> {
				var nested = new BValue();
				nested.i = 2;
				table.put(1, nested);
				return -1L; // 回滚覆盖
			}).call();

			assertEquals(1, table.get(1).i, "nested overwrite rolled back: outer put must survive");
			return -1L;
		}).call();
		assertEquals(-1L, rc);
	}

	/** 嵌套remove回滚后，外层getOrAdd必须读到原值，不得用空bean覆盖。 */
	@Test
	public void testNestRemoveRollbackKeepsOrigin() throws Exception {
		rocks.registerTableTemplate("TestNestRollbackPutLog.table", Integer.class, BValue.class);
		var table = table();

		var rc = rocks.newProcedure(() -> {
			var origin = new BValue();
			origin.i = 7;
			table.put(1, origin);

			rocks.newProcedure(() -> {
				table.remove(1);
				return -1L; // 回滚删除
			}).call();

			assertEquals(7, table.getOrAdd(1).i, "nested remove rolled back: origin value must survive");
			return -1L;
		}).call();
		assertEquals(-1L, rc);
	}
}
