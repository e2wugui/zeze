package Zeze.World.Astar;

import java.io.Closeable;
import Zeze.Serialize.Vector3;

public interface IResourceMap extends Closeable {
	long getWidth();

	long getHeight();

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

	default NodeIndex toIndex(long x, long z) {
		// 这是grid2d的实现.
		return new NodeIndex(x, z, 0);
	}

	// 判断一个单位的可达
	// grid2d 返回可达,
	// voxel 总是true.
	boolean walkable(long x, long z);

	// 从from到to是否可达,from,to是相邻的.
	// grid2d 放回walkable(to.x, to.z);
	// voxel 判断斜率;
	// to展开,没用Index类型是为了在不是必要的时候少创建一个对象.
	// from是已经扫描过的Node,index肯定创建好了.
	// 这样有点不一致的感觉,但也有好处,不同的类型比起一串int要好理解一点.
	default boolean walkable(NodeIndex from, long toX, long toZ, long toYIndex) {
		return walkable(toX, toZ);
	}

	/**
	 * 搜索邻居.
	 *
	 * @param astar astar
	 * @param current current
	 * @param target target,这个参数抽象不好,属于上下文信息.
	 */
	void traverseNeighbors(IAstar astar, Node current, Node target);
}
