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
		CubeIndex tempVar = new CubeIndex();
		tempVar.setX((long)(vector3.x / gridX));
		tempVar.setY((long)(vector3.y / gridY));
		if (gridZ != 0) // gridZ == 0 表示2d切割。
			tempVar.setZ((long)(vector3.z / gridZ));
		return tempVar;
	}

	public final CubeIndex toIndex(float x, float y, float z) {
		CubeIndex tempVar = new CubeIndex();
		tempVar.setX((long)(x / gridX));
		tempVar.setY((long)(y / gridY));
		if (gridZ != 0) // gridZ == 0 表示2d切割。
			tempVar.setZ((long)(z / gridZ));
		return tempVar;
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

	/**
	 * 返回 center 为中心，+-rangeX, +- rangeY 范围的内的所有Cube，2d切割没有z轴方向.
	 * @param center 中心
	 * @param rangeX x轴左右
	 * @param rangeY y轴左右
	 * @return List Of Cube.
	 */
	public final SortedMap<CubeIndex, Cube> cubes2d(CubeIndex center, int rangeX, int rangeY) {
		var result = new TreeMap<CubeIndex, Cube>();
		for (long i = center.getX() - rangeX; i <= center.getX() + rangeX; ++i) {
			for (long j = center.getY() - rangeY; j <= center.getY() + rangeY; ++j) {
				var index = new CubeIndex();
				index.setX(i);
				index.setY(j);
				var cube = cubs.computeIfAbsent(index, (key) -> new Cube());
				result.put(index, cube);
			}
		}
		return result;
	}

	public final SortedMap<CubeIndex, Cube> cubes3d(Zeze.Util.CubeIndex center, int rangeX, int rangeY, int rangeZ) {
		if (gridZ == 0)
			throw new RuntimeException("cube 3d not enabled.");

		var result = new TreeMap<CubeIndex, Cube>();
		for (long i = center.getX() - rangeX; i <= center.getX() + rangeX; ++i) {
			for (long j = center.getY() - rangeY; j <= center.getY() + rangeY; ++j) {
				for (long k = center.getZ() - rangeZ; k <= center.getZ() + rangeZ; ++k) {
					var index = new CubeIndex();
					index.setX(i);
					index.setY(j);
					index.setZ(k);
					var cube = cubs.computeIfAbsent(index, (key) -> new Cube());
					result.put(index, cube);
				}
			}
		}
		return result;
	}
}
