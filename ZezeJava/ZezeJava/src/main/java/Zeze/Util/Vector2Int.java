package Zeze.Util;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

public class Vector2Int implements Serializable {
	public int x;
	public int y;

	public Vector2Int() {
	}

	public Vector2Int(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Vector2Int(Vector2Int v) {
		x = v.x;
		y = v.y;
	}

	public void assign(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void assign(Vector2Int v) {
		x = v.x;
		y = v.y;
	}

	public boolean isZero() {
		return (x | y) == 0;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteInt(x);
		bb.WriteInt(y);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		x = bb.ReadInt();
		y = bb.ReadInt();
	}

	public void Decode(ByteBuffer bb, int type) {
		type &= ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.VECTOR2INT) {
			x = bb.ReadInt();
			y = bb.ReadInt();
		} else
			bb.SkipUnknownField(type);
	}

	public static Vector2Int create(ByteBuffer bb) {
		var v = new Vector2Int();
		v.Decode(bb);
		return v;
	}

	public static Vector2Int create(ByteBuffer bb, int type) {
		var v = new Vector2Int();
		v.Decode(bb, type);
		return v;
	}

	@Override
	public int preAllocSize() {
		return 8;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != Vector2Int.class)
			return false;
		Vector2Int v = (Vector2Int)o;
		return x == v.x && y == v.y;
	}

	@Override
	public int hashCode() {
		return x ^ y;
	}

	@Override
	public String toString() {
		return "Vector2Int(" + x + ',' + y + ')';
	}
}
