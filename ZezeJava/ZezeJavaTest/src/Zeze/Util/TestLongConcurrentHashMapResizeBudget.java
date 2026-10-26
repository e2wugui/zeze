package Zeze.Util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestLongConcurrentHashMapResizeBudget {

	@Test
	public void forwardedBinWritesDoNotJoinAFullResizeTeam() throws Exception {
		verifyResizeBudget(63); // Old bucket 31 has already been forwarded.
	}

	@Test
	public void insertionCountingDoesNotJoinAFullResizeTeam() throws Exception {
		verifyResizeBudget(1); // An unforwarded bucket reaches addCount directly.
	}

	private static void verifyResizeBudget(long key) throws Exception {
		var map = fullResizeTeam();
		map.put(key, "new");
		assertEquals(16, field("transferIndex").getInt(map), "a new helper must not claim any transfer range");
		assertEquals("seed", map.get(31));
		assertEquals("new", map.get(key));
		assertEquals(2, map.size());
	}

	private static LongConcurrentHashMap<String> fullResizeTeam() throws Exception {
		var map = new LongConcurrentHashMap<String>(16);
		map.put(31, "seed");
		var oldTable = (Object[])field("table").get(map);
		assert oldTable != null;
		assertEquals(32, oldTable.length);
		var nextTable = (Object[])Array.newInstance(oldTable.getClass().getComponentType(), 64);
		nextTable[31] = oldTable[31];
		var forwardingClass = Class.forName(LongConcurrentHashMap.class.getName() + "$ForwardingNode");
		var constructor = forwardingClass.getDeclaredConstructor(oldTable.getClass());
		constructor.setAccessible(true);
		var forwarded = constructor.newInstance((Object)nextTable);
		Arrays.fill(oldTable, 16, 32, forwarded);
		field("nextTable").set(map, nextTable);
		field("transferIndex").setInt(map, 16);
		// A valid protocol snapshot: the upper half is transferred; all registered
		// helpers may be paused after admission and before claiming the lower half.
		// Construct the boundary directly instead of starting 65,000 test threads.
		int stamp = Integer.numberOfLeadingZeros(32) | (1 << 15);
		field("sizeCtl").setInt(map, (stamp << 16) + 0xffff);
		return map;
	}

	private static Field field(String name) throws NoSuchFieldException {
		var field = LongConcurrentHashMap.class.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}
}
