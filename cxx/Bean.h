#pragma once

#include <cstdint>
#include <vector>
#include <set>
#include <map>
#include <functional>
#include "ByteBuffer.h"

namespace Zeze {
	class Bean : public Serializable {
	public:
		virtual int64_t TypeId() const = 0;
		virtual void Assign(const Bean& other) = 0;
	};

	class EmptyBean : public Bean {
	public:
		static const int64_t TYPEID = 0LL;

		virtual int64_t TypeId() const override {
			return TYPEID;
		}

		virtual void Encode(ByteBuffer& bb) const override {
			bb.WriteByte(0);
		}

		virtual void Decode(ByteBuffer& bb) override {
			bb.SkipUnknownField(ByteBuffer::BEAN);
		}

		virtual void Assign(const Bean& other) override {

		}
	};

	class DynamicBean : public Bean {
		Bean* bean; // unique_ptr??????????????????????????????????
		int64_t typeId;

		std::function<int64_t(Bean*)> getBean;
		std::function<Bean*(int64_t)> createBean;

	public:
		DynamicBean(int variableId, std::function<int64_t(Bean*)> get, std::function<Bean*(int64_t)> create) {
			bean = new EmptyBean();
			typeId = EmptyBean::TYPEID;
			getBean = get;
			createBean = create;
		}

		virtual int64_t TypeId() const {
			return typeId;
		}

		Bean* GetBean() {
			return bean;
		}

		void SetBean(Bean* value) {
			if (NULL == value)
				throw new std::invalid_argument("value is null");
			typeId = getBean(value);
			bean = value;
		}

        Bean* NewBean(int64_t typeId)
        {
            if (bean)
                delete bean;
            bean = createBean(typeId);
            if (!bean)
                bean = new EmptyBean();
            this->typeId = typeId != 0 ? typeId : getBean(bean);
            return bean;
        }

		virtual void Encode(ByteBuffer& bb) const override {
		}

		virtual void Decode(ByteBuffer& bb) override {
		}

		virtual void Assign(const Bean& other) {

		}

		bool Empty() const {
			return true;
		}
	};

	class Vector2 : public Serializable {
	public:
		float x;
		float y;

		Vector2() {
			x = 0;
			y = 0;
		}

		virtual bool isZero() const {
			return x == 0 && y == 0;
		}

		virtual void Encode(ByteBuffer& bb) const {
			bb.WriteFloat(x);
			bb.WriteFloat(y);
		}

		virtual void Decode(ByteBuffer& bb) {
			x = bb.ReadFloat();
			y = bb.ReadFloat();
		}
	};

	class Vector3 : public Vector2 {
	public:
		float z;

		Vector3() {
			z = 0;
		}

		virtual bool isZero() const override {
			return x == 0 && y == 0 && z == 0;
		}

		virtual void Encode(ByteBuffer& bb) const {
			bb.WriteFloat(x);
			bb.WriteFloat(y);
			bb.WriteFloat(z);
		}

		virtual void Decode(ByteBuffer& bb) {
			x = bb.ReadFloat();
			y = bb.ReadFloat();
			z = bb.ReadFloat();
		}
	};

	class Vector4 : public Vector3 {
	public:
		float w;

		Vector4() {
			w = 0;
		}

		virtual bool isZero() const override {
			return x == 0 && y == 0 && z == 0 && w == 0;
		}

		virtual void Encode(ByteBuffer& bb) const {
			bb.WriteFloat(x);
			bb.WriteFloat(y);
			bb.WriteFloat(z);
			bb.WriteFloat(w);
		}

		virtual void Decode(ByteBuffer& bb) {
			x = bb.ReadFloat();
			y = bb.ReadFloat();
			z = bb.ReadFloat();
			w = bb.ReadFloat();
		}
	};

	class Quaternion : public Vector4 {

	};

	class Vector2Int : public Serializable {
	public:
		int x;
		int y;

		Vector2Int() {
			x = 0;
			y = 0;
		}

		virtual int CompateTo(const Vector2Int& other) const {
			if (&other == this)
				return 0;
			int c;
			c = Integer::Compare(x, other.x);
			if (c != 0)
				return c;
			c = Integer::Compare(y, other.y);
			return c;
		}

		virtual bool isZero() const {
			return x == 0 && y == 0;
		}

		virtual void Encode(ByteBuffer& bb) const {
			bb.WriteInt(x);
			bb.WriteInt(y);
		}

		virtual void Decode(ByteBuffer& bb) {
			x = bb.ReadInt();
			y = bb.ReadInt();
		}
	};

	class Vector3Int : public Vector2Int {
	public:
		int z;

		Vector3Int() {
			z = 0;
		}

		virtual bool isZero() const {
			return x == 0 && y == 0 && z == 0;
		}

		virtual int CompateTo(const Vector3Int& other) const {
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

		virtual void Encode(ByteBuffer& bb) const {
			bb.WriteInt(x);
			bb.WriteInt(y);
			bb.WriteInt(z);
		}

		virtual void Decode(ByteBuffer& bb) {
			x = bb.ReadInt();
			y = bb.ReadInt();
			z = bb.ReadInt();
		}
	};
}