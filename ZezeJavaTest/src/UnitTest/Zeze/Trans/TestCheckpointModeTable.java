package UnitTest.Zeze.Trans;

import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import Zeze.Transaction.Procedure;
import Zeze.Util.Task;

public class TestCheckpointModeTable{
	
	@Before
	public final void testInit() {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() {
		demo.App.getInstance().Stop();
	}

	private void Check(int expect) {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
					var value = demo.App.getInstance().demo_Module1.getTableImportant().GetOrAdd(1L);
					return value.getInt1() == expect ? Procedure.Success : Procedure.LogicError;
		}, "TestCheckpointModeTable.Check", null).Call();
	}

	@Test
	public final void test1() {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
					var value = demo.App.getInstance().demo_Module1.getTableImportant().GetOrAdd(1L);
					value.setInt1(0);
					return Procedure.Success;
		}, "TestCheckpointModeTable.Init", null).Call();
		Check(0);

		int sum = 0; {
			Task[] tasks = new Task[1000];
			for (int i = 0; i < tasks.length; ++i) {
				tasks[i] = Zeze.Util.Task.Run(demo.App.getInstance().Zeze.NewProcedure(this::Add, "TestCheckpointModeTable.Add", null), null, null);
			}
			for (int i = 0; i < tasks.length; ++i) {
				try {
					tasks[i].get();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			sum += tasks.length;
			Check(sum);
		}

		{
			Task[] tasks = new Task[1000];
			for (int i = 0; i < tasks.length; ++i) {
				tasks[i] = Zeze.Util.Task.Run(demo.App.getInstance().Zeze.NewProcedure(this::Add2, "TestCheckpointModeTable.Add2", null), null, null);
			}
			for (int i = 0; i < tasks.length; ++i) {
				try {
					tasks[i].get();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			sum += tasks.length;
			Check(sum);
		}
	}

	private int Add() {
		var value = demo.App.getInstance().demo_Module1.getTableImportant().GetOrAdd(1l);
		value.setInt1 (value.getInt1() + 1);
		return Procedure.Success;
	}

	private int Add2() {
		var value = demo.App.getInstance().demo_Module1.getTableImportant().GetOrAdd(1l);
		value.setInt1(value.getInt1() + 1);
		var value2 = demo.App.getInstance().demo_Module1.getTable1().GetOrAdd(1l);
		value2.setInt1 ( value2.getInt1() + 1);
		return Procedure.Success;
	}
}