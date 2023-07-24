package Zeze.World.Astar;

import java.util.List;
import java.util.Vector;
import javax.persistence.criteria.CriteriaBuilder;
import Zeze.Serialize.Vector3;
import Zeze.World.CubeIndex;

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

	default Index toIndex(Vector3 position) {
		// position.y 未用!

		// 这是grid2d的实现.
		var xi = (int)(position.x / getUnitWidth());
		var zi = (int)(position.z / getUnitHeight());
		return new Index(xi, zi, 0);
	}

	default Index toIndex(int x, int z) {
		// 这是grid2d的实现.
		return new Index(x, z, 0);
	}

	// 判断一个单位的可达
	// grid2d 返回可达,
	// voxel 总是true.
	boolean walkable(int x, int z);

	// 从from到to是否可达,from,to是相邻的.
	// grid2d 放回walkable(to.x, to.z);
	// voxel 判断斜率;
	default boolean walkable(int fromX, int fromZ, int toX, int toZ) {
		return walkable(toX, toZ);
	}

	// 返回当前格子的周围可走格子.
	// [问题],对于grid2d,voxel,直接就是4-direct or 8-direct.
	// 抽象效率比较低.
	List<CubeIndex> ways(int x, int z);
}
