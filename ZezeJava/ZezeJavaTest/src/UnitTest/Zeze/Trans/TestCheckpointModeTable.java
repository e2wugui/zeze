package UnitTest.Zeze.Trans;

import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import Zeze.Transaction.Procedure;

public class TestCheckpointModeTable{

	@Before
	public final void testInit() throws Throwable {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Throwable {
		demo.App.getInstance().Stop();
	}

	private void Check(int expect) throws Throwable {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
					var value = demo.App.getInstance().demo_Module1.getTableImportant().getOrAdd(1L);
					return value.getInt1() == expect ? Procedure.Success : Procedure.LogicError;
		}, "TestCheckpointModeTable.Check").Call();
	}

	@Test
	public final void test1() throws Throwable {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
					var value = demo.App.getInstance().demo_Module1.getTableImportant().getOrAdd(1L);
					value.setInt1(0);
					return Procedure.Success;
		}, "TestCheckpointModeTable.Init").Call();
		Check(0);

		int sum = 0; {
			Future<?>[] tasks = new Future[1000];
			for (int i = 0; i < tasks.length; ++i) {
				tasks[i] = Zeze.Util.Task.run(demo.App.getInstance().Zeze.NewProcedure(this::Add, "TestCheckpointModeTable.Add"), null, null);
			}
			for (Future<?> task : tasks) {
				try {
					task.get();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
			sum += tasks.length;
			Check(sum);
		}

		{
			Future<?>[] tasks = new Future[1000];
			for (int i = 0; i < tasks.length; ++i) {
				tasks[i] = Zeze.Util.Task.run(demo.App.getInstance().Zeze.NewProcedure(this::Add2, "TestCheckpointModeTable.Add2"), null, null);
			}
			for (Future<?> task : tasks) {
				try {
					task.get();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			sum += tasks.length;
			Check(sum);
		}
	}

	private long Add() {
		var value = demo.App.getInstance().demo_Module1.getTableImportant().getOrAdd(1L);
		value.setInt1 (value.getInt1() + 1);
		return Procedure.Success;
	}

	private long Add2() {
		var value = demo.App.getInstance().demo_Module1.getTableImportant().getOrAdd(1L);
		value.setInt1(value.getInt1() + 1);
		var value2 = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(1L);
		value2.setInt1 ( value2.getInt1() + 1);
		return Procedure.Success;
	}
}
