package UnitTest.Zeze.Raft;

import java.io.File;
import java.util.function.BiPredicate;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Raft.RocksRaft.Record;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Raft.RocksRaft.TableTemplate;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.Task;

/**
 * RR2-F2 回归：TableTemplate.openTable(templateId, callback)命中已存在表时callback参数
 * 被静默丢弃（computeIfAbsent不重建已存在值），带callback重载的存在意义被废掉。
 * 修复：命中已存在表时补设回调（最后一次调用生效）。
 */
@Fast
public class TestRocksRaftTableTemplateCallback {
	private static final String raftName = "127.0.0.1:17771";
	private static final String dbHome = "TestRocksRaftTableTemplateCallback.raft";

	private Rocks rocks;

	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17771" DbHome="TestRocksRaftTableTemplateCallback.raft">
					<node Host="127.0.0.1" Port="17771"/>
					<node Host="127.0.0.1" Port="17772"/>
					<node Host="127.0.0.1" Port="17773"/>
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

	@Test
	public void testCallbackNotDroppedOnExistingTable() {
		rocks.registerTableTemplate("TestRocksRaftTableTemplateCallback.t", Integer.class, TestBean.class);
		TableTemplate<Integer, Bean> template =
				rocks.<Integer, Bean>getTableTemplate("TestRocksRaftTableTemplateCallback.t");

		BiPredicate<Integer, Record<Integer>> first = (k, r) -> false;
		BiPredicate<Integer, Record<Integer>> second = (k, r) -> true;

		// 首次打开：新表携带callback
		Table<Integer, ?> t1 = template.openTable(0, first);
		Assertions.assertSame(first, t1.getLruTryRemoveCallback());

		// 再次打开（表已存在）：callback不得被静默丢弃，最后一次调用生效
		Table<Integer, ?> t2 = template.openTable(0, second);
		Assertions.assertSame(t1, t2, "同模板同id必须返回同一表实例");
		Assertions.assertSame(second, t2.getLruTryRemoveCallback(),
				"命中已存在表时必须补设callback（修复前静默丢弃）");
	}

	/** 最小测试bean。 */
	public static final class TestBean extends Bean {
		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
		}

		@Override
		public void encode(ByteBuffer bb) {
		}

		@Override
		public void decode(IByteBuffer bb) {
		}

		@Override
		public Bean copy() {
			return new TestBean();
		}

		@Override
		public void followerApply(Log log) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void leaderApplyNoRecursive(Log log) {
		}
	}
}
