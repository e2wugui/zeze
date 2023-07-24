package Zeze.World.Astar;

import Zeze.Serialize.Vector3;

public interface IResourceMap {
	default int getWidth() {
		return 1024 * 30 / 16;
	}

	default int getHeight() {
		return 768 * 30 / 16;
	}

	default float getUnitWidth() {
		return 16;
	}

	default float getUnitHeight() {
		return 16;
	}

	default NodeIndex toIndex(Vector3 position) {
		// position.y 未用!

		// 这是grid2d的实现.
		var xi = (int)(position.x / getUnitWidth());
		var zi = (int)(position.z / getUnitHeight());
		return toIndex(xi, zi);
	}

	default NodeIndex toIndex(int x, int z) {
		// 这是grid2d的实现.
		return new NodeIndex(x, z, 0);
	}

	// 判断一个单位的可达
	// grid2d 返回可达,
	// voxel 总是true.
	boolean walkable(int x, int z);

	// 从from到to是否可达,from,to是相邻的.
	// grid2d 放回walkable(to.x, to.z);
	// voxel 判断斜率;
	default boolean walkable(NodeIndex from, int toX, int toZ, int toYIndex) {
		return walkable(toX, toZ);
	}

	/**
	 * 搜索邻居.
	 *
	 * @param astar astar
	 * @param current current
	 * @param target target,这个参数抽象不好,属于上下文信息.
	 */
	void traverseNeighbors(IIAstar astar, Node current, Node target);
}
