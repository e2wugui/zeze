package Zeze.Util;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

public class Vector3Int implements Serializable {
	public int x;
	public int y;
	public int z;

	public Vector3Int() {
	}

	public Vector3Int(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3Int(Vector3Int v) {
		x = v.x;
		y = v.y;
		z = v.z;
	}

	public void assign(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void assign(Vector3Int v) {
		x = v.x;
		y = v.y;
		z = v.z;
	}

	public boolean isZero() {
		return (x | y | z) != 0;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteInt4(x);
		bb.WriteInt4(y);
		bb.WriteInt4(z);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		x = bb.ReadInt4();
		y = bb.ReadInt4();
		z = bb.ReadInt4();
	}

	public void Decode(ByteBuffer bb, int type) {
		type &= ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.VECTOR3INT) {
			x = bb.ReadInt4();
			y = bb.ReadInt4();
			z = bb.ReadInt4();
		} else
			bb.SkipUnknownField(type);
	}

	public static Vector3Int create(ByteBuffer bb) {
		var v = new Vector3Int();
		v.Decode(bb);
		return v;
	}

	public static Vector3Int create(ByteBuffer bb, int type) {
		var v = new Vector3Int();
		v.Decode(bb, type);
		return v;
	}

	@Override
	public int getPreAllocSize() {
		return 12;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != Vector3Int.class)
			return false;
		Vector3Int v = (Vector3Int)o;
		return x == v.x && y == v.y && z == v.z;
	}

	@Override
	public int hashCode() {
		return x ^ y ^ z;
	}

	@Override
	public String toString() {
		return "Vector3Int(" + x + ',' + y + ',' + z + ')';
	}
}
