#pragma once

#include "Serializable.h"
#include "Compare.h"

namespace Zeze {
	class Vector2Int;

	class Vector2 : public Serializable {
	public:
		float x;
		float y;

		Vector2() : x(0), y(0) {}

		Vector2(float x_, float y_) : x(x_), y(y_) {}

		Vector2(const Vector2& v) : x(v.x), y(v.y) {}
		Vector2(const Vector2Int& v);

		virtual bool isZero() const {
			return x == 0 && y == 0;
		}

		virtual void Encode(ByteBuffer& bb) const override;
		virtual void Decode(ByteBuffer& bb) override;
	};

	class Vector2Int : public Serializable {
	public:
		int x;
		int y;

		Vector2Int() : x(0), y(0) {}

		Vector2Int(int x_, int y_) : x(x_), y(y_) {}

		Vector2Int(const Vector2& v) : x(v.x), y(v.y) {}
		Vector2Int(const Vector2Int& v) : x(v.x), y(v.y) {}

		virtual int CompareTo(const Vector2Int& other) const {
			if (&other == this)
				return 0;
			int c;
			c = Integer::Compare(x, other.x);
			if (c != 0)
				return c;
			c = Integer::Compare(y, other.y);
			return c;
		}

		bool operator < (const Vector2Int& other) const {
			return CompareTo(other) < 0;
		}

		virtual bool isZero() const {
			return x == 0 && y == 0;
		}

		virtual void Encode(ByteBuffer& bb) const override;
		virtual void Decode(ByteBuffer& bb) override;
	};

	class Vector3Int;

	class Vector3 : public Vector2 {
	public:
		float z;

		Vector3() : z(0) {}

		Vector3(float x_, float y_, float z_) : Vector2(x_, y_), z(z_) {}

		Vector3(const Vector2& v) : Vector2(v.x, v.y), z(0) {}
		Vector3(const Vector2Int& v) : Vector2(v.x, v.y), z(0) {}
		Vector3(const Vector3& v) : Vector3(v.x, v.y, v.z) {}
		Vector3(const Vector3Int& v);

		virtual bool isZero() const override {
			return x == 0 && y == 0 && z == 0;
		}

		virtual void Encode(ByteBuffer& bb) const override;
		virtual void Decode(ByteBuffer& bb) override;
	};

	class Vector3Int : public Vector2Int {
	public:
		int z;

		Vector3Int() : z(0) {}

		Vector3Int(int x_, int y_, int z_) : Vector2Int(x_, y_), z(z_) {}

		Vector3Int(const Vector2& v) : Vector2Int(v.x, v.y), z(0) {}
		Vector3Int(const Vector2Int& v) : Vector2Int(v.x, v.y), z(0) {}
		Vector3Int(const Vector3& v) : Vector2Int(v.x, v.y), z(v.z) {}
		Vector3Int(const Vector3Int& v) : Vector2Int(v.x, v.y), z(v.z) {}

		virtual bool isZero() const {
			return x == 0 && y == 0 && z == 0;
		}

		virtual int CompareTo(const Vector3Int& other) const {
			if (&other == this)
				return 0;
			int c;
			c = Integer::Compare(x, other.x);
			if (c != 0)
				return c;
			c = Integer::Compare(y, other.y);
			if (c != 0)
				return c;
			c = Integer::Compare(z, other.z);
			return c;
		}

		bool operator < (const Vector3Int& other) const {
			return CompareTo(other) < 0;
		}

		virtual void Encode(ByteBuffer& bb) const override;
		virtual void Decode(ByteBuffer& bb) override;
	};

	class Vector4 : public Vector3 {
	public:
		float w;

		Vector4() : w(0) {}

		Vector4(float x_, float y_, float z_, float w_) : Vector3(x_, y_, z_), w(w_) {}

		Vector4(const Vector2& v) : Vector3(v.x, v.y, 0), w(0) {}
		Vector4(const Vector2Int& v) : Vector3(v.x, v.y, 0), w(0) {}
		Vector4(const Vector3& v) : Vector3(v.x, v.y, v.z), w(0) {}
		Vector4(const Vector3Int& v) : Vector3(v.x, v.y, v.z), w(0) {}
		Vector4(const Vector4& v) : Vector3(v.x, v.y, v.z), w(v.w) {}

		virtual bool isZero() const override {
			return x == 0 && y == 0 && z == 0 && w == 0;
		}

		virtual void Encode(ByteBuffer& bb) const override;
		virtual void Decode(ByteBuffer& bb) override;
	};

	class Quaternion : public Vector4 {
	public:
		Quaternion() : Vector4() {}
		Quaternion(float x_, float y_, float z_, float w_) : Vector4(x_, y_, z_, w_) {}

		Quaternion(const Vector2& v) : Vector4(v.x, v.y, 0, 0) {}
		Quaternion(const Vector2Int& v) : Vector4(v.x, v.y, 0, 0) {}
		Quaternion(const Vector3& v) : Vector4(v.x, v.y, v.z, 0) {}
		Quaternion(const Vector3Int& v) : Vector4(v.x, v.y, v.z, 0) {}
		Quaternion(const Vector4& v) : Vector4(v.x, v.y, v.z, v.w) {}
	};
}
