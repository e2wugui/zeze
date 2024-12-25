package UnitTest.Zeze.Trans;

import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.Locks;
import Zeze.Transaction.Record;
import Zeze.Transaction.Record1;
import Zeze.Transaction.TableKey;
import Zeze.Transaction.Transaction;
import demo.Bean1;
import demo.web.tMap2Bean1;
import org.junit.Assert;
import org.junit.Test;

public class TestGTable {
	private final Zeze.Transaction.Locks locks = new Locks();

	@Test
	public void testGTableBegin() {
		Transaction.create(locks);
		try {
			Transaction.getCurrent().begin();
			// process
			// 设置假的rootInfo，不能commit，用来测试。
			var tTable = new tMap2Bean1();
			var record = new Record1(tTable, 1, null);
			var bean = new Bean1();
			var root = new Record.RootInfo(record, new TableKey(1, 1));

			var table = new GTable1<>(Integer.class, Integer.class, Integer.class);
			table.getPMap2().initRootInfo(root, bean); // 设置假的rootInfo，不能commit，用来测试。

			Assert.assertTrue(table.isEmpty());

			table.put(1, 1, 1);
			var value = table.get(1, 1);
			Assert.assertNotNull(value);
			Assert.assertEquals(1, value.intValue());

			Transaction.getCurrent().rollback();
			Assert.assertTrue(table.isEmpty());
		} finally {
			Transaction.destroy();
		}
	}
}

