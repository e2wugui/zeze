package UnitTest.Zeze.Transaction;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import Zeze.Builtin.AccountOnline.BAccountLink;
import Zeze.Transaction.Collections.CollOne;
import Zeze.Transaction.Collections.LogOne;
import Zeze.Transaction.Database;
import Zeze.Transaction.IGlobalAgent;
import Zeze.Transaction.Record;
import Zeze.Transaction.RecordAccessed;
import Zeze.Transaction.Table;
import Zeze.Transaction.TableKey;

@Fast
public class TestCollOneFollowerApply {
	@Test
	public void testFollowerApplyInitRootInfo() {
		// 纯单元：手工构造managed状态（正常由表记录初始化），不需要应用环境。
		// Record是抽象类，这里只需要它作为RootInfo的载体，桩掉抽象方法即可。
		var record = new Record(null) {
			@Override
			public Table getTable() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Object getObjectKey() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setDirty() {
			}

			@Override
			public IGlobalAgent.AcquireResult acquire(int state, boolean fresh, boolean noWait) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void encode0() {
			}

			@Override
			public void flush(Database.Transaction t, Database.Transaction lct) {
			}

			@Override
			public void commit(RecordAccessed accessed) {
			}

			@Override
			public void cleanup() {
			}
		};

		var parent = new BAccountLink();
		var collOne = new CollOne<>(new BAccountLink(), BAccountLink.class);
		var rootInfo = new Record.RootInfo(record, new TableKey(1, "TestCollOneFollowerApply"));
		collOne.initRootInfo(rootInfo, parent);
		assertTrue(collOne.isManaged());

		// follower应用"整体替换value"的日志：新bean必须完成rootInfo初始化，
		// 与PList2/PMap2/RocksRaft的CollList2/CollMap2全部同类实现一致。
		var newValue = new BAccountLink();
		var log = new LogOne<>(parent, 0, collOne, newValue);
		collOne.followerApply(log);

		assertTrue(newValue.isManaged());
	}
}
