package RelationalMapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import Zeze.Schemas;
import org.junit.Assert;
import org.junit.Test;

public class TestRelationalTableDiff {
	private static void addColumn(ArrayList<Schemas.Column> columns, int id) {
		addColumn(columns, id, String.valueOf(id));
	}

	private static void addColumn(ArrayList<Schemas.Column> columns, int id, String name) {
		var variable = new Schemas.Variable();
		variable.id = id;
		variable.type = new Schemas.Type();
		variable.type.name = "int";
		var col = new Schemas.Column(name, new int[] { id }, variable, "");
		columns.add(col);
	}

	private static Set<Integer> varialbleIds(ArrayList<Schemas.Column> columns) {
		var result = new HashSet<Integer>();
		for (var column : columns)
			result.add(column.variableId);
		return result;
	}

	@Test
	public void testDiff() {
		// 【警告！这个测试构造的Column是不完整的】
		{
			var r = new Schemas.RelationalTable("table1");
			r.diff();
			Assert.assertTrue(r.add.isEmpty());
			Assert.assertTrue(r.remove.isEmpty());
			Assert.assertTrue(r.change.isEmpty());
		}
		{
			var r = new Schemas.RelationalTable("table2");
			addColumn(r.current, 1);
			addColumn(r.current, 2);
			addColumn(r.current, 3);
			var comparator = new Schemas.ColumnComparator();
			r.current.sort(comparator);

			addColumn(r.previous, 1);
			addColumn(r.previous, 2);
			addColumn(r.previous, 3);
			r.previous.sort(comparator);

			r.diff();
			Assert.assertTrue(r.add.isEmpty());
			Assert.assertTrue(r.remove.isEmpty());
			Assert.assertTrue(r.change.isEmpty());
		}
		{
			var r = new Schemas.RelationalTable("table3");
			addColumn(r.current, 1);
			addColumn(r.current, 3);
			addColumn(r.current, 4);
			addColumn(r.current, 5);
			addColumn(r.current, 6);
			var comparator = new Schemas.ColumnComparator();
			r.current.sort(comparator);

			addColumn(r.previous, 1);
			addColumn(r.previous, 2);
			addColumn(r.previous, 4, "44");
			addColumn(r.previous, 5);
			addColumn(r.previous, 7);
			r.previous.sort(comparator);

			r.diff();

			System.out.println(r.add);
			System.out.println(r.remove);
			System.out.println(r.change);

			Assert.assertEquals(Set.of(3, 6), varialbleIds(r.add));
			Assert.assertEquals(Set.of(2, 7), varialbleIds(r.remove));
			Assert.assertEquals(Set.of(4), varialbleIds(r.change));
		}
		{
			var r = new Schemas.RelationalTable("table4");
			addColumn(r.current, 1);
			addColumn(r.current, 3);
			addColumn(r.current, 4);
			addColumn(r.current, 5);
			addColumn(r.current, 8);
			var comparator = new Schemas.ColumnComparator();
			r.current.sort(comparator);

			addColumn(r.previous, 1);
			addColumn(r.previous, 2);
			addColumn(r.previous, 4, "44");
			addColumn(r.previous, 5);
			addColumn(r.previous, 7);
			r.previous.sort(comparator);

			r.diff();

			System.out.println(r.add);
			System.out.println(r.remove);
			System.out.println(r.change);

			Assert.assertEquals(Set.of(3, 8), varialbleIds(r.add));
			Assert.assertEquals(Set.of(2, 7), varialbleIds(r.remove));
			Assert.assertEquals(Set.of(4), varialbleIds(r.change));
		}
	}
}
