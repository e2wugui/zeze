package UnitTest.Zeze.Trans;

import demo.Module1.BValue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import Zeze.Transaction.*;

public class TestTableNest {
	@Before
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Exception {
		demo.App.getInstance().Stop();
	}

	@Test
	public final void testNest() throws Exception {
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestTableNest::ProcTableRemove, "ProcTableRemove").call());
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestTableNest::ProcTableAdd, "ProcTableAdd").call());
	}

	private static long ProcTableRemove() {
		demo.App.getInstance().demo_Module1.getTable1().remove(4321L);
		return Procedure.Success;
	}

	private static long ProcTableAdd() throws Exception {
		BValue v1 = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(4321L);
		Assert.assertNotNull(v1);
		Assert.assertNotEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestTableNest::ProcTablePutNestAndRollback, "ProcTablePutNestAndRollback").call());
		BValue v2 = demo.App.getInstance().demo_Module1.getTable1().get(4321L);
		Assert.assertNotNull(v1);
		Assert.assertEquals(v1, v2);
		return Procedure.Success;
	}

	private static long ProcTablePutNestAndRollback() {
		BValue v = new BValue();
		demo.App.getInstance().demo_Module1.getTable1().put(4321L, v);
		return Procedure.Unknown;
	}
}
