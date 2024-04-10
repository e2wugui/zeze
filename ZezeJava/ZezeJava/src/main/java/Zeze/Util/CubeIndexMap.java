package Zeze.Util;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 把三维空间划分成一个个相邻的Cube。
 * 地图中的玩家或者物品Id记录在所在的Cube中。
 * 用来快速找到某个坐标周围的玩家或物体。
 */
public class CubeIndexMap<TCube extends Cube<TObject>, TObject> {
	private final ConcurrentHashMap<CubeIndex, TCube> cubes = new ConcurrentHashMap<>();
	private final int cubeSizeX;
	private final int cubeSizeY;
	private final int cubeSizeZ;
	private final Factory<TCube> factory;

	public final int getCubeSizeX() {
		return cubeSizeX;
	}

	public final int getCubeSizeY() {
		return cubeSizeY;
	}

	public final int getCubeSizeZ() {
		return cubeSizeZ;
	}

	public final CubeIndex toIndex(double x, double y, double z) {
		CubeIndex tempVar = new CubeIndex();
		tempVar.setX((long)(x / getCubeSizeX()));
		tempVar.setY((long)(y / getCubeSizeY()));
		tempVar.setZ((long)(z / getCubeSizeZ()));
		return tempVar;
	}

	public final CubeIndex toIndex(float x, float y, float z) {
		CubeIndex tempVar = new CubeIndex();
		tempVar.setX((long)(x / getCubeSizeX()));
		tempVar.setY((long)(y / getCubeSizeY()));
		tempVar.setZ((long)(z / getCubeSizeZ()));
		return tempVar;
	}

	public final CubeIndex toIndex(long x, long y, long z) {
		CubeIndex tempVar = new CubeIndex();
		tempVar.setX(x / getCubeSizeX());
		tempVar.setY(y / getCubeSizeY());
		tempVar.setZ(z / getCubeSizeZ());
		return tempVar;
	}

	public CubeIndexMap(Factory<TCube> factory, int cubeSizeX, int cubeSizeY, int cubeSizeZ) {
		if (cubeSizeX <= 0)
			throw new IllegalArgumentException("cubeSizeX <= 0");
		if (cubeSizeY <= 0)
			throw new IllegalArgumentException("cubeSizeY <= 0");
		if (cubeSizeZ <= 0)
			throw new IllegalArgumentException("cubeSizeZ <= 0");

		this.factory = factory;
		this.cubeSizeX = cubeSizeX;
		this.cubeSizeY = cubeSizeY;
		this.cubeSizeZ = cubeSizeZ;
	}

	public interface CubeHandle<TCube> {
		void handle(CubeIndex index, TCube cube);
	}

	/**
	 * perform action if cube exist.
	 * under lock (cube)
	 */
	public final void tryPerform(CubeIndex index, CubeHandle<TCube> action) {
		var cube = cubes.get(index);
		if (cube != null) {
			cube.lock();
			try {
				if (cube.getCubeState() != Cube.StateRemoved)
					action.handle(index, cube);
			} finally {
				cube.unlock();
			}
		}
	}

	/**
	 * perform action for Cubes.GetOrAdd.
	 * under lock (cube)
	 */
	public final void perform(CubeIndex index, CubeHandle<TCube> action) {
		while (true) {
			var cube = cubes.computeIfAbsent(index, __ -> factory.create());
			cube.lock();
			try {
				if (cube.getCubeState() == Cube.StateRemoved)
					continue;
				action.handle(index, cube);
				break;
			} finally {
				cube.unlock();
			}
		}
	}

	/**
	 * 角色进入地图时
	 */
	public final void onEnter(TObject obj, double x, double y, double z) {
		perform(toIndex(x, y, z), (index, cube) -> cube.add(index, obj));
	}

	public final void onEnter(TObject obj, float x, float y, float z) {
		perform(toIndex(x, y, z), (index, cube) -> cube.add(index, obj));
	}

	public final void onEnter(TObject obj, long x, long y, long z) {
		perform(toIndex(x, y, z), (index, cube) -> cube.add(index, obj));
	}

	public final void onEnter(TObject obj, CubeIndex index) {
		perform(index, (index2, cube) -> cube.add(index2, obj));
	}

	private void removeObject(CubeIndex index, TCube cube, TObject obj) {
		if (cube.remove(index, obj)) {
			cube.setCubeState(Cube.StateRemoved);
			cubes.remove(index, cube);
		}
	}

	private boolean onMove(CubeIndex oIndex, CubeIndex nIndex, TObject obj) {
		if (oIndex.equals(nIndex))
			return false;

		tryPerform(oIndex, (index, cube) -> removeObject(index, cube, obj));
		perform(nIndex, (index, cube) -> cube.add(index, obj));
		return true;
	}

	/**
	 * 角色位置变化时，
	 * return true 如果cube发生了变化。
	 * return false 还在原来的cube中。
	 */
	public final boolean onMove(TObject obj, double oldX, double oldY, double oldZ, double newX, double newY, double newZ) {
		return onMove(toIndex(oldX, oldY, oldZ), toIndex(newX, newY, newZ), obj);
	}

	public final boolean onMove(TObject obj, float oldX, float oldY, float oldZ, float newX, float newY, float newZ) {
		return onMove(toIndex(oldX, oldY, oldZ), toIndex(newX, newY, newZ), obj);
	}

	public final boolean onMove(TObject obj, long oldX, long oldY, long oldZ, long newX, long newY, long newZ) {
		return onMove(toIndex(oldX, oldY, oldZ), toIndex(newX, newY, newZ), obj);
	}

	public final boolean onMove(TObject obj, CubeIndex oldIndex, CubeIndex newIndex) {
		return onMove(oldIndex, newIndex, obj);
	}

	/**
	 * 角色离开地图时
	 */
	public final void onLeave(TObject obj, double x, double y, double z) {
		tryPerform(toIndex(x, y, z), (index, cube) -> removeObject(index, cube, obj));
	}

	public final void onLeave(TObject obj, float x, float y, float z) {
		tryPerform(toIndex(x, y, z), (index, cube) -> removeObject(index, cube, obj));
	}

	public final void onLeave(TObject obj, long x, long y, long z) {
		tryPerform(toIndex(x, y, z), (index, cube) -> removeObject(index, cube, obj));
	}

	public final void onLeave(TObject obj, CubeIndex index) {
		tryPerform(index, (index2, cube) -> removeObject(index2, cube, obj));
	}

	public final ArrayList<TCube> getCubes(CubeIndex center, int rangeX, int rangeY, int rangeZ) {
		var result = new ArrayList<TCube>();
		for (long i = center.getX() - rangeX; i <= center.getX() + rangeX; ++i) {
			for (long j = center.getY() - rangeY; j <= center.getY() + rangeY; ++j) {
				for (long k = center.getZ() - rangeZ; k <= center.getZ() + rangeZ; ++k) {
					var index = new CubeIndex();
					index.setX(i);
					index.setY(j);
					index.setZ(k);
					var cube = cubes.get(index);
					if (cube != null)
						result.add(cube);
				}
			}
		}
		return result;
	}

	/**
	 * 返回 center 坐标所在的 cube 周围的所有 cube。
	 * 可以遍历返回的Cube的所有角色，进一步进行精确的距离判断。
	 */

	public final ArrayList<TCube> getCubes(double centerX, double centerY, double centerZ, int rangeX, int rangeY) {
		return getCubes(centerX, centerY, centerZ, rangeX, rangeY, 4);
	}

	public final ArrayList<TCube> getCubes(double centerX, double centerY, double centerZ, int rangeX) {
		return getCubes(centerX, centerY, centerZ, rangeX, 4, 4);
	}

	public final ArrayList<TCube> getCubes(double centerX, double centerY, double centerZ) {
		return getCubes(centerX, centerY, centerZ, 4, 4, 4);
	}

	public final ArrayList<TCube> getCubes(double centerX, double centerY, double centerZ, int rangeX, int rangeY, int rangeZ) {
		return getCubes(toIndex(centerX, centerY, centerZ), rangeX, rangeY, rangeZ);
	}

	public final ArrayList<TCube> getCubes(float centerX, float centerY, float centerZ, int rangeX, int rangeY) {
		return getCubes(centerX, centerY, centerZ, rangeX, rangeY, 4);
	}

	public final ArrayList<TCube> getCubes(float centerX, float centerY, float centerZ, int rangeX) {
		return getCubes(centerX, centerY, centerZ, rangeX, 4, 4);
	}

	public final ArrayList<TCube> getCubes(float centerX, float centerY, float centerZ) {
		return getCubes(centerX, centerY, centerZ, 4, 4, 4);
	}

	public final ArrayList<TCube> getCubes(float centerX, float centerY, float centerZ, int rangeX, int rangeY, int rangeZ) {
		return getCubes(toIndex(centerX, centerY, centerZ), rangeX, rangeY, rangeZ);
	}

	public final ArrayList<TCube> getCubes(long centerX, long centerY, long centerZ, int rangeX, int rangeY) {
		return getCubes(centerX, centerY, centerZ, rangeX, rangeY, 4);
	}

	public final ArrayList<TCube> getCubes(long centerX, long centerY, long centerZ, int rangeX) {
		return getCubes(centerX, centerY, centerZ, rangeX, 4, 4);
	}

	public final ArrayList<TCube> getCubes(long centerX, long centerY, long centerZ) {
		return getCubes(centerX, centerY, centerZ, 4, 4, 4);
	}

	public final ArrayList<TCube> getCubes(long centerX, long centerY, long centerZ, int rangeX, int rangeY, int rangeZ) {
		return getCubes(toIndex(centerX, centerY, centerZ), rangeX, rangeY, rangeZ);
	}
}
