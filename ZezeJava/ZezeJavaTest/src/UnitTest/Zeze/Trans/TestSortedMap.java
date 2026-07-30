package UnitTest.Zeze.Trans;

import demo.App;
import demo.Module1.BSimple;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestSortedMap {
	@Before
	public void before() throws Exception {
		App.getInstance().Start();
	}

	@Test
	public void testSortedMap() {
		App.Instance.getZeze().newProcedure(
			() -> {
				App.Instance.demo_Module1.getTable1().remove(9182394L);
				return 0;
			},
			"removeSortedMapRecord"
		).call();
		App.Instance.getZeze().newProcedure(
			() -> {
				var r = App.Instance.demo_Module1.getTable1().getOrAdd(9182394L);
				r.getSortedmap1().put(1, 1);
				r.getSortedmap2().put(1, new BSimple());
				return 0;
			},
			"removeSortedMapRecord"
		).call();
		App.Instance.getZeze().newProcedure(
			() -> {
				var r = App.Instance.demo_Module1.getTable1().getOrAdd(9182394L);
				Assert.assertEquals(1, r.getSortedmap1().get(1).intValue());
				Assert.assertEquals(new BSimple(), r.getSortedmap2().get(1));
				return 0;
			},
			"removeSortedMapRecord"
		).call();
		App.Instance.getZeze().newProcedure(
			() -> {
				var r = App.Instance.demo_Module1.getTable1().getOrAdd(9182394L);
				r.getSortedmap1().remove(1);
				r.getSortedmap2().remove(1);
				return 0;
			},
			"removeSortedMapRecord"
		).call();
		App.Instance.getZeze().newProcedure(
			() -> {
				var r = App.Instance.demo_Module1.getTable1().getOrAdd(9182394L);
				Assert.assertTrue(r.getSortedmap1().isEmpty());
				Assert.assertTrue(r.getSortedmap2().isEmpty());
				return 0;
			},
			"removeSortedMapRecord"
		).call();
	}
}
