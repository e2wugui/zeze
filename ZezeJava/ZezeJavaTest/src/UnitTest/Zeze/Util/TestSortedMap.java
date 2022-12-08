package UnitTest.Zeze.Util;

import java.util.Arrays;
import Zeze.Util.SortedMap;
import org.junit.Assert;
import org.junit.Test;

public class TestSortedMap {
	@Test
	public void testSortedMap() {
		var sortedMap = new SortedMap<Long, Long>();
		var src = new long[10];
		for (var i = 0; i < src.length; ++i)
			src[i] = Zeze.Util.Random.getInstance().nextLong() & 0xffffffffL;
		for (var hash : src)
			sortedMap.add(hash, hash);
		Arrays.sort(src);
		for (var i = 0; i < src.length; ++i)
			Assert.assertEquals(Long.valueOf(src[i]), sortedMap.get(i).getKey());
		//System.out.println(sortedMap);
	}

	@Test
	public void testSortedMapLowerBound() {
		var sortedMap = new SortedMap<Long, Long>();
		var src = new long[] { 1, 3, 5, 6, 9};
		for (var hash : src)
			sortedMap.add(hash, hash);
		//System.out.println(sortedMap);
		Assert.assertEquals(sortedMap.lowerBoundIndex(0L), 0);
		Assert.assertEquals(sortedMap.lowerBoundIndex(1L), 0);
		Assert.assertEquals(sortedMap.lowerBoundIndex(2L), 1);
		Assert.assertEquals(sortedMap.lowerBoundIndex(3L), 1);
		Assert.assertEquals(sortedMap.lowerBoundIndex(4L), 2);
		Assert.assertEquals(sortedMap.lowerBoundIndex(5L), 2);
		Assert.assertEquals(sortedMap.lowerBoundIndex(6L), 3);
		Assert.assertEquals(sortedMap.lowerBoundIndex(7L), 4);
		Assert.assertEquals(sortedMap.lowerBoundIndex(8L), 4);
		Assert.assertEquals(sortedMap.lowerBoundIndex(9L), 4);
		Assert.assertEquals(sortedMap.lowerBoundIndex(10L), 5);
	}

	@Test
	public void testSortedMapUpperBound() {
		var sortedMap = new SortedMap<Long, Long>();
		var src = new long[] { 1, 3, 5, 6, 9};
		for (var hash : src)
			sortedMap.add(hash, hash);
		//System.out.println(sortedMap);
		//System.out.println(sortedMap.upperBoundIndex(0L));
		Assert.assertEquals(sortedMap.upperBoundIndex(0L), 0);
		Assert.assertEquals(sortedMap.upperBoundIndex(1L), 1); //
		Assert.assertEquals(sortedMap.upperBoundIndex(2L), 1);
		Assert.assertEquals(sortedMap.upperBoundIndex(3L), 2); //
		Assert.assertEquals(sortedMap.upperBoundIndex(4L), 2);
		Assert.assertEquals(sortedMap.upperBoundIndex(5L), 3); //
		Assert.assertEquals(sortedMap.upperBoundIndex(6L), 4); //
		Assert.assertEquals(sortedMap.upperBoundIndex(7L), 4);
		Assert.assertEquals(sortedMap.upperBoundIndex(8L), 4);
		Assert.assertEquals(sortedMap.upperBoundIndex(9L), 5); //
		Assert.assertEquals(sortedMap.upperBoundIndex(10L), 5);
		//System.out.println(sortedMap.upperBoundIndex(10L));
	}
}
