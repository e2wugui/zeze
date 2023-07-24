package Zeze.World.Astar;

public interface IScanNodes {
	boolean isOutofRange(IResourceMap amap);
	int getMaxWidth();
	int getMaxHeight();
	Node getNode(int x, int z); // 适合grid2d, voxel，NavMesh考虑完全另建一套，不考虑抽象。
	void initialize();
	void open(Node node, Node parent, int cost, Node target);
	void close(Node node);
	boolean isClosed(Node node);
	boolean isOpen(Node node);
}
