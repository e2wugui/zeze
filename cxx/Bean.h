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
}
