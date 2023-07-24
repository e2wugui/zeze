package Zeze.World.Astar;

import Zeze.Util.FastPriorityQueue;
import Zeze.World.CubeIndex;
import java.util.LinkedList;

public class Astar {
	private final IScanNodes scanNodes;

	public Astar(IScanNodes scanNodes) {
		this.scanNodes = scanNodes;
	}

	public boolean find(IResourceMap amap, CubeIndex from, CubeIndex to, LinkedList<Node> path /* out */) {
		if (scanNodes.isOutofRange(amap))
			return false; // 地图大小超过限制（see ScanNodesFixed 说明）

		//if (!amap.walkable(targetX, targetY)) return false; // 目标点不可达,起始点允许是障碍
		// 这个对于体素（3d），需要from，to；对于grid2d，仅本grid即可描述，怎么抽象？

		if (from.equals(to))
			return false; // 起始点==目标点，后面的算法不能处理这种情况。

		// Reset some variables that need to be cleared
		scanNodes.initialize();
		var openList = new FastPriorityQueue<Node>(1000, Integer.MAX_VALUE);

		// 准备起始点和目标点。把起始点加入到打开列表。
		var target = scanNodes.getNode(to.x, to.y);
		var start = scanNodes.getNode(from.x, from.y);
		start.gcost = 0; // 设置起始点的权值，作为初始parent的参数。fcost 没有使用，不初始化了。
		openList.enqueue(start);

		// 开始查找，如果打开列表为空。查找失败。
		while (openList.count() > 0) {
			// Pop the first item off the open list.
			var parent = openList.dequeue();
			scanNodes.close(parent);

			// ugly 可以提高10%性能。循环的写法也比较烦。很多判断。代码后便注释里面。
			ACORNER(amap, openList, parent, target, -1, -1, 7, 0, -1, -1, 0);
			ACROSS(amap, openList, parent, target, 0, -1, 5);
			ACORNER(amap, openList, parent, target, +1, -1, 7, 0, -1, +1, 0);
			ACROSS(amap, openList, parent, target, -1,  0, 5);
			ACROSS(amap, openList, parent, target, +1,  0, 5);
			ACORNER(amap, openList, parent, target, -1, +1, 7, -1, 0,  0, +1);
			ACROSS(amap, openList, parent, target, 0, +1, 5);
			ACORNER(amap, openList, parent, target,+1, +1, 7, +1, 0, 0, +1);

			// 当目标加入打开列表，查找成功。
			if (scanNodes.isOpen(target)) {
				path.clear();
				for (var p = target; p != start; p = p.parent)
					path.add(0, p);
				//path.add(0, start); // insert start.
				return true;
			}
		}
		return false;
	}

	public void ACORNER(IResourceMap amap, FastPriorityQueue<Node> openList,
						Node parent, Node target,
						int cx, int cy, int cost, int cx0, int cy0, int cx1, int cy1) {
		var x = parent.x + cx;
		var y = parent.y + cy;
		if (amap.walkable(x, y)
				&& amap.walkable(parent.x + cx0, parent.y + cy0)
				&& amap.walkable(parent.x + cx1, parent.y + cy1)) {
			var child = scanNodes.getNode(x, y);
			if (!scanNodes.isClosed(child)) {
				if (!scanNodes.isOpen(child)) {
					scanNodes.open(child, parent, cost, target);
					openList.enqueue(child);
				} else {
					openList.updatePriority(child);
					//if (child.adjust(parent, cost))
					//	openList.adjust(child);
				}
			}
		}
	}

	public void ACROSS(IResourceMap amap, FastPriorityQueue<Node> openList,
					   Node parent, Node target,
					   int cx, int cy, int cost) {
		var x = parent.x + cx;
		var y = parent.y + cy;
		if (amap.walkable(x, y)) {
			var child = scanNodes.getNode(x, y);
			if (!scanNodes.isClosed(child)) {
				if (!scanNodes.isOpen(child)) {
					scanNodes.open(child, parent, cost, target);
					openList.enqueue(child);
				} else {
					openList.updatePriority(child);
					//if (child.adjust(parent, cost))
					//	openList.adjust(child);
				}
			}
		}
	}
}
