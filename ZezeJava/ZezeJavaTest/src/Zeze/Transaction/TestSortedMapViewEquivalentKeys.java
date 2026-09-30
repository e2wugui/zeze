package Zeze.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import Zeze.Transaction.Collections.PSortedMap1;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestSortedMapViewEquivalentKeys {
	private static PSortedMap1<BigDecimal, Integer> newMap() {
		var map = new PSortedMap1<BigDecimal, Integer>(BigDecimal.class, Integer.class);
		map.put(new BigDecimal("1.0"), 10);
		map.put(new BigDecimal("2.0"), 20);
		return map;
	}

	@Test
	public void keyViewUsesMapEquivalenceForMembershipAndRemoval() {
		var map = newMap();
		var key = new BigDecimal("1.00");
		assertTrue(map.containsKey(key));
		assertTrue(map.keySet().contains(key));
		assertTrue(map.keySet().remove(key));
		assertFalse(map.containsKey(key));
		assertEquals(20, map.get(new BigDecimal("2")));
	}

	@Test
	public void entryViewUsesEquivalentKeyAndExactValue() {
		var map = newMap();
		var present = Map.entry(new BigDecimal("1.00"), 10);
		var differentValue = Map.entry(new BigDecimal("1.00"), 11);
		assertTrue(map.entrySet().contains(present));
		assertFalse(map.entrySet().contains(differentValue));
		assertFalse(map.entrySet().remove(differentValue));
		assertTrue(map.entrySet().remove(present));
		assertFalse(map.containsKey(new BigDecimal("1")));
	}

	@Test
	public void bulkViewsDoNotDependOnArgumentSizeOrKeyScale() {
		var map = newMap();
		// 与目标同样大：AbstractSet默认实现会转为在参数Collection中按equals查找。
		assertTrue(map.keySet().removeAll(List.of(new BigDecimal("1.00"), new BigDecimal("9"))));
		assertFalse(map.containsKey(new BigDecimal("1")));
		assertTrue(map.keySet().retainAll(List.of(new BigDecimal("9"))));
		assertTrue(map.isEmpty());
		map = newMap();
		assertTrue(map.keySet().retainAll(List.of(new BigDecimal("1.000"))));
		assertEquals(10, map.get(new BigDecimal("1")));
		assertEquals(1, map.size());

		map = newMap();
		assertTrue(map.entrySet().removeAll(List.of(
				Map.entry(new BigDecimal("1.00"), 10), Map.entry(new BigDecimal("9"), 90))));
		assertFalse(map.containsKey(new BigDecimal("1")));
		map = newMap();
		assertTrue(map.entrySet().retainAll(List.of(Map.entry(new BigDecimal("1.000"), 10))));
		assertEquals(10, map.get(new BigDecimal("1")));
		assertEquals(1, map.size());
		assertTrue(map.entrySet().retainAll(List.of(Map.entry(new BigDecimal("1.00"), 11))));
		assertTrue(map.isEmpty());
	}
}
