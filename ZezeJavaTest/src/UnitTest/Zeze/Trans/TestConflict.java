package UnitTest.Zeze.Trans;

import Zeze.Transaction.*;
import UnitTest.*;

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestClass] public class TestConflict
public class TestConflict {
	private int sum;

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

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void TestConflictAdd()
	public final void TestConflictAdd() {
		assert Procedure.Success == demo.App.getInstance().getZeze().NewProcedure(::ProcRemove, "ProcRemove", null).Call();
		Task[] tasks = new Task[2000];
		for (int i = 0; i < tasks.length; ++i) {
		   tasks[i] = Zeze.Util.Task.Run(demo.App.getInstance().getZeze().NewProcedure(::ProcAdd, "ProcAdd", null), null, null);
		}
		Task.WaitAll(tasks);
		sum = tasks.length;
		assert Procedure.Success == demo.App.getInstance().getZeze().NewProcedure(::ProcVerify, "ProcVerify", null).Call();
		assert Procedure.Success == demo.App.getInstance().getZeze().NewProcedure(::ProcRemove, "ProcRemove", null).Call();
	}

	private int ProcRemove() {
		demo.App.getInstance().getDemoModule1().getTable1().Remove(123123);
		return Procedure.Success;
	}

	private int ProcAdd() {
		demo.Module1.Value v = demo.App.getInstance().getDemoModule1().getTable1().GetOrAdd(123123);
		v.Int1 += 1;
		return Procedure.Success;
	}

	private int ProcVerify() {
		demo.Module1.Value v = demo.App.getInstance().getDemoModule1().getTable1().GetOrAdd(123123);
		assert v.getInt1() == sum;
		return Procedure.Success;
	}
}