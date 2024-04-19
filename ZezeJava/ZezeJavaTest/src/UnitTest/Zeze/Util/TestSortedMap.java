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
		for (int i = 0; i < src.length; i++) {
			var hash = src[i];
			sortedMap.add(hash, hash, i);
		}
		Arrays.sort(src);
		for (var i = 0; i < src.length; ++i)
			Assert.assertEquals(Long.valueOf(src[i]), sortedMap.getAt(i).getKey());
		//System.out.println(sortedMap);
	}

	@Test
	public void testSortedMapLowerBound() {
		var sortedMap = new SortedMap<Long, Long>();
		var src = new long[]{1, 3, 5, 6, 9};
		for (int i = 0; i < src.length; i++) {
			var hash = src[i];
			sortedMap.add(hash, hash, i);
		}
		//System.out.println(sortedMap);
		Assert.assertEquals(0, sortedMap.lowerBoundIndex(0L));
		Assert.assertEquals(0, sortedMap.lowerBoundIndex(1L));
		Assert.assertEquals(1, sortedMap.lowerBoundIndex(2L));
		Assert.assertEquals(1, sortedMap.lowerBoundIndex(3L));
		Assert.assertEquals(2, sortedMap.lowerBoundIndex(4L));
		Assert.assertEquals(2, sortedMap.lowerBoundIndex(5L));
		Assert.assertEquals(3, sortedMap.lowerBoundIndex(6L));
		Assert.assertEquals(4, sortedMap.lowerBoundIndex(7L));
		Assert.assertEquals(4, sortedMap.lowerBoundIndex(8L));
		Assert.assertEquals(4, sortedMap.lowerBoundIndex(9L));
		Assert.assertEquals(5, sortedMap.lowerBoundIndex(10L));
	}

	@Test
	public void testSortedMapUpperBound() {
		var sortedMap = new SortedMap<Long, Long>();
		var src = new long[]{1, 3, 5, 6, 9};
		for (int i = 0; i < src.length; i++) {
			var hash = src[i];
			sortedMap.add(hash, hash, i);
		}
		//System.out.println(sortedMap);
		//System.out.println(sortedMap.upperBoundIndex(0L));
		Assert.assertEquals(0, sortedMap.upperBoundIndex(0L));
		Assert.assertEquals(1, sortedMap.upperBoundIndex(1L)); //
		Assert.assertEquals(1, sortedMap.upperBoundIndex(2L));
		Assert.assertEquals(2, sortedMap.upperBoundIndex(3L)); //
		Assert.assertEquals(2, sortedMap.upperBoundIndex(4L));
		Assert.assertEquals(3, sortedMap.upperBoundIndex(5L)); //
		Assert.assertEquals(4, sortedMap.upperBoundIndex(6L)); //
		Assert.assertEquals(4, sortedMap.upperBoundIndex(7L));
		Assert.assertEquals(4, sortedMap.upperBoundIndex(8L));
		Assert.assertEquals(5, sortedMap.upperBoundIndex(9L)); //
		Assert.assertEquals(5, sortedMap.upperBoundIndex(10L));
		//System.out.println(sortedMap.upperBoundIndex(10L));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	public void testSortedMapFind() {
		var sortedMap = new SortedMap<Long, Long>();
		var src = new long[]{1, 3, 5, 6, 9};
		for (int i = 0; i < src.length; i++) {
			var hash = src[i];
			sortedMap.add(hash, hash, i);
		}
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
		Assert.assertNull(sortedMap.get(0L));
		Assert.assertEquals(sortedMap.get(1L).getKey(), Long.valueOf(1));
		Assert.assertNull(sortedMap.get(2L));
		Assert.assertEquals(sortedMap.get(3L).getKey(), Long.valueOf(3));
		Assert.assertNull(sortedMap.get(4L));
		Assert.assertEquals(sortedMap.get(5L).getKey(), Long.valueOf(5));
		Assert.assertEquals(sortedMap.get(6L).getKey(), Long.valueOf(6));
		Assert.assertNull(sortedMap.get(7L));
		Assert.assertNull(sortedMap.get(8L));
		Assert.assertEquals(sortedMap.get(9L).getKey(), Long.valueOf(9));
		Assert.assertNull(sortedMap.get(10L));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	public void testAddAll() {
		var m = new SortedMap<Integer, Integer>();
		m.addAll(new Integer[]{10, 30, 50, 90, 70}, 1);
		Assert.assertEquals(5, m.size());
		Assert.assertEquals(5, m.keySize());
		m.addAll(new Integer[]{80, 50, 20}, 2);
		Assert.assertEquals(8, m.size());
		Assert.assertEquals(7, m.keySize());
		m.addAll(new Integer[]{100, 40, 0}, 3);
		Assert.assertEquals(11, m.size());
		Assert.assertEquals(10, m.keySize());
//		System.out.println(m);
		Assert.assertEquals(Integer.valueOf(1), m.get(10).getValue());
		Assert.assertEquals(Integer.valueOf(2), m.get(20).getValue());
		Assert.assertEquals(Integer.valueOf(1), m.get(30).getValue());
		Assert.assertEquals(Integer.valueOf(3), m.get(40).getValue());
		Assert.assertEquals(Integer.valueOf(1), m.get(50).getValue());
		Assert.assertNull(m.get(60));
		Assert.assertEquals(Integer.valueOf(1), m.get(70).getValue());
		Assert.assertEquals(Integer.valueOf(2), m.get(80).getValue());
		Assert.assertEquals(Integer.valueOf(1), m.get(90).getValue());
		Assert.assertEquals(Integer.valueOf(3), m.get(100).getValue());
	}
}
