package UnitTest.Zeze.Collections;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import UnitTest.Zeze.BMyBean;
import Zeze.Transaction.Procedure;
import demo.App;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;

@SuppressWarnings("DataFlowIssue")
@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestLinkedMap {
	@BeforeEach
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@AfterEach
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	@Test
	public final void test1_LinkedMapPut() {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var map = demo.App.getInstance().LinkedMapModule.open("test1", BMyBean.class);
			for (int i = 100; i < 110; i++) {
				var bean = new BMyBean();
				bean.setI(i);
				map.put(i, bean);
			}
			return Procedure.Success;
		}, "test1_LinkedMapPut").call();
		Assertions.assertEquals(Procedure.Success, ret);
	}

	@Test
	public final void test2_LinkedMapGet() {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var map = demo.App.getInstance().LinkedMapModule.open("test1", BMyBean.class);
			for (int i = 100; i < 110; i++) {
				var bean = map.get(i);
				Assertions.assertEquals(bean.getI(), i);
			}
			return Procedure.Success;
		}, "test2_LinkedMapGet").call();
		Assertions.assertEquals(Procedure.Success, ret);
	}

	@Test
	public final void test3_LinkedMapWalk() throws Exception {
		var map = demo.App.getInstance().LinkedMapModule.open("test1", BMyBean.class);
		var i = new AtomicInteger(0);
		var arr = Arrays.asList(100, 101, 102, 103, 104, 105, 106, 107, 108, 109);
		Collections.reverse(arr);
		map.walk(((key, value) -> {
			Assertions.assertTrue(i.get() < 10);
			Assertions.assertEquals(value.getI(), (int)arr.get(i.getAndAdd(1)));
			return true;
		}));
		Assertions.assertEquals(10, i.get());
	}

	@Test
	public final void test4_LinkedMapRemove() {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var map = demo.App.getInstance().LinkedMapModule.open("test1", BMyBean.class);
			for (int i = 100; i < 110; i++) {
				var bean = map.remove(i);
				Assertions.assertEquals(bean.getI(), i);
			}
			Assertions.assertTrue(map.isEmpty());
			return Procedure.Success;
		}, "test2_LinkedMapRemove").call();
		Assertions.assertEquals(Procedure.Success, ret);
	}

	@Test
	public void test5_PutAndClear() throws Exception {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var map = demo.App.getInstance().LinkedMapModule.open("test1", BMyBean.class);
			for (int i = 100; i < 110; i++) {
				var bean = new BMyBean();
				bean.setI(i);
				map.put(i, bean);
			}
			return Procedure.Success;
		}, "test1_LinkedMapPut").call();
		Assertions.assertEquals(Procedure.Success, ret);

		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			App.Instance.LinkedMapModule.open("test1", BMyBean.class).clear();
			return 0;
		}, "clear").call());

		Thread.sleep(2000);
	}
}
