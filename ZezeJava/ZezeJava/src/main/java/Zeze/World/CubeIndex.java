package Zeze.World;

import Zeze.Serialize.Vector3;

public class CubeIndex implements Comparable<CubeIndex> {
	public final int x;
	public final int y;
	public final int z;

	public CubeIndex() {
		x = 0;
		y = 0;
		z = 0;
	}

	public Vector3 toVector3() {
		return new Vector3(x, y, z);
	}

	public CubeIndex(int x, int z) {
		this.x = x;
		this.y = 0;
		this.z = z;
	}

	public CubeIndex(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + Integer.hashCode(x);
		result = prime * result + Integer.hashCode(y);
		result = prime * result + Integer.hashCode(z);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj instanceof CubeIndex) {
			CubeIndex other = (CubeIndex)obj;
			return x == other.x && y == other.y && z == other.z;
		}
		return false;
	}

	@Override
	public final int compareTo(CubeIndex other) {
		int c = Long.compare(x, other.x);
		if (c != 0)
			return c;
		c = Long.compare(y, other.y);
		if (c != 0)
			return c;
		return Long.compare(z, other.z);
	}

	@Override
	public String toString() {
		return "(" + x + "," + y + "," + z + ")";
	}

	/**
	 * 垂直距离。x,y,z轴方向上的最大差距的绝对值。
	 *
	 * @param other 另一个索引
	 * @return 垂直距离。
	 */
	public long distancePerpendicular(CubeIndex other) {
		var dx = Math.abs(x - other.x);
		var dy = Math.abs(y - other.y);
		var dz = Math.abs(z - other.z);

		return Math.max(Math.max(dx, dy), dz);
	}
}
