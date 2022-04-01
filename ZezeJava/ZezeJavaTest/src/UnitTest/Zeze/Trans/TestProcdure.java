package UnitTest.Zeze.Trans;

import UnitTest.Zeze.MyBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import Zeze.Transaction.Procedure;
import Zeze.Transaction.Record1;
import Zeze.Transaction.TableKey;

public class TestProcdure{
	private MyBean bean = new MyBean();

	public final long ProcTrue() {
		bean.setI(123);
		assert bean.getI() == 123;
		return Procedure.Success;
	}

	public final long ProcFalse() {
		bean.setI(456);
		assert bean.getI() == 456;
		return Procedure.Unknown;
	}

	public final long ProcNest() throws Throwable {
		assert bean.getI() == 0;
		bean.setI(1);
		assert bean.getI() == 1; {
			long r = demo.App.getInstance().Zeze.NewProcedure(this::ProcFalse, "ProcFalse").Call();
			assert r != Procedure.Success;
			assert bean.getI() == 1;
		}

		{
			long r = demo.App.getInstance().Zeze.NewProcedure(this::ProcTrue, "ProcFalse").Call();
			assert r == Procedure.Success;
			assert bean.getI() == 123;
		}

		return Procedure.Success;
	}

	@Before
	public final void testInit() throws Throwable {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Throwable {
		demo.App.getInstance().Stop();
	}

	@Test
	public final void test1() throws Throwable {
		TableKey root = new TableKey("1", 1);
		// 特殊测试，拼凑一个record用来提供需要的信息。
		var r = new Record1<>(null, 1L, bean);
		bean.InitRootInfo(r.CreateRootInfoIfNeed(root), null);
		long rc = demo.App.getInstance().Zeze.NewProcedure(this::ProcNest, "ProcNest").Call();
		assert rc == Procedure.Success;
		// 最后一个 Call，事务外，bean 已经没法访问事务支持的属性了。直接访问内部变量。
		assert bean._i == 123;
	}
}