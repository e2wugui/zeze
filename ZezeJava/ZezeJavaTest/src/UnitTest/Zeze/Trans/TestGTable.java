package UnitTest.Zeze.Trans;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Transaction.Procedure;
import Zeze.Util.Json;
import demo.App;
import demo.Bean1ReadOnly;
import demo.ModuleGTable.Bean1;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestGTable {
	@Before
	public void before() throws Exception {
		App.getInstance().Start();
	}

	@Test
	public void testGTable1Basic() {
		App.Instance.Zeze.newProcedure(() -> {
			var table = App.getInstance().demo_ModuleGTable.getGTable();
			table.remove(1L);
			return 0;
		}, "Remove Record").call();
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
		}, "putGTableRollback").call();
		// check 1 exist and 2 null
		App.getInstance().Zeze.newProcedure(() -> {
			var table = App.getInstance().demo_ModuleGTable.getGTable();
			var gTable1 = table.getOrAdd(1L);
			Assert.assertNotNull(gTable1.getGTable().get(1, 1));
			Assert.assertNull(gTable1.getGTable().get(2, 2));
			return 0; // readonly and commit
		}, "GTableCheck").call();
	}

	@Test
	public void testGTable2Basic() {
		App.Instance.Zeze.newProcedure(() -> {
			var table = App.getInstance().demo_ModuleGTable.getGTable2();
			table.remove(1L);
			return 0;
		}, "Remove Record").call();
		// putGTable
		App.getInstance().Zeze.newProcedure(() -> {
			var table = App.getInstance().demo_ModuleGTable.getGTable2();
			var gTable1 = table.getOrAdd(1L);
			Assert.assertNull(gTable1.getGTable().get(1, 1));
			gTable1.getGTable().put(1, 1, new Bean1());
			Assert.assertEquals(new Bean1(), gTable1.getGTable().get(1, 1));
			return 0;
		}, "putGTable").call();
		// check and put 2 and rollback
		App.getInstance().Zeze.newProcedure(() -> {
			var table = App.getInstance().demo_ModuleGTable.getGTable2();
			var gTable1 = table.getOrAdd(1L);
			Assert.assertNull(gTable1.getGTable().get(2, 2));
			gTable1.getGTable().put(2, 2, new Bean1());
			Assert.assertEquals(new Bean1(), gTable1.getGTable().get(2, 2));
			return Procedure.LogicError; // rollback
		}, "putGTableRollback").call();
		// check 1 exist and 2 null
		App.getInstance().Zeze.newProcedure(() -> {
			var table = App.getInstance().demo_ModuleGTable.getGTable2();
			var gTable1 = table.getOrAdd(1L);
			Assert.assertNotNull(gTable1.getGTable().get(1, 1));
			Assert.assertNull(gTable1.getGTable().get(2, 2));
			return 0; // readonly and commit
		}, "checkGTable").call();
		App.getInstance().Zeze.newProcedure(() -> {
			var table = App.getInstance().demo_ModuleGTable.getGTable2();
			var gTable1 = table.getOrAdd(1L);
			var bean1 = gTable1.getGTable().get(1, 1);
			Assert.assertNotNull(bean1);
			bean1.setIntVar(123);
			return Procedure.LogicError; // rollback
		}, "setGTableBeanRollback").call();
		App.getInstance().Zeze.newProcedure(() -> {
			var table = App.getInstance().demo_ModuleGTable.getGTable2();
			var gTable1 = table.getOrAdd(1L);
			var bean1 = gTable1.getGTable().get(1, 1);
			Assert.assertNotNull(bean1);
			Assert.assertEquals(0, bean1.getIntVar());
			return 0; // rollback
		}, "setGTableBeanRollbackCheck").call();
	}

	static class C1 {
		final GTable1<Integer, Long, Float> g = new GTable1<>(Integer.class, Long.class, Float.class);
	}

	static class C2 {
		final GTable2<Integer, Long, Bean1, Bean1ReadOnly> g = new GTable2<>(Integer.class, Long.class, Bean1.class);
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
		var b = new Bean1();
		b.setIntVar(3);
		g.put(1, 2L, b);
		var s = Json.toCompactString(c);
		System.out.println(s);
		g.clear();
		Json.parse(s, c);
		System.out.println(g);
	}

	@Test
	public void testGTableRowCol() {
		App.Instance.Zeze.newProcedure(() -> {
			var table = App.getInstance().demo_ModuleGTable.getGTable();
			table.remove(1L);
			return 0;
		}, "Remove Record").call();
		App.getInstance().Zeze.newProcedure(() -> {
			var table = App.getInstance().demo_ModuleGTable.getGTable();
			var gTable1 = table.getOrAdd(1L);
			gTable1.getGTable().put(1, 1, 11);
			gTable1.getGTable().put(1, 2, 12);
			gTable1.getGTable().put(2, 1, 21);
			gTable1.getGTable().put(2, 2, 22);

			var result = Set.of(1, 2);
			var col = gTable1.getGTable().column(1);
			var row = gTable1.getGTable().row(1);
			Assert.assertEquals(col.keySet(), result);
			Assert.assertEquals(row.keySet(), result);
			col.put(3, 31);
			row.put(3, 13);
			result = Set.of(1, 2, 3);
			Assert.assertEquals(col.keySet(), result);
			Assert.assertEquals(row.keySet(), result);
			return 0;
		}, "putEntrys").call();
	}

	public static void main(String[] args) {
		System.out.println("PROXY".getBytes(StandardCharsets.UTF_8).length);
	}
}
