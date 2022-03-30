package UnitTest.Zeze.Trans;

import org.junit.After;
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
		assert 0L == demo.App.Instance.Zeze.NewProcedure(() -> {
				demo.App.Instance.demo_Module1.getTable1().remove(1L);
				return 0L;
			}, "ModifyMapRemove").Call();

		assert 0L == demo.App.Instance.Zeze.NewProcedure(() -> {
				var value = demo.App.Instance.demo_Module1.getTable1().getOrAdd(1L);
				value.getMap15().put(1L, 1L);

				assert 0 != demo.App.Instance.Zeze.NewProcedure(() -> {
					assert null != value.getMap15().get(1L);
					assert 1 == value.getMap15().get(1L);
					value.getMap15().put(1L, 2L);
					assert 2 == value.getMap15().get(1L);
					return Zeze.Transaction.Procedure.LogicError;
					}, "ModifyMapPut2").Call();

				assert 1 == value.getMap15().get(1L);
				return 0L;
            }, "ModifyMapPut1").Call();
	}

 	@Test
	public void TestNestModifySet() throws Throwable {
		assert 0L == demo.App.Instance.Zeze.NewProcedure(() -> {
				demo.App.Instance.demo_Module1.getTable1().remove(1L);
				return 0L;
            }, "ModifyMapRemove").Call();

		assert 0L == demo.App.Instance.Zeze.NewProcedure(() -> {
				var value = demo.App.Instance.demo_Module1.getTable1().getOrAdd(1L);
				value.getSet10().add(1);

			assert 0L != demo.App.Instance.Zeze.NewProcedure(() -> {
					assert true == value.getSet10().contains(1);
					value.getSet10().remove(1);
					assert false == value.getSet10().contains(1);
					return Zeze.Transaction.Procedure.LogicError;
					}, "ModifySetRemove1").Call();

				assert true == value.getSet10().contains(1);
				return 0L;
			}, "ModifySetAdd1").Call();
	}
}
