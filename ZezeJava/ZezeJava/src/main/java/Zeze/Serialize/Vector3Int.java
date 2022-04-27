package Zeze.Serialize;

public class Vector3Int extends Vector2Int {
	private int z;

	public Vector3Int() {
	}

	public Vector3Int(int x, int y, int z) {
		super(x, y);
		this.z = z;
	}

	public Vector3Int(Vector3Int v3) {
		super(v3);
		z = v3.z;
	}

	public Vector3Int(Vector3 v3) {
		super(v3);
		z = (int)v3.getZ();
	}

	public Vector3Int(Vector2Int v2) {
		super(v2);
	}

	public Vector3Int(Vector2 v2) {
		super(v2);
	}

	public int getZ() {
		return z;
	}

	@Override
	public boolean isZero() {
		return (getX() | getY() | z) != 0;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		super.Encode(bb);
		bb.WriteInt(z);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		super.Decode(bb);
		z = bb.ReadInt();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != Vector3Int.class)
			return false;
		Vector3Int v = (Vector3Int)o;
		return getX() == v.getX() && getY() == v.getY() && z == v.z;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ z;
	}

	@Override
	public String toString() {
		return "Vector3Int(" + getX() + ',' + getY() + ',' + z + ')';
	}
}
