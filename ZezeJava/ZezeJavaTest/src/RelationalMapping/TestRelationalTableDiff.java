package RelationalMapping;

import java.util.Set;
import java.util.TreeMap;
import Zeze.Schemas;
import org.junit.Assert;
import org.junit.Test;

public class TestRelationalTableDiff {
	private static void addColumn(TreeMap<Integer, Schemas.Column> columns, int id) {
		addColumn(columns, id, String.valueOf(id));
	}

	private static void addColumn(TreeMap<Integer, Schemas.Column> columns, int id, String name) {
		var variable = new Schemas.Variable();
		variable.id = id;
		var col = new Schemas.Column(name, null, null, variable, "");
		columns.put(id, col);
	}

	@Test
	public void testDiff() {
		{
			var r = new Schemas.RelationalTable();
			r.diff();
			Assert.assertTrue(r.add.isEmpty());
			Assert.assertTrue(r.remove.isEmpty());
			Assert.assertTrue(r.change.isEmpty());
		}
		{
			var r = new Schemas.RelationalTable();
			addColumn(r.current, 1);
			addColumn(r.current, 2);
			addColumn(r.current, 3);

			addColumn(r.previous, 1);
			addColumn(r.previous, 2);
			addColumn(r.previous, 3);
			r.diff();
			Assert.assertTrue(r.add.isEmpty());
			Assert.assertTrue(r.remove.isEmpty());
			Assert.assertTrue(r.change.isEmpty());
		}
		{
			var r = new Schemas.RelationalTable();
			addColumn(r.current, 1);
			addColumn(r.current, 3);
			addColumn(r.current, 4);
			addColumn(r.current, 5);
			addColumn(r.current, 6);

			addColumn(r.previous, 1);
			addColumn(r.previous, 2);
			addColumn(r.previous, 4, "44");
			addColumn(r.previous, 5);
			addColumn(r.previous, 7);
			r.diff();

			System.out.println(r.add);
			System.out.println(r.remove);
			System.out.println(r.change);

			Assert.assertEquals(Set.of(3, 6), r.add.keySet());
			Assert.assertEquals(Set.of(2, 7), r.remove.keySet());
			Assert.assertEquals(Set.of(4), r.change.keySet());
		}
		{
			var r = new Schemas.RelationalTable();
			addColumn(r.current, 1);
			addColumn(r.current, 3);
			addColumn(r.current, 4);
			addColumn(r.current, 5);
			addColumn(r.current, 8);

			addColumn(r.previous, 1);
			addColumn(r.previous, 2);
			addColumn(r.previous, 4, "44");
			addColumn(r.previous, 5);
			addColumn(r.previous, 7);
			r.diff();

			System.out.println(r.add);
			System.out.println(r.remove);
			System.out.println(r.change);

			Assert.assertEquals(Set.of(3, 8), r.add.keySet());
			Assert.assertEquals(Set.of(2, 7), r.remove.keySet());
			Assert.assertEquals(Set.of(4), r.change.keySet());
		}
	}
}
