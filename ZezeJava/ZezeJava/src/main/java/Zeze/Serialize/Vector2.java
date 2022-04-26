package Zeze.Serialize;

public class Vector2 implements Serializable {
	private float x;
	private float y;

	public float getX() { return x; }
	public float getY() { return y; }

	public Vector2() {

	}

	public Vector2(float x, float y) {
		this.x = x;
		this.y = y;
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
}
