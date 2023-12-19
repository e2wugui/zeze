package Zeze.World.Astar;

import java.util.Deque;
import Zeze.Serialize.Vector3;

// 开始是为了 ResourceMapView
// 顺便抽象出来,以后也可以实现不同的astar.
public interface IAstar {
	// 寻路接口.
	// 坐标是世界坐标.
	boolean find(IResourceMap map, Vector3 fromV3, Vector3 toV3, Deque<Node> path /* out */);

	// ResourceMap2D 专用, 参数有点丑.看看能不能调整一下.
	// ResourceMapVoxel 如果也需要直接走斜对角,需要重构.
	void traverseCorner(IResourceMap map,
						Node current, Node target,
						int cx, int cz, int cost, int cx0, int cz0, int cx1, int cz1);

	// ResourceMap2D, ResourceMapVoxel 都使用.
	// FIXME cost 参数去掉,让astar实现自己内部定义?
	void traverseCross(IResourceMap map,
					   Node current, Node target,
					   NodeIndex to, int cost);
}
