package Zeze.World;

public class CubeIndex implements Comparable<CubeIndex> {
	public final long x;
	public final long y;
	public final long z;

	public CubeIndex() {
		x = 0;
		y = 0;
		z = 0;
	}

	public CubeIndex(long x, long y) {
		this.x = x;
		this.y = y;
		this.z = 0;
	}

	public CubeIndex(long x, long y, long z) {
		this.x = x;
		this.y = y;
		this.z = z;
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
