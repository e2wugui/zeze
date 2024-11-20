package UnitTest.Zeze.Trans;

import java.util.Comparator;
import UnitTest.Zeze.BMyBean;
import Zeze.Serialize.Vector2;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Record1;
import Zeze.Transaction.TableKey;
import Zeze.Util.Random;
import demo.App;
import demo.Bean1;
import demo.Module1.BSimple;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestProcedure {
	private final BMyBean bean = new BMyBean();

	public final long ProcTrue() {
		bean.setI(123);
		Assert.assertEquals(123, bean.getI());
		return Procedure.Success;
	}

	public final long ProcFalse() {
		bean.setI(456);
		Assert.assertEquals(456, bean.getI());
		return Procedure.Unknown;
	}

	public final long ProcNest() {
		Assert.assertEquals(0, bean.getI());
		bean.setI(1);
		Assert.assertEquals(1, bean.getI());
		{
			long r = demo.App.getInstance().Zeze.newProcedure(this::ProcFalse, "ProcFalse").call();
			Assert.assertNotEquals(Procedure.Success, r);
			Assert.assertEquals(1, bean.getI());
		}

		{
			long r = demo.App.getInstance().Zeze.newProcedure(this::ProcTrue, "ProcFalse").call();
			Assert.assertEquals(Procedure.Success, r);
			Assert.assertEquals(123, bean.getI());
		}

		return Procedure.Success;
	}

	@Before
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	@Test
	public final void test1() throws Exception {
		TableKey root = new TableKey(1, 1);
		// 特殊测试，拼凑一个record用来提供需要的信息。
		//noinspection DataFlowIssue
		var r = new Record1<>(null, 1L, bean);
		bean.initRootInfo(r.createRootInfoIfNeed(root), null);
		long rc = demo.App.getInstance().Zeze.newProcedure(this::ProcNest, "ProcNest").call();
		Assert.assertEquals(Procedure.Success, rc);
		// 最后一个 Call，事务外，bean 已经没法访问事务支持的属性了。直接访问内部变量。
		Assert.assertEquals(123, bean._i);
	}

	@Test
	public final void testVector() {
		App.getInstance().Zeze.newProcedure(() -> {
			var v = App.getInstance().demo_Module1.getTable1().getOrAdd(999L);
			v.setVector2(new Vector2(1, 2));
			Assert.assertEquals(new Vector2(1, 2), v.getVector2());
			return 0;
		}, "testVector1").call();

		App.getInstance().Zeze.newProcedure(() -> {
			var v = App.getInstance().demo_Module1.getTable1().getOrAdd(999L);
			Assert.assertEquals(new Vector2(1, 2), v.getVector2());
			v.setVector2(new Vector2(3, 4));
			Assert.assertEquals(new Vector2(3, 4), v.getVector2());
			return Procedure.LogicError;
		}, "testVector2").call();

		App.getInstance().Zeze.newProcedure(() -> {
			var v = App.getInstance().demo_Module1.getTable1().getOrAdd(999L);
			Assert.assertEquals(new Vector2(1, 2), v.getVector2());
			App.getInstance().demo_Module1.getTable1().remove(999L);
			return 0;
		}, "testVector3").call();
	}

	@Test
	public void testNestLogOneLogDynamic() {
		Assert.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var value = App.Instance.demo_Module1.getTable1().getOrAdd(18989L);
			value.setBean12(new BSimple());
			value.getDynamic14().setBean(new BSimple());
			value.getSet10().add(1);
			value.getMap15().put(1L, 1L);
			value.getList9().add(new demo.Bean1());
			Assert.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
				var value2 = App.Instance.demo_Module1.getTable1().getOrAdd(18989L);
				value2.setBean12(new BSimple());
				value2.getDynamic14().setBean(new BSimple());
				value2.getSet10().add(1);
				value2.getMap15().put(1L, 1L);
				value2.getList9().add(new demo.Bean1());
				return 0;
			}, "Nest").call());
			return 0;
		}, "testNestLogOneLogDynamic").call());
	}

	@Test
	public void testSortList() {
		Assert.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var value = App.Instance.demo_Module1.getTable1().getOrAdd(18990L);
			value.getList9().clear();
			for (int i = 0; i < 10; i++) {
				var b1 = new Bean1();
				b1.setV1(Random.getInstance().nextInt());
				value.getList9().add(b1);
			}
			return 0;
		}, "testSortList1").call());

		Assert.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var value = App.Instance.demo_Module1.getTable1().getOrAdd(18990L);
			value.getList9().sort(Comparator.comparingInt(Bean1::getV1));
			return 0;
		}, "testSortList2").call());

		var b = App.Instance.demo_Module1.getTable1().selectDirty(18990L);
		Assert.assertNotNull(b);
		var last = Integer.MIN_VALUE;
		for (Bean1 b1 : b.getList9()) {
			// System.out.println(b1.getV1());
			Assert.assertTrue(last <= b1.getV1());
			last = b1.getV1();
		}
		// System.out.println("OK");
	}
}
