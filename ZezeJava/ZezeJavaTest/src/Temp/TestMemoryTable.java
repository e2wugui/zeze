package Temp;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import Zeze.Transaction.TableX;
import Zeze.Util.OutInt;
import Zeze.Util.TaskSpec;
import demo.App;
import demo.Bean1;
import org.junit.Assert;
import org.junit.Test;

public class TestMemoryTable {
	@Test
	public void test() throws Exception {
		App.Instance.Start();
		App.Instance.demo_Module1.tMemorySize().getTableConf().setCacheCapacity(1);
		App.Instance.demo_Module1.tMemorySize().getTableConf().setCacheFactor(1);

		test(false, false);
		test(false, true);
		test(true, false);
		test(true, true);
	}

	private static void callNewLruHot(TableX<?, ?> table) throws ReflectiveOperationException {
		var tableCache = table.getCache();
		var method = tableCache.getClass().getDeclaredMethod("newLruHot");
		method.setAccessible(true);
		method.invoke(tableCache);
	}

	public static void test(boolean checkpoint1, boolean checkpoint2) throws Exception {
		var ref = new SoftReference<>(new OutInt(1234));

		TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(() -> {
			Assert.assertNull(App.Instance.demo_Module1.tMemorySize().get(123L));
			Assert.assertNull(App.Instance.demo_Module1.tMemorySize().get(456L));
//			System.out.println(App.Instance.demo_Module1.getTable5().get(123L));
			return 0L;
		}, "get0")).call();

		TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.tMemorySize().put(123L, new Bean1(1));
			App.Instance.demo_Module1.tMemorySize().put(456L, new Bean1(2));
//			App.Instance.demo_Module1.getTable5().put(123L, new BValue(1));
			return 0L;
		}, "put")).call();

		Assert.assertEquals(2, App.Instance.demo_Module1.tMemorySize().getCacheSize());

		TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(() -> {
			Assert.assertNotNull(App.Instance.demo_Module1.tMemorySize().get(123L));
			Assert.assertNotNull(App.Instance.demo_Module1.tMemorySize().get(456L));
//			System.out.println(App.Instance.demo_Module1.getTable5().get(123L));
			return 0L;
		}, "get1")).call();

		if (checkpoint1)
			App.Instance.Zeze.checkpointRun();

		TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(() -> {
			Assert.assertNotNull(App.Instance.demo_Module1.tMemorySize().get(123L));
			Assert.assertNotNull(App.Instance.demo_Module1.tMemorySize().get(456L));
//			System.out.println(App.Instance.demo_Module1.getTable5().get(123L));
			return 0L;
		}, "get2")).call();

		callNewLruHot(App.Instance.demo_Module1.tMemorySize());
		App.Instance.demo_Module1.tMemorySize().getCache().cleanNow();
		//Thread.sleep(12000); // wait cache clean

		TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(() -> {
			Assert.assertNotNull(App.Instance.demo_Module1.tMemorySize().get(123L));
			Assert.assertNotNull(App.Instance.demo_Module1.tMemorySize().get(456L));
//			System.out.println(App.Instance.demo_Module1.getTable5().get(123L));
			return 0L;
		}, "get3")).call();

		try {
			//noinspection MismatchedQueryAndUpdateOfCollection
			var objs = new ArrayList<long[]>();
			for (; ; )
				objs.add(new long[2_000_000_000]);
		} catch (OutOfMemoryError err) {
			System.out.println("OutOfMemoryError");
		}

		Assert.assertNull(ref.get());

		TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(() -> {
			Assert.assertNotNull(App.Instance.demo_Module1.tMemorySize().get(123L));
			Assert.assertNotNull(App.Instance.demo_Module1.tMemorySize().get(456L));
//			System.out.println(App.Instance.demo_Module1.getTable5().get(123L));
			return 0L;
		}, "get4")).call();

		TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.tMemorySize().remove(123L);
			App.Instance.demo_Module1.tMemorySize().remove(456L);
//			App.Instance.demo_Module1.getTable5().remove(123L);
			return 0L;
		}, "remove")).call();

		Assert.assertEquals(0, App.Instance.demo_Module1.tMemorySize().getCacheSize());

		TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(() -> {
			Assert.assertNull(App.Instance.demo_Module1.tMemorySize().get(123L));
			Assert.assertNull(App.Instance.demo_Module1.tMemorySize().get(456L));
//			System.out.println(App.Instance.demo_Module1.getTable5().get(123L));
			return 0L;
		}, "get5")).call();

		if (checkpoint2)
			App.Instance.Zeze.checkpointRun();

		TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(() -> {
			Assert.assertNull(App.Instance.demo_Module1.tMemorySize().get(123L));
			Assert.assertNull(App.Instance.demo_Module1.tMemorySize().get(456L));
//			System.out.println(App.Instance.demo_Module1.getTable5().get(123L));
			return 0L;
		}, "get6")).call();

		callNewLruHot(App.Instance.demo_Module1.tMemorySize());
		App.Instance.demo_Module1.tMemorySize().getCache().cleanNow();
		//Thread.sleep(12000); // wait cache clean

		Assert.assertEquals(0, App.Instance.demo_Module1.tMemorySize().getCacheSize());

		TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(() -> {
			Assert.assertNull(App.Instance.demo_Module1.tMemorySize().get(123L));
			Assert.assertNull(App.Instance.demo_Module1.tMemorySize().get(456L));
//			System.out.println(App.Instance.demo_Module1.getTable5().get(123L));
			return 0L;
		}, "get7")).call();
	}
}
