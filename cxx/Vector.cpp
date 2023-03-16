#include "Vector.h"
#include "ByteBuffer.h"

namespace Zeze {
	Vector2::Vector2(const Vector2Int& v) : x((float)v.x), y((float)v.y) {}

	void Vector2::Encode(ByteBuffer& bb) const {
		bb.WriteFloat(x);
		bb.WriteFloat(y);
	}

	void Vector2::Decode(ByteBuffer& bb) {
		x = bb.ReadFloat();
		y = bb.ReadFloat();
	}

	void Vector2Int::Encode(ByteBuffer& bb) const {
		bb.WriteInt(x);
		bb.WriteInt(y);
	}

	void Vector2Int::Decode(ByteBuffer& bb) {
		x = bb.ReadInt();
		y = bb.ReadInt();
	}

	Vector3::Vector3(const Vector3Int& v) : Vector2((float)v.x, (float)v.y), z((float)v.z) {}

	void Vector3::Encode(ByteBuffer& bb) const {
		bb.WriteFloat(x);
		bb.WriteFloat(y);
		bb.WriteFloat(z);
	}

	void Vector3::Decode(ByteBuffer& bb) {
		x = bb.ReadFloat();
		y = bb.ReadFloat();
		z = bb.ReadFloat();
	}

	void Vector3Int::Encode(ByteBuffer& bb) const {
		bb.WriteInt(x);
		bb.WriteInt(y);
		bb.WriteInt(z);
	}

	void Vector3Int::Decode(ByteBuffer& bb) {
		x = bb.ReadInt();
		y = bb.ReadInt();
		z = bb.ReadInt();
	}

	void Vector4::Encode(ByteBuffer& bb) const {
		bb.WriteFloat(x);
		bb.WriteFloat(y);
		bb.WriteFloat(z);
		bb.WriteFloat(w);
	}

	void Vector4::Decode(ByteBuffer& bb) {
		x = bb.ReadFloat();
		y = bb.ReadFloat();
		z = bb.ReadFloat();
		w = bb.ReadFloat();
	}
}
