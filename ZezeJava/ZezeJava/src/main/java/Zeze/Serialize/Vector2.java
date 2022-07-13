package Zeze.Serialize;

public class Vector2 implements Serializable {
	private float x;
	private float y;

	public Vector2() {
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
		x = v2.getX();
		y = v2.getY();
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public boolean isZero() {
		return x == 0 & y == 0;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteFloat(x);
		bb.WriteFloat(y);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		x = bb.ReadFloat();
		y = bb.ReadFloat();
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
	public String toString() {
		return "Vector2(" + x + ',' + y + ')';
	}
}
