package Zeze.Serialize;

public class Vector4 extends Vector3 {
	private float w;

	public float getW() { return w; }

	public Vector4() {

	}

	public Vector4(Vector2 v2) {
		super(v2);
	}
	public Vector4(Vector3 v3) {
		super(v3.getX(), v3.getY(), v3.getZ());
	}

	public Vector4(float x, float y, float z, float w) {
		super(x, y, z);
		this.w = w;
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
}
