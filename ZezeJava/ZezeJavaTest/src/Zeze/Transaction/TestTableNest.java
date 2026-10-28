package Zeze.Transaction;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.NullMarked;

import demo.Module1.BValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Transaction.*;

@NullMarked
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
		Assertions.assertEquals(Procedure.Success, requireNonNull(demo.App.getInstance().Zeze).newProcedure(TestTableNest::ProcTableRemove, "ProcTableRemove").call());
		Assertions.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestTableNest::ProcTableAdd, "ProcTableAdd").call());
	}

	private static long ProcTableRemove() {
		requireNonNull(demo.App.getInstance().demo_Module1).getTable1().remove(4321L);
		return Procedure.Success;
	}

	private static long ProcTableAdd() throws Exception {
		BValue v1 = requireNonNull(demo.App.getInstance().demo_Module1).getTable1().getOrAdd(4321L);
		Assertions.assertNotNull(v1);
		Assertions.assertNotEquals(Procedure.Success, requireNonNull(demo.App.getInstance().Zeze).newProcedure(TestTableNest::ProcTablePutNestAndRollback, "ProcTablePutNestAndRollback").call());
		BValue v2 = demo.App.getInstance().demo_Module1.getTable1().get(4321L);
		Assertions.assertNotNull(v1);
		Assertions.assertEquals(v1, v2);
		return Procedure.Success;
	}

	private static long ProcTablePutNestAndRollback() {
		BValue v = new BValue();
		requireNonNull(demo.App.getInstance().demo_Module1).getTable1().put(4321L, v);
		return Procedure.Unknown;
	}
}
