package Zeze.World.Astar;

// for ResourceMapView
public interface IIAstar {
	void traverseCorner(IResourceMap map,
						Node current, Node target,
						int cx, int cz, int cost, int cx0, int cz0, int cx1, int cz1);
	void traverseCross(IResourceMap map,
					   Node current, Node target,
					   NodeIndex to, int cost);
}
