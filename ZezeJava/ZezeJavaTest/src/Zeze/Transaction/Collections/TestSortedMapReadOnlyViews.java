package Zeze.Transaction.Collections;

import demo.Module1.BValue;
import demo.Module1.BValueReadOnly;
import harness.Fast;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pcollections.TreePMap;

@Fast
public class TestSortedMapReadOnlyViews {
	private static <K> void verifyViews(Set<K> keys, Set<? extends Map.Entry<K, ?>> entries,
			K equivalentKey, Object value) {
		Assertions.assertTrue(keys.contains(equivalentKey));
		Assertions.assertTrue(keys.containsAll(Set.of(equivalentKey)));
		Assertions.assertTrue(entries.contains(Map.entry(equivalentKey, value)));
		Assertions.assertFalse(entries.contains(Map.entry(equivalentKey, new Object())));
		Assertions.assertThrows(UnsupportedOperationException.class, () -> keys.remove(keys.iterator().next()));
		var iterator = entries.iterator();
		iterator.next();
		Assertions.assertThrows(UnsupportedOperationException.class, iterator::remove);
	}

	@Test
	public void scalarViewsUseNaturalOrderingForMembership() {
		var map = new PSortedMap1<>(BigDecimal.class, Integer.class);
		map.put(new BigDecimal("1.0"), 10);
		var view = new PSortedMap1ReadOnly<>(map);
		var equivalent = new BigDecimal("1.00");
		Assertions.assertTrue(view.containsKey(equivalent));
		verifyViews(view.keySet(), view.entrySet(), equivalent, 10);
		var keys = view.keySet();
		var entries = view.entrySet();
		map.clear();
		Assertions.assertTrue(keys.isEmpty());
		Assertions.assertTrue(entries.isEmpty());
		map.put(new BigDecimal("1.000"), 20);
		verifyViews(keys, entries, equivalent, 20);
	}

	@Test
	public void beanViewsUseNaturalOrderingForMembership() {
		var map = new PSortedMap2<>(BigDecimal.class, BValue.class);
		var value = new BValue();
		value.setInt_1(10);
		map.put(new BigDecimal("1.0"), value);
		var view = new PSortedMap2ReadOnly<BigDecimal, BValue, BValueReadOnly>(map);
		var equivalent = new BigDecimal("1.00");
		Assertions.assertSame(value, view.get(equivalent));
		verifyViews(view.keySet(), view.entrySet(), equivalent, value);
		var keys = view.keySet();
		var entries = view.entrySet();
		map.clear();
		Assertions.assertTrue(keys.isEmpty());
		Assertions.assertTrue(entries.isEmpty());
		map.put(new BigDecimal("1.000"), value);
		verifyViews(keys, entries, equivalent, value);
	}

	@Test
	public void scalarViewsKeepTheSourceComparator() {
		var map = new PSortedMap1<>(String.class, Integer.class);
		map.map = TreePMap.empty(String.CASE_INSENSITIVE_ORDER);
		map.put("Alpha", 10);
		var view = new PSortedMap1ReadOnly<>(map);
		Assertions.assertTrue(view.containsKey("ALPHA"));
		verifyViews(view.keySet(), view.entrySet(), "ALPHA", 10);
	}

	@Test
	public void beanViewsKeepTheSourceComparator() {
		var map = new PSortedMap2<>(String.class, BValue.class);
		map.map = TreePMap.empty(String.CASE_INSENSITIVE_ORDER);
		var value = new BValue();
		map.put("Alpha", value);
		var view = new PSortedMap2ReadOnly<String, BValue, BValueReadOnly>(map);
		Assertions.assertSame(value, view.get("ALPHA"));
		verifyViews(view.keySet(), view.entrySet(), "ALPHA", value);
	}
}
