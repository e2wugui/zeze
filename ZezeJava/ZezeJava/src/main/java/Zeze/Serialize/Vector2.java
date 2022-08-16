package Zeze.Serialize;

@SuppressWarnings("rawtypes")
public class Vector2 implements Comparable, Cloneable {
	public final float x;
	public final float y;

	public Vector2() {
		x = 0;
		y = 0;
	}

	public Vector2(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Vector2(Vector2 v2) {
		x = v2.x;
		y = v2.y;
	}

	public Vector2(Vector2Int v2) {
		x = v2.x;
		y = v2.y;
	}

	public boolean isZero() {
		return x == 0 & y == 0;
	}

	@Override
	public int compareTo(Object o) {
		Vector2 v = (Vector2)o;
		int c = Float.compare(x, v.x);
		return c != 0 ? c : Float.compare(y, v.y);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != Vector2.class)
			return false;
		Vector2 v = (Vector2)o;
		return x == v.x && y == v.y;
	}

	@Override
	public int hashCode() {
		return Float.floatToRawIntBits(x) ^ Float.floatToRawIntBits(y);
	}

	@Override
	public Vector2 clone() {
		try {
			return (Vector2)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return "Vector2(" + x + ',' + y + ')';
	}
}
