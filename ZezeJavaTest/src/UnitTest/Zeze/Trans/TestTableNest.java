package UnitTest.Zeze.Trans;

import Zeze.Transaction.*;
import UnitTest.*;

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestClass] public class TestTableNest
public class TestTableNest {
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
//ORIGINAL LINE: [TestMethod] public void TestNest()
	public final void TestNest() {
		assert Procedure.Success == demo.App.getInstance().getZeze().NewProcedure(::ProcTableRemove, "ProcTableRemove", null).Call();
		assert Procedure.Success == demo.App.getInstance().getZeze().NewProcedure(::ProcTableAdd, "ProcTableAdd", null).Call();
	}

	private int ProcTableRemove() {
		demo.App.getInstance().getDemoModule1().getTable1().Remove(4321);
		return Procedure.Success;
	}

	private int ProcTableAdd() {
		demo.Module1.Value v1 = demo.App.getInstance().getDemoModule1().getTable1().GetOrAdd(4321);
		assert v1 != null;
		assert Procedure.Success != demo.App.getInstance().getZeze().NewProcedure(::ProcTablePutNestAndRollback, "ProcTablePutNestAndRollback", null).Call();
		demo.Module1.Value v2 = demo.App.getInstance().getDemoModule1().getTable1().Get(4321);
		assert v1 != null;
		assert v1 == v2;
		return Procedure.Success;
	}

	private int ProcTablePutNestAndRollback() {
		demo.Module1.Value v = new demo.Module1.Value();
		demo.App.getInstance().getDemoModule1().getTable1().Put(4321, v);
		return Procedure.Unknown;
	}
}