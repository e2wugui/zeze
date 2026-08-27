package UnitTest.Zeze.Trans;

import java.util.Comparator;
import Zeze.Serialize.Vector2;
import Zeze.Transaction.Procedure;
import Zeze.Util.Random;
import demo.App;
import demo.Bean1;
import demo.Module1.BSimple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestProcedure {
	@BeforeEach
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@AfterEach
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	@Test
	public final void testVector() {
		App.getInstance().Zeze.newProcedure(() -> {
			var v = App.getInstance().demo_Module1.getTable1().getOrAdd(999L);
			v.setVector2(new Vector2(1, 2));
			Assertions.assertEquals(new Vector2(1, 2), v.getVector2());
			return 0;
		}, "testVector1").call();

		App.getInstance().Zeze.newProcedure(() -> {
			var v = App.getInstance().demo_Module1.getTable1().getOrAdd(999L);
			Assertions.assertEquals(new Vector2(1, 2), v.getVector2());
			v.setVector2(new Vector2(3, 4));
			Assertions.assertEquals(new Vector2(3, 4), v.getVector2());
			return Procedure.LogicError;
		}, "testVector2").call();

		App.getInstance().Zeze.newProcedure(() -> {
			var v = App.getInstance().demo_Module1.getTable1().getOrAdd(999L);
			Assertions.assertEquals(new Vector2(1, 2), v.getVector2());
			App.getInstance().demo_Module1.getTable1().remove(999L);
			return 0;
		}, "testVector3").call();
	}

	@Test
	public void testNestLogOneLogDynamic() {
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var value = App.Instance.demo_Module1.getTable1().getOrAdd(18989L);
			value.setBean12(new BSimple());
			value.getDynamic14().setBean(new BSimple());
			value.getSet10().add(1);
			value.getMap15().put(1L, 1L);
			value.getList9().add(new demo.Bean1());
			Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
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
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var value = App.Instance.demo_Module1.getTable1().getOrAdd(18990L);
			value.getList9().clear();
			for (int i = 0; i < 10; i++) {
				var b1 = new Bean1();
				b1.setV1(Random.getInstance().nextInt());
				value.getList9().add(b1);
			}
			return 0;
		}, "testSortList1").call());

		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var value = App.Instance.demo_Module1.getTable1().getOrAdd(18990L);
			value.getList9().sort(Comparator.comparingInt(Bean1::getV1));
			return 0;
		}, "testSortList2").call());

		var b = App.Instance.demo_Module1.getTable1().selectDirty(18990L);
		Assertions.assertNotNull(b);
		var last = Integer.MIN_VALUE;
		for (Bean1 b1 : b.getList9()) {
			// System.out.println(b1.getV1());
			Assertions.assertTrue(last <= b1.getV1());
			last = b1.getV1();
		}
		// System.out.println("OK");
	}
}
