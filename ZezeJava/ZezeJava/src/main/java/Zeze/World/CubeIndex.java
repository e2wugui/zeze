package Zeze.World;

public class CubeIndex implements Comparable<CubeIndex> {
	private long x;
	private long y;
	private long z;

	public final long getX() {
		return x;
	}
	public final long getY() {
		return y;
	}
	public final long getZ() {
		return z;
	}
	public final void setX(long value) {
		x = value;
	}
	public final void setY(long value) {
		y = value;
	}
	public final void setZ(long value) {
		z = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + Long.hashCode(x);
		result = prime * result + Long.hashCode(y);
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
}
