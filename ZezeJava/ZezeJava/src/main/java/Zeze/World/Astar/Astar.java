package Zeze.World.Astar;

import Zeze.Serialize.Vector3;
import Zeze.Util.FastPriorityQueue;
import java.util.Deque;

public class Astar implements IAstar {
	private final ScanNodes scanNodes;
	private final FastPriorityQueue<Node> openList;

	public Astar(ScanNodes scanNodes) {
		this.scanNodes = scanNodes;
		this.openList = new FastPriorityQueue<>(1000, Integer.MAX_VALUE);
	}

	@Override
	public boolean find(IResourceMap map, Vector3 fromV3, Vector3 toV3, Deque<Node> path /* out */) {
		var to = map.toIndex(toV3);
		if (!map.walkable(to.x, to.z))
			return false; // 目标点不可达,起始点允许是障碍

		var from = map.toIndex(fromV3);
		if (from.equals(to))
			return false; // 起始点==目标点，后面的算法不能处理这种情况。

		// Reset some variables that need to be cleared
		scanNodes.initialize();

		// 准备起始点和目标点。把起始点加入到打开列表。
		var target = scanNodes.getNode(to);
		var start = scanNodes.getNode(from);
		start.gcost = 0; // 设置起始点的权值，作为初始parent的参数。fcost 没有使用，不初始化了。
		openList.enqueue(start);

		// 开始查找，如果打开列表为空。查找失败。
		while (openList.count() > 0) {
			// Pop the first item off the open list.
			var current = openList.dequeue();
			scanNodes.close(current);
			map.traverseNeighbors(this, current, target);
			// 当目标加入打开列表，查找成功。
			if (scanNodes.isOpen(target)) {
				path.clear();
				for (var p = target; p != start; p = p.parent)
					path.addFirst(p);
				//path.add(0, start); // insert start.
				openList.clear();
				return true;
			}
		}
		openList.clear();
		return false;
	}

	/*
	 * 尝试进入斜对角格子.
	 * ResourceMap2D 专用.
	 * 参数风格统一?
	 */
	@Override
	public void traverseCorner(IResourceMap map,
							   Node current, Node target,
							   int cx, int cz, int cost, int cx0, int cz0, int cx1, int cz1) {
		var to = map.toIndex(current.index.x + cx, current.index.z + cz);
		if (map.walkable(current.index, to.x, to.z, 0)
				&& map.walkable(current.index, current.index.x + cx0, current.index.z + cz0, 0)
				&& map.walkable(current.index, current.index.x + cx1, current.index.z + cz1, 0)) {
			var child = scanNodes.getNode(to);
			if (!scanNodes.isClosed(child)) {
				if (!scanNodes.isOpen(child)) {
					scanNodes.open(child, current, cost, target);
					openList.enqueue(child);
				} else {
					if (child.adjust(current, cost))
						openList.updatePriority(child);
				}
			}
		}
	}

	/*
	 * 尝试进入正交临近格子.
	 */
	@Override
	public void traverseCross(IResourceMap map,
							  Node current, Node target,
							  NodeIndex to, int cost) {
		if (map.walkable(current.index, to.x, to.z, to.yIndex)) {
			var child = scanNodes.getNode(to);
			if (!scanNodes.isClosed(child)) {
				if (!scanNodes.isOpen(child)) {
					scanNodes.open(child, current, cost, target);
					openList.enqueue(child);
				} else {
					if (child.adjust(current, cost))
						openList.updatePriority(child);
				}
			}
		}
	}
}
