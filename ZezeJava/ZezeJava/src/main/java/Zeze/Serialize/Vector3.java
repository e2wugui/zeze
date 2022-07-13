package Zeze.Serialize;

public class Vector3 extends Vector2 {
	private float z;

	public Vector3() {
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
		z = v3.getZ();
	}

	public Vector3(Vector2 v2) {
		super(v2);
	}

	public Vector3(Vector2Int v2) {
		super(v2);
	}

	public float getZ() {
		return z;
	}

	@Override
	public boolean isZero() {
		return getX() == 0 & getY() == 0 & z == 0;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		super.Encode(bb);
		bb.WriteFloat(z);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		super.Decode(bb);
		z = bb.ReadFloat();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != Vector3.class)
			return false;
		Vector3 v = (Vector3)o;
		return getX() == v.getX() && getY() == v.getY() && z == v.z;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Float.floatToRawIntBits(z);
	}

	@Override
	public String toString() {
		return "Vector3(" + getX() + ',' + getY() + ',' + z + ')';
	}
}
