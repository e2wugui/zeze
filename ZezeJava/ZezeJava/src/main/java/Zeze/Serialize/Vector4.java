package Zeze.Serialize;

public class Vector4 extends Vector3 {
	public final float w;

	public Vector4() {
		super(0, 0, 0);
		w = 0;
	}

	public Vector4(float x, float y, float z, float w) {
		super(x, y, z);
		this.w = w;
	}

	public Vector4(Vector4 v4) {
		super(v4);
		w = v4.w;
	}

	public Vector4(Vector3 v3) {
		super(v3);
		w = 0;
	}

	public Vector4(Vector3Int v3) {
		super(v3);
		w = 0;
	}

	public Vector4(Vector2 v2) {
		super(v2);
		w = 0;
	}

	public Vector4(Vector2Int v2) {
		super(v2);
		w = 0;
	}

	@Override
	public boolean isZero() {
		return x == 0 & y == 0 & z == 0 & w == 0;
	}

	@Override
	public int compareTo(Object o) {
		Vector4 v = (Vector4)o;
		int c = Float.compare(x, v.x);
		if (c != 0)
			return c;
		c = Float.compare(y, v.y);
		if (c != 0)
			return c;
		c = Float.compare(z, v.z);
		return c != 0 ? c : Float.compare(w, v.w);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != Vector4.class && o.getClass() != Quaternion.class)
			return false;
		Vector4 v = (Vector4)o;
		return x == v.x && y == v.y && z == v.z && w == v.w;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Float.floatToRawIntBits(w);
	}

	@Override
	public Vector4 clone() {
		return (Vector4)super.clone();
	}

	@Override
	public String toString() {
		return "Vector4(" + x + ',' + y + ',' + z + ',' + w + ')';
	}
}
