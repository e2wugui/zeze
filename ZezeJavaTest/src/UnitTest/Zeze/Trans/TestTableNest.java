package UnitTest.Zeze.Trans;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import Zeze.Transaction.*;

public class TestTableNest{
	
	@Before
	public final void testInit() {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() {
		demo.App.getInstance().Stop();
	}

	@Test
	public final void testNest() {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(this::ProcTableRemove, "ProcTableRemove", null).Call();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(this::ProcTableAdd, "ProcTableAdd", null).Call();
	}

	private int ProcTableRemove() {
		demo.App.getInstance().demo_Module1.getTable1().Remove(4321L);
		return Procedure.Success;
	}

	private int ProcTableAdd() {
		demo.Module1.Value v1 = demo.App.getInstance().demo_Module1.getTable1().GetOrAdd(4321L);
		assert v1 != null;
		assert Procedure.Success != demo.App.getInstance().Zeze.NewProcedure(this::ProcTablePutNestAndRollback, "ProcTablePutNestAndRollback", null).Call();
		demo.Module1.Value v2 = demo.App.getInstance().demo_Module1.getTable1().Get(4321L);
		assert v1 != null;
		assert v1.equals(v2);
		return Procedure.Success;
	}

	private int ProcTablePutNestAndRollback() {
		demo.Module1.Value v = new demo.Module1.Value();
		demo.App.getInstance().demo_Module1.getTable1().Put(4321L, v);
		return Procedure.Unknown;
	}
}