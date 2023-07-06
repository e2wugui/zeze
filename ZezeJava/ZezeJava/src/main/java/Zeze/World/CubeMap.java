package Zeze.World;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Serialize.Vector3;

/**
 * 把二维空间划分成一个个相邻的Grid。
 * 地图中的玩家或者物品记录在所在的Grid中。
 * 用来快速找到某个坐标周围的实体。
 */
public class CubeMap {
	private final ConcurrentHashMap<CubeIndex, Cube> cubs = new ConcurrentHashMap<>();
	private final int gridX;
	private final int gridY;
	private final int gridZ;

	public final int getGridX() {
		return gridX;
	}

	public final int getGridY() {
		return gridY;
	}
	public final int getGridZ() {
		return gridZ;
	}

	public final CubeIndex toIndex(Vector3 vector3) {
		var x = (long)(vector3.x / gridX);
		var y = gridY != 0 ? (long)(vector3.y / gridY) : 0;
		var z = (long)(vector3.z / gridZ);
		return new CubeIndex(x, y, z);
	}

	public final CubeIndex toIndex(float _x, float _y, float _z) {
		var x = (long)(_x / gridX);
		var y = gridY != 0 ? (long)(_z / gridY) : 0;
		var z = (long)(_y / gridZ);
		return new CubeIndex(x, y, z);
	}

	/**
	 * 构造地图实例-2d切割，参数为切割长宽。
	 * @param gridX 切割长度
	 * @param gridZ 切割宽度
	 */
	public CubeMap(int gridX, int gridZ) {
		this(gridX, 0, gridZ);
	}

	/**
	 * 构造地图实例-3d切割，参数为切割长宽。
	 * @param gridX 切割长度
	 * @param gridY 切割宽度
	 * @param gridZ 切割高度
	 */
	public CubeMap(int gridX, int gridY, int gridZ) {
		if (gridX <= 0)
			throw new IllegalArgumentException("cubeSizeX <= 0");
		if (gridY < 0)
			throw new IllegalArgumentException("cubeSizeY < 0");
		if (gridZ <= 0) // gridZ 可以为0，表示2d切割。
			throw new IllegalArgumentException("cubeSizeZ <= 0");

		this.gridX = gridX;
		this.gridY = gridY;
		this.gridZ = gridZ;
	}

	public final Cube getOrAdd(CubeIndex index) {
		return cubs.computeIfAbsent(index, (key) -> new Cube());
	}

	public final void collect(SortedMap<CubeIndex, Cube> result, CubeIndex index) {
		result.put(index, getOrAdd(index));
	}

	////////////////////////////////////////////////////////////
	// 2d
	// 选出一个封闭多边形包含的cube。
	// points，两两之间是一个线段，首尾连起来。
	public final SortedMap<CubeIndex, Cube> polygon2d(java.util.List<Vector3> points, boolean convex) {
		// cube-box 包围体
		var boxMinX = Long.MAX_VALUE;
		var boxMinZ = Long.MAX_VALUE;

		var boxMaxX = Long.MIN_VALUE;
		var boxMaxZ = Long.MIN_VALUE;

		// 转换成把CubeIndex看成点的多边形。
		var cubePoints = new ArrayList<CubeIndex>();
		for (var point : points) {
			var index = toIndex(point);

			if (index.x < boxMinX) boxMinX = index.x;
			if (index.x > boxMaxX) boxMaxX = index.x;
			if (index.z < boxMinZ) boxMinZ = index.z;
			if (index.z > boxMaxZ) boxMaxZ = index.z;

			// 第一个cube或者跟最近的一个不相等的时候加入。
			if (cubePoints.isEmpty() || !cubePoints.get(cubePoints.size() - 1).equals(index))
				cubePoints.add(index);
		}

		//System.out.println(cubePoints);
		//System.out.println(boxMinX + "~" + boxMaxX + ", " + boxMinZ + "~" + boxMaxZ);
		var result = new TreeMap<CubeIndex, Cube>();
		if (cubePoints.size() < 3) {
			for (var index : cubePoints)
				collect(result, index);
			return result;
			// 不可能构成多边形。这实际上是多数情况。
		}

		// 边线用画线法得到。【因为后面的多边形内判断方式得不到边线的cube??? 需确认，另外画线法比后面的判断快】
		for (int i = 0, j = cubePoints.size() - 1; i < cubePoints.size(); j = i, ++i) {
			var p1 = cubePoints.get(i);
			var p2 = cubePoints.get(j);
			//System.out.println("bresenham2d " + p1 + "->" + p2);
			Graphics2D.bresenham2d(p1.x, p1.z, p2.x, p2.z, (x, z) -> {
				var index = new CubeIndex(x, 0, z);
				//System.out.println("collect " + index);
				collect(result, index);
			});
		}

		// 【注意】绝大多数情况，box都是空的。所以根本不会进行多边形判断。
		// box 边线上面已经处理了，这里判空一下。
		var boxEmpty = boxMaxX - boxMinX <= 1 || boxMaxZ - boxMinZ <= 1;
		if (boxEmpty)
			return result;

		if (convex) {
			for (var i = boxMinX; i <= boxMaxX; ++i) {
				for (var k = boxMinZ; k <= boxMaxZ; ++k) {
					var index = new CubeIndex(i, 0, k);
					if (Graphics2D.insideConvexPolygon(index, cubePoints))
						collect(result, index);
				}
			}
		} else {
			for (var i = boxMinX; i <= boxMaxX; ++i) {
				for (var k = boxMinZ; k <= boxMaxZ; ++k) {
					var index = new CubeIndex(i, 0, k);
					if (Graphics2D.insidePolygon(index, cubePoints))
						collect(result, index);
				}
			}
		}
		return result;
	}

	// 选出position开始，面向direct方向，distance距离，直线路径经过的cube。
	public final SortedMap<CubeIndex, Cube> line2d(Vector3 position, Vector3 direct, int distance) {
		// todo 计算结束cube的索引。
		// direct 向量有约定？就是归一化什么的？不清楚。
		var endX = position.x + distance * direct.x;
		var endY = position.x + distance * direct.x;

		var endIndex = toIndex(endX, endY, 0);
		var beginIndex = toIndex(position);

		var result = new TreeMap<CubeIndex, Cube>();
		Graphics2D.bresenham2d(beginIndex.x, beginIndex.z, endIndex.x, endIndex.z,
				(x, y) -> collect(result, new CubeIndex(x, y)));
		return result;
	}

	/**
	 * 返回 center 为中心，+-rangeX, +- rangeY 范围的内的所有Cube，2d切割没有z轴方向.
	 * @param center 中心
	 * @param rangeX x轴左右
	 * @param rangeY y轴左右
	 * @return cubes
	 */
	public final SortedMap<CubeIndex, Cube> center2d(CubeIndex center, int rangeX, int rangeY) {
		var result = new TreeMap<CubeIndex, Cube>();
		for (long i = center.x - rangeX; i <= center.x + rangeX; ++i) {
			for (long j = center.y - rangeY; j <= center.y + rangeY; ++j) {
				var index = new CubeIndex(i, j);
				result.put(index, getOrAdd(index));
			}
		}
		return result;
	}

	public final SortedMap<CubeIndex, Cube> center2d(Vector3 center, int rangeX, int rangeY) {
		return center2d(toIndex(center), rangeX, rangeY);
	}

	public final SortedMap<CubeIndex, Cube> center2d(float centerX, float centerY, float centerZ, int rangeX, int rangeY) {
		return center2d(toIndex(centerX, centerY, centerZ), rangeX, rangeY);
	}

	////////////////////////////////////////////////////////////
	// 3d
	public final SortedMap<CubeIndex, Cube> center3d(Vector3 center, int rangeX, int rangeY, int rangeZ) {
		return center3d(toIndex(center), rangeX, rangeY,rangeZ);
	}

	public final SortedMap<CubeIndex, Cube> center3d(float centerX, float centerY, float centerZ,
													 int rangeX, int rangeY, int rangeZ) {
		return center3d(toIndex(centerX, centerY, centerZ), rangeX, rangeY,rangeZ);
	}

	public final SortedMap<CubeIndex, Cube> center3d(CubeIndex center, int rangeX, int rangeY, int rangeZ) {
		if (gridZ == 0)
			throw new RuntimeException("cube 3d not enabled.");

		var result = new TreeMap<CubeIndex, Cube>();
		for (long i = center.x - rangeX; i <= center.x + rangeX; ++i) {
			for (long j = center.y - rangeY; j <= center.y + rangeY; ++j) {
				for (long k = center.z - rangeZ; k <= center.z + rangeZ; ++k) {
					var index = new CubeIndex(i, j, k);
					result.put(index, getOrAdd(index));
				}
			}
		}
		return result;
	}
}
