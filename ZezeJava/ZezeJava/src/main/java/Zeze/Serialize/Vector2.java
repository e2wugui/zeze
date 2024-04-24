package Zeze.Serialize;

import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Vector2 implements Comparable<Vector2>, Cloneable {
	public static final Vector2 ZERO = new Vector2(0, 0);

	public final float x;
	public final float y;

	public Vector2(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Vector2(@NotNull Vector2 v2) {
		x = v2.x;
		y = v2.y;
	}

	public Vector2(@NotNull Vector2Int v2) {
		x = v2.x;
		y = v2.y;
	}

	public boolean isZero() {
		return x == 0 && y == 0;
	}

	@Override
	public int compareTo(@NotNull Vector2 v) {
		int c = Float.compare(x, v.x);
		return c != 0 ? c : Float.compare(y, v.y);
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != Vector2.class)
			return false;
		Vector2 v = (Vector2)o;
		return x == v.x && y == v.y;
	}

	@Override
	public int hashCode() {
		return Float.floatToRawIntBits(x) ^ Float.floatToRawIntBits(y);
	}

	@Override
	public @NotNull Vector2 clone() {
		try {
			return (Vector2)super.clone();
		} catch (CloneNotSupportedException e) {
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	@Override
	public @NotNull String toString() {
		return "Vector2(" + x + ',' + y + ')';
	}
}
