package Zeze.World;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Serialize.Vector3;

/**
 * 把二维空间划分成一个个相邻的Grid。
 * 地图中的玩家或者物品记录在所在的Grid中。
 * 用来快速找到某个坐标周围的实体。
 */
public class CubeIndexMap {
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
		var y = (long)(vector3.y / gridY);
		var z = gridZ != 0 ? (long)(vector3.z / gridZ) : 0;
		return new CubeIndex(x, y, z);
	}

	public final CubeIndex toIndex(float _x, float _y, float _z) {
		var x = (long)(_x / gridX);
		var y = (long)(_y / gridY);
		var z = gridZ != 0 ? (long)(_z / gridZ) : 0;
		return new CubeIndex(x, y, z);
	}

	/**
	 * 构造地图实例-2d切割，参数为切割长宽。
	 * @param gridX 切割长度
	 * @param gridY 切割宽度
	 */
	public CubeIndexMap(int gridX, int gridY) {
		this(gridX, gridY, 0);
	}

	/**
	 * 构造地图实例-3d切割，参数为切割长宽。
	 * @param gridX 切割长度
	 * @param gridY 切割宽度
	 * @param gridZ 切割高度
	 */
	public CubeIndexMap(int gridX, int gridY, int gridZ) {
		if (gridX <= 0)
			throw new IllegalArgumentException("cubeSizeX <= 0");
		if (gridY <= 0)
			throw new IllegalArgumentException("cubeSizeY <= 0");
		if (gridZ < 0) // gridZ 可以为0，表示2d切割。
			throw new IllegalArgumentException("cubeSizeZ < 0");

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

	// 优化的Bresenham算法,不包括源端点,只支持 7fffffff 以内的坐标
	// 这个算法从以前 share/astar(a*) 扒出来的，原来的算法是int，现扩展到long，移位操作16改成了32，常量加了两个字节。
	public static void bresenham2d(long srcx, long srcy, long dstx, long dsty, Action2dLong plot) {
		if (srcx == dstx && srcy == dsty)
			return;

		boolean ylonger = false;
		var lenmin = dsty - srcy;
		var lenmax = dstx - srcx;
		if(Math.abs(lenmin) > Math.abs(lenmax))
		{
			// swap(lenmin, lenmax)
			var tmp = lenmin;
			lenmin = lenmax;
			lenmax = tmp;
			ylonger = true;
		}
		var delta = (lenmax == 0 ? 0 : (lenmin << 32) / lenmax);
		delta += delta < 0 ? 1 : 0;  // 更接近原Bresenham算法的修正

		if(ylonger)
		{
			if(lenmax > 0)
			{
				lenmax += srcy;
				for(var i = 0x7fffffff + (srcx << 32) + delta; ++srcy <= lenmax; i += delta)
					plot.run(i >> 32, srcy);

				return; // done
			}
			lenmax += srcy;
			for(var i = 0x80000000 + (srcx << 32) - delta; --srcy >= lenmax; i -= delta)
				plot.run(i >> 32, srcy);

			return; // done
		}

		if(lenmax > 0)
		{
			lenmax += srcx;
			for(var j = 0x7fffffff + (srcy << 32) + delta; ++srcx <= lenmax; j += delta)
				plot.run(srcx, j >> 32);

			return; // done
		}
		lenmax += srcx;
		for(var j = 0x80000000 + (srcy << 32) - delta; --srcx >= lenmax; j -= delta)
			plot.run(srcx, j >> 32);
		// return; // done
	}

	////////////////////////////////////////////////////////////
	// 2d
	// 选出position面向direct方向distance距离内的cube。
	// direct 向量有约定？就是归一化什么的？不清楚。
	public final SortedMap<CubeIndex, Cube> line2d(Vector3 position, Vector3 direct, int distance) {
		// todo 计算结束cube的索引。
		var endX = position.x + distance * direct.x;
		var endY = position.x + distance * direct.x;

		var endIndex = toIndex(endX, endY, 0);
		var beginIndex = toIndex(position);

		// lineTo(beginIndex, endIndex)，收集直线路径上的所有cube。
		var result = new TreeMap<CubeIndex, Cube>();
		result.put(beginIndex, getOrAdd(beginIndex));
		bresenham2d(beginIndex.x, beginIndex.y, endIndex.x, endIndex.y,
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
