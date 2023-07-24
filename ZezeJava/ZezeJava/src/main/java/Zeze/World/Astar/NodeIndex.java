package Zeze.World.Astar;

import org.jetbrains.annotations.NotNull;

public class NodeIndex implements Comparable<NodeIndex> {
	public final int x;
	public final int z;
	public final int yIndex;

	public NodeIndex(int x, int z, int yIndex) {
		this.x = x;
		this.z = z;
		this.yIndex = yIndex;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof NodeIndex) {
			var other = (NodeIndex)o;
			return x == other.x && z == other.z && yIndex == other.yIndex;
		}
		return false;
	}

	@Override
	public int compareTo(@NotNull NodeIndex o) {
		var c = Integer.compare(x, o.x);
		if (0 != c)
			return c;
		c = Integer.compare(z, o.z);
		if (0 != c)
			return c;
		return Integer.compare(yIndex, o.yIndex);
	}

	public NodeIndex sub(NodeIndex o, IResourceMap map) {
		var x = this.x - o.x;
		var z = this.z - o.z;
		return map.toIndex(x, z);
	}

	@Override
	public String toString() {
		return "(" + x + "," + z + " " + yIndex + ")";
	}
}
