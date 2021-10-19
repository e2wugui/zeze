package UnitTest.Zeze.Trans;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import Zeze.Transaction.Procedure;


public class TestTable {

	@Before
	public final void testInit() {
		demo.App.getInstance().Start();
		demo.App.getInstance().Zeze.NewProcedure(() -> {
				demo.App.getInstance().demo_Module1.getTable1().Remove(1L);
				demo.App.getInstance().demo_Module1.getTable2().Remove(new demo.Module1.Key((short)1));
				return Procedure.Success;
		}, "RemoveDataFirst", null).Call();
	}

	@After
	public final void testCleanup() {
		demo.App.getInstance().Stop();
	}

	public final void TestUpdate() {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(this::ProcGetOrAdd, "ProcGetOrAdd", null).Call();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(this::ProcGetUpdate, "ProcGetUpdate", null).Call();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(this::ProcGetUpdateCheckRemove, "ProcGetUpdateCheckRemove", null).Call();
	}

	private int ProcGetUpdate() {
		demo.Module1.Value v = demo.App.getInstance().demo_Module1.getTable1().Get(1L);

		v.setInt1(1);
		v.setLong2(22);
		v.setString3("33");
		v.setBool4(true);
		v.setShort5((short) 55);
		v.setFloat6(66);
		v.setDouble7(77);
		v.getList9().add(new demo.Bean1());
		v.getSet10().add(1010);
		v.getMap11().put(2L, new demo.Module2.Value());
		v.getBean12().setInt1(1212);
		v.setByte13((byte)131);
		return Procedure.Success;
	}

	private int ProcGetUpdateCheckRemove() {
		demo.Module1.Value v = demo.App.getInstance().demo_Module1.getTable1().Get(1L);

		assert v.getInt1() == 11;
		assert v.getLong2() == 22;
		assert v.getString3().equals("33");
		assert v.isBool4();
		assert v.getShort5() == 55;
		assert v.getFloat6() == 66;
		assert v.getDouble7() == 77;
		assert v.getList9().size() == 2;
		assert v.getSet10().contains(10);
		assert v.getSet10().contains(1010);
		assert v.getSet10().size() == 2;
		assert v.getMap11().size() == 2;
		assert v.getBean12().getInt1() == 1212;
		assert v.getByte13() == 131;
		return Procedure.Success;
	}
	@Test
	public final void testGetOrAdd() {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(this::ProcGetOrAdd, "ProcGetOrAdd", null).Call();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(this::ProcGetOrAddCheckAndRemove, "ProcGetOrAddCheckAndRemove", null).Call();
	}

	private int ProcGetOrAdd() {
		demo.Module1.Value v = demo.App.getInstance().demo_Module1.getTable1().GetOrAdd((long) 1);
		v.setInt1(1);
		v.setLong2(2);
		v.setString3("3");
		v.setBool4(true);
		v.setShort5((short) 5);
		v.setFloat6(6);
		v.setDouble7(7);
		v.getList9().add(new demo.Bean1());
		v.getSet10().add(10);
		v.getMap11().put(1L, new demo.Module2.Value());
		v.getBean12().setInt1(12);
		v.setByte13((byte)13);	

		return Procedure.Success;
	}

	private int ProcGetOrAddCheckAndRemove() {
		var v = demo.App.getInstance().demo_Module1.getTable1().Get(1L);
		assert v != null;

		assert v.getInt1() == 1;
		assert v.getLong2() == 2;
		assert v.getString3().equals("3");
		assert v.isBool4();
		assert v.getShort5() == 5;
		assert v.getFloat6() == 6;
		assert v.getDouble7() == 7;
		assert v.getList9().size() == 1;
		assert v.getSet10().contains(10);
		assert v.getSet10().size() == 1;
		assert v.getMap11().size() == 1;
		assert v.getBean12().getInt1() == 12;
		assert v.getByte13() == 13;

		demo.App.getInstance().demo_Module1.getTable1().Remove(1L);
		assert demo.App.getInstance().demo_Module1.getTable1().Get(1L) == null;
		return Procedure.Success;
	}

	@Test
	public final void test1TableGetPut() {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(this::ProcGet11, "ProcGet11", null).Call();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(this::ProcGet12, "ProcGet12", null).Call();
	}
	
	@Test
	public final void test2TableGetPut() {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(this::ProcGet21, "ProcGet21", null).Call();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(this::ProcGet22, "ProcGet22", null).Call();
	}

	private int ProcGet21() {
		ProcGet11();
		demo.Module1.Key key = new demo.Module1.Key((short)1);
		assert demo.App.getInstance().demo_Module1.getTable2().Get(key) == null;
		demo.Module1.Value v = new demo.Module1.Value();
		v.setInt1(1);
		v.setLong2(2);
		v.setString3("3");
		v.setBool4(true);
		v.setShort5((short) 5);
		v.setFloat6(6);
		v.setDouble7(7);
		v.getList9().add(new demo.Bean1());
		v.getSet10().add(10);
		v.getMap11().put(1L, new demo.Module2.Value());
		v.getBean12().setInt1(12);
		v.setByte13((byte)13);	
	

		demo.App.getInstance().demo_Module1.getTable2().Put(key, v);
		assert v == demo.App.getInstance().demo_Module1.getTable2().Get(key);
		return Procedure.Success;
	}

	private int ProcGet22() {
		ProcGet12();
		demo.Module1.Key key = new demo.Module1.Key((short)1);
		var v = demo.App.getInstance().demo_Module1.getTable2().Get(key);
		assert v != null;

		assert v.getInt1() == 1;
		assert v.getLong2() == 2;
		assert v.getString3().equals("3");
		assert v.isBool4();
		assert v.getShort5() == 5;
		assert v.getFloat6() == 6;
		assert v.getDouble7() == 7;
		assert v.getList9().size() == 1;
		assert v.getSet10().contains(10);
		assert v.getSet10().size() == 1;
		assert v.getMap11().size() == 1;
		assert v.getBean12().getInt1() == 12;
		assert v.getByte13() == 13;

		demo.App.getInstance().demo_Module1.getTable2().Remove(key);
		assert demo.App.getInstance().demo_Module1.getTable2().Get(key) == null;
		return Procedure.Success;
	}

	private int ProcGet11() {
		assert demo.App.getInstance().demo_Module1.getTable1().Get(1L) == null;
		demo.Module1.Value v = new demo.Module1.Value();
		v.setInt1(1);
		v.setLong2(2);
		v.setString3("3");
		v.setBool4(true);
		v.setShort5((short) 5);
		v.setFloat6(6);
		v.setDouble7(7);
		v.getList9().add(new demo.Bean1());
		v.getSet10().add(10);
		v.getMap11().put(1L, new demo.Module2.Value());
		v.getBean12().setInt1(12);
		v.setByte13((byte)13);	
	
		demo.App.getInstance().demo_Module1.getTable1().Put(1L, v);
		assert v == demo.App.getInstance().demo_Module1.getTable1().Get(1L);
		return Procedure.Success;
	}

	private int ProcGet12() {
		var v = demo.App.getInstance().demo_Module1.getTable1().Get(1L);
		assert v != null;

		assert v.getInt1() == 1;
		assert v.getLong2() == 2;
		assert v.getString3().equals("3");
		assert v.isBool4();
		assert v.getShort5() == 5;
		assert v.getFloat6() == 6;
		assert v.getDouble7() == 7;
		assert v.getList9().size() == 1;
		assert v.getSet10().contains(10);
		assert v.getSet10().size() == 1;
		assert v.getMap11().size() == 1;
		assert v.getBean12().getInt1() == 12;
		assert v.getByte13() == 13;

		demo.App.getInstance().demo_Module1.getTable1().Remove(1L);
		assert demo.App.getInstance().demo_Module1.getTable1().Get(1L) == null;
		return Procedure.Success;
	}
}