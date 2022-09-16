package UnitTest.Zeze.Collections;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import UnitTest.Zeze.BMyBean;
import Zeze.Transaction.Procedure;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestLinkedMap {
	@Before
	public final void testInit() throws Throwable {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Throwable {
		demo.App.getInstance().Stop();
	}

	@Test
	public final void test1_LinkedMapPut() throws Throwable {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var map = demo.App.getInstance().LinkedMapModule.open("test1", BMyBean.class);
			for (int i = 100; i < 110; i++) {
				var bean = new BMyBean();
				bean.setI(i);
				map.put(i, bean);
			}
			return Procedure.Success;
		}, "test1_LinkedMapPut").Call();
		Assert.assertEquals(ret, Procedure.Success);
	}

	@Test
	public final void test2_LinkedMapGet() throws Throwable {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var map = demo.App.getInstance().LinkedMapModule.open("test1", BMyBean.class);
			for (int i = 100; i < 110; i++) {
				var bean = map.get(i);
				Assert.assertEquals(bean.getI(), i);
			}
			return Procedure.Success;
		}, "test2_LinkedMapGet").Call();
		Assert.assertEquals(ret, Procedure.Success);
	}

	@Test
	public final void test3_LinkedMapWalk() {
		var map = demo.App.getInstance().LinkedMapModule.open("test1", BMyBean.class);
		var i = new AtomicInteger(0);
		var arr = Arrays.asList(100, 101, 102, 103, 104, 105, 106, 107, 108, 109);
		Collections.reverse(arr);
		map.walk(((key, value) -> {
			Assert.assertTrue(i.get() < 10);
			Assert.assertEquals(value.getI(), (int)arr.get(i.getAndAdd(1)));
			return true;
		}));
		Assert.assertEquals(i.get(), 10);
	}

	@Test
	public final void test4_LinkedMapRemove() throws Throwable {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var map = demo.App.getInstance().LinkedMapModule.open("test1", BMyBean.class);
			for (int i = 100; i < 110; i++) {
				var bean = map.remove(i);
				Assert.assertEquals(bean.getI(), i);
			}
			Assert.assertTrue(map.isEmpty());
			return Procedure.Success;
		}, "test2_LinkedMapRemove").Call();
		Assert.assertEquals(ret, Procedure.Success);
	}
}
