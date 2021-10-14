package UnitTest.Zeze.Trans;

import Zeze.Serialize.*;
import Zeze.Transaction.*;
import UnitTest.*;

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestClass] public class TestProcdure
public class TestProcdure {
	private TestBegin.MyBean bean = new TestBegin.MyBean();

	public final int ProcTrue() {
		bean.setI(123);
		assert bean.getI() == 123;
		return Procedure.Success;
	}

	public final int ProcFalse() {
		bean.setI(456);
		assert bean.getI() == 456;
		return Procedure.Unknown;
	}

	public final int ProcNest() {
		assert bean.getI() == 0;
		bean.setI(1);
		assert bean.getI() == 1; {
			int r = demo.App.getInstance().getZeze().NewProcedure(::ProcFalse, "ProcFalse", null).Call();
			assert r != Procedure.Success;
			assert bean.getI() == 1;
		}

		{
			int r = demo.App.getInstance().getZeze().NewProcedure(::ProcTrue, "ProcFalse", null).Call();
			assert r == Procedure.Success;
			assert bean.getI() == 123;
		}

		return Procedure.Success;
	}

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
//ORIGINAL LINE: [TestMethod] public void Test1()
	public final void Test1() {
		TableKey root = new TableKey(1, 1);
		// 特殊测试，拼凑一个record用来提供需要的信息。
		var r = new Record<Long, TestBegin.MyBean>(null, 1, bean);
		bean.InitRootInfo(r.CreateRootInfoIfNeed(root), null);
		int rc = demo.App.getInstance().getZeze().NewProcedure(::ProcNest, "ProcNest", null).Call();
		assert rc == Procedure.Success;
		// 最后一个 Call，事务外，bean 已经没法访问事务支持的属性了。直接访问内部变量。
		assert bean._i == 123;
	}
}