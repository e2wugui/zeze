package Zeze.Util;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 邻域查询的long全域闭区间不得裸算 center±range 与 ++ 终止：
 * 溢出回绕使终点小于起点（极值邻域静默空结果），终点为Long.MAX_VALUE时
 * ++回绕到MIN令 i<=end 恒真（极值中心+零range死循环，同根因）。
 * 修复：端点饱和到long边界+到终点即break，负range显式拒绝。
 */
@Fast
public class TestCubeIndexMapBoundaryQuery {

	private static final class TestCube extends Cube<String> {
		final ArrayList<String> objects = new ArrayList<>();

		@Override
		public void add(CubeIndex index, String obj) {
			objects.add(obj);
		}

		@Override
		public boolean remove(CubeIndex index, String obj) {
			return objects.remove(obj);
		}
	}

	private static CubeIndexMap<TestCube, String> newMap() {
		return new CubeIndexMap<>(TestCube::new, 1, 1, 1);
	}

	private static CubeIndex index(long x, long y, long z) {
		var i = new CubeIndex();
		i.setX(x);
		i.setY(y);
		i.setZ(z);
		return i;
	}

	/** 极值邻域不空结果：center=MAX-1, range=4 的终点溢出回绕为负（修复前静默空结果）。 */
	@Test
	public void overflowNeighborhoodStillReturnsCubes() {
		var map = newMap();
		long nearMax = Long.MAX_VALUE - 1;
		map.onEnter("a", nearMax, nearMax, nearMax); // cubeSize=1：索引即坐标

		var cubes = map.getCubes(index(nearMax, nearMax, nearMax), 4, 4, 4);
		assertEquals(1, cubes.size(), "终点溢出回绕不得使极值邻域静默空结果");
		assertTrue(cubes.get(0).objects.contains("a"));
	}

	/**
	 * 极值中心+零range必须有限返回：修复前 ++越过MAX回绕到MIN，
	 * i<=end恒真死循环。看门狗把死循环变成限时失败。
	 */
	@Test
	public void extremeCenterZeroRangeTerminates() throws Exception {
		var map = newMap();
		var done = new CountDownLatch(1);
		var worker = Thread.ofPlatform().daemon().start(() -> {
			map.getCubes(index(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE), 0, 0, 0);
			map.getCubes(index(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE), 0, 0, 0);
			done.countDown();
		});
		assertTrue(done.await(2, TimeUnit.SECONDS), "极值中心+零range必须在有限时间返回（回绕死循环）");
	}

	/** 负range是非法输入：显式拒绝而不是回绕语义。 */
	@Test
	public void negativeRangeRejected() {
		var map = newMap();
		assertThrows(IllegalArgumentException.class, () -> map.getCubes(index(0, 0, 0), -1, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> map.getCubes(index(0, 0, 0), 0, -1, 0));
		assertThrows(IllegalArgumentException.class, () -> map.getCubes(index(0, 0, 0), 0, 0, -1));
	}

	/** 普通域查询语义保持（闭区间含两端）。 */
	@Test
	public void normalRangeSemanticsUnchanged() {
		var map = newMap();
		map.onEnter("a", 0, 0, 0);

		Assertions.assertEquals(1, map.getCubes(index(2, 2, 2), 2, 2, 2).size(), "闭区间含端点");
		Assertions.assertEquals(1, map.getCubes(index(-2, -2, -2), 2, 2, 2).size(), "闭区间含另一端");
		assertTrue(map.getCubes(index(3, 3, 3), 2, 2, 2).isEmpty(), "端点外不可达");
	}
}
