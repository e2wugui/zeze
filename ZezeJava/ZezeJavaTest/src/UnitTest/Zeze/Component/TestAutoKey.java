package UnitTest.Zeze.Component;

import Zeze.Transaction.Procedure;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestAutoKey {

	@Before
	public final void testInit() throws Throwable {
		System.out.println("testInit");
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Throwable {
		System.out.println("testCleanup");
		demo.App.getInstance().Stop();
	}

	@Test
	public final void test1_AutoKey() throws Throwable {
		System.out.println("testAutoKey1");
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.GetAutoKey("test1");
			var id = autoKey.nextId();
			Assert.assertEquals(id, 1);
			return Procedure.Success;
		}, "test1_AutoKey").Call();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.GetAutoKey("test1");
			var id = autoKey.nextId();
			Assert.assertEquals(id, 2);
			return Procedure.Success;
		}, "test1_AutoKey").Call();
	}

	@Test
	public final void test2_AutoKey() throws Throwable {
		System.out.println("testAutoKey2");
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.GetAutoKey("test1");
			var id = autoKey.nextId();
			Assert.assertEquals(id, 501);
			return Procedure.Success;
		}, "test2_AutoKey").Call();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.GetAutoKey("test1");
			var id = autoKey.nextId();
			Assert.assertEquals(id, 502);
			return Procedure.Success;
		}, "test2_AutoKey").Call();
	}

	@Test
	public final void test3_AutoKey() throws Throwable {
		System.out.println("testAutoKey2");
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.GetAutoKey("test1");
			var id = autoKey.nextId();
			Assert.assertEquals(id, 1001);
			return Procedure.Success;
		}, "test3_AutoKey").Call();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			var autoKey = demo.App.getInstance().Zeze.GetAutoKey("test1");
			var id = autoKey.nextId();
			Assert.assertEquals(id, 1002);
			return Procedure.Success;
		}, "test3_AutoKey").Call();
	}
}
