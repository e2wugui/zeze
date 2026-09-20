package UnitTest.Zeze.Util;

import Zeze.Util.Cube;
import Zeze.Util.CubeIndex;
import Zeze.Util.CubeIndexMap;
import Zeze.Util.Factory;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * U3-F1 姊妹/U1-F2：perform 把调用方可变 CubeIndex 直接用作 ConcurrentHashMap 的键，
 * 事后 setX/Y/Z 使键与哈希桶失联，cube 永久失联且无法回收。
 * 修复：perform 入口防御性拷贝后再作键；tryPerform 仅 get 按值查找，不拷贝。
 */
@Fast
@Execution(ExecutionMode.SAME_THREAD)
public class TestCubeIndexMapKeyOwnsIndex {

	private static final class TestCube extends Cube<Integer> {
		private final Set<Integer> objects = ConcurrentHashMap.newKeySet();

		@Override
		public void add(CubeIndex index, Integer obj) {
			objects.add(obj);
		}

		@Override
		public boolean remove(CubeIndex index, Integer obj) {
			objects.remove(obj);
			return objects.isEmpty();
		}
	}

	private static CubeIndexMap<TestCube, Integer> newMap() {
		return new CubeIndexMap<>(new Factory<TestCube>() {
			@Override
			public TestCube create() {
				return new TestCube();
			}
		}, 256, 256, 256);
	}

	@Test
	public void testPerformKeyOwnsIndex() {
		var map = newMap();
		var idx = map.toIndex(10L, 0L, 0L);
		map.perform(idx, (index, cube) -> cube.add(index, 1));
		// 调用方在 perform 之后变异传入的 index：map 侧的键不得受影响
		idx.setX(999);
		idx.setY(999);
		idx.setZ(999);
		var cubes = map.getCubes(10L, 0L, 0L, 0, 0, 0);
		assertEquals(1, cubes.size(), "cube 必须仍按原坐标可达（修复前键随变异失联）");
		assertTrue(cubes.get(0).objects.contains(1));
		// 原坐标可正常离开并回收 cube（修复前 removeObject 找不到条目，cube 永久泄漏）
		map.onLeave(1, 10L, 0L, 0L);
		assertEquals(0, map.getCubes(10L, 0L, 0L, 0, 0, 0).size());
	}

	/**
	 * 未变异时行为不变：同一 index 多次 perform 命中同一 cube；
	 * 变异后的 index 按值等价去往新 cube（get 按值查找）。
	 */
	@Test
	public void testUnmutatedBehaviorUnchanged() {
		var map = newMap();
		var idx = map.toIndex(10L, 0L, 0L);
		map.perform(idx, (index, cube) -> cube.add(index, 1));
		map.perform(idx, (index, cube) -> cube.add(index, 2));
		assertEquals(1, map.getCubes(10L, 0L, 0L, 0, 0, 0).size());
		// idx 未变异时其值与原键相等：tryPerform 应命中同一 cube
		var hit = new boolean[]{false};
		map.tryPerform(idx, (index, cube) -> hit[0] = cube.objects.contains(2));
		assertTrue(hit[0]);
	}
}
