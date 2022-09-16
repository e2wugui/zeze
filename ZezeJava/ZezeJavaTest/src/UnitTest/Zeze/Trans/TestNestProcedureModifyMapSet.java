package UnitTest.Zeze.Trans;

import Zeze.Transaction.Procedure;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestNestProcedureModifyMapSet {
	@Before
	public final void testInit() throws Throwable {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Throwable {
		demo.App.getInstance().Stop();
	}

	@Test
	public void testNestModifyMap() throws Throwable {
		Assert.assertEquals(Procedure.Success, demo.App.Instance.Zeze.newProcedure(() -> {
			demo.App.Instance.demo_Module1.getTable1().remove(1L);
			return 0L;
		}, "ModifyMapRemove").Call());

		Assert.assertEquals(Procedure.Success, demo.App.Instance.Zeze.newProcedure(() -> {
			var value = demo.App.Instance.demo_Module1.getTable1().getOrAdd(1L);
			value.getMap15().put(1L, 1L);

			Assert.assertNotEquals(Procedure.Success, demo.App.Instance.Zeze.newProcedure(() -> {
				Assert.assertNotNull(value.getMap15().get(1L));
				Assert.assertEquals(1, (long)value.getMap15().get(1L));
				value.getMap15().put(1L, 2L);
				Assert.assertEquals(2, (long)value.getMap15().get(1L));
				return Zeze.Transaction.Procedure.LogicError;
			}, "ModifyMapPut2").Call());

			Assert.assertEquals(1, (long)value.getMap15().get(1L));
			return 0L;
		}, "ModifyMapPut1").Call());
	}

	@Test
	public void TestNestModifySet() throws Throwable {
		Assert.assertEquals(Procedure.Success, demo.App.Instance.Zeze.newProcedure(() -> {
			demo.App.Instance.demo_Module1.getTable1().remove(1L);
			return 0L;
		}, "ModifyMapRemove").Call());

		Assert.assertEquals(Procedure.Success, demo.App.Instance.Zeze.newProcedure(() -> {
			var value = demo.App.Instance.demo_Module1.getTable1().getOrAdd(1L);
			value.getSet10().add(1);

			Assert.assertNotEquals(Procedure.Success, demo.App.Instance.Zeze.newProcedure(() -> {
				Assert.assertTrue(value.getSet10().contains(1));
				value.getSet10().remove(1);
				Assert.assertFalse(value.getSet10().contains(1));
				return Zeze.Transaction.Procedure.LogicError;
			}, "ModifySetRemove1").Call());

			Assert.assertTrue(value.getSet10().contains(1));
			return 0L;
		}, "ModifySetAdd1").Call());
	}
}
