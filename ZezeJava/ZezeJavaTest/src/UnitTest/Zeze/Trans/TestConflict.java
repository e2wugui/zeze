package UnitTest.Zeze.Trans;

import java.util.ArrayList;
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
		var tasks = new ArrayList<Future<?>>();
		for (int i = 0; i < 2000; ++i) {
			tasks.add(Zeze.Util.Task.runUnsafe(
					demo.App.getInstance().Zeze.newProcedure(this::ProcAdd, "ProcAdd"),
					DispatchMode.Normal));
			if ((i+1) % 200 == 0) {
				for (Future<?> task : tasks)
					task.get();
				sum += tasks.size();
				tasks.clear();
			}
		}
		for (Future<?> task : tasks) {
			task.get();
		}
		sum += tasks.size();
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
		return Procedure.Success;
	}

	private long ProcVerify() {
		BValue v = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(123123L);
		Assert.assertEquals(v.getInt_1(), sum);
		return Procedure.Success;
	}
}
