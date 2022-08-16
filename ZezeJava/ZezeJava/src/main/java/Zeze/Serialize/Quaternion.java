package Zeze.Serialize;

public class Quaternion extends Vector4 {
	public Quaternion() {
		super(0, 0, 0, 0);
	}

	public Quaternion(float x, float y, float z, float w) {
		super(x, y, z, w);
	}

	public Quaternion(Vector4 v4) {
		super(v4);
	}

	public Quaternion(Vector3 v3) {
		super(v3);
	}

	public Quaternion(Vector3Int v3) {
		super(v3);
	}

	public Quaternion(Vector2 v2) {
		super(v2);
	}

	public Quaternion(Vector2Int v2) {
		super(v2);
	}

	@Override
	public Quaternion clone() {
		return (Quaternion)super.clone();
	}

	@Override
	public String toString() {
		return "Quaternion(" + x + ',' + y + ',' + z + ',' + w + ')';
	}
}
