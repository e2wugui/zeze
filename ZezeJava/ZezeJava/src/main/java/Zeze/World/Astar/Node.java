package Zeze.World.Astar;

import Zeze.Util.FastPriorityQueueNode;

public class Node implements FastPriorityQueueNode<Node> {
	// Node的信息多数都可以根据上下文计算得到。为了速度，采用冗余方式。
	NodeIndex index;   // 当前节点坐标。

	Node parent; // 最小权值路径的父节点。在查找过程中可能会发生改变。
	int gcost;     // 起始点到当前节点的路径权值。
	int fcost;     // 算法权值，_fcost = gcost + hcost。
	int whichList; // 记录节点状态：未用、打开、关闭。其中0表示未用，打开关闭的编号有外面分配。
	int openpos;   // 在open堆中的位置。

	public Node(NodeIndex index) {
		this.index = index;
	}

	public void open(Node parent, int cost, Node target) {
		this.parent = parent;
		this.gcost  = parent.gcost + cost;
		// 评价函数3d
		this.fcost  = this.gcost + 5 * (Math.abs(target.index.x - this.index.x)
				+ Math.abs(target.index.z - this.index.z));
	}

	public boolean adjust(Node parent, int cost) {
		int newGCost = parent.gcost + cost;
		if (newGCost < this.gcost) {
			this.fcost = newGCost + this.fcost - this.gcost;
			this.gcost = newGCost;
			this.parent = parent;
			return true;
		}
		return false;
	}

	@Override
	public int getQueueIndex() {
		return openpos;
	}

	@Override
	public void setQueueIndex(int index) {
		this.openpos = index;
	}

	@Override
	public boolean hasHigherPriority(Node lower) {
		return gcost < lower.gcost;
	}
}
