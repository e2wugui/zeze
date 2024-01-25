package Zeze.Serialize;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Vector3 extends Vector2 {
	public static final Vector3 ZERO = new Vector3(0, 0, 0);

	public final float z;

	public Vector3(float x, float y, float z) {
		super(x, y);
		this.z = z;
	}

	public Vector3(@NotNull Vector3 v3) {
		super(v3);
		z = v3.z;
	}

	public Vector3(@NotNull Vector3Int v3) {
		super(v3);
		z = v3.z;
	}

	public Vector3(@NotNull Vector2 v2) {
		super(v2);
		z = 0;
	}

	public Vector3(@NotNull Vector2Int v2) {
		super(v2);
		z = 0;
	}

	@Override
	public boolean isZero() {
		return x == 0 && y == 0 && z == 0;
	}

	@Override
	public int compareTo(@NotNull Object o) {
		Vector3 v = (Vector3)o;
		int c = Float.compare(x, v.x);
		if (c != 0)
			return c;
		c = Float.compare(y, v.y);
		return c != 0 ? c : Float.compare(z, v.z);
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != Vector3.class)
			return false;
		Vector3 v = (Vector3)o;
		return x == v.x && y == v.y && z == v.z;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Float.floatToRawIntBits(z);
	}

	@Override
	public @NotNull Vector3 clone() {
		return (Vector3)super.clone();
	}

	@Override
	public @NotNull String toString() {
		return "Vector3(" + x + ',' + y + ',' + z + ')';
	}

	public Vector3 add(Vector3 b) {
		return new Vector3(x + b.x, y + b.y, z + b.z);
	}

	public Vector3 sub(Vector3 b) {
		return new Vector3(x - b.x, y - b.y, z - b.z);
	}

	public Vector3 subtract(Vector3 b) {
		return sub(b);
	}

	public Vector3 multiply(float m) {
		return new Vector3(x * m, y * m, z * m);
	}

	public Vector3 normalized() {
		double magnitude = Math.sqrt((double)x * x + (double)y * y + (double)z * z);
		if (magnitude > 1e-6f)
			return new Vector3((float)(x / magnitude), (float)(y / magnitude), (float)(z / magnitude));
		return this;
	}

	public float sqrMagnitude() {
		return x * x + y * y + z * z;
	}

	public float magnitude() {
		return (float)Math.sqrt(x * x + y * y + z * z);
	}

	public static Vector3 cross(Vector3 u, Vector3 v) {
		var x = u.y * v.z - u.z * v.y;
		var y = u.z * v.x - u.x * v.z;
		var z = u.x * v.y - u.y * v.x;
		return new Vector3(x, y, z);
	}

	public static float dot(Vector3 u, Vector3 v) {
		return u.x * v.x + u.y * v.y + u.z * v.z;
	}
}
