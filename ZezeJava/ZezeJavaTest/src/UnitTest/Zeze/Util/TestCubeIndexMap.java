package UnitTest.Zeze.Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Util.Cube;
import Zeze.Util.CubeIndex;
import Zeze.Util.CubeIndexMap;
import Zeze.Util.Factory;

/**
 * 回归：toIndex 对负坐标必须向下取整（Math.floorDiv / Math.floor），
 * 不能向零截断，否则 size=256 时 [-256,0) 与 [0,256) 被并进同一个 cube，
 * 0 号 cube 宽度变成 2*size，负半轴其余格子全部错位。
 */
@Fast
@Execution(ExecutionMode.SAME_THREAD)
public class TestCubeIndexMap {
	private static final int SIZE = 256;

	/**
	 * 最小可观察 Cube：只记录自己当前持有的对象，无外部依赖。
	 */
	private static final class TestCube extends Cube<Integer> {
		private final Set<Integer> objects = ConcurrentHashMap.newKeySet();

		@Override
		public void add(CubeIndex index, Integer obj) {
			// under lock(cube)
			objects.add(obj);
		}

		@Override
		public boolean remove(CubeIndex index, Integer obj) {
			// under lock(cube)
			objects.remove(obj);
			return objects.isEmpty();
		}
	}

	private static CubeIndexMap<TestCube, Integer> newMap() {
		return newMap(SIZE);
	}

	private static CubeIndexMap<TestCube, Integer> newMap(int size) {
		return new CubeIndexMap<>(new Factory<TestCube>() {
			@Override
			public TestCube create() {
				return new TestCube();
			}
		}, size, size, size);
	}

	/**
	 * 返回 xyz 所在 cube 的对象集合。注意必须传 long 字面量：
	 * 传 int 会选中 getCubes 的 float 重载，坐标会被当成 float 处理。
	 */
	private static Set<Integer> objectsAt(CubeIndexMap<TestCube, Integer> map, long x, long y, long z) {
		var cubes = map.getCubes(x, y, z, 0, 0, 0);
		assertEquals(1, cubes.size());
		return cubes.get(0).objects;
	}

	/**
	 * cube 内最后一个对象离开时，removeObject 会把空 cube 从表里摘掉。
	 */
	private static void assertCubeGone(CubeIndexMap<TestCube, Integer> map, long x, long y, long z) {
		assertEquals(0, map.getCubes(x, y, z, 0, 0, 0).size());
	}

	/**
	 * x 轴区间 [x0,x1] 覆盖到的格子数。格子等宽时恒等于 (x1-x0)/size+1；
	 * 向零截断时 0 号格偏宽，负半轴取到的格子数更少。
	 */
	private static int cellCount(CubeIndexMap<TestCube, Integer> map, long x0, long x1) {
		var cells = new HashSet<CubeIndex>();
		for (long x = x0; x <= x1; ++x)
			cells.add(map.toIndex(x, 0L, 0L));
		return cells.size();
	}

	private static void assertIndex(long expectX, long expectY, long expectZ, CubeIndex index) {
		assertEquals((Long)expectX, (Long)index.getX());
		assertEquals((Long)expectY, (Long)index.getY());
		assertEquals((Long)expectZ, (Long)index.getZ());
	}

	@Test
	public void testToIndexBoundary() {
		var map = newMap();
		assertIndex(0, 0, 0, map.toIndex(0L, 0L, 0L));
		assertIndex(0, 0, 0, map.toIndex(255L, 255L, 255L));
		assertIndex(1, 1, 1, map.toIndex(256L, 256L, 256L));
		assertIndex(1, 1, 1, map.toIndex(511L, 511L, 511L));
		assertIndex(2, 2, 2, map.toIndex(512L, 512L, 512L));
		// 负半轴：-1 必须落到 -1，不能向零截断成 0
		assertIndex(-1, -1, -1, map.toIndex(-1L, -1L, -1L));
		assertIndex(-1, -1, -1, map.toIndex(-255L, -255L, -255L));
		assertIndex(-1, -1, -1, map.toIndex(-256L, -256L, -256L));
		assertIndex(-2, -2, -2, map.toIndex(-257L, -257L, -257L));
		assertIndex(-2, -2, -2, map.toIndex(-512L, -512L, -512L));
		assertIndex(-3, -3, -3, map.toIndex(-513L, -513L, -513L));
	}

	@Test
	public void testToIndexAxesAreIndependent() {
		var map = newMap();
		assertIndex(0, -1, 1, map.toIndex(10L, -10L, 300L));
		assertIndex(0, -1, 1, map.toIndex(10.0, -10.0, 300.0));
		assertIndex(0, -1, 1, map.toIndex(10.0f, -10.0f, 300.0f));
	}

	@Test
	public void testToIndexOverloadsAgree() {
		var map = newMap();
		long[][] values = {
				{0, 0, 0},
				{255, 255, 255},
				{256, 256, 256},
				{-1, -1, -1},
				{-256, -256, -256},
				{-257, -257, -257},
				{-1024, -700, 1024},
		};
		for (var v : values) {
			var byLong = map.toIndex(v[0], v[1], v[2]);
			var byDouble = map.toIndex((double)v[0], (double)v[1], (double)v[2]);
			var byFloat = map.toIndex((float)v[0], (float)v[1], (float)v[2]);
			assertEquals(byLong, byDouble);
			assertEquals(byLong, byFloat);
		}
	}

	@Test
	public void testToIndexFractional() {
		var map = newMap();
		// floor(255.9 / 256) == 0；floor(-0.1 / 256) == -1
		assertEquals(map.toIndex(255L, 255L, 255L), map.toIndex(255.9, 255.9, 255.9));
		assertEquals(map.toIndex(-1L, -1L, -1L), map.toIndex(-0.1, -0.1, -0.1));
		assertIndex(-1, -1, -1, map.toIndex(-0.1f, -0.1f, -0.1f));
		assertIndex(-2, -2, -2, map.toIndex(-256.5, -256.5, -256.5));
	}

	/**
	 * 格子处处等宽：任意一段等长区间的格子数相同。
	 * 向零截断时 0 号格宽 2*size，负半轴同样长度会多出/少掉一格。
	 */
	@Test
	public void testGridIsUniform() {
		var map = newMap();
		// size=256，格宽 256：每段 1024 长度恰好 4 格，且 0 号格居中
		assertEquals(4, cellCount(map, 0, 1023));
		assertEquals(4, cellCount(map, -1024, -1)); // 旧实现：[-255,0) 被 0 号格吃掉，负侧多出 1 格
		assertEquals(2, cellCount(map, -512, -1)); // 旧实现：同上，多出 1 格
		assertEquals(1, cellCount(map, -256, -1)); // -1 与 0 必须分属两格
		assertEquals(1, cellCount(map, -512, -257));
		assertEquals(3, cellCount(map, -256, 511));
		// 最小 size=1：每格宽 1，格子数与坐标数相同
		var one = newMap(1);
		assertEquals(2, cellCount(one, -1, 0));
		assertEquals(5, cellCount(one, -2, 2));
	}

	/**
	 * 邻域查询按格号取包围盒，格宽不等时中心的负邻格取错：
	 * size=256、center=-1（-1 号格），应见 -2、-1、0 三格。
	 */
	@Test
	public void testNeighborRange() {
		var map = newMap();
		map.onEnter(1, 255L, 0L, 0L); // 0 号格
		map.onEnter(2, 256L, 0L, 0L); // 1 号格
		map.onEnter(3, -1L, 0L, 0L); // -1 号格
		map.onEnter(4, -256L, 0L, 0L); // -1 号格，与 3 同格
		// 0 号格两侧必须各有一格邻居
		var around0 = map.getCubes(0L, 0L, 0L, 1, 0, 0);
		assertEquals(3, around0.size()); // x = -1, 0, 1
		assertEquals(3, map.getCubes(0.0, 0.0, 0.0, 1, 0, 0).size());
		assertEquals(3, map.getCubes(0.0f, 0.0f, 0.0f, 1, 0, 0).size());
		assertTrue(objectsAt(map, -1L, 0L, 0L).contains(3));
		assertTrue(objectsAt(map, -1L, 0L, 0L).contains(4));
		assertTrue(objectsAt(map, 255L, 0L, 0L).contains(1));
		assertTrue(objectsAt(map, 256L, 0L, 0L).contains(2));
		// 以负坐标为中心：-1 号格的邻居含 -2 号格，故 -257 必须落在查询结果里
		map.onEnter(5, -257L, 0L, 0L); // -2 号格
		var aroundNeg = map.getCubes(-1L, 0L, 0L, 1, 0, 0);
		assertEquals(3, aroundNeg.size()); // x = -2, -1, 0，旧实现只有 2 格
		assertEquals(3, map.getCubes(-1.0, 0.0, 0.0, 1, 0, 0).size());
		assertEquals(3, map.getCubes(-1.0f, 0.0f, 0.0f, 1, 0, 0).size());
		// range=0 的 -2 号格就在上面这个包围盒里
		assertTrue(aroundNeg.containsAll(map.getCubes(-257L, 0L, 0L, 0, 0, 0)));
		// range=0 只含 -1 号格，取不到 -2 号格的 5
		assertFalse(objectsAt(map, -1L, 0L, 0L).contains(5));
		// -256 与 -257 分属两格
		assertFalse(objectsAt(map, -256L, 0L, 0L).contains(5));
	}

	@Test
	public void testOnMoveCrossBoundary() {
		var map = newMap();
		map.onEnter(1, 255L, 0L, 0L);
		// 255 与 256 跨格（旧实现两者都在 0 号格，会误判为未移动）
		assertTrue(map.onMove(1, 255L, 0L, 0L, 256L, 0L, 0L));
		assertCubeGone(map, 0, 0, 0); // 0 号格随最后一个对象离开被回收
		assertTrue(objectsAt(map, 256L, 0L, 0L).contains(1));
		// 0 与 -1 跨格（旧实现两者都在 0 号格）
		assertTrue(map.onMove(1, 0L, 0L, 0L, -1L, 0L, 0L));
		assertTrue(objectsAt(map, -1L, 0L, 0L).contains(1));
		// -256 与 -257 跨格（旧实现两者都在 -1 号格）
		assertTrue(map.onMove(1, -256L, 0L, 0L, -257L, 0L, 0L));
		assertTrue(objectsAt(map, -257L, 0L, 0L).contains(1));
		// 同一格内移动不换格
		assertFalse(map.onMove(1, -257L, 0L, 0L, -300L, 0L, 0L));
		assertFalse(map.onMove(1, -300L, 0L, 0L, -511L, 0L, 0L));
		assertTrue(objectsAt(map, -511L, 0L, 0L).contains(1));
		// double/float 重载跨格判定
		map.onEnter(2, -0.5, 0.0, 0.0);
		assertTrue(map.onMove(2, -0.5, 0.0, 0.0, 0.5, 0.0, 0.0));
		assertFalse(map.onMove(2, 0.5, 0.0, 0.0, 255.9, 0.0, 0.0));
		assertTrue(map.onMove(2, 255.9f, 0.0f, 0.0f, 256.1f, 0.0f, 0.0f));
		assertTrue(objectsAt(map, 256L, 0L, 0L).contains(2));
	}

	/**
	 * onEnter / onLeave 必须命中同一个 cube：
	 * 全部 leave 后每个坐标的 cube 都被回收，邻域查询归零。
	 */
	@Test
	public void testEnterLeaveRoundTrip() {
		var map = newMap();
		long[] xs = {-1, -256, -257, -512, -513, 0, 255, 256};
		for (int i = 0; i < xs.length; ++i)
			map.onEnter(i, xs[i], 0L, 0L);
		for (int i = 0; i < xs.length; ++i)
			map.onLeave(i, xs[i], 0L, 0L);
		for (long x : xs)
			assertCubeGone(map, x, 0, 0);
	}
}
