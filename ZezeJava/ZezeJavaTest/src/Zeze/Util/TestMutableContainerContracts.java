package Zeze.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestMutableContainerContracts {


	@Test
	public void aliasedInsertionReadsTheOriginalSlice() {
		var ints = IntList.wrap(new int[] {1, 2, 3, 0, 0}, 3);
		ints.insert(0, ints.array(), 1, 1);
		assertArrayEquals(new int[] {2, 1, 2, 3}, ints.toArray());
		var longs = LongList.wrap(new long[] {1, 2, 3, 0, 0}, 3);
		longs.insert(0, longs.array(), 1, 1);
		assertArrayEquals(new long[] {2, 1, 2, 3}, longs.toArray());
		var floats = FloatList.wrap(new float[] {1, 2, 3, 0, 0}, 3);
		floats.insert(0, floats.array(), 1, 1);
		assertArrayEquals(new float[] {2, 1, 2, 3}, floats.toArray());
		var pairs = KVList.wrap(new Object[] {1, 2, 3, null, null},
				new Object[] {10, 20, 30, null, null}, 3);
		pairs.insert(0, pairs.keys(), pairs.values(), 1, 1);
		assertEquals(2, pairs.getKey(0));
		assertEquals(20, pairs.getValue(0));
		assertEquals(4, pairs.size());
	}

	@Test
	public void zeroAdditionReturnsAMutableValueAndSelfRemovalClears() {
		var one = Id128.Zero.add(1);
		assertEquals(1, one.getLow());
		one.increment(1);
		assertEquals(2, one.getLow());
		assertEquals(0, Id128.Zero.getLow());
	}


}
