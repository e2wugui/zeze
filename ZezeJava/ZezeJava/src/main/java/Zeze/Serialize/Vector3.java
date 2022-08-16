package Zeze.Serialize;

public class Vector3 extends Vector2 {
	public final float z;

	public Vector3() {
		super(0, 0);
		z = 0;
	}

	public Vector3(float x, float y, float z) {
		super(x, y);
		this.z = z;
	}

	public Vector3(Vector3 v3) {
		super(v3);
		z = v3.z;
	}

	public Vector3(Vector3Int v3) {
		super(v3);
		z = v3.z;
	}

	public Vector3(Vector2 v2) {
		super(v2);
		z = 0;
	}

	public Vector3(Vector2Int v2) {
		super(v2);
		z = 0;
	}

	@Override
	public boolean isZero() {
		return x == 0 & y == 0 & z == 0;
	}

	@Override
	public int compareTo(Object o) {
		Vector3 v = (Vector3)o;
		int c = Float.compare(x, v.x);
		if (c != 0)
			return c;
		c = Float.compare(y, v.y);
		return c != 0 ? c : Float.compare(z, v.z);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != Vector3.class)
			return false;
		Vector3 v = (Vector3)o;
		return x == v.x && y == v.y && z == v.z;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Float.floatToRawIntBits(z);
	}

	@Override
	public Vector3 clone() {
		return (Vector3)super.clone();
	}

	@Override
	public String toString() {
		return "Vector3(" + x + ',' + y + ',' + z + ')';
	}
}
