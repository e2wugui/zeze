package Zeze.Serialize;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
	public int compareTo(@NotNull Vector2 v) {
		int c = Float.compare(x, v.x);
		if (c != 0)
			return c;
		c = Float.compare(y, v.y);
		if (c != 0)
			return c;
		if (!(v instanceof Vector3))
			return 1;
		c = Float.compare(z, ((Vector3)v).z);
		if (c != 0)
			return c;
		if (!(v instanceof Vector4))
			return 1;
		c = Float.compare(w, ((Vector4)v).w);
		if (c != 0)
			return c;
		return v.getClass() == Quaternion.class ? 0 :
				v.getClass() == Vector4.class ? 1 : -1;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != Quaternion.class)
			return false;
		Vector4 v = (Vector4)o;
		return Float.floatToRawIntBits(x) == Float.floatToRawIntBits(v.x) &&
				Float.floatToRawIntBits(y) == Float.floatToRawIntBits(v.y) &&
				Float.floatToRawIntBits(z) == Float.floatToRawIntBits(v.z) &&
				Float.floatToRawIntBits(w) == Float.floatToRawIntBits(v.w);
	}

	@Override
	public @NotNull String toString() {
		return "Quaternion(" + x + ',' + y + ',' + z + ',' + w + ')';
	}

	@Override
	public @NotNull Quaternion normalized() {
		double magnitude = Math.sqrt((double)x * x + (double)y * y + (double)z * z + (double)w * w);
		if (magnitude > 1e-6f) {
			double f = 1 / magnitude;
			return new Quaternion((float)(x * f), (float)(y * f), (float)(z * f), (float)(w * f));
		}
		return this;
	}
}
