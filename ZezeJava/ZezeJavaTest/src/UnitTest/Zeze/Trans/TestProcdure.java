package UnitTest.Zeze.Trans;

import UnitTest.Zeze.BMyBean;
import Zeze.Serialize.Vector2;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Record1;
import Zeze.Transaction.TableKey;
import demo.App;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestProcdure {
	private final BMyBean bean = new BMyBean();

	public final long ProcTrue() {
		bean.setI(123);
		Assert.assertEquals(bean.getI(), 123);
		return Procedure.Success;
	}

	public final long ProcFalse() {
		bean.setI(456);
		Assert.assertEquals(bean.getI(), 456);
		return Procedure.Unknown;
	}

	public final long ProcNest() throws Throwable {
		Assert.assertEquals(bean.getI(), 0);
		bean.setI(1);
		Assert.assertEquals(bean.getI(), 1);
		{
			long r = demo.App.getInstance().Zeze.NewProcedure(this::ProcFalse, "ProcFalse").Call();
			Assert.assertNotEquals(r, Procedure.Success);
			Assert.assertEquals(bean.getI(), 1);
		}

		{
			long r = demo.App.getInstance().Zeze.NewProcedure(this::ProcTrue, "ProcFalse").Call();
			Assert.assertEquals(r, Procedure.Success);
			Assert.assertEquals(bean.getI(), 123);
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
		TableKey root = new TableKey(1, 1);
		// 特殊测试，拼凑一个record用来提供需要的信息。
		var r = new Record1<>(null, 1L, bean);
		bean.InitRootInfo(r.CreateRootInfoIfNeed(root), null);
		long rc = demo.App.getInstance().Zeze.NewProcedure(this::ProcNest, "ProcNest").Call();
		Assert.assertEquals(rc, Procedure.Success);
		// 最后一个 Call，事务外，bean 已经没法访问事务支持的属性了。直接访问内部变量。
		Assert.assertEquals(bean._i, 123);
	}

	@Test
	public final void testVector() throws Throwable {
		App.getInstance().Zeze.NewProcedure(() -> {
			var v = App.getInstance().demo_Module1.getTable1().getOrAdd(999L);
			v.setVector2(new Vector2(1, 2));
			Assert.assertEquals(new Vector2(1, 2), v.getVector2());
			return 0;
		}, "testVector1").Call();

		App.getInstance().Zeze.NewProcedure(() -> {
			var v = App.getInstance().demo_Module1.getTable1().getOrAdd(999L);
			Assert.assertEquals(new Vector2(1, 2), v.getVector2());
			v.setVector2(new Vector2(3, 4));
			Assert.assertEquals(new Vector2(3, 4), v.getVector2());
			return Procedure.LogicError;
		}, "testVector2").Call();

		App.getInstance().Zeze.NewProcedure(() -> {
			var v = App.getInstance().demo_Module1.getTable1().getOrAdd(999L);
			Assert.assertEquals(new Vector2(1, 2), v.getVector2());
			App.getInstance().demo_Module1.getTable1().remove(999L);
			return 0;
		}, "testVector3").Call();
	}
}
