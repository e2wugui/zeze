package UnitTest.Zeze.Trans;

import Zeze.Serialize.*;
import Zeze.Transaction.*;
import UnitTest.*;

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestClass] public class TestCheckpoint
public class TestCheckpoint {
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
//ORIGINAL LINE: [TestMethod] public void TestCp()
	public final void TestCp() {
		assert demo.App.getInstance().getZeze().NewProcedure(::ProcClear, "ProcClear", null).Call() == Procedure.Success;
		assert demo.App.getInstance().getZeze().NewProcedure(::ProcChange, "ProcChange", null).Call() == Procedure.Success;
		demo.App.getInstance().getZeze().CheckpointRun();
		demo.Module1.Table1 table = demo.App.getInstance().getDemoModule1().getTable1();
		ByteBuffer value = table.GetStorageForTestOnly("IKnownWhatIAmDoing").DatabaseTable.Find(table.EncodeKey(56));
		assert value != null;
		assert value == bytesInTrans;
	}

	private int ProcClear() {
		demo.App.getInstance().getDemoModule1().getTable1().Remove(56);
		return Procedure.Success;
	}

	private ByteBuffer bytesInTrans;
	private int ProcChange() {
		demo.Module1.Value v = demo.App.getInstance().getDemoModule1().getTable1().GetOrAdd(56);
		v.Int1 = 1;
		bytesInTrans = ByteBuffer.Allocate();
		v.Encode(bytesInTrans);
		return Procedure.Success;
	}
}