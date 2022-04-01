package UnitTest.Zeze.Collections;

import UnitTest.Zeze.MyBean;
import Zeze.Transaction.Procedure;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestQueue {

	@Before
	public final void testInit() throws Throwable {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Throwable {
		demo.App.getInstance().Stop();
	}

	@Test
	public final void test1_QueueAdd() throws Throwable {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			var queueModule = demo.App.getInstance().Zeze.getQueueModule();
			var queue = queueModule.open("test1", MyBean.class);
			for (int i = 0; i < 10; i++) {
				var bean = new MyBean();
				bean.setI(i);
				queue.add(bean);
			}
			var bean = queue.peek();
			assert bean.getI() == 0;
			return Procedure.Success;
		}, "test1_QueueAdd").Call();
	}

	@Test
	public final void test2_QueuePop() throws Throwable {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			var queueModule = demo.App.getInstance().Zeze.getQueueModule();
			var queue = queueModule.open("test1", MyBean.class);
			for (int i = 0; i < 10; i++) {
				var bean = queue.pop();
				assert bean.getI() == i;
			}
			return Procedure.Success;
		}, "test2_QueuePop").Call();
	}

	@Test
	public final void test3_QueuePush() throws Throwable {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			var queueModule = demo.App.getInstance().Zeze.getQueueModule();
			var queue = queueModule.open("test1", MyBean.class);
			for (int i = 0; i < 10; i++) {
				var bean = new MyBean();
				bean.setI(i);
				queue.push(bean);
			}
			var bean = queue.peek();
			assert bean.getI() == 9;
			return Procedure.Success;
		}, "test3_QueuePush").Call();
	}

	@Test
	public final void test4_QueuePop() throws Throwable {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			var queueModule = demo.App.getInstance().Zeze.getQueueModule();
			var queue = queueModule.open("test1", MyBean.class);
			for (int i = 9; i >= 0; i--) {
				var bean = queue.pop();
				assert bean.getI() == i;
			}
			return Procedure.Success;
		}, "test4_QueuePop").Call();
	}
}
