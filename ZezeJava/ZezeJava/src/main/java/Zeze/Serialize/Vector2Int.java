package Zeze.Serialize;

@SuppressWarnings("rawtypes")
public class Vector2Int implements Comparable, Cloneable {
	public static final Vector2Int ZERO = new Vector2Int(0, 0);

	public final int x;
	public final int y;

	public Vector2Int(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Vector2Int(Vector2Int v2) {
		x = v2.x;
		y = v2.y;
	}

	public Vector2Int(Vector2 v2) {
		x = (int)v2.x;
		y = (int)v2.y;
	}

	public boolean isZero() {
		return (x | y) == 0;
	}

	@Override
	public int compareTo(Object o) {
		Vector2Int v = (Vector2Int)o;
		int c = Integer.compare(x, v.x);
		return c != 0 ? c : Integer.compare(y, v.y);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != Vector2Int.class)
			return false;
		Vector2Int v = (Vector2Int)o;
		return x == v.x && y == v.y;
	}

	@Override
	public int hashCode() {
		return x ^ y;
	}

	@Override
	public Vector2Int clone() {
		try {
			return (Vector2Int)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return "Vector2Int(" + x + ',' + y + ')';
	}
}
