package Zeze.World;

public class GridIndex implements Comparable<GridIndex> {
	private long x;
	private long y;

	public final long getX() {
		return x;
	}

	public final void setX(long value) {
		x = value;
	}

	public final long getY() {
		return y;
	}

	public final void setY(long value) {
		y = value;
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

		if (obj instanceof GridIndex) {
			GridIndex other = (GridIndex)obj;
			return x == other.x && y == other.y;
		}
		return false;
	}

	@Override
	public final int compareTo(GridIndex other) {
		int c = Long.compare(x, other.x);
		if (c != 0)
			return c;
		return Long.compare(y, other.y);
	}
}
