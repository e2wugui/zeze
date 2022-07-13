package Zeze.Serialize;

public class Vector4 extends Vector3 {
	private float w;

	public Vector4() {
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
	}

	public Vector4(Vector3Int v3) {
		super(v3);
	}

	public Vector4(Vector2 v2) {
		super(v2);
	}

	public Vector4(Vector2Int v2) {
		super(v2);
	}

	public float getW() {
		return w;
	}

	@Override
	public boolean isZero() {
		return getX() == 0 & getY() == 0 & getZ() == 0 & w == 0;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		super.Encode(bb);
		bb.WriteFloat(w);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		super.Decode(bb);
		w = bb.ReadFloat();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != Vector4.class && o.getClass() != Quaternion.class)
			return false;
		Vector4 v = (Vector4)o;
		return getX() == v.getX() && getY() == v.getY() && getZ() == v.getZ() && w == v.w;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Float.floatToRawIntBits(w);
	}

	@Override
	public String toString() {
		return "Vector4(" + getX() + ',' + getY() + ',' + getZ() + ',' + w + ')';
	}
}
