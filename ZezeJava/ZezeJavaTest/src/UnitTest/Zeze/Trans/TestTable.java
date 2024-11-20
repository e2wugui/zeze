package UnitTest.Zeze.Trans;

import demo.App;
import demo.Module1.BValue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import Zeze.Transaction.Procedure;

@SuppressWarnings("DataFlowIssue")
public class TestTable {
	@Before
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	@Test
	public final void TestUpdate() {
		demo.App.getInstance().Zeze.newProcedure(() -> {
			demo.App.getInstance().demo_Module1.getTable1().remove(1L);
			demo.App.getInstance().demo_Module1.getTable2().remove(new demo.Module1.Key((short)1, ""));
			return Procedure.Success;
		}, "RemoveDataFirst").call();

		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestTable::ProcGetOrAdd, "ProcGetOrAdd").call());
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestTable::ProcGetUpdate, "ProcGetUpdate").call());
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestTable::ProcGetUpdateCheckRemove, "ProcGetUpdateCheckRemove").call());
	}

	private static long ProcGetUpdate() {
		BValue v = demo.App.getInstance().demo_Module1.getTable1().get(1L);

		v.setInt_1(11);
		v.setLong2(22);
		v.setString3("33");
		v.setBool4(true);
		v.setShort5((short)55);
		v.setFloat6(66);
		v.setDouble7(77);
		v.getList9().add(new demo.Bean1());
		v.getSet10().add(1010);
		v.getMap11().put(2L, new demo.Module2.BValue());
		v.getBean12().setInt_1(1212);
		v.setByte13((byte)131);
		return Procedure.Success;
	}

	private static long ProcGetUpdateCheckRemove() {
		BValue v = demo.App.getInstance().demo_Module1.getTable1().get(1L);

		Assert.assertEquals(11, v.getInt_1());
		Assert.assertEquals(22, v.getLong2());
		Assert.assertEquals("33", v.getString3());
		Assert.assertTrue(v.isBool4());
		Assert.assertEquals(55, v.getShort5());
		Assert.assertEquals(66, v.getFloat6(), 0.001);
		Assert.assertEquals(77, v.getDouble7(), 0.001);
		Assert.assertEquals(2, v.getList9().size());
		Assert.assertTrue(v.getSet10().contains(10));
		Assert.assertTrue(v.getSet10().contains(1010));
		Assert.assertEquals(2, v.getSet10().size());
		Assert.assertEquals(2, v.getMap11().size());
		Assert.assertEquals(1212, v.getBean12().getInt_1());
		Assert.assertEquals((byte)131, v.getByte13());
		return Procedure.Success;
	}

	@Test
	public final void testGetOrAdd() {
		demo.App.getInstance().Zeze.newProcedure(() -> {
			demo.App.getInstance().demo_Module1.getTable1().remove(1L);
			demo.App.getInstance().demo_Module1.getTable2().remove(new demo.Module1.Key((short)1, ""));
			return Procedure.Success;
		}, "RemoveDataFirst").call();

		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestTable::ProcGetOrAdd, "ProcGetOrAdd").call());
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestTable::ProcGetOrAddCheckAndRemove, "ProcGetOrAddCheckAndRemove").call());
	}

	private static long ProcGetOrAdd() {
		BValue v = demo.App.getInstance().demo_Module1.getTable1().getOrAdd((long)1);
		v.setInt_1(1);
		v.setLong2(2);
		v.setString3("3");
		v.setBool4(true);
		v.setShort5((short)5);
		v.setFloat6(6);
		v.setDouble7(7);
		v.getList9().add(new demo.Bean1());
		v.getSet10().add(10);
		v.getMap11().put(1L, new demo.Module2.BValue());
		v.getBean12().setInt_1(12);
		v.setByte13((byte)13);

		return Procedure.Success;
	}

	private static long ProcGetOrAddCheckAndRemove() {
		var v = demo.App.getInstance().demo_Module1.getTable1().get(1L);
		Assert.assertNotNull(v);

		Assert.assertEquals(1, v.getInt_1());
		Assert.assertEquals(2, v.getLong2());
		Assert.assertEquals("3", v.getString3());
		Assert.assertTrue(v.isBool4());
		Assert.assertEquals(5, v.getShort5());
		Assert.assertEquals(6, v.getFloat6(), 0.001);
		Assert.assertEquals(7, v.getDouble7(), 0.001);
		Assert.assertEquals(1, v.getList9().size());
		Assert.assertTrue(v.getSet10().contains(10));
		Assert.assertEquals(1, v.getSet10().size());
		Assert.assertEquals(1, v.getMap11().size());
		Assert.assertEquals(12, v.getBean12().getInt_1());
		Assert.assertEquals(13, v.getByte13());

		demo.App.getInstance().demo_Module1.getTable1().remove(1L);
		Assert.assertNull(App.getInstance().demo_Module1.getTable1().get(1L));
		return Procedure.Success;
	}

	@Test
	public final void test1TableGetPut() {
		demo.App.getInstance().Zeze.newProcedure(() -> {
			demo.App.getInstance().demo_Module1.getTable1().remove(1L);
			demo.App.getInstance().demo_Module1.getTable2().remove(new demo.Module1.Key((short)1, ""));
			return Procedure.Success;
		}, "RemoveDataFirst").call();

		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestTable::ProcGet11, "ProcGet11").call());
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestTable::ProcGet12, "ProcGet12").call());
	}

	@Test
	public final void test2TableGetPut() {
		demo.App.getInstance().Zeze.newProcedure(() -> {
			demo.App.getInstance().demo_Module1.getTable1().remove(1L);
			demo.App.getInstance().demo_Module1.getTable2().remove(new demo.Module1.Key((short)1, ""));
			return Procedure.Success;
		}, "RemoveDataFirst").call();

		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestTable::ProcGet21, "ProcGet21").call());
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestTable::ProcGet22, "ProcGet22").call());
	}

	private static long ProcGet21() {
		ProcGet11();
		demo.Module1.Key key = new demo.Module1.Key((short)1, "");
		Assert.assertNull(App.getInstance().demo_Module1.getTable2().get(key));
		BValue v = new BValue();
		v.setInt_1(1);
		v.setLong2(2);
		v.setString3("3");
		v.setBool4(true);
		v.setShort5((short)5);
		v.setFloat6(6);
		v.setDouble7(7);
		v.getList9().add(new demo.Bean1());
		v.getSet10().add(10);
		v.getMap11().put(1L, new demo.Module2.BValue());
		v.getBean12().setInt_1(12);
		v.setByte13((byte)13);

		demo.App.getInstance().demo_Module1.getTable2().put(key, v);
		Assert.assertTrue(v.isManaged());
		Assert.assertEquals(v, demo.App.getInstance().demo_Module1.getTable2().get(key));
		return Procedure.Success;
	}

	private static long ProcGet22() {
		ProcGet12();
		demo.Module1.Key key = new demo.Module1.Key((short)1, "");
		var v = demo.App.getInstance().demo_Module1.getTable2().get(key);
		Assert.assertNotNull(v);

		Assert.assertEquals(1, v.getInt_1());
		Assert.assertEquals(2, v.getLong2());
		Assert.assertEquals("3", v.getString3());
		Assert.assertTrue(v.isBool4());
		Assert.assertEquals(5, v.getShort5());
		Assert.assertEquals(6, v.getFloat6(), 0.001);
		Assert.assertEquals(7, v.getDouble7(), 0.001);
		Assert.assertEquals(1, v.getList9().size());
		Assert.assertTrue(v.getSet10().contains(10));
		Assert.assertEquals(1, v.getSet10().size());
		Assert.assertEquals(1, v.getMap11().size());
		Assert.assertEquals(12, v.getBean12().getInt_1());
		Assert.assertEquals(13, v.getByte13());

		demo.App.getInstance().demo_Module1.getTable2().remove(key);
		Assert.assertNull(App.getInstance().demo_Module1.getTable2().get(key));
		return Procedure.Success;
	}

	private static long ProcGet11() {
		Assert.assertNull(App.getInstance().demo_Module1.getTable1().get(1L));
		BValue v = new BValue();
		v.setInt_1(1);
		v.setLong2(2);
		v.setString3("3");
		v.setBool4(true);
		v.setShort5((short)5);
		v.setFloat6(6);
		v.setDouble7(7);
		v.getList9().add(new demo.Bean1());
		v.getSet10().add(10);
		v.getMap11().put(1L, new demo.Module2.BValue());
		v.getBean12().setInt_1(12);
		v.setByte13((byte)13);

		demo.App.getInstance().demo_Module1.getTable1().put(1L, v);
		Assert.assertEquals(v, demo.App.getInstance().demo_Module1.getTable1().get(1L));
		return Procedure.Success;
	}

	private static long ProcGet12() {
		var v = demo.App.getInstance().demo_Module1.getTable1().get(1L);
		Assert.assertNotNull(v);

		Assert.assertEquals(1, v.getInt_1());
		Assert.assertEquals(2, v.getLong2());
		Assert.assertEquals("3", v.getString3());
		Assert.assertTrue(v.isBool4());
		Assert.assertEquals(5, v.getShort5());
		Assert.assertEquals(6, v.getFloat6(), 0.001);
		Assert.assertEquals(7, v.getDouble7(), 0.001);
		Assert.assertEquals(1, v.getList9().size());
		Assert.assertTrue(v.getSet10().contains(10));
		Assert.assertEquals(1, v.getSet10().size());
		Assert.assertEquals(1, v.getMap11().size());
		Assert.assertEquals(12, v.getBean12().getInt_1());
		Assert.assertEquals(13, v.getByte13());

		demo.App.getInstance().demo_Module1.getTable1().remove(1L);
		Assert.assertNull(App.getInstance().demo_Module1.getTable1().get(1L));
		return Procedure.Success;
	}
}
