package Zeze.Serialize;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Vector4 extends Vector3 {
	public static final Vector4 ZERO = new Vector4(0, 0, 0, 0);

	public final float w;

	public Vector4(float x, float y, float z, float w) {
		super(x, y, z);
		this.w = w;
	}

	public Vector4(@NotNull Vector4 v4) {
		super(v4);
		w = v4.w;
	}

	public Vector4(@NotNull Vector3 v3) {
		super(v3);
		w = 0;
	}

	public Vector4(@NotNull Vector3Int v3) {
		super(v3);
		w = 0;
	}

	public Vector4(@NotNull Vector2 v2) {
		super(v2);
		w = 0;
	}

	public Vector4(@NotNull Vector2Int v2) {
		super(v2);
		w = 0;
	}

	@Override
	public boolean isZero() {
		return x == 0 && y == 0 && z == 0 && w == 0;
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
		return v.getClass() == Vector4.class ? 0 : -1;
	}

	@Override
	public float magnitude() {
		return (float)Math.sqrt((double)x * x + (double)y * y + (double)z * z + (double)w * w);
	}

	@Override
	public @NotNull Vector4 normalized() {
		double magnitude = Math.sqrt((double)x * x + (double)y * y + (double)z * z + (double)w * w);
		if (magnitude > 1e-6f) {
			double f = 1 / magnitude;
			return new Vector4((float)(x * f), (float)(y * f), (float)(z * f), (float)(w * f));
		}
		return this;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != Vector4.class)
			return false;
		Vector4 v = (Vector4)o;
		return Float.floatToRawIntBits(x) == Float.floatToRawIntBits(v.x) &&
				Float.floatToRawIntBits(y) == Float.floatToRawIntBits(v.y) &&
				Float.floatToRawIntBits(z) == Float.floatToRawIntBits(v.z) &&
				Float.floatToRawIntBits(w) == Float.floatToRawIntBits(v.w);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Float.floatToRawIntBits(w);
	}

	@Override
	public @NotNull String toString() {
		return "Vector4(" + x + ',' + y + ',' + z + ',' + w + ')';
	}
}
