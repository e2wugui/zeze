package UnitTest.Zeze.Serialize;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.RawBean;
import Zeze.Transaction.TableCache;
import Zeze.Transaction.TableX;
import demo.App;
import demo.Module1.BValue;
import junit.framework.TestCase;

public class TestRawBean extends TestCase {
	public void testBasic() {
		var b1 = new BValue();
		b1.setInt_1(123);
		b1.setString3("abc");
		var bin = new Binary(b1);

		var rb1 = new RawBean(b1.typeId(), bin);
		System.out.println(rb1);
		var bb = ByteBuffer.encode(rb1);

		var rb2 = new RawBean(rb1.typeId());
		rb2.decode(bb);
		assertEquals(rb1, rb2);

		var rb3 = new RawBean(rb1.typeId());
		rb3.decode(ByteBuffer.Wrap(bin));
		assertEquals(rb1, rb3);

		var b2 = new BValue();
		b2.decode(ByteBuffer.Wrap(rb2.getRawData()));
		assertEquals(b1, b2);
	}

	/*
	// clean all cache, force reload next time
	private static void forceCleanCache(TableX<?, ?> table) throws Exception {
		int oldCacheCapacity = table.getTableConf().getCacheCapacity();
		try {
			table.getTableConf().setCacheCapacity(0);
			var tableCache = table.getCache();
			var m = TableCache.class.getDeclaredMethod("newLruHot");
			m.setAccessible(true);
			m.invoke(tableCache);
			tableCache.cleanNow();
		} finally {
			table.getTableConf().setCacheCapacity(oldCacheCapacity);
		}
	}

	public final void testTransaction() throws Exception {
		demo.App.Instance.Start();

		var b1 = new BValue();
		b1.setInt_1(123);
		b1.setString3("abc");
		var bin = new Binary(b1);

		App.Instance.Zeze.newProcedure(() -> {
			var v = App.Instance.demo_Module1.getTable1().getOrAdd(999L);
			var rb = new RawBean(b1.typeId());
			rb.setRawDataUnsafe(bin);
			v.getDynamic23().setBean(rb);
			return 0;
		}, "testRawBean1").call();

		forceCleanCache(App.Instance.demo_Module1.getTable1());

		App.Instance.Zeze.newProcedure(() -> {
			var v = App.Instance.demo_Module1.getTable1().getOrAdd(999L);
			var b = v.getDynamic23().getBean();
			assertEquals(BValue.class, b.getClass());
			assertEquals(b1.typeId(), b.typeId());
			assertEquals(b1, b);
			return 0;
		}, "testRawBean2").call();

		forceCleanCache(App.Instance.demo_Module1.getTable1());

		App.Instance.Zeze.newProcedure(() -> {
			var v = App.Instance.demo_Module1.getTable1().getOrAdd(999L);
			var rb = new RawBean(12345);
			rb.setRawDataUnsafe(bin);
			v.getDynamic23().setBean(rb);
			return 0;
		}, "testRawBean3").call();

		forceCleanCache(App.Instance.demo_Module1.getTable1());

		App.Instance.Zeze.newProcedure(() -> {
			var v = App.Instance.demo_Module1.getTable1().getOrAdd(999L);
			var b = v.getDynamic23().getBean();
			assertEquals(RawBean.class, b.getClass());
			assertEquals(12345, b.typeId());
			assertEquals(bin, ((RawBean)b).getRawData());
			b.reset();
			return 0;
		}, "testRawBean4").call();

		forceCleanCache(App.Instance.demo_Module1.getTable1());

		App.Instance.Zeze.newProcedure(() -> {
			var v = App.Instance.demo_Module1.getTable1().getOrAdd(999L);
			var b = v.getDynamic23().getBean();
			assertEquals(RawBean.class, b.getClass());
			assertEquals(12345, b.typeId());
			assertEquals(new Binary(new byte[]{0}), ((RawBean)b).getRawData());
			return 0;
		}, "testRawBean5").call();
	}
	*/
}
