package UnitTest.Zeze.Trans;

import Zeze.Serialize.*;
import Zeze.Transaction.*;
import UnitTest.*;

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestClass] public class TestTable
public class TestTable {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestInitialize] public void TestInit()
	public final void TestInit() {
		demo.App.getInstance().Start();
		demo.App.getInstance().getZeze().NewProcedure(() -> {
				demo.App.getInstance().getDemoModule1().getTable1().Remove(1);
				demo.App.getInstance().getDemoModule1().getTable2().Remove(new demo.Module1.Key((short)1));
				return Procedure.Success;
		}, "RemoveDataFirst", null).Call();
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestCleanup] public void TestCleanup()
	public final void TestCleanup() {
		demo.App.getInstance().Stop();
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void TestUpdate()
	public final void TestUpdate() {
		assert Procedure.Success == demo.App.getInstance().getZeze().NewProcedure(::ProcGetOrAdd, "ProcGetOrAdd", null).Call();
		assert Procedure.Success == demo.App.getInstance().getZeze().NewProcedure(::ProcGetUpdate, "ProcGetUpdate", null).Call();
		assert Procedure.Success == demo.App.getInstance().getZeze().NewProcedure(::ProcGetUpdateCheckRemove, "ProcGetUpdateCheckRemove", null).Call();
	}

	private int ProcGetUpdate() {
		demo.Module1.Value v = demo.App.getInstance().getDemoModule1().getTable1().Get(1);

		v.Int1 = 11;
		v.Long2 = 22;
		v.String3 = "33";
		v.Bool4 = true;
		v.Short5 = 55;
		v.Float6 = 66;
		v.Double7 = 77;
		v.getList9().Add(new demo.Bean1());
		v.getSet10().Add(1010);
		v.getMap11().Add(2, new demo.Module2.Value());
		v.getBean12().Int1 = 1212;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: v.Byte13 = 131;
		v.Byte13 = (byte)131;
		return Procedure.Success;
	}

	private int ProcGetUpdateCheckRemove() {
		demo.Module1.Value v = demo.App.getInstance().getDemoModule1().getTable1().Get(1);

		assert v.getInt1() == 11;
		assert v.getLong2() == 22;
		assert v.getString3().equals("33");
		assert v.getBool4();
		assert v.getShort5() == 55;
		assert v.getFloat6() == 66;
		assert v.getDouble7() == 77;
		assert v.getList9().Count == 2;
		assert v.getSet10().Contains(10);
		assert v.getSet10().Contains(1010);
		assert v.getSet10().Count == 2;
		assert v.getMap11().Count == 2;
		assert v.getBean12().getInt1() == 1212;
		assert v.getByte13() == 131;

		return Procedure.Success;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void TestGetOrAdd()
	public final void TestGetOrAdd() {
		assert Procedure.Success == demo.App.getInstance().getZeze().NewProcedure(::ProcGetOrAdd, "ProcGetOrAdd", null).Call();
		assert Procedure.Success == demo.App.getInstance().getZeze().NewProcedure(::ProcGetOrAddCheckAndRemove, "ProcGetOrAddCheckAndRemove", null).Call();
	}

	private int ProcGetOrAdd() {
		demo.Module1.Value v = demo.App.getInstance().getDemoModule1().getTable1().GetOrAdd(1);

		v.Int1 = 1;
		v.Long2 = 2;
		v.String3 = "3";
		v.Bool4 = true;
		v.Short5 = 5;
		v.Float6 = 6;
		v.Double7 = 7;
		v.getList9().Add(new demo.Bean1());
		v.getSet10().Add(10);
		v.getMap11().Add(1, new demo.Module2.Value());
		v.getBean12().Int1 = 12;
		v.Byte13 = 13;

		return Procedure.Success;
	}

	private int ProcGetOrAddCheckAndRemove() {
		var v = demo.App.getInstance().getDemoModule1().getTable1().Get(1);
		assert v != null;

		assert v.getInt1() == 1;
		assert v.getLong2() == 2;
		assert v.getString3().equals("3");
		assert v.getBool4();
		assert v.getShort5() == 5;
		assert v.getFloat6() == 6;
		assert v.getDouble7() == 7;
		assert v.getList9().Count == 1;
		assert v.getSet10().Contains(10);
		assert v.getSet10().Count == 1;
		assert v.getMap11().Count == 1;
		assert v.getBean12().getInt1() == 12;
		assert v.getByte13() == 13;

		demo.App.getInstance().getDemoModule1().getTable1().Remove(1);
		assert demo.App.getInstance().getDemoModule1().getTable1().Get(1) == null;
		return Procedure.Success;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void Test1TableGetPut()
	public final void Test1TableGetPut() {
		assert Procedure.Success == demo.App.getInstance().getZeze().NewProcedure(::ProcGet11, "ProcGet11", null).Call();
		assert Procedure.Success == demo.App.getInstance().getZeze().NewProcedure(::ProcGet12, "ProcGet12", null).Call();
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void Test2TableGetPut()
	public final void Test2TableGetPut() {
		assert Procedure.Success == demo.App.getInstance().getZeze().NewProcedure(::ProcGet21, "ProcGet21", null).Call();
		assert Procedure.Success == demo.App.getInstance().getZeze().NewProcedure(::ProcGet22, "ProcGet22", null).Call();
	}

	private int ProcGet21() {
		ProcGet11();
		demo.Module1.Key key = new demo.Module1.Key((short)1);
		assert demo.App.getInstance().getDemoModule1().getTable2().Get(key) == null;
		demo.Module1.Value v = new demo.Module1.Value();

		v.Int1 = 1;
		v.Long2 = 2;
		v.String3 = "3";
		v.Bool4 = true;
		v.Short5 = 5;
		v.Float6 = 6;
		v.Double7 = 7;
		v.getList9().Add(new demo.Bean1());
		v.getSet10().Add(10);
		v.getMap11().Add(1, new demo.Module2.Value());
		v.getBean12().Int1 = 12;
		v.Byte13 = 13;

		demo.App.getInstance().getDemoModule1().getTable2().Put(key, v);
		assert v == demo.App.getInstance().getDemoModule1().getTable2().Get(key);
		return Procedure.Success;
	}

	private int ProcGet22() {
		ProcGet12();
		demo.Module1.Key key = new demo.Module1.Key((short)1);
		var v = demo.App.getInstance().getDemoModule1().getTable2().Get(key);
		assert v != null;

		assert v.getInt1() == 1;
		assert v.getLong2() == 2;
		assert v.getString3().equals("3");
		assert v.getBool4();
		assert v.getShort5() == 5;
		assert v.getFloat6() == 6;
		assert v.getDouble7() == 7;
		assert v.getList9().Count == 1;
		assert v.getSet10().Contains(10);
		assert v.getSet10().Count == 1;
		assert v.getMap11().Count == 1;
		assert v.getBean12().getInt1() == 12;
		assert v.getByte13() == 13;

		demo.App.getInstance().getDemoModule1().getTable2().Remove(key);
		assert demo.App.getInstance().getDemoModule1().getTable2().Get(key) == null;
		return Procedure.Success;
	}

	private int ProcGet11() {
		assert demo.App.getInstance().getDemoModule1().getTable1().Get(1) == null;
		demo.Module1.Value v = new demo.Module1.Value();

		v.Int1 = 1;
		v.Long2 = 2;
		v.String3 = "3";
		v.Bool4 = true;
		v.Short5 = 5;
		v.Float6 = 6;
		v.Double7 = 7;
		v.getList9().Add(new demo.Bean1());
		v.getSet10().Add(10);
		v.getMap11().Add(1, new demo.Module2.Value());
		v.getBean12().Int1 = 12;
		v.Byte13 = 13;

		demo.App.getInstance().getDemoModule1().getTable1().Put(1, v);
		assert v == demo.App.getInstance().getDemoModule1().getTable1().Get(1);
		return Procedure.Success;
	}

	private int ProcGet12() {
		var v = demo.App.getInstance().getDemoModule1().getTable1().Get(1);
		assert v != null;

		assert v.getInt1() == 1;
		assert v.getLong2() == 2;
		assert v.getString3().equals("3");
		assert v.getBool4();
		assert v.getShort5() == 5;
		assert v.getFloat6() == 6;
		assert v.getDouble7() == 7;
		assert v.getList9().Count == 1;
		assert v.getSet10().Contains(10);
		assert v.getSet10().Count == 1;
		assert v.getMap11().Count == 1;
		assert v.getBean12().getInt1() == 12;
		assert v.getByte13() == 13;

		demo.App.getInstance().getDemoModule1().getTable1().Remove(1);
		assert demo.App.getInstance().getDemoModule1().getTable1().Get(1) == null;
		return Procedure.Success;
	}
}