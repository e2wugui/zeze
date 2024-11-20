package UnitTest.Zeze.Trans;

import Zeze.Transaction.*;
import junit.framework.TestCase;
import org.junit.Assert;

public class TestTableKey extends TestCase {
	public final void test1() {
		{
			TableKey tkey1 = new TableKey(1, 1);
			TableKey tkey2 = new TableKey(1, 1);

			int c = tkey1.compareTo(tkey2);
			Assert.assertEquals(0, c);
		}

		{
			TableKey tkey1 = new TableKey(1, 1);
			TableKey tkey2 = new TableKey(2, 1);

			int c = tkey1.compareTo(tkey2);
			Assert.assertEquals(-1, c);
		}

		{
			TableKey tkey1 = new TableKey(1, 1L);
			TableKey tkey2 = new TableKey(1, 1L);

			int c = tkey1.compareTo(tkey2);
			Assert.assertEquals(0, c);
		}

		{
			TableKey tkey1 = new TableKey(1, 1L);
			TableKey tkey2 = new TableKey(1, 2L);

			int c = tkey1.compareTo(tkey2);
			Assert.assertEquals(-1, c);
		}

		{
			TableKey tkey1 = new TableKey(1, false);
			TableKey tkey2 = new TableKey(1, true);

			int c = tkey1.compareTo(tkey2);
			Assert.assertEquals(-1, c);
		}

		{
			TableKey tkey1 = new TableKey(1, 1);
			TableKey tkey2 = new TableKey(1, 2);

			int c = tkey1.compareTo(tkey2);
			Assert.assertEquals(-1, c);
		}

		{
			demo.Module1.Key k1 = new demo.Module1.Key((short)1, "");
			demo.Module1.Key k2 = new demo.Module1.Key((short)1, "");

			TableKey tkey1 = new TableKey(1, k1);
			TableKey tkey2 = new TableKey(1, k2);

			int c = tkey1.compareTo(tkey2);
			Assert.assertEquals(0, c);
		}

		{
			demo.Module1.Key k1 = new demo.Module1.Key((short)1, "");
			demo.Module1.Key k2 = new demo.Module1.Key((short)2, "");

			TableKey tkey1 = new TableKey(1, k1);
			TableKey tkey2 = new TableKey(1, k2);

			int c = tkey1.compareTo(tkey2);
			Assert.assertEquals(-1, c);
		}
	}
}
