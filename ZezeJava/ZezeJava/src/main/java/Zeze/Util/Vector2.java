package Zeze.Util;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

public class Vector2 implements Serializable {
	public float x;
	public float y;

	public Vector2() {
	}

	public Vector2(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Vector2(Vector2 v) {
		x = v.x;
		y = v.y;
	}

	public void assign(Vector2 v) {
		x = v.x;
		y = v.y;
	}

	public void assign(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public boolean isZero() {
		return x == 0 & y == 0;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteFloat(x);
		bb.WriteFloat(y);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		x = bb.ReadFloat();
		y = bb.ReadFloat();
	}

	public void Decode(ByteBuffer bb, int type) {
		type &= ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.VECTOR2) {
			x = bb.ReadFloat();
			y = bb.ReadFloat();
		} else
			bb.SkipUnknownField(type);
	}

	public static Vector2 create(ByteBuffer bb) {
		var v = new Vector2();
		v.Decode(bb);
		return v;
	}

	public static Vector2 create(ByteBuffer bb, int type) {
		var v = new Vector2();
		v.Decode(bb, type);
		return v;
	}

	@Override
	public int getPreAllocSize() {
		return 8;
	}

	@Override
	public boolean equals(Object o) {
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
	public String toString() {
		return "Vector2(" + x + ',' + y + ')';
	}
}
