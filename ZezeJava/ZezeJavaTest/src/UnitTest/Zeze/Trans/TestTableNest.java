package UnitTest.Zeze.Trans;

import demo.Module1.BValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Transaction.*;

public class TestTableNest {
	@BeforeEach
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@AfterEach
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	@Test
	public final void testNest() throws Exception {
		Assertions.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestTableNest::ProcTableRemove, "ProcTableRemove").call());
		Assertions.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestTableNest::ProcTableAdd, "ProcTableAdd").call());
	}

	private static long ProcTableRemove() {
		demo.App.getInstance().demo_Module1.getTable1().remove(4321L);
		return Procedure.Success;
	}

	private static long ProcTableAdd() throws Exception {
		BValue v1 = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(4321L);
		Assertions.assertNotNull(v1);
		Assertions.assertNotEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestTableNest::ProcTablePutNestAndRollback, "ProcTablePutNestAndRollback").call());
		BValue v2 = demo.App.getInstance().demo_Module1.getTable1().get(4321L);
		Assertions.assertNotNull(v1);
		Assertions.assertEquals(v1, v2);
		return Procedure.Success;
	}

	private static long ProcTablePutNestAndRollback() {
		BValue v = new BValue();
		demo.App.getInstance().demo_Module1.getTable1().put(4321L, v);
		return Procedure.Unknown;
	}
}
