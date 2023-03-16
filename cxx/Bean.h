#pragma once

#include <cstdint>
#include <vector>
#include <set>
#include <map>
#include <functional>
#include "ByteBuffer.h"
#include <memory>

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
		std::shared_ptr<Bean> bean; // unique_ptr??????????????????????????????????
		int64_t typeId;

		std::function<int64_t(Bean*)> getBean;
		std::function<Bean*(int64_t)> createBean;

	public:
		DynamicBean()
			: bean(new EmptyBean())
		{
			typeId = EmptyBean::TYPEID;
		}

		DynamicBean(std::function<int64_t(Bean*)> get, std::function<Bean*(int64_t)> create)
			: bean(new EmptyBean())
		{
			typeId = EmptyBean::TYPEID;
			getBean = get;
			createBean = create;
		}

		virtual int64_t TypeId() const {
			return typeId;
		}

		Bean* GetBean() const {
			return bean.get();
		}

		void SetBean(Bean* value) {
			if (nullptr == value)
				throw new std::invalid_argument("value is null");
			typeId = getBean(value);
			bean.reset(value);
		}

		Bean* NewBean(int64_t typeId)
		{
			auto bean = createBean(typeId);
			if (!bean)
				bean = new EmptyBean();
			this->typeId = typeId != 0 ? typeId : getBean(bean);
			this->bean.reset(bean);
			return bean;
		}

		virtual void Encode(ByteBuffer& bb) const override {
			bb.WriteLong(typeId);
			bean.get()->Encode(bb);
		}

		virtual void Decode(ByteBuffer& bb) override {
			int64_t typeId = bb.ReadLong();
			std::shared_ptr<Bean> real(createBean(typeId));
			if (real.get() != nullptr) {
				real->Decode(bb);
				this->typeId = typeId;
				this->bean = real;
			}
			else {
				bb.SkipUnknownField(ByteBuffer::BEAN);
				this->typeId = EmptyBean::TYPEID;
				this->bean.reset(new EmptyBean());
			}
		}

		// 深度拷贝
		virtual void Assign(const Bean& other) {
			Assign((const DynamicBean&)other);
		}

		// 深度拷贝
		void Assign(const DynamicBean& other) {
			this->getBean = other.getBean;
			this->createBean = other.createBean;
			auto copy = NewBean(other.TypeId()); // 已经设置到shared_ptr中了。
			copy->Assign(*other.GetBean());
		}

		// 浅拷贝，为了用于容器内，共享了(shared_ptr)一个Bean的引用。
		DynamicBean& operator=(const DynamicBean& other) {
			Assign(other);
			return *this;
		}

		bool Empty() const {
			return typeId == EmptyBean::TYPEID;
		}
	};
}
