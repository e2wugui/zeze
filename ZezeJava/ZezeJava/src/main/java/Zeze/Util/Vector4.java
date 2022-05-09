package Zeze.Util;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

public class Vector4 implements Serializable {
	public float x;
	public float y;
	public float z;
	public float w;

	public Vector4() {
	}

	public Vector4(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	public Vector4(Vector4 v) {
		x = v.x;
		y = v.y;
		z = v.z;
		w = v.w;
	}

	public void assign(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	public void assign(Vector4 v) {
		x = v.x;
		y = v.y;
		z = v.z;
		w = v.w;
	}

	public boolean isZero() {
		return (Float.floatToRawIntBits(x) |
				Float.floatToRawIntBits(y) |
				Float.floatToRawIntBits(z) |
				Float.floatToRawIntBits(w)) == 0;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteFloat(x);
		bb.WriteFloat(y);
		bb.WriteFloat(z);
		bb.WriteFloat(w);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		x = bb.ReadFloat();
		y = bb.ReadFloat();
		z = bb.ReadFloat();
		w = bb.ReadFloat();
	}

	public void Decode(ByteBuffer bb, int type) {
		type &= ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.VECTOR4) {
			x = bb.ReadFloat();
			y = bb.ReadFloat();
			z = bb.ReadFloat();
			w = bb.ReadFloat();
		} else
			bb.SkipUnknownField(type);
	}

	public static Vector4 create(ByteBuffer bb) {
		var v = new Vector4();
		v.Decode(bb);
		return v;
	}

	public static Vector4 create(ByteBuffer bb, int type) {
		var v = new Vector4();
		v.Decode(bb, type);
		return v;
	}

	@Override
	public int getPreAllocSize() {
		return 16;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != Vector4.class)
			return false;
		Vector4 v = (Vector4)o;
		return x == v.x && y == v.y && z == v.z && w == v.w;
	}

	@Override
	public int hashCode() {
		return Float.floatToRawIntBits(x) ^ Float.floatToRawIntBits(y) ^
				Float.floatToRawIntBits(z) ^ Float.floatToRawIntBits(w);
	}

	@Override
	public String toString() {
		return "Vector4(" + x + ',' + y + ',' + z + ',' + w + ')';
	}
}
