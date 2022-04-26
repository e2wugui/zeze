package Zeze.Serialize;

public class Vector3 extends Vector2 {
	private float z;

	public float getZ() { return z; }

	public Vector3() {

	}

	public Vector3(Vector2 v2) {
		super(v2.getX(), v2.getY());
	}

	public Vector3(float x, float y, float z) {
		super(x, y);
		this.z = z;
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
}
