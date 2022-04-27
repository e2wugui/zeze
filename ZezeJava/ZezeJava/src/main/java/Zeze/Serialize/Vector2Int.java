package Zeze.Serialize;

public class Vector2Int implements Serializable {
	private int x;
	private int y;

	public Vector2Int() {
	}

	public Vector2Int(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Vector2Int(Vector2Int v2) {
		x = v2.getX();
		y = v2.getY();
	}

	public Vector2Int(Vector2 v2) {
		x = (int)v2.getX();
		y = (int)v2.getY();
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public boolean isZero() {
		return (x | y) != 0;
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
	public String toString() {
		return "Vector2Int(" + x + ',' + y + ')';
	}
}
