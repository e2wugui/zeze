package Zeze.Trans;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.NullMarked;

import demo.App;
import demo.Module1.BSimple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@NullMarked
public class TestSortedMap {
	@BeforeEach
	public void before() throws Exception {
		App.getInstance().Start();
	}

	@Test
	public void testSortedMap() {
		App.Instance.getZeze().newProcedure(
			() -> {
				requireNonNull(App.Instance.demo_Module1).getTable1().remove(9182394L);
				return 0;
			},
			"removeSortedMapRecord"
		).call();
		App.Instance.getZeze().newProcedure(
			() -> {
				var r = requireNonNull(App.Instance.demo_Module1).getTable1().getOrAdd(9182394L);
				r.getSortedmap1().put(1, 1);
				r.getSortedmap2().put(1, new BSimple());
				return 0;
			},
			"removeSortedMapRecord"
		).call();
		App.Instance.getZeze().newProcedure(
			() -> {
				var r = requireNonNull(App.Instance.demo_Module1).getTable1().getOrAdd(9182394L);
				Assertions.assertEquals(1, requireNonNull(r.getSortedmap1().get(1)).intValue());
				Assertions.assertEquals(new BSimple(), r.getSortedmap2().get(1));
				return 0;
			},
			"removeSortedMapRecord"
		).call();
		App.Instance.getZeze().newProcedure(
			() -> {
				var r = requireNonNull(App.Instance.demo_Module1).getTable1().getOrAdd(9182394L);
				r.getSortedmap1().remove(1);
				r.getSortedmap2().remove(1);
				return 0;
			},
			"removeSortedMapRecord"
		).call();
		App.Instance.getZeze().newProcedure(
			() -> {
				var r = requireNonNull(App.Instance.demo_Module1).getTable1().getOrAdd(9182394L);
				Assertions.assertTrue(r.getSortedmap1().isEmpty());
				Assertions.assertTrue(r.getSortedmap2().isEmpty());
				return 0;
			},
			"removeSortedMapRecord"
		).call();
	}
}
