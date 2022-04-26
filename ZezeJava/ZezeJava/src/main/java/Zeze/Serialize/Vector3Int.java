package Zeze.Serialize;

public class Vector3Int extends Vector2Int {
	private int z;

	public int getZ() { return z; }

	public Vector3Int() {

	}

	public Vector3Int(Vector2Int v2) {
		super(v2.getX(), v2.getY());
	}

	public Vector3Int(int x, int y, int z) {
		super(x, y);
		this.z = z;
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
}
