#include "octets.h"
#include "utils.h"

namespace limax {

	static inline void* fast_memmove(void* dest, const void* src, size_t n)
	{
		unsigned char* d = (unsigned char*)dest;
		unsigned char* s = (unsigned char*)src;
		if (s < d && s + n >= d)
			return memmove(dest, src, n);
		else
			return memcpy(dest, src, n);
	}

	struct Octets::Rep
	{
		size_t cap;
		size_t len;
		char* data;

		Rep() : cap(0), len(0), data(nullptr)
		{}

		Rep(size_t cap)
		{
			this->cap = cap;
			this->len = 0;
			if (cap)
				this->data = new char[cap];
			else
				this->data = nullptr;
		}

		Rep(const Rep& src) : cap(src.cap), len(src.len), data(nullptr)
		{
			if (cap)
				data = new char[cap];
			if (len)
				memcpy(data, src.data, len);
		}

		Rep(size_t cap, const Rep& src) : cap(cap), len(src.len), data(nullptr)
		{
			if (cap < len)
				cap = len;
			if (cap)
				data = new char[cap];
			if (len)
				memcpy(data, src.data, len);
		}

		~Rep()
		{
			delete[] data;
		}

		inline void* GetData()
		{
			return data;
		}

		inline void* reserve(size_t size)
		{
			size = frob_size(size);
			if (size > cap)
			{
				char* temp = data;
				cap = size;
				data = new char[cap];
				memcpy(data, temp, len);
				delete[] temp;
			}
			return data;
		}

		inline static size_t frob_size(size_t size)
		{
			size_t tmp = 16;
			while (size > tmp)
				tmp <<= 1;
			return tmp;
		}

		inline size_t hash_code() const
		{
			size_t result = 1;
			for (size_t i = 0; i < len; i++)
				result = 31 * result + (size_t)data[i];
			return result;
		}

		static std::shared_ptr<Rep> null;
	};

	std::shared_ptr<Octets::Rep> Octets::Rep::null = std::shared_ptr<Octets::Rep>(new Octets::Rep());

	void Octets::unique()
	{
		if (!base.unique())
			base = std::shared_ptr<Rep>(new Rep(*base.get()));
	}

	Octets::Octets() : base(Rep::null)
	{}

	Octets::Octets(size_t size) : base(new Rep(size))
	{}

	Octets::Octets(const void *x, size_t size) : base(new Rep(size))
	{
		memcpy(base->data, x, size);
		base->len = size;
	}

	Octets::Octets(const void *x, const void *y) : base(new Rep((char*)y - (char*)x))
	{
		size_t size = (char*)y - (char*)x;
		memcpy(base->data, x, size);
		base->len = size;
	}

	Octets::Octets(const Octets &x)
		: base(x.base)
	{}

	Octets::Octets(const Octets &&x)
		: base(std::move(x.base))
	{}

	Octets::~Octets()
	{}

	Octets& Octets::operator = (const Octets& x)
	{
		if (&x != this)
			base = x.base;
		return *this;
	}

	Octets& Octets::operator = (const Octets&& x)
	{
		Octets(std::move(x)).swap(*this);
		return *this;
	}

	bool Octets::equals(const Octets& x) const
	{
		return 0 == compare(x);
	}

	int Octets::compare(const Octets& x) const
	{
		int c = limax::compareTo((uint64_t)size(), (uint64_t)x.size());
		return c ? c : memcmp(base->data, x.base->data, size());
	}

	bool Octets::operator == (const Octets &x) const
	{
		return 0 == compare(x);
	}

	bool Octets::operator != (const Octets &x) const
	{
		return 0 != compare(x);
	}

	void* Octets::begin()
	{
		unique();
		return base->data;
	}

	void* Octets::end()
	{
		unique();
		return base->data + base->len;
	}

	const void* Octets::begin() const
	{
		return base->data;
	}

	const void* Octets::end() const
	{
		return base->data + base->len;
	}

	size_t Octets::size() const
	{
		return base->len;
	}
	size_t Octets::capacity() const
	{
		return base->cap;
	}

	Octets& Octets::swap(Octets &x)
	{
		base.swap(x.base);
		return *this;
	}

	Octets& Octets::reserve(size_t size)
	{
		if (size <= base->cap)
			return *this;
		if (base.unique())
			base->reserve(size);
		else
			base = std::shared_ptr<Rep>(new Rep(size, *base.get()));
		return *this;
	}

	Octets& Octets::replace(const void *data, size_t size)
	{
		reserve(size);
		memcpy(base->data, data, size);
		base->len = size;
		return *this;
	}

	Octets& Octets::clear()
	{
		unique();
		base->len = 0;
		return *this;
	}

	Octets& Octets::erase(size_t pos, size_t len)
	{
		char* x = (char*)begin();
		return erase(x + pos, x + pos + len);
	}

	Octets& Octets::erase(void* x, void* y)
	{
		if (x != y)
		{
			char* tmp = base->data;
			unique();
			ptrdiff_t o = base->data - tmp;
			if (o)
			{
				x = (char*)x + o;
				y = (char*)y + o;
			}
			fast_memmove((char*)x, (char*)y, (base->data + base->len) - (char*)y);
			base->len -= ((char*)y - (char*)x);
		}
		return *this;
	}

	Octets& Octets::insert(void* pos, const void* x, size_t len)
	{
		ptrdiff_t off = (char*)pos - base->data;
		reserve(size() + len);
		pos = base->data + off;
		size_t adjust = size() - off;
		if (adjust)
			fast_memmove((char*)pos + len, pos, adjust);
		fast_memmove(pos, x, len);
		base->len += len;
		return *this;
	}

	Octets& Octets::insert(void* pos, const void* x, const void* y)
	{
		insert(pos, x, (char*)y - (char*)x);
		return *this;
	}

	Octets& Octets::resize(size_t size)
	{
		reserve(size);
		base->len = size;
		return *this;
	}

	size_t Octets::hash_code() const 
	{
		return base->hash_code();
	}
} // namespace limax {
