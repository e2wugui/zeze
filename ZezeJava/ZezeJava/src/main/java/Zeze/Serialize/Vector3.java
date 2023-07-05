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

	public Vector3 multiply(float m) {
		return new Vector3(x * m, y * m, z * m);
	}

	public Vector3 normalized() {
		return this;
	}

	public static Vector3 cross(Vector3 left, Vector3 right) {
		var x = left.y * right.z - left.z * right.y;
		var y = left.z * right.x - left.x * right.z;
		var z = left.x * right.y - left.y * right.x;
		return new Vector3(x, y, z);
	}
}
