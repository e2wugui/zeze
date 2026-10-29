package Zeze.Util;

import java.util.Arrays;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestKVListAliasedCopies {
	private static KVList<@Nullable Object, @Nullable Object> pairs(int capacity, int count) {
		var keys = new @Nullable Object[capacity];
		var values = new @Nullable Object[capacity];
		for (int i = 0; i < capacity; i++) {
			keys[i] = i + 1;
			values[i] = (i + 1) * 10;
		}
		return KVList.wrap(keys, values, count);
	}

	private static void assertPairs(KVList<?, ?> pairs, Object[] keys, Object[] values) {
		assertEquals(keys.length, pairs.size());
		assertEquals(keys.length, values.length);
		for (int i = 0; i < keys.length; i++) {
			assertEquals(keys[i], pairs.getKey(i), "key at " + i);
			assertEquals(values[i], pairs.getValue(i), "value at " + i);
		}
	}

	@Test
	public void replacementSwapsTheOriginalColumns() {
		var target = pairs(8, 3);
		target.replace(KVList.wrap(target.values(), target.keys(), 3));
		assertPairs(target, new Object[] {10, 20, 30}, new Object[] {1, 2, 3});
	}

	@Test
	public void replacementReadsTheOriginalValueSlice() {
		var target = pairs(8, 3);
		target.replace(new Object[] {8, 9, 10}, target.keys(), 1, 2);
		assertPairs(target, new Object[] {9, 10}, new Object[] {2, 3});
	}

	@Test
	public void replacementSupportsOverlappingSameColumnSlices() {
		var target = pairs(8, 3);
		target.replace(target.keys(), target.values(), 1, 2);
		assertPairs(target, new Object[] {2, 3}, new Object[] {20, 30});
	}

	@Test
	public void appendReadsBothColumnsBeforeOverwritingSharedSource() {
		var target = pairs(8, 2);
		var source = KVList.wrap(target.values(), target.keys(), 4);
		target.addAll(source);
		assertPairs(target, new Object[] {1, 2, 10, 20, 30, 40}, new Object[] {10, 20, 1, 2, 3, 4});
	}

	@Test
	public void appendReadsTheOriginalCrossColumnSlice() {
		var target = pairs(8, 2);
		target.addAll(target.values(), target.keys(), 1, 3);
		assertPairs(target, new Object[] {1, 2, 20, 30, 40}, new Object[] {10, 20, 2, 3, 4});
	}

	@Test
	public void appendSnapshotsCollectionsBackedByTheDestinationColumns() {
		var target = pairs(8, 2);
		target.addAll(Arrays.asList(target.values()).subList(0, 4),
				Arrays.asList(target.keys()).subList(0, 4));
		assertPairs(target, new Object[] {1, 2, 10, 20, 30, 40}, new Object[] {10, 20, 1, 2, 3, 4});
	}

	@Test
	public void insertAtTheEndUsesTheSamePairCopyContract() {
		var target = pairs(8, 2);
		var source = KVList.wrap(target.values(), target.keys(), 4);
		target.insert(target.size(), source);
		assertPairs(target, new Object[] {1, 2, 10, 20, 30, 40}, new Object[] {10, 20, 1, 2, 3, 4});
	}

	@Test
	public void insertionPreservesCrossColumnSourcesBeforeMovingTheTail() {
		var target = pairs(8, 3);
		target.insert(1, target.values(), target.keys(), 1, 2);
		assertPairs(target, new Object[] {1, 20, 30, 2, 3}, new Object[] {10, 2, 3, 20, 30});
	}

	@Test
	public void appendCanDetachCrossColumnSourcesDuringGrowth() {
		var target = pairs(4, 3);
		target.addAll(KVList.wrap(target.values(), target.keys(), 4));
		assertPairs(target, new Object[] {1, 2, 3, 10, 20, 30, 40}, new Object[] {10, 20, 30, 1, 2, 3, 4});
	}

	@Test
	public void appendSupportsItsOwnUnswappedColumns() {
		var target = pairs(8, 3);
		target.addAll(target);
		assertPairs(target, new Object[] {1, 2, 3, 1, 2, 3}, new Object[] {10, 20, 30, 10, 20, 30});
	}
}
