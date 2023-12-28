package Zeze.World.Astar;

import org.jetbrains.annotations.NotNull;

public class NodeIndex implements Comparable<NodeIndex> {
	public final long x;
	public final long z;
	public final long yIndex;

	public NodeIndex(long x, long z, long yIndex) {
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
		var c = Long.compare(x, o.x);
		if (0 != c)
			return c;
		c = Long.compare(z, o.z);
		if (0 != c)
			return c;
		return Long.compare(yIndex, o.yIndex);
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
