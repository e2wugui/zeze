package Zeze.World.Astar;

import javax.persistence.criteria.CriteriaBuilder;
import org.jetbrains.annotations.NotNull;

public class Index implements Comparable<Index> {
	public final int x;
	public final int z;
	public final int yIndex;

	public Index(int x, int z, int yIndex) {
		this.x = x;
		this.z = z;
		this.yIndex = yIndex;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Index) {
			var other = (Index)o;
			return x == other.x && z == other.z && yIndex == other.yIndex;
		}
		return false;
	}

	@Override
	public int compareTo(@NotNull Index o) {
		var c = Integer.compare(x, o.x);
		if (0 != c)
			return c;
		c = Integer.compare(z, o.z);
		if (0 != c)
			return c;
		return Integer.compare(yIndex, o.yIndex);
	}

	public Index sub(Index o, IResourceMap map) {
		var x = this.x - o.x;
		var z = this.z - o.z;
		return map.toIndex(x, z);
	}

	@Override
	public String toString() {
		return "(" + x + "," + z + " " + yIndex + ")";
	}
}
