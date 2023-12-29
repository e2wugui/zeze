package Zeze.World.Astar;

import java.util.HashMap;

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
 * ResourceMapView(offsetWidth, offsetHeight, width, height);
 */
public class ScanNodes {
	private final long maxScanNodes;
	private final HashMap<NodeIndex, Node> nodes = new HashMap<>();
	private int onClosedList;  // 记录，节点是否是关闭的标志。用来避免初始化节点。
	private int onOpenList;

	public ScanNodes() {
		this(100 * 1024 * 1024);
	}

	public ScanNodes(long maxScanNodes) {
		this.maxScanNodes = maxScanNodes;
	}

	public Node getNode(NodeIndex index) {
		var node = nodes.computeIfAbsent(index, Node::new);
		if (nodes.size() > maxScanNodes) {
			nodes.clear();
			throw new TooManyScanNodes();
		}
		return node;
	}

	public void initialize() {
		if (onClosedList == 0) {
			// 标志已经超出整数范围，循环回来。重新初始化一次节点的标志位。
			onOpenList   = 1;
			onClosedList = 2;
		} else {
			onOpenList   += 2;
			onClosedList += 2;
		}
	}

	public void open(Node node, Node parent, int cost, Node target) {
		node.open(parent, cost, target);
		node.whichList = onOpenList;
	}

	public void close(Node node) {
		node.whichList = onClosedList;
	}

	public boolean isClosed(Node node) {
		return onClosedList == node.whichList;
	}

	public boolean isOpen(Node node) {
		return onOpenList == node.whichList;
	}
}
