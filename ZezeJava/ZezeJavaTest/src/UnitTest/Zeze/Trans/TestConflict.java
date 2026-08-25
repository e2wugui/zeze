package UnitTest.Zeze.Trans;

import java.util.ArrayList;
import java.util.concurrent.Future;
import demo.Module1.BValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Transaction.Procedure;

public class TestConflict {
	private int sum;

	@BeforeEach
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@AfterEach
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	@Test
	public final void testConflictAdd() throws Exception {
		Assertions.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestConflict::ProcRemove, "ProcRemove").call());
		var tasks = new ArrayList<Future<?>>();
		for (int i = 0; i < 2000; ++i) {
			tasks.add(Zeze.Util.TaskSpec.ofProcedure(
					demo.App.getInstance().Zeze.newProcedure(TestConflict::ProcAdd, "ProcAdd")).submitNow());
			if ((i + 1) % 200 == 0) {
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
		Assertions.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(this::ProcVerify, "ProcVerify").call());
		Assertions.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestConflict::ProcRemove, "ProcRemove").call());
	}

	private static long ProcRemove() {
		demo.App.getInstance().demo_Module1.getTable1().remove(123123L);
		return Procedure.Success;
	}

	private static long ProcAdd() {
		BValue v = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(123123L);
		v.setInt_1(v.getInt_1() + 1);
		return Procedure.Success;
	}

	private long ProcVerify() {
		BValue v = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(123123L);
		Assertions.assertEquals(v.getInt_1(), sum);
		return Procedure.Success;
	}
}
