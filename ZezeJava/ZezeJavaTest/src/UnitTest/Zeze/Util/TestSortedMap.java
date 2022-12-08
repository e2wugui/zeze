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
			Assert.assertEquals(Long.valueOf(src[i]), sortedMap.getAt(i).getKey());
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

	@Test
	public void testSortedMapFind() {
		var sortedMap = new SortedMap<Long, Long>();
		var src = new long[] { 1, 3, 5, 6, 9};
		for (var hash : src)
			sortedMap.add(hash, hash);
		/*
		System.out.println(sortedMap.get(0L));
		System.out.println(sortedMap.get(1L));
		System.out.println(sortedMap.get(2L));
		System.out.println(sortedMap.get(3L));
		System.out.println(sortedMap.get(4L));
		System.out.println(sortedMap.get(5L));
		System.out.println(sortedMap.get(6L));
		System.out.println(sortedMap.get(7L));
		System.out.println(sortedMap.get(8L));
		System.out.println(sortedMap.get(9L));
		System.out.println(sortedMap.get(10L));
		*/
		Assert.assertEquals(sortedMap.get(0L), null);
		Assert.assertEquals(sortedMap.get(1L).getKey(), Long.valueOf(1));
		Assert.assertEquals(sortedMap.get(2L), null);
		Assert.assertEquals(sortedMap.get(3L).getKey(), Long.valueOf(3));
		Assert.assertEquals(sortedMap.get(4L), null);
		Assert.assertEquals(sortedMap.get(5L).getKey(), Long.valueOf(5));
		Assert.assertEquals(sortedMap.get(6L).getKey(), Long.valueOf(6));
		Assert.assertEquals(sortedMap.get(7L), null);
		Assert.assertEquals(sortedMap.get(8L), null);
		Assert.assertEquals(sortedMap.get(9L).getKey(), Long.valueOf(9));
		Assert.assertEquals(sortedMap.get(10L), null);
	}

	@Test
	public void testAddAll() {
		var m = new SortedMap<Integer, Integer>();
		Assert.assertEquals(0, m.addAll(new Integer[]{10, 30, 50,70, 90}, 111).size());
		Assert.assertEquals(1, m.addAll(new Integer[]{80, 50, 20}, 222).size());
		Assert.assertEquals(0, m.addAll(new Integer[]{100, 40, 0}, 333).size());
		Assert.assertEquals(10, m.size());
//		System.out.println(m);
	}
}
