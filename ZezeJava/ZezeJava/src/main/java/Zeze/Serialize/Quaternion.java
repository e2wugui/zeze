package Zeze.Serialize;

import org.jetbrains.annotations.NotNull;

public class Quaternion extends Vector4 {
	public static final Quaternion ZERO = new Quaternion(0, 0, 0, 0);

	public Quaternion(float x, float y, float z, float w) {
		super(x, y, z, w);
	}

	public Quaternion(@NotNull Vector4 v4) {
		super(v4);
	}

	public Quaternion(@NotNull Vector3 v3) {
		super(v3);
	}

	public Quaternion(@NotNull Vector3Int v3) {
		super(v3);
	}

	public Quaternion(@NotNull Vector2 v2) {
		super(v2);
	}

	public Quaternion(@NotNull Vector2Int v2) {
		super(v2);
	}

	@Override
	public @NotNull Quaternion clone() {
		return (Quaternion)super.clone();
	}

	@Override
	public @NotNull String toString() {
		return "Quaternion(" + x + ',' + y + ',' + z + ',' + w + ')';
	}

	@Override
	public Quaternion normalized() {
		var len = Math.sqrt((double)x * x + (double)y * y + (double)z * z + (double)w * w);
		if (len > 1e-6f)
			return new Quaternion((float)(x / len), (float)(y / len), (float)(z / len), (float)(w / len));
		return this;
	}
}
