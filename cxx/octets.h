#pragma once

#include "common.h"
#include <memory>
#include <condition_variable>

namespace limax {

	class LIMAX_DLL_EXPORT_API Octets
	{
		struct Rep;
		std::shared_ptr<Rep> base;
		void unique();
	public:
		Octets();
		Octets(size_t size);
		Octets(const void *x, size_t size);
		Octets(const void *x, const void *y);
		Octets(const Octets &x);
		Octets(const Octets &&x);
		virtual ~Octets();
	public:
		Octets& operator = (const Octets&x);
		Octets& operator = (const Octets&&x);
		bool operator == (const Octets &x) const;
		bool operator != (const Octets &x) const;

		bool equals(const Octets&) const;
		int compare(const Octets&) const;

		void* begin();
		void* end();
		const void* begin() const;
		const void* end() const;
		size_t size() const;
		size_t capacity() const;

		Octets& swap(Octets &x);
		Octets& reserve(size_t size);
		Octets& replace(const void *data, size_t size);
		Octets& clear();
		Octets& erase(size_t pos, size_t len);
		Octets& erase(void* x, void* y);

		Octets& insert(void* pos, const void* x, size_t len);
		Octets& insert(void* pos, const void* x, const void* y);
		Octets& resize(size_t size);

		size_t hash_code() const;
	};

} // namespace limax {
