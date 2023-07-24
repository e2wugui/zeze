package Zeze.World.Astar;

/**
 * 一次性分配所有nodes。避免内存分配。
 * 用于服务器。
 * 【更多？】
 * 增加offsetW,offsetH，实现这样的特性：
 * 可达地图IResourceMap(mapWidth, mapHeight)，
 * 搜索范围(findWidth,findHeight),
 * ScanNodes(>=findWidth,>=findHeight)，
 * 也就是说，ScanNodes只需要适应搜索，可以比可达地图范围小。
 * 这对于ai是很合适的。
 *
 * IResourceMap 有width,height，可以包装成一个新的限制搜索范围，
 * ResourceMapLimit(offsetWidth, offsetHeight, width, height);
 */
public class ScanNodesFixed implements IScanNodes {
	private final int maxWidth;
	private final int maxHeight;
	private final Node[][] nodes;
	private int onClosedList;  // 记录，节点是否是关闭的标志。用来避免初始化节点。
	private int onOpenList;

	private void allocate() {
		for (var i = 0; i < this.maxWidth; ++i) {
			for (var j = 0; j < this.maxHeight; ++j)
				nodes[i][j] = new Node();
		}
	}

	public ScanNodesFixed(int maxWidth, int maxHeight) {
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
		this.nodes = new Node[this.maxWidth][];
		allocate();
	}

	@Override
	public boolean isOutofRange(IResourceMap amap) {
		return amap.getWidth() > maxWidth || amap.getHeight() > maxHeight;
	}

	@Override
	public int getMaxWidth() {
		return maxWidth;
	}

	@Override
	public int getMaxHeight() {
		return maxHeight;
	}

	@Override
	public Node getNode(int x, int z) {
		return nodes[x][z];
	}

	@Override
	public void initialize() {
		if (onClosedList == 0) {
			// 标志已经超出整数范围，循环回来。重新初始化一次节点的标志位。
			allocate();
			onOpenList   = 1;
			onClosedList = 2;
		} else {
			onOpenList   += 2;
			onClosedList += 2;
		}
	}

	@Override
	public void open(Node node, Node parent, int cost, Node target) {
		node.open(parent, cost, target);
		node.whichList = onOpenList;
	}

	@Override
	public void close(Node node) {
		node.whichList = onClosedList;
	}

	@Override
	public boolean isClosed(Node node) {
		return onClosedList == node.whichList;
	}

	@Override
	public boolean isOpen(Node node) {
		return onOpenList == node.whichList;
	}
}
