package UnitTest.Zeze.Trans;

import Zeze.Transaction.*;
import UnitTest.*;

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestClass] public class TestCheckpointModeTable
public class TestCheckpointModeTable {
//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestInitialize] public void TestInit()
	public final void TestInit() {
		demo.App.getInstance().Start();
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestCleanup] public void TestCleanup()
	public final void TestCleanup() {
		demo.App.getInstance().Stop();
	}

	private void Check(int expect) {
		assert Procedure.Success == demo.App.getInstance().getZeze().NewProcedure(() -> {
					var value = demo.App.getInstance().getDemoModule1().getTableImportant().GetOrAdd(1);
					return value.getInt1() == expect ? Procedure.Success : Procedure.LogicError;
		}, "TestCheckpointModeTable.Check", null).Call();
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void Test1()
	public final void Test1() {
		assert Procedure.Success == demo.App.getInstance().getZeze().NewProcedure(() -> {
					var value = demo.App.getInstance().getDemoModule1().getTableImportant().GetOrAdd(1);
					value.Int1 = 0;
					return Procedure.Success;
		}, "TestCheckpointModeTable.Init", null).Call();
		Check(0);

		int sum = 0; {
			Task[] tasks = new Task[1000];
			for (int i = 0; i < tasks.length; ++i) {
				tasks[i] = Zeze.Util.Task.Run(demo.App.getInstance().getZeze().NewProcedure(::Add, "TestCheckpointModeTable.Add", null), null, null);
			}
			Task.WaitAll(tasks);
			sum += tasks.length;
			Check(sum);
		}

		{
			Task[] tasks = new Task[1000];
			for (int i = 0; i < tasks.length; ++i) {
				tasks[i] = Zeze.Util.Task.Run(demo.App.getInstance().getZeze().NewProcedure(::Add2, "TestCheckpointModeTable.Add2", null), null, null);
			}
			Task.WaitAll(tasks);
			sum += tasks.length;
			Check(sum);
		}
	}

	private int Add() {
		var value = demo.App.getInstance().getDemoModule1().getTableImportant().GetOrAdd(1);
		value.Int1 = value.getInt1() + 1;
		return Procedure.Success;
	}

	private int Add2() {
		var value = demo.App.getInstance().getDemoModule1().getTableImportant().GetOrAdd(1);
		value.Int1 = value.getInt1() + 1;
		var value2 = demo.App.getInstance().getDemoModule1().getTable1().GetOrAdd(1);
		value2.Int1 = value2.getInt1() + 1;
		return Procedure.Success;
	}
}