package UnitTest.Zeze.Util;

import java.util.HashMap;
import java.util.HashSet;
import Zeze.Util.IntHashMap;
import Zeze.Util.IntHashSet;
import Zeze.Util.LongHashMap;
import Zeze.Util.LongHashSet;
import Zeze.Util.Random;
import junit.framework.TestCase;

public class TestContainers extends TestCase {
	private static void checkSet(IntHashSet m1, HashSet<Integer> m2) {
		assertEquals(m2.size(), m1.size());
		m1.foreach(k -> assertTrue(m2.contains(k)));
		for (var it = m1.iterator(); it.moveToNext(); )
			assertTrue(m2.contains(it.value()));
		m2.forEach(k -> assertTrue(m1.contains(k)));
	}

	private static void checkSet(LongHashSet m1, HashSet<Long> m2) {
		assertEquals(m2.size(), m1.size());
		m1.foreach(k -> assertTrue(m2.contains(k)));
		for (var it = m1.iterator(); it.moveToNext(); )
			assertTrue(m2.contains(it.value()));
		m2.forEach(k -> assertTrue(m1.contains(k)));
	}

	private static void checkMap(IntHashMap<Integer> m1, HashMap<Integer, Integer> m2) {
		assertEquals(m2.size(), m1.size());
		m1.foreach((k, v) -> {
			assertEquals(Integer.valueOf(k), v);
			assertEquals(m2.get(k), v);
		});
		for (var it = m1.iterator(); it.moveToNext(); ) {
			var k = it.key();
			var v = it.value();
			assertEquals(Integer.valueOf(k), v);
			assertEquals(m2.get(k), v);
		}
		m1.foreachKey(k -> assertTrue(m2.containsKey(k)));
		m2.forEach((k, v) -> {
			assertEquals(k, v);
			assertEquals(v, m1.get(k));
		});
	}

	private static void checkMap(LongHashMap<Long> m1, HashMap<Long, Long> m2) {
		assertEquals(m2.size(), m1.size());
		m1.foreach((k, v) -> {
			assertEquals(Long.valueOf(k), v);
			assertEquals(m2.get(k), v);
		});
		for (var it = m1.iterator(); it.moveToNext(); ) {
			var k = it.key();
			var v = it.value();
			assertEquals(Long.valueOf(k), v);
			assertEquals(m2.get(k), v);
		}
		m1.foreachKey(k -> assertTrue(m2.containsKey(k)));
		m2.forEach((k, v) -> {
			assertEquals(k, v);
			assertEquals(v, m1.get(k));
		});
	}

	public void testIntHashSet() {
		var r = Random.getInstance();
		var s1 = new IntHashSet();
		var s2 = new HashSet<Integer>();
		for (int i = 0; i < 10000; i++) {
			if (r.nextInt(100) < 10) {
				s1.clear();
				s2.clear();
			}
			checkSet(s1, s2);

			for (int j = 0, n = r.nextInt(10); j < n; j++) {
				int k = r.nextInt(100);
				s1.add(k);
				s2.add(k);
			}
			checkSet(s1, s2);

			for (int j = 0, n = r.nextInt(10); j < n; j++) {
				int k = r.nextInt(100);
				s1.remove(k);
				s2.remove(k);
			}
			checkSet(s1, s2);
		}
	}

	public void testLongHashSet() {
		var r = Random.getInstance();
		var s1 = new LongHashSet();
		var s2 = new HashSet<Long>();
		for (int i = 0; i < 10000; i++) {
			if (r.nextInt(100) < 10) {
				s1.clear();
				s2.clear();
			}
			checkSet(s1, s2);

			for (int j = 0, n = r.nextInt(10); j < n; j++) {
				long k = r.nextInt(100);
				s1.add(k);
				s2.add(k);
			}
			checkSet(s1, s2);

			for (int j = 0, n = r.nextInt(10); j < n; j++) {
				long k = r.nextInt(100);
				s1.remove(k);
				s2.remove(k);
			}
			checkSet(s1, s2);
		}
	}

	public void testIntHashMap() {
		var r = Random.getInstance();
		var m1 = new IntHashMap<Integer>();
		var m2 = new HashMap<Integer, Integer>();
		for (int i = 0; i < 10000; i++) {
			if (r.nextInt(100) < 10) {
				m1.clear();
				m2.clear();
			}
			checkMap(m1, m2);

			for (int j = 0, n = r.nextInt(10); j < n; j++) {
				int k = r.nextInt(100);
				m1.put(k, k);
				m2.put(k, k);
			}
			checkMap(m1, m2);

			for (int j = 0, n = r.nextInt(10); j < n; j++) {
				int k = r.nextInt(100);
				m1.remove(k);
				m2.remove(k);
			}
			checkMap(m1, m2);
		}
	}

	public void testLongHashMap() {
		var r = Random.getInstance();
		var m1 = new LongHashMap<Long>();
		var m2 = new HashMap<Long, Long>();
		for (int i = 0; i < 10000; i++) {
			if (r.nextInt(100) < 10) {
				m1.clear();
				m2.clear();
			}
			checkMap(m1, m2);

			for (int j = 0, n = r.nextInt(10); j < n; j++) {
				long k = r.nextInt(100);
				m1.put(k, k);
				m2.put(k, k);
			}
			checkMap(m1, m2);

			for (int j = 0, n = r.nextInt(10); j < n; j++) {
				long k = r.nextInt(100);
				m1.remove(k);
				m2.remove(k);
			}
			checkMap(m1, m2);
		}
	}
}
