package Zeze.Util;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 把三维空间划分成一个个相邻的Cube。
 * 地图中的玩家或者物品Id记录在所在的Cube中。
 * 用来快速找到某个坐标周围的玩家或物体。
 */
public class CubeIndexMap<TCube extends Cube<TObject>, TObject> {
	private final ConcurrentHashMap<CubeIndex, TCube> Cubes = new ConcurrentHashMap<>();
	private final int CubeSizeX;
	private final int CubeSizeY;
	private final int CubeSizeZ;
	private final Factory<TCube> Factory;

	public final int getCubeSizeX() {
		return CubeSizeX;
	}

	public final int getCubeSizeY() {
		return CubeSizeY;
	}

	public final int getCubeSizeZ() {
		return CubeSizeZ;
	}

	public final CubeIndex ToIndex(double x, double y, double z) {
		CubeIndex tempVar = new CubeIndex();
		tempVar.setX((long)(x / getCubeSizeX()));
		tempVar.setY((long)(y / getCubeSizeY()));
		tempVar.setZ((long)(z / getCubeSizeZ()));
		return tempVar;
	}

	public final CubeIndex ToIndex(float x, float y, float z) {
		CubeIndex tempVar = new CubeIndex();
		tempVar.setX((long)(x / getCubeSizeX()));
		tempVar.setY((long)(y / getCubeSizeY()));
		tempVar.setZ((long)(z / getCubeSizeZ()));
		return tempVar;
	}

	public final CubeIndex ToIndex(long x, long y, long z) {
		CubeIndex tempVar = new CubeIndex();
		tempVar.setX(x / getCubeSizeX());
		tempVar.setY(y / getCubeSizeY());
		tempVar.setZ(z / getCubeSizeZ());
		return tempVar;
	}

	public CubeIndexMap(Factory<TCube> factory, int cubeSizeX, int cubeSizeY, int cubeSizeZ) {
		Factory = factory;

		if (cubeSizeX <= 0) {
			throw new IllegalArgumentException("cubeSizeX <= 0");
		}
		if (cubeSizeY <= 0) {
			throw new IllegalArgumentException("cubeSizeY <= 0");
		}
		if (cubeSizeZ <= 0) {
			throw new IllegalArgumentException("cubeSizeZ <= 0");
		}

		CubeSizeX = cubeSizeX;
		CubeSizeY = cubeSizeY;
		CubeSizeZ = cubeSizeZ;
	}

	public interface CubeHandle<TCube> {
		void handle(CubeIndex index, TCube cube);
	}

	/**
	 * perform action if cube exist.
	 * under lock (cube)
	 */
	public final void TryPerform(CubeIndex index, CubeHandle<TCube> action) {
		TCube cube = Cubes.get(index);
		if (null != cube) {
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (cube) {
				if (cube.getState() != Cube.StateRemoved) {
					action.handle(index, cube);
				}
			}
		}
	}

	/**
	 * perform action for Cubes.GetOrAdd.
	 * under lock (cube)
	 */
	public final void Perform(CubeIndex index, CubeHandle<TCube> action) {
		while (true) {
			TCube cube = Cubes.computeIfAbsent(index, (key) -> Factory.create());
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (cube) {
				if (cube.getState() == Cube.StateRemoved) {
					continue;
				}
				action.handle(index, cube);
				break;
			}
		}
	}

	/**
	 * 角色进入地图时
	 */
	public final void OnEnter(TObject obj, double x, double y, double z) {
		Perform(ToIndex(x, y, z), (index, cube) -> cube.Add(index, obj));
	}

	public final void OnEnter(TObject obj, float x, float y, float z) {
		Perform(ToIndex(x, y, z), (index, cube) -> cube.Add(index, obj));
	}

	public final void OnEnter(TObject obj, long x, long y, long z) {
		Perform(ToIndex(x, y, z), (index, cube) -> cube.Add(index, obj));
	}

	public final void OnEnter(TObject obj, CubeIndex index) {
		Perform(index, (index2, cube) -> cube.Add(index2, obj));
	}

	private void RemoveObject(CubeIndex index, TCube cube, TObject obj) {
		if (cube.Remove(index, obj)) {
			cube.setState(Cube.StateRemoved);
			Cubes.remove(index, cube);
		}
	}

	private boolean OnMove(CubeIndex oIndex, CubeIndex nIndex, TObject obj) {
		if (oIndex.equals(nIndex)) {
			return false;
		}

		TryPerform(oIndex, (index, cube) -> RemoveObject(index, cube, obj));
		Perform(nIndex, (index, cube) -> cube.Add(index, obj));
		return true;
	}

	/**
	 * 角色位置变化时，
	 * return true 如果cube发生了变化。
	 * return false 还在原来的cube中。
	 */
	public final boolean OnMove(TObject obj, double oldX, double oldY, double oldZ, double newX, double newY, double newZ) {
		return OnMove(ToIndex(oldX, oldY, oldZ), ToIndex(newX, newY, newZ), obj);
	}

	public final boolean OnMove(TObject obj, float oldX, float oldY, float oldZ, float newX, float newY, float newZ) {
		return OnMove(ToIndex(oldX, oldY, oldZ), ToIndex(newX, newY, newZ), obj);
	}

	public final boolean OnMove(TObject obj, long oldX, long oldY, long oldZ, long newX, long newY, long newZ) {
		return OnMove(ToIndex(oldX, oldY, oldZ), ToIndex(newX, newY, newZ), obj);
	}

	public final boolean OnMove(TObject obj, CubeIndex oldIndex, CubeIndex newIndex) {
		return OnMove(oldIndex, newIndex, obj);
	}

	/**
	 * 角色离开地图时
	 */
	public final void OnLeave(TObject obj, double x, double y, double z) {
		TryPerform(ToIndex(x, y, z), (index, cube) -> RemoveObject(index, cube, obj));
	}

	public final void OnLeave(TObject obj, float x, float y, float z) {
		TryPerform(ToIndex(x, y, z), (index, cube) -> RemoveObject(index, cube, obj));
	}

	public final void OnLeave(TObject obj, long x, long y, long z) {
		TryPerform(ToIndex(x, y, z), (index, cube) -> RemoveObject(index, cube, obj));
	}

	public final void OnLeave(TObject obj, CubeIndex index) {
		TryPerform(index, (index2, cube) -> RemoveObject(index2, cube, obj));
	}

	public final ArrayList<TCube> GetCubes(CubeIndex center, int rangeX, int rangeY, int rangeZ) {
		ArrayList<TCube> result = new ArrayList<>();
		for (long i = center.getX() - rangeX; i <= center.getX() + rangeX; ++i) {
			for (long j = center.getY() - rangeY; j <= center.getY() + rangeY; ++j) {
				for (long k = center.getZ() - rangeZ; k <= center.getZ() + rangeZ; ++k) {
					var index = new CubeIndex();
					index.setX(i);
					index.setY(j);
					index.setZ(k);
					TCube cube = Cubes.get(index);
					if (null != cube)
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

	public final java.util.ArrayList<TCube> GetCubes(double centerX, double centerY, double centerZ, int rangeX, int rangeY) {
		return GetCubes(centerX, centerY, centerZ, rangeX, rangeY, 4);
	}

	public final java.util.ArrayList<TCube> GetCubes(double centerX, double centerY, double centerZ, int rangeX) {
		return GetCubes(centerX, centerY, centerZ, rangeX, 4, 4);
	}

	public final java.util.ArrayList<TCube> GetCubes(double centerX, double centerY, double centerZ) {
		return GetCubes(centerX, centerY, centerZ, 4, 4, 4);
	}

	public final ArrayList<TCube> GetCubes(double centerX, double centerY, double centerZ, int rangeX, int rangeY, int rangeZ) {
		return GetCubes(ToIndex(centerX, centerY, centerZ), rangeX, rangeY, rangeZ);
	}

	public final java.util.ArrayList<TCube> GetCubes(float centerX, float centerY, float centerZ, int rangeX, int rangeY) {
		return GetCubes(centerX, centerY, centerZ, rangeX, rangeY, 4);
	}

	public final java.util.ArrayList<TCube> GetCubes(float centerX, float centerY, float centerZ, int rangeX) {
		return GetCubes(centerX, centerY, centerZ, rangeX, 4, 4);
	}

	public final java.util.ArrayList<TCube> GetCubes(float centerX, float centerY, float centerZ) {
		return GetCubes(centerX, centerY, centerZ, 4, 4, 4);
	}

	public final ArrayList<TCube> GetCubes(float centerX, float centerY, float centerZ, int rangeX, int rangeY, int rangeZ) {
		return GetCubes(ToIndex(centerX, centerY, centerZ), rangeX, rangeY, rangeZ);
	}

	public final java.util.ArrayList<TCube> GetCubes(long centerX, long centerY, long centerZ, int rangeX, int rangeY) {
		return GetCubes(centerX, centerY, centerZ, rangeX, rangeY, 4);
	}

	public final java.util.ArrayList<TCube> GetCubes(long centerX, long centerY, long centerZ, int rangeX) {
		return GetCubes(centerX, centerY, centerZ, rangeX, 4, 4);
	}

	public final java.util.ArrayList<TCube> GetCubes(long centerX, long centerY, long centerZ) {
		return GetCubes(centerX, centerY, centerZ, 4, 4, 4);
	}

	public final ArrayList<TCube> GetCubes(long centerX, long centerY, long centerZ, int rangeX, int rangeY, int rangeZ) {
		return GetCubes(ToIndex(centerX, centerY, centerZ), rangeX, rangeY, rangeZ);
	}
}
