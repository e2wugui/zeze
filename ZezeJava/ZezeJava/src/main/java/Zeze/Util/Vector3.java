package Zeze.Util;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

public class Vector3 implements Serializable {
	public float x;
	public float y;
	public float z;

	public Vector3() {
	}

	public Vector3(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3(Vector3 v) {
		x = v.x;
		y = v.y;
		z = v.z;
	}

	public void assign(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void assign(Vector3 v) {
		x = v.x;
		y = v.y;
		z = v.z;
	}

	public boolean isZero() {
		return x == 0 & y == 0 & z == 0;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteFloat(x);
		bb.WriteFloat(y);
		bb.WriteFloat(z);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		x = bb.ReadFloat();
		y = bb.ReadFloat();
		z = bb.ReadFloat();
	}

	public void Decode(ByteBuffer bb, int type) {
		type &= ByteBuffer.TAG_MASK;
		if (type == ByteBuffer.VECTOR3) {
			x = bb.ReadFloat();
			y = bb.ReadFloat();
			z = bb.ReadFloat();
		} else
			bb.SkipUnknownField(type);
	}

	public static Vector3 create(ByteBuffer bb) {
		var v = new Vector3();
		v.Decode(bb);
		return v;
	}

	public static Vector3 create(ByteBuffer bb, int type) {
		var v = new Vector3();
		v.Decode(bb, type);
		return v;
	}

	@Override
	public int preAllocSize() {
		return 12;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || o.getClass() != Vector3.class)
			return false;
		Vector3 v = (Vector3)o;
		return x == v.x && y == v.y && z == v.z;
	}

	@Override
	public int hashCode() {
		return Float.floatToRawIntBits(x) ^ Float.floatToRawIntBits(y) ^ Float.floatToRawIntBits(z);
	}

	@Override
	public String toString() {
		return "Vector3(" + x + ',' + y + ',' + z + ')';
	}
}
