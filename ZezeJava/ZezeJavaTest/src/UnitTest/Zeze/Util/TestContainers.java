package UnitTest.Zeze.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import Zeze.Util.FloatList;
import Zeze.Util.IntHashMap;
import Zeze.Util.IntHashSet;
import Zeze.Util.IntList;
import Zeze.Util.LongHashMap;
import Zeze.Util.LongHashSet;
import Zeze.Util.LongList;
import Zeze.Util.OutInt;
import Zeze.Util.Random;
import junit.framework.TestCase;

public class TestContainers extends TestCase {
	private static final int KEY_RANGE = 100;
	private static final int TEST_COUNT = 10_0000;

	private static void checkList(IntList v1, ArrayList<Integer> v2) {
		int n = v1.size();
		assertEquals(n, v1.size());
		for (int i = 0; i < n; i++)
			assertEquals((int)v2.get(i), v1.get(i));
	}

	private static void checkList(LongList v1, ArrayList<Long> v2) {
		int n = v2.size();
		assertEquals(n, v1.size());
		for (int i = 0; i < n; i++)
			assertEquals((long)v2.get(i), v1.get(i));
	}

	private static void checkList(FloatList v1, ArrayList<Float> v2) {
		int n = v2.size();
		assertEquals(n, v1.size());
		for (int i = 0; i < n; i++)
			assertEquals(v2.get(i), v1.get(i));
	}

	private static void checkSet(IntHashSet m1, HashSet<Integer> m2) {
		int n = m2.size();
		assertEquals(n, m1.size());
		var c = new OutInt();
		m1.foreach(k -> {
			assertTrue(m2.contains(k));
			c.value++;
		});
		assertEquals(n, c.value);
		c.value = 0;
		for (var it = m1.iterator(); it.moveToNext(); c.value++)
			assertTrue(m2.contains(it.value()));
		assertEquals(n, c.value);
		for (int i = 0; i < KEY_RANGE; i++)
			assertEquals(m2.contains(i), m1.contains(i));
	}

	private static void checkSet(LongHashSet m1, HashSet<Long> m2) {
		int n = m2.size();
		assertEquals(n, m1.size());
		var c = new OutInt();
		m1.foreach(k -> {
			assertTrue(m2.contains(k));
			c.value++;
		});
		assertEquals(n, c.value);
		c.value = 0;
		for (var it = m1.iterator(); it.moveToNext(); c.value++)
			assertTrue(m2.contains(it.value()));
		assertEquals(n, c.value);
		for (int i = 0; i < KEY_RANGE; i++)
			assertEquals(m2.contains((long)i), m1.contains(i));
	}

	private static void checkMap(IntHashMap<Integer> m1, HashMap<Integer, Integer> m2) {
		int n = m2.size();
		assertEquals(n, m1.size());
		var c = new OutInt();
		m1.foreach((k, v) -> {
			assertEquals(Integer.valueOf(k), v);
			assertEquals(m2.get(k), v);
			c.value++;
		});
		assertEquals(n, c.value);
		c.value = 0;
		for (var it = m1.iterator(); it.moveToNext(); ) {
			var k = it.key();
			var v = it.value();
			assertEquals(Integer.valueOf(k), v);
			assertEquals(m2.get(k), v);
			c.value++;
		}
		assertEquals(n, c.value);
		for (int i = 0; i < KEY_RANGE; i++) {
			assertEquals(m2.containsKey(i), m1.containsKey(i));
			assertEquals(m2.get(i), m1.get(i));
		}
	}

	private static void checkMap(LongHashMap<Long> m1, HashMap<Long, Long> m2) {
		int n = m2.size();
		assertEquals(n, m1.size());
		var c = new OutInt();
		m1.foreach((k, v) -> {
			assertEquals(Long.valueOf(k), v);
			assertEquals(m2.get(k), v);
			c.value++;
		});
		assertEquals(n, c.value);
		c.value = 0;
		for (var it = m1.iterator(); it.moveToNext(); ) {
			var k = it.key();
			var v = it.value();
			assertEquals(Long.valueOf(k), v);
			assertEquals(m2.get(k), v);
			c.value++;
		}
		assertEquals(n, c.value);
		for (int i = 0; i < KEY_RANGE; i++) {
			assertEquals(m2.containsKey((long)i), m1.containsKey(i));
			assertEquals(m2.get((long)i), m1.get(i));
		}
	}

	public void testIntList() {
		var r = Random.getInstance();
		var v1 = new IntList();
		var v2 = new ArrayList<Integer>();
		for (int i = 0; i < TEST_COUNT; i++) {
			int s = r.nextInt(100);
			if (s < 80 || v2.isEmpty()) {
				int v = r.nextInt();
				v1.add(v);
				v2.add(v);
			} else if (s < 99) {
				int j = r.nextInt(v2.size());
				v1.remove(j);
				v2.remove(j);
			} else {
				v1.clear();
				v2.clear();
			}
			checkList(v1, v2);
		}
	}

	public void testLongList() {
		var r = Random.getInstance();
		var v1 = new LongList();
		var v2 = new ArrayList<Long>();
		for (int i = 0; i < TEST_COUNT; i++) {
			int s = r.nextInt(100);
			if (s < 80 || v2.isEmpty()) {
				long v = r.nextLong();
				v1.add(v);
				v2.add(v);
			} else if (s < 99) {
				int j = r.nextInt(v2.size());
				v1.remove(j);
				v2.remove(j);
			} else {
				v1.clear();
				v2.clear();
			}
			checkList(v1, v2);
		}
	}

	public void testFloatList() {
		var r = Random.getInstance();
		var v1 = new FloatList();
		var v2 = new ArrayList<Float>();
		for (int i = 0; i < TEST_COUNT; i++) {
			int s = r.nextInt(100);
			if (s < 80 || v2.isEmpty()) {
				float v = r.nextFloat();
				v1.add(v);
				v2.add(v);
			} else if (s < 99) {
				int j = r.nextInt(v2.size());
				v1.remove(j);
				v2.remove(j);
			} else {
				v1.clear();
				v2.clear();
			}
			checkList(v1, v2);
		}
	}

	public void testIntHashSet() {
		var r = Random.getInstance();
		var s1 = new IntHashSet();
		var s2 = new HashSet<Integer>();
		for (int i = 0; i < TEST_COUNT; i++) {
			int s = r.nextInt(100);
			if (s < 50) {
				int k = r.nextInt(KEY_RANGE);
				s1.add(k);
				s2.add(k);
			} else if (s < 99) {
				int k = r.nextInt(KEY_RANGE);
				s1.remove(k);
				s2.remove(k);
			} else {
				s1.clear();
				s2.clear();
			}
			checkSet(s1, s2);
		}
	}

	public void testLongHashSet() {
		var r = Random.getInstance();
		var s1 = new LongHashSet();
		var s2 = new HashSet<Long>();
		for (int i = 0; i < TEST_COUNT; i++) {
			int s = r.nextInt(100);
			if (s < 50) {
				long k = r.nextInt(KEY_RANGE);
				s1.add(k);
				s2.add(k);
			} else if (s < 99) {
				long k = r.nextInt(KEY_RANGE);
				s1.remove(k);
				s2.remove(k);
			} else {
				s1.clear();
				s2.clear();
			}
			checkSet(s1, s2);
		}
	}

	public void testIntHashMap() {
		var r = Random.getInstance();
		var m1 = new IntHashMap<Integer>();
		var m2 = new HashMap<Integer, Integer>();
		for (int i = 0; i < TEST_COUNT; i++) {
			int s = r.nextInt(100);
			if (s < 50) {
				int k = r.nextInt(KEY_RANGE);
				m1.put(k, k);
				m2.put(k, k);
			} else if (s < 99) {
				int k = r.nextInt(KEY_RANGE);
				m1.remove(k);
				m2.remove(k);
			} else {
				m1.clear();
				m2.clear();
			}
			checkMap(m1, m2);
		}
	}

	public void testLongHashMap() {
		var r = Random.getInstance();
		var m1 = new LongHashMap<Long>();
		var m2 = new HashMap<Long, Long>();
		for (int i = 0; i < TEST_COUNT; i++) {
			int s = r.nextInt(100);
			if (s < 50) {
				long k = r.nextInt(KEY_RANGE);
				m1.put(k, k);
				m2.put(k, k);
			} else if (s < 99) {
				long k = r.nextInt(KEY_RANGE);
				m1.remove(k);
				m2.remove(k);
			} else {
				m1.clear();
				m2.clear();
			}
			checkMap(m1, m2);
		}
	}
}
