package UnitTest.Zeze.Collections;

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
public class TestQueue {

	@Before
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	@Test
	public final void test1_QueueAdd() throws Exception {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var queueModule = demo.App.getInstance().Zeze.getQueueModule();
			var queue = queueModule.open("test1", BMyBean.class);
			for (int i = 0; i < 10; i++) {
				var bean = new BMyBean();
				bean.setI(i);
				queue.add(bean);
			}
			var bean = queue.peek();
			Assert.assertEquals(bean.getI(), 0);
			return Procedure.Success;
		}, "test1_QueueAdd").call();
		Assert.assertEquals(ret, Procedure.Success);
	}

	@Test
	public final void test2_QueueWalk() throws Exception {
		var queueModule = demo.App.getInstance().Zeze.getQueueModule();
		var queue = queueModule.open("test1", BMyBean.class);
		var i = new AtomicInteger(0);
		int[] arr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
		queue.walk(((key, value) -> {
			Assert.assertTrue(i.get() < 10);
			Assert.assertEquals(value.getI(), arr[i.getAndAdd(1)]);
			return true;
		}));
		Assert.assertEquals(i.get(), 10);
	}

	@Test
	public final void test3_QueuePop() throws Exception {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var queueModule = demo.App.getInstance().Zeze.getQueueModule();
			var queue = queueModule.open("test1", BMyBean.class);
			for (int i = 0; i < 10; i++) {
				var bean = queue.pop();
				Assert.assertEquals(bean.getI(), i);
			}
			Assert.assertTrue(queue.isEmpty());
			return Procedure.Success;
		}, "test2_QueuePop").call();
		Assert.assertEquals(ret, Procedure.Success);
	}

	@Test
	public final void test4_QueuePush() throws Exception {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var queueModule = demo.App.getInstance().Zeze.getQueueModule();
			var queue = queueModule.open("test1", BMyBean.class);
			for (int i = 0; i < 10; i++) {
				var bean = new BMyBean();
				bean.setI(i);
				queue.push(bean);
			}
			var bean = queue.peek();
			Assert.assertEquals(bean.getI(), 9);
			return Procedure.Success;
		}, "test3_QueuePush").call();
		Assert.assertEquals(ret, Procedure.Success);
	}

	@Test
	public final void test5_QueueWalk() throws Exception {
		var queueModule = demo.App.getInstance().Zeze.getQueueModule();
		var queue = queueModule.open("test1", BMyBean.class);
		var i = new AtomicInteger(0);
		int[] arr = {9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
		queue.walk(((key, value) -> {
			Assert.assertTrue(i.get() < 10);
			Assert.assertEquals(value.getI(), arr[i.getAndAdd(1)]);
			return true;
		}));
		Assert.assertEquals(i.get(), 10);
	}

	@Test
	public final void test6_QueuePop() throws Exception {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var queueModule = demo.App.getInstance().Zeze.getQueueModule();
			var queue = queueModule.open("test1", BMyBean.class);
			for (int i = 9; i >= 0; i--) {
				var bean = queue.pop();
				Assert.assertEquals(bean.getI(), i);
			}
			Assert.assertTrue(queue.isEmpty());
			return Procedure.Success;
		}, "test4_QueuePop").call();
		Assert.assertEquals(ret, Procedure.Success);
	}
}
