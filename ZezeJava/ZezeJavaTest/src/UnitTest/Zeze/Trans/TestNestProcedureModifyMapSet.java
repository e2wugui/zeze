package UnitTest.Zeze.Trans;

import Zeze.Transaction.Procedure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestNestProcedureModifyMapSet {
	@BeforeEach
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@AfterEach
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	@Test
	public void testNestModifyMap() throws Exception {
		Assertions.assertEquals(Procedure.Success, demo.App.Instance.Zeze.newProcedure(() -> {
			demo.App.Instance.demo_Module1.getTable1().remove(1L);
			return 0L;
		}, "ModifyMapRemove").call());

		Assertions.assertEquals(Procedure.Success, demo.App.Instance.Zeze.newProcedure(() -> {
			var value = demo.App.Instance.demo_Module1.getTable1().getOrAdd(1L);
			value.getMap15().put(1L, 1L);

			Assertions.assertNotEquals(Procedure.Success, demo.App.Instance.Zeze.newProcedure(() -> {
				Assertions.assertNotNull(value.getMap15().get(1L));
				Assertions.assertEquals(1, (long)value.getMap15().get(1L));
				value.getMap15().put(1L, 2L);
				Assertions.assertEquals(2, (long)value.getMap15().get(1L));
				return Zeze.Transaction.Procedure.LogicError;
			}, "ModifyMapPut2").call());

			Assertions.assertEquals(1, (long)value.getMap15().get(1L));
			return 0L;
		}, "ModifyMapPut1").call());
	}

	@Test
	public void TestNestModifySet() throws Exception {
		Assertions.assertEquals(Procedure.Success, demo.App.Instance.Zeze.newProcedure(() -> {
			demo.App.Instance.demo_Module1.getTable1().remove(1L);
			return 0L;
		}, "ModifyMapRemove").call());

		Assertions.assertEquals(Procedure.Success, demo.App.Instance.Zeze.newProcedure(() -> {
			var value = demo.App.Instance.demo_Module1.getTable1().getOrAdd(1L);
			value.getSet10().add(1);

			Assertions.assertNotEquals(Procedure.Success, demo.App.Instance.Zeze.newProcedure(() -> {
				Assertions.assertTrue(value.getSet10().contains(1));
				value.getSet10().remove(1);
				Assertions.assertFalse(value.getSet10().contains(1));
				return Zeze.Transaction.Procedure.LogicError;
			}, "ModifySetRemove1").call());

			Assertions.assertTrue(value.getSet10().contains(1));
			return 0L;
		}, "ModifySetAdd1").call());
	}
}
