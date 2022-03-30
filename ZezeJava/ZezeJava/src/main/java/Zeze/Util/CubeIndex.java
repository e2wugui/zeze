package Zeze.Util;

public class CubeIndex implements java.lang.Comparable<CubeIndex> {
	private long X;
	public final long getX() {
		return X;
	}
	public final void setX(long value) {
		X = value;
	}
	private long Y;
	public final long getY() {
		return Y;
	}
	public final void setY(long value) {
		Y = value;
	}
	private long Z;
	public final long getZ() {
		return Z;
	}
	public final void setZ(long value) {
		Z = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + Long.hashCode(getX());
		result = prime * result + Long.hashCode(getY());
		result = prime * result + Long.hashCode(getZ());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		boolean tempVar = obj instanceof CubeIndex;
		CubeIndex other = tempVar ? (CubeIndex)obj : null;
		if (tempVar) {
			return getX() == other.getX() && getY() == other.getY() && getZ() == other.getZ();
		}
		return false;
	}

	@Override
	public final int compareTo(CubeIndex other) {
		int c = Long.compare(getX(), other.getX());
		if (c != 0) {
			return c;
		}
		c = Long.compare(getY(), other.getY());
		if (c != 0) {
			return c;
		}
		return Long.compare(getZ(), other.getZ());
	}
}
