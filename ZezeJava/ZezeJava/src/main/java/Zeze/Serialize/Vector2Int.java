package Zeze.Serialize;

public class Vector2Int implements Serializable {
	private int x;
	private int y;

	public int getX() { return x; }
	public int getY() { return y; }

	public Vector2Int() {

	}

	public Vector2Int(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteInt(x);
		bb.WriteInt(y);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		x = bb.ReadInt();
		y = bb.ReadInt();
	}
}
