package UnitTest.Zeze.Trans;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import Zeze.Transaction.*;

public class TestTableNest{
	
	@Before
	public final void testInit() throws Throwable {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Throwable {
		demo.App.getInstance().Stop();
	}

	@Test
	public final void testNest() throws Throwable {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(this::ProcTableRemove, "ProcTableRemove").Call();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(this::ProcTableAdd, "ProcTableAdd").Call();
	}

	private long ProcTableRemove() {
		demo.App.getInstance().demo_Module1.getTable1().remove(4321L);
		return Procedure.Success;
	}

	private long ProcTableAdd() throws Throwable {
		demo.Module1.Value v1 = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(4321L);
		assert v1 != null;
		assert Procedure.Success != demo.App.getInstance().Zeze.NewProcedure(this::ProcTablePutNestAndRollback, "ProcTablePutNestAndRollback").Call();
		demo.Module1.Value v2 = demo.App.getInstance().demo_Module1.getTable1().get(4321L);
		assert v1 != null;
		assert v1.equals(v2);
		return Procedure.Success;
	}

	private long ProcTablePutNestAndRollback() {
		demo.Module1.Value v = new demo.Module1.Value();
		demo.App.getInstance().demo_Module1.getTable1().put(4321L, v);
		return Procedure.Unknown;
	}
}