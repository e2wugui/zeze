package UnitTest.Zeze.Trans;

import java.util.concurrent.Future;
import Zeze.Transaction.DispatchMode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import Zeze.Transaction.Procedure;

public class TestCheckpointModeTable{

	@Before
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Exception {
		demo.App.getInstance().Stop();
	}

	private static void Check(int expect) throws Exception {
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
					var value = demo.App.getInstance().demo_Module1.getTableImportant().getOrAdd(1L);
					return value.getInt1() == expect ? Procedure.Success : Procedure.LogicError;
		}, "TestCheckpointModeTable.Check").call());
	}

	@Test
	public final void test1() throws Exception {
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
					var value = demo.App.getInstance().demo_Module1.getTableImportant().getOrAdd(1L);
					value.setInt1(0);
					return Procedure.Success;
		}, "TestCheckpointModeTable.Init").call());
		Check(0);

		int sum = 0; {
			Future<?>[] tasks = new Future[1000];
			for (int i = 0; i < tasks.length; ++i) {
				tasks[i] = Zeze.Util.Task.runUnsafe(
						demo.App.getInstance().Zeze.newProcedure(TestCheckpointModeTable::Add, "TestCheckpointModeTable.Add"),
						null, null, DispatchMode.Normal);
			}
			for (Future<?> task : tasks) {
				try {
					task.get();
				} catch (Throwable e) {
					// print stacktrace.
					e.printStackTrace();
				}
			}
			sum += tasks.length;
			Check(sum);
		}

		{
			Future<?>[] tasks = new Future[1000];
			for (int i = 0; i < tasks.length; ++i) {
				tasks[i] = Zeze.Util.Task.runUnsafe(
						demo.App.getInstance().Zeze.newProcedure(TestCheckpointModeTable::Add2, "TestCheckpointModeTable.Add2"),
						null, null, DispatchMode.Normal);
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

	private static long Add() {
		var value = demo.App.getInstance().demo_Module1.getTableImportant().getOrAdd(1L);
		value.setInt1 (value.getInt1() + 1);
		return Procedure.Success;
	}

	private static long Add2() {
		var value = demo.App.getInstance().demo_Module1.getTableImportant().getOrAdd(1L);
		value.setInt1(value.getInt1() + 1);
		var value2 = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(1L);
		value2.setInt1 ( value2.getInt1() + 1);
		return Procedure.Success;
	}
}
