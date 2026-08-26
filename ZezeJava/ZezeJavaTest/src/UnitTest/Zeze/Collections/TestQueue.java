package UnitTest.Zeze.Collections;

import java.util.concurrent.atomic.AtomicInteger;
import UnitTest.Zeze.BMyBean;
import Zeze.Transaction.Procedure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestQueue {
	@BeforeEach
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@AfterEach
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	@Test
	public final void test1_QueueAdd() {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var queueModule = demo.App.getInstance().Zeze.getQueueModule();
			var queue = queueModule.open("test1", BMyBean.class);
			var queueSize = queue.size();
			for (int i = 0; i < 10; i++) {
				var bean = new BMyBean();
				bean.setI(i);
				queue.add(bean);
			}
			Assertions.assertEquals(10, queue.size() - queueSize);
			var bean = queue.peek();
			Assertions.assertEquals(0, bean.getI());
			return Procedure.Success;
		}, "test1_QueueAdd").call();
		Assertions.assertEquals(Procedure.Success, ret);
	}

	@Test
	public final void test2_QueueWalk() throws Exception {
		var queueModule = demo.App.getInstance().Zeze.getQueueModule();
		var queue = queueModule.open("test1", BMyBean.class);
		var i = new AtomicInteger(0);
		int[] arr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
		queue.walk(((key, value) -> {
			Assertions.assertTrue(i.get() < 10);
			Assertions.assertEquals(value.getI(), arr[i.getAndAdd(1)]);
			return true;
		}));
		Assertions.assertEquals(10, i.get());
	}

	@Test
	public final void test3_QueuePop() {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var queueModule = demo.App.getInstance().Zeze.getQueueModule();
			var queue = queueModule.open("test1", BMyBean.class);
			var queueSize =  queue.size();
			for (int i = 0; i < 10; i++) {
				var bean = queue.pop();
				Assertions.assertEquals(bean.getI(), i);
			}
			Assertions.assertEquals(queueSize - 10, queue.size());
			Assertions.assertTrue(queue.isEmpty());
			return Procedure.Success;
		}, "test2_QueuePop").call();
		Assertions.assertEquals(Procedure.Success, ret);
	}

	@Test
	public final void test4_QueuePush() {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var queueModule = demo.App.getInstance().Zeze.getQueueModule();
			var queue = queueModule.open("test1", BMyBean.class);
			var queueSize =  queue.size();
			for (int i = 0; i < 10; i++) {
				var bean = new BMyBean();
				bean.setI(i);
				queue.push(bean);
			}
			Assertions.assertEquals(10, queue.size() - queueSize);
			var bean = queue.peek();
			Assertions.assertEquals(9, bean.getI());
			return Procedure.Success;
		}, "test3_QueuePush").call();
		Assertions.assertEquals(Procedure.Success, ret);
	}

	@Test
	public final void test5_QueueWalk() throws Exception {
		var queueModule = demo.App.getInstance().Zeze.getQueueModule();
		var queue = queueModule.open("test1", BMyBean.class);
		var i = new AtomicInteger(0);
		int[] arr = {9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
		queue.walk(((key, value) -> {
			Assertions.assertTrue(i.get() < 10);
			Assertions.assertEquals(value.getI(), arr[i.getAndAdd(1)]);
			return true;
		}));
		Assertions.assertEquals(10, i.get());
	}

	@Test
	public final void test6_QueuePop() {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var queueModule = demo.App.getInstance().Zeze.getQueueModule();
			var queue = queueModule.open("test1", BMyBean.class);
			var queueSize =  queue.size();
			for (int i = 9; i >= 0; i--) {
				var bean = queue.pop();
				Assertions.assertEquals(bean.getI(), i);
			}
			Assertions.assertEquals(queueSize - 10, queue.size());
			Assertions.assertTrue(queue.isEmpty());
			return Procedure.Success;
		}, "test4_QueuePop").call();
		Assertions.assertEquals(Procedure.Success, ret);
	}
}
