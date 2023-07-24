package Zeze.World.Astar;

import java.util.List;
import Zeze.World.CubeIndex;

public interface IResourceMap {
	int getWidth();
	int getHeight();

	// 判断一个单位的可达
	// grid2d 返回可达,
	// voxel 总是true.
	boolean walkable(int x, int z);

	// 从from到to是否可达,from,to是相邻的.
	// grid2d 放回walkable(to.x, to.z);
	// voxel 判断斜率;
	boolean walkable(int fromX, int fromZ, int toX, int toZ);

	// 返回当前格子的周围可走格子.
	// [问题],对于grid2d,voxel,直接就是4-direct or 8-direct.
	// 抽象效率比较低.
	List<CubeIndex> ways(int x, int z);
}
