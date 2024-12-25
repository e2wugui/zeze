package UnitTest.Zeze.Trans;

import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.Locks;
import Zeze.Transaction.Transaction;
import demo.Bean1;
import org.junit.Assert;
import org.junit.Test;

public class TestGTable {
	private final Zeze.Transaction.Locks locks = new Locks();

	@Test
	public void testGTableBegin() {
		Transaction.create(locks);
		try {
			/*
			Transaction.getCurrent().begin();
			// process
			var table = new GTable1<>(Integer.class);
			Assert.assertTrue(table.isEmpty());

			table.put(1, 1, 1);
			var value = table.get(1, 1);
			Assert.assertNotNull(value);
			Assert.assertEquals(1, value.intValue());

			Transaction.getCurrent().rollback();
			// TODO table 必须managed才会开启事务模式，所以这里暂时通不过。需要gen实现了才能测试了。
			//Assert.assertTrue(table.isEmpty());
			*/
		} finally {
			Transaction.destroy();
		}
	}
}

