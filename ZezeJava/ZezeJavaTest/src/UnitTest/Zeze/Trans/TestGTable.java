package UnitTest.Zeze.Trans;

import Zeze.Builtin.Auth.BAccountAuth;
import Zeze.Builtin.Auth.BAccountAuthReadOnly;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Transaction.Procedure;
import Zeze.Util.Json;
import demo.App;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestGTable {
	@Before
	public void before() throws Exception {
		App.getInstance().Start();
	}

	@Test
	public void testGTableBegin() {
		// putGTable
		App.getInstance().Zeze.newProcedure(() -> {
			var table = App.getInstance().demo_ModuleGTable.getGTable();
			var gTable1 = table.getOrAdd(1L);
			Assert.assertNull(gTable1.getGTable().get(1, 1));
			gTable1.getGTable().put(1, 1, 1);
			Assert.assertEquals(Integer.valueOf(1), gTable1.getGTable().get(1, 1));
			return 0;
		}, "putGTable").call();
		// check and put 2 and rollback
		App.getInstance().Zeze.newProcedure(() -> {
			var table = App.getInstance().demo_ModuleGTable.getGTable();
			var gTable1 = table.getOrAdd(1L);
			Assert.assertNull(gTable1.getGTable().get(2, 2));
			gTable1.getGTable().put(2, 2, 2);
			Assert.assertEquals(Integer.valueOf(2), gTable1.getGTable().get(2, 2));
			return Procedure.LogicError; // rollback
		}, "putGTable").call();
		// check 1 exist and 2 null
		App.getInstance().Zeze.newProcedure(() -> {
			var table = App.getInstance().demo_ModuleGTable.getGTable();
			var gTable1 = table.getOrAdd(1L);
			Assert.assertNotNull(gTable1.getGTable().get(1, 1));
			Assert.assertNull(gTable1.getGTable().get(2, 2));
			return 0; // readonly and commit
		}, "putGTable").call();
	}

	static class C1 {
		GTable1<Integer, Long, Float> g = new GTable1<>(Integer.class, Long.class, Float.class);
	}

	static class C2 {
		GTable2<Integer, Long, BAccountAuth, BAccountAuthReadOnly> g = new GTable2<>(Integer.class, Long.class, BAccountAuth.class);
	}

	@Test
	public void testGTable1() {
		var c = new C1();
		var g = c.g;
		g.put(1, 2L, 3.0f);
		var s = Json.toCompactString(c);
		System.out.println(s);
		g.clear();
		Json.parse(s, c);
		System.out.println(g);
	}

	@Test
	public void testGTable2() {
		var c = new C2();
		var g = c.g;
		var b = new BAccountAuth();
		b.getRoles().add("abc");
		g.put(1, 2L, b);
		var s = Json.toCompactString(c);
		System.out.println(s);
		g.clear();
		Json.parse(s, c);
		System.out.println(g);
	}
}
