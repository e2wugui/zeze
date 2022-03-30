package UnitTest.Zeze.Trans;

import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import Zeze.Transaction.Procedure;
import Zeze.Util.Task;

public class TestConflict {
	private int sum;

	@Before
	public final void testInit() throws Throwable {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Throwable {
		demo.App.getInstance().Stop();
	}

	@Test
	public final void testConflictAdd() throws Throwable {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(this::ProcRemove, "ProcRemove").Call();
		Future<?>[] tasks = new Future[2000];
		for (int i = 0; i < 2000; ++i) {
			tasks[i]=Zeze.Util.Task.run(demo.App.getInstance().Zeze.NewProcedure(this::ProcAdd, "ProcAdd"), null, null);
		}
		for (int i = 0; i < tasks.length; ++i) {
			try {
				tasks[i].get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		sum = tasks.length;
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(this::ProcVerify, "ProcVerify").Call();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(this::ProcRemove, "ProcRemove").Call();
	}

	private long ProcRemove() {
		demo.App.getInstance().demo_Module1.getTable1().remove(123123l);
		return Procedure.Success;
	}

	private long ProcAdd() {
		demo.Module1.Value v = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(123123L);
		v.setInt1(v.getInt1() + 1);
		sum++;
		return Procedure.Success;
	}

	private long ProcVerify() {
		demo.Module1.Value v = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(123123L);
		assert v.getInt1() == sum;
		return Procedure.Success;
	}
}
