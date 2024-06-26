package UnitTest.Zeze.Trans;

import java.util.concurrent.Future;
import Zeze.Transaction.DispatchMode;
import demo.Module1.BValue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import Zeze.Transaction.Procedure;

public class TestConflict {
	private int sum;

	@Before
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	@Test
	public final void testConflictAdd() throws Exception {
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestConflict::ProcRemove, "ProcRemove").call());
		Future<?>[] tasks = new Future[2000];
		for (int i = 0; i < 2000; ++i) {
			tasks[i]=Zeze.Util.Task.runUnsafe(
					demo.App.getInstance().Zeze.newProcedure(this::ProcAdd, "ProcAdd"),
					DispatchMode.Normal);
		}
		for (Future<?> task : tasks) {
			try {
				task.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		sum = tasks.length;
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(this::ProcVerify, "ProcVerify").call());
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestConflict::ProcRemove, "ProcRemove").call());
	}

	private static long ProcRemove() {
		demo.App.getInstance().demo_Module1.getTable1().remove(123123L);
		return Procedure.Success;
	}

	private long ProcAdd() {
		BValue v = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(123123L);
		v.setInt_1(v.getInt_1() + 1);
		sum++;
		return Procedure.Success;
	}

	private long ProcVerify() {
		BValue v = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(123123L);
		Assert.assertEquals(v.getInt_1(), sum);
		return Procedure.Success;
	}
}
