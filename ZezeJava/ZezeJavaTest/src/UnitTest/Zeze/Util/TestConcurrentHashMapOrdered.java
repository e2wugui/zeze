package UnitTest.Zeze.Util;

import java.util.ArrayList;
import java.util.List;
import Zeze.Util.ConcurrentHashMapOrdered;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestConcurrentHashMapOrdered {
	@Test
	public void testConcurrentHashMapOrdered() {
		var orderedMap = new ConcurrentHashMapOrdered<String, String>();
		var keys = List.of("3", "2", "1");
		for (var key : keys)
			orderedMap.put(key, key);
		//orderedMap.dumpMap();
		var foreachKeys = new ArrayList<String>();
		orderedMap.foreach((key, value) -> foreachKeys.add(key));
		var itKeys = new ArrayList<String>();
		for (var it = orderedMap.iterator(); it.hasNext(); ) {
			it.next();
			itKeys.add(it.key());
		}
		assertEquals(foreachKeys, keys);
		assertEquals(itKeys, keys);
	}

	@Test
	public void testSize() {
		ConcurrentHashMapOrdered<String, String> map = new ConcurrentHashMapOrdered<>();
		map.put("key1", "value1");
		map.put("key2", "value2");
		assertEquals(2, map.size());
	}

	@Test
	public void testIsEmpty() {
		ConcurrentHashMapOrdered<String, String> map = new ConcurrentHashMapOrdered<>();
		assertTrue(map.isEmpty());
		map.put("key1", "value1");
		assertFalse(map.isEmpty());
	}

	@Test
	public void testContainsKey() {
		ConcurrentHashMapOrdered<String, String> map = new ConcurrentHashMapOrdered<>();
		map.put("key1", "value1");
		assertTrue(map.containsKey("key1"));
		assertFalse(map.containsKey("key2"));
	}

	@Test
	public void testContainsValue() {
		ConcurrentHashMapOrdered<String, String> map = new ConcurrentHashMapOrdered<>();
		map.put("key1", "value1");
		assertTrue(map.containsValue("value1"));
		assertFalse(map.containsValue("value2"));
	}

	@Test
	public void testClear() {
		ConcurrentHashMapOrdered<String, String> map = new ConcurrentHashMapOrdered<>();
		map.put("key1", "value1");
		map.put("key2", "value2");
		map.clear();
		assertTrue(map.isEmpty());
		assertEquals(0, map.size());
	}

	@Test
	public void testPut() {
		ConcurrentHashMapOrdered<String, String> map = new ConcurrentHashMapOrdered<>();
		assertNull(map.put("key1", "value1"));
		assertEquals("value1", map.get("key1"));
		assertEquals(1, map.size());
	}

	@Test
	public void testPutIfAbsent() {
		ConcurrentHashMapOrdered<String, String> map = new ConcurrentHashMapOrdered<>();
		assertNull(map.putIfAbsent("key1", "value1"));
		assertEquals("value1", map.get("key1"));
		assertEquals("value1", map.putIfAbsent("key1", "value2"));
		assertEquals("value1", map.get("key1"));
		assertEquals(1, map.size());
	}

	@Test
	public void testGet() {
		ConcurrentHashMapOrdered<String, String> map = new ConcurrentHashMapOrdered<>();
		map.put("key1", "value1");
		assertEquals("value1", map.get("key1"));
		assertNull(map.get("key2"));
		assertEquals(1, map.size());
	}

	@Test
	public void testRemove() {
		ConcurrentHashMapOrdered<String, String> map = new ConcurrentHashMapOrdered<>();
		map.put("key1", "value1");
		assertEquals("value1", map.remove("key1"));
		assertNull(map.remove("key1"));
		assertEquals(0, map.size());
	}

	@Test
	public void testReplace() {
		ConcurrentHashMapOrdered<String, String> map = new ConcurrentHashMapOrdered<>();
		map.put("key1", "value1");
		assertEquals("value1", map.replace("key1", "value2"));
		assertEquals("value2", map.get("key1"));
		assertNull(map.replace("key2", "value3"));
		assertEquals(1, map.size());
	}

	@Test
	public void testForeach() {
		ConcurrentHashMapOrdered<String, String> map = new ConcurrentHashMapOrdered<>();
		map.put("key1", "value1");
		map.put("key2", "value2");
		StringBuilder sb = new StringBuilder();
		map.foreach((key, value) -> sb.append(key).append("=").append(value).append(", "));
		assertEquals("key1=value1, key2=value2, ", sb.toString());
		assertEquals(2, map.size());
	}

	@Test
	public void testOrderedIterator() {
		ConcurrentHashMapOrdered<String, String> map = new ConcurrentHashMapOrdered<>();
		map.put("key1", "value1");
		map.put("key2", "value2");
		StringBuilder sb = new StringBuilder();
		for (String value : map) {
			sb.append(value).append(", ");
		}
		assertEquals("value1, value2, ", sb.toString());
		assertEquals(2, map.size());
	}
}
