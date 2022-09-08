package UnitTest.Zeze.Trans;

import demo.App;
import demo.Module1.BValue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import Zeze.Transaction.Procedure;

public class TestTable {
	@Before
	public final void testInit() throws Throwable {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Throwable {
		demo.App.getInstance().Stop();
	}

	@Test
	public final void TestUpdate() throws Throwable {
		demo.App.getInstance().Zeze.NewProcedure(() -> {
			demo.App.getInstance().demo_Module1.getTable1().remove(1L);
			demo.App.getInstance().demo_Module1.getTable2().remove(new demo.Module1.Key((short)1));
			return Procedure.Success;
		}, "RemoveDataFirst").Call();

		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.NewProcedure(TestTable::ProcGetOrAdd, "ProcGetOrAdd").Call());
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.NewProcedure(TestTable::ProcGetUpdate, "ProcGetUpdate").Call());
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.NewProcedure(TestTable::ProcGetUpdateCheckRemove, "ProcGetUpdateCheckRemove").Call());
	}

	private static long ProcGetUpdate() {
		BValue v = demo.App.getInstance().demo_Module1.getTable1().get(1L);

		v.setInt1(11);
		v.setLong2(22);
		v.setString3("33");
		v.setBool4(true);
		v.setShort5((short)55);
		v.setFloat6(66);
		v.setDouble7(77);
		v.getList9().add(new demo.Bean1());
		v.getSet10().add(1010);
		v.getMap11().put(2L, new demo.Module2.BValue());
		v.getBean12().setInt1(1212);
		v.setByte13((byte)131);
		return Procedure.Success;
	}

	private static long ProcGetUpdateCheckRemove() {
		BValue v = demo.App.getInstance().demo_Module1.getTable1().get(1L);

		Assert.assertEquals(v.getInt1(), 11);
		Assert.assertEquals(v.getLong2(), 22);
		Assert.assertEquals(v.getString3(), "33");
		Assert.assertTrue(v.isBool4());
		Assert.assertEquals(v.getShort5(), 55);
		Assert.assertEquals(v.getFloat6(), 66, 0.001);
		Assert.assertEquals(v.getDouble7(), 77, 0.001);
		Assert.assertEquals(v.getList9().size(), 2);
		Assert.assertTrue(v.getSet10().contains(10));
		Assert.assertTrue(v.getSet10().contains(1010));
		Assert.assertEquals(v.getSet10().size(), 2);
		Assert.assertEquals(v.getMap11().size(), 2);
		Assert.assertEquals(v.getBean12().getInt1(), 1212);
		Assert.assertEquals(v.getByte13(), (byte)131);
		return Procedure.Success;
	}

	@Test
	public final void testGetOrAdd() throws Throwable {
		demo.App.getInstance().Zeze.NewProcedure(() -> {
			demo.App.getInstance().demo_Module1.getTable1().remove(1L);
			demo.App.getInstance().demo_Module1.getTable2().remove(new demo.Module1.Key((short)1));
			return Procedure.Success;
		}, "RemoveDataFirst").Call();

		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.NewProcedure(TestTable::ProcGetOrAdd, "ProcGetOrAdd").Call());
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.NewProcedure(TestTable::ProcGetOrAddCheckAndRemove, "ProcGetOrAddCheckAndRemove").Call());
	}

	private static long ProcGetOrAdd() {
		BValue v = demo.App.getInstance().demo_Module1.getTable1().getOrAdd((long)1);
		v.setInt1(1);
		v.setLong2(2);
		v.setString3("3");
		v.setBool4(true);
		v.setShort5((short)5);
		v.setFloat6(6);
		v.setDouble7(7);
		v.getList9().add(new demo.Bean1());
		v.getSet10().add(10);
		v.getMap11().put(1L, new demo.Module2.BValue());
		v.getBean12().setInt1(12);
		v.setByte13((byte)13);

		return Procedure.Success;
	}

	private static long ProcGetOrAddCheckAndRemove() {
		var v = demo.App.getInstance().demo_Module1.getTable1().get(1L);
		Assert.assertNotNull(v);

		Assert.assertEquals(v.getInt1(), 1);
		Assert.assertEquals(v.getLong2(), 2);
		Assert.assertEquals(v.getString3(), "3");
		Assert.assertTrue(v.isBool4());
		Assert.assertEquals(v.getShort5(), 5);
		Assert.assertEquals(v.getFloat6(), 6, 0.001);
		Assert.assertEquals(v.getDouble7(), 7, 0.001);
		Assert.assertEquals(v.getList9().size(), 1);
		Assert.assertTrue(v.getSet10().contains(10));
		Assert.assertEquals(v.getSet10().size(), 1);
		Assert.assertEquals(v.getMap11().size(), 1);
		Assert.assertEquals(v.getBean12().getInt1(), 12);
		Assert.assertEquals(v.getByte13(), 13);

		demo.App.getInstance().demo_Module1.getTable1().remove(1L);
		Assert.assertNull(App.getInstance().demo_Module1.getTable1().get(1L));
		return Procedure.Success;
	}

	@Test
	public final void test1TableGetPut() throws Throwable {
		demo.App.getInstance().Zeze.NewProcedure(() -> {
			demo.App.getInstance().demo_Module1.getTable1().remove(1L);
			demo.App.getInstance().demo_Module1.getTable2().remove(new demo.Module1.Key((short)1));
			return Procedure.Success;
		}, "RemoveDataFirst").Call();

		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.NewProcedure(TestTable::ProcGet11, "ProcGet11").Call());
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.NewProcedure(TestTable::ProcGet12, "ProcGet12").Call());
	}

	@Test
	public final void test2TableGetPut() throws Throwable {
		demo.App.getInstance().Zeze.NewProcedure(() -> {
			demo.App.getInstance().demo_Module1.getTable1().remove(1L);
			demo.App.getInstance().demo_Module1.getTable2().remove(new demo.Module1.Key((short)1));
			return Procedure.Success;
		}, "RemoveDataFirst").Call();

		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.NewProcedure(TestTable::ProcGet21, "ProcGet21").Call());
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.NewProcedure(TestTable::ProcGet22, "ProcGet22").Call());
	}

	private static long ProcGet21() {
		ProcGet11();
		demo.Module1.Key key = new demo.Module1.Key((short)1);
		Assert.assertNull(App.getInstance().demo_Module1.getTable2().get(key));
		BValue v = new BValue();
		v.setInt1(1);
		v.setLong2(2);
		v.setString3("3");
		v.setBool4(true);
		v.setShort5((short)5);
		v.setFloat6(6);
		v.setDouble7(7);
		v.getList9().add(new demo.Bean1());
		v.getSet10().add(10);
		v.getMap11().put(1L, new demo.Module2.BValue());
		v.getBean12().setInt1(12);
		v.setByte13((byte)13);

		demo.App.getInstance().demo_Module1.getTable2().put(key, v);
		Assert.assertTrue(v.isManaged());
		Assert.assertEquals(v, demo.App.getInstance().demo_Module1.getTable2().get(key));
		return Procedure.Success;
	}

	private static long ProcGet22() {
		ProcGet12();
		demo.Module1.Key key = new demo.Module1.Key((short)1);
		var v = demo.App.getInstance().demo_Module1.getTable2().get(key);
		Assert.assertNotNull(v);

		Assert.assertEquals(v.getInt1(), 1);
		Assert.assertEquals(v.getLong2(), 2);
		Assert.assertEquals(v.getString3(), "3");
		Assert.assertTrue(v.isBool4());
		Assert.assertEquals(v.getShort5(), 5);
		Assert.assertEquals(v.getFloat6(), 6, 0.001);
		Assert.assertEquals(v.getDouble7(), 7, 0.001);
		Assert.assertEquals(v.getList9().size(), 1);
		Assert.assertTrue(v.getSet10().contains(10));
		Assert.assertEquals(v.getSet10().size(), 1);
		Assert.assertEquals(v.getMap11().size(), 1);
		Assert.assertEquals(v.getBean12().getInt1(), 12);
		Assert.assertEquals(v.getByte13(), 13);

		demo.App.getInstance().demo_Module1.getTable2().remove(key);
		Assert.assertNull(App.getInstance().demo_Module1.getTable2().get(key));
		return Procedure.Success;
	}

	private static long ProcGet11() {
		Assert.assertNull(App.getInstance().demo_Module1.getTable1().get(1L));
		BValue v = new BValue();
		v.setInt1(1);
		v.setLong2(2);
		v.setString3("3");
		v.setBool4(true);
		v.setShort5((short)5);
		v.setFloat6(6);
		v.setDouble7(7);
		v.getList9().add(new demo.Bean1());
		v.getSet10().add(10);
		v.getMap11().put(1L, new demo.Module2.BValue());
		v.getBean12().setInt1(12);
		v.setByte13((byte)13);

		demo.App.getInstance().demo_Module1.getTable1().put(1L, v);
		Assert.assertEquals(v, demo.App.getInstance().demo_Module1.getTable1().get(1L));
		return Procedure.Success;
	}

	private static long ProcGet12() {
		var v = demo.App.getInstance().demo_Module1.getTable1().get(1L);
		Assert.assertNotNull(v);

		Assert.assertEquals(v.getInt1(), 1);
		Assert.assertEquals(v.getLong2(), 2);
		Assert.assertEquals(v.getString3(), "3");
		Assert.assertTrue(v.isBool4());
		Assert.assertEquals(v.getShort5(), 5);
		Assert.assertEquals(v.getFloat6(), 6, 0.001);
		Assert.assertEquals(v.getDouble7(), 7, 0.001);
		Assert.assertEquals(v.getList9().size(), 1);
		Assert.assertTrue(v.getSet10().contains(10));
		Assert.assertEquals(v.getSet10().size(), 1);
		Assert.assertEquals(v.getMap11().size(), 1);
		Assert.assertEquals(v.getBean12().getInt1(), 12);
		Assert.assertEquals(v.getByte13(), 13);

		demo.App.getInstance().demo_Module1.getTable1().remove(1L);
		Assert.assertNull(App.getInstance().demo_Module1.getTable1().get(1L));
		return Procedure.Success;
	}
}
