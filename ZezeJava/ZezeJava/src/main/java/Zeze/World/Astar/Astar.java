package Zeze.World.Astar;

import Zeze.Serialize.Vector3;
import Zeze.Util.FastPriorityQueue;
import java.util.Deque;

public class Astar {
	private final ScanNodes scanNodes;
	private final FastPriorityQueue<Node> openList;

	public Astar(ScanNodes scanNodes) {
		this.scanNodes = scanNodes;
		this.openList = new FastPriorityQueue<>(1000, Integer.MAX_VALUE);
	}

	public boolean find(IResourceMap map, Vector3 fromV3, Vector3 toV3, Deque<Node> path /* out */) {
		if (scanNodes.isOutofRange(map))
			return false; // 地图大小超过限制（see ScanNodesFixed 说明）

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
			var parent = openList.dequeue();
			scanNodes.close(parent);

			// ugly 可以提高10%性能。循环的写法也比较烦。很多判断。代码后便注释里面。
			ACORNER(map, openList, parent, target, -1, -1, 7, 0, -1, -1, 0);
			ACROSS(map, openList, parent, target, 0, -1, 5);
			ACORNER(map, openList, parent, target, +1, -1, 7, 0, -1, +1, 0);
			ACROSS(map, openList, parent, target, -1,  0, 5);
			ACROSS(map, openList, parent, target, +1,  0, 5);
			ACORNER(map, openList, parent, target, -1, +1, 7, -1, 0,  0, +1);
			ACROSS(map, openList, parent, target, 0, +1, 5);
			ACORNER(map, openList, parent, target,+1, +1, 7, +1, 0, 0, +1);

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

	public void ACORNER(IResourceMap map, FastPriorityQueue<Node> openList,
						Node parent, Node target,
						int cx, int cz, int cost, int cx0, int cz0, int cx1, int cz1) {
		var to = map.toIndex(parent.index.x + cx, parent.index.z + cz);
		if (map.walkable(parent.index.x, parent.index.z, to.x, to.z)
				&& map.walkable(parent.index.x, parent.index.z, parent.index.x + cx0, parent.index.z + cz0)
				&& map.walkable(parent.index.x, parent.index.z, parent.index.x + cx1, parent.index.z + cz1)) {
			var child = scanNodes.getNode(to);
			if (!scanNodes.isClosed(child)) {
				if (!scanNodes.isOpen(child)) {
					scanNodes.open(child, parent, cost, target);
					openList.enqueue(child);
				} else {
					if (child.adjust(parent, cost))
						openList.updatePriority(child);
				}
			}
		}
	}

	public void ACROSS(IResourceMap amap, FastPriorityQueue<Node> openList,
					   Node parent, Node target,
					   int cx, int cy, int cost) {
		var to = amap.toIndex(parent.index.x + cx, parent.index.z + cy);
		if (amap.walkable(parent.index.x, parent.index.z, to.x, to.z)) {
			var child = scanNodes.getNode(to);
			if (!scanNodes.isClosed(child)) {
				if (!scanNodes.isOpen(child)) {
					scanNodes.open(child, parent, cost, target);
					openList.enqueue(child);
				} else {
					if (child.adjust(parent, cost))
						openList.updatePriority(child);
				}
			}
		}
	}
}
