package Zeze.Serialize;

public class Quaternion extends Vector4 {
	public Quaternion() {

	}

	public Quaternion(Vector2 v2) {
		super(v2);
	}
	public Quaternion(Vector3 v3) {
		super(v3);
	}
	public Quaternion(Vector4 v4) {
		super(v4.getX(), v4.getY(), v4.getZ(), v4.getW());
	}

	public Quaternion(float x, float y, float z, float w) {
		super(x, y, z, w);
	}
}
