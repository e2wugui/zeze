package Zeze.Serialize;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Vector3Int extends Vector2Int {
	public static final Vector3Int ZERO = new Vector3Int(0, 0, 0);

	public final int z;

	public Vector3Int(int x, int y, int z) {
		super(x, y);
		this.z = z;
	}

	public Vector3Int(@NotNull Vector3Int v3) {
		super(v3);
		z = v3.z;
	}

	public Vector3Int(@NotNull Vector3 v3) {
		super(v3);
		z = (int)v3.z;
	}

	public Vector3Int(@NotNull Vector2Int v2) {
		super(v2);
		z = 0;
	}

	public Vector3Int(@NotNull Vector2 v2) {
		super(v2);
		z = 0;
	}

	@Override
	public boolean isZero() {
		return (x | y | z) == 0;
	}

	@Override
	public int compareTo(@NotNull Vector2Int o) {
		Vector3Int v = (Vector3Int)o;
		int c = Integer.compare(x, v.x);
		if (c != 0)
			return c;
		c = Integer.compare(y, v.y);
		return c != 0 ? c : Integer.compare(z, v.z);
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != Vector3Int.class)
			return false;
		Vector3Int v = (Vector3Int)o;
		return x == v.x && y == v.y && z == v.z;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ z;
	}

	@Override
	public @NotNull Vector3Int clone() {
		return (Vector3Int)super.clone();
	}

	@Override
	public @NotNull String toString() {
		return "Vector3Int(" + x + ',' + y + ',' + z + ')';
	}
}
