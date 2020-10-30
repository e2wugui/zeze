#pragma once

#include "common.h"
#include <functional>
#include <vector>
#include <list>
#include <unordered_map>
#include <unordered_set>
#include <deque>
#include <mutex>
#include <thread>
#include <stdexcept>
#include "octets.h"

namespace limax {
#pragma region Utils
	// compareTo

	inline int signum(int64_t a) // see java.lang.Long.signum
	{
		return (int)(a >> 63 | ((uint64_t)-a) >> 63);
	}

	inline int compareTo(int64_t a, int64_t b)
	{
		return signum(a - b);
	}

	inline int compareTo(uint64_t a, uint64_t b)
	{
		return signum(a - b);
	}

	inline int compareTo(bool a, bool b)
	{
		return (a == b) ? 0 : (a ? 1 : -1);
	}

	// equals and hash_code
	template<class B> bool equals(const B&  a, const B& b)
	{
		return a.equals(b);
	}
	template<class H> inline size_t hash_code(const H& h)
	{
		return h.hash_code();
	}

	template<class H> struct HashMapHasher
	{
		inline size_t operator()(const H& h) const
		{
			return hash_code(h);
		}
	};
	template<class H> struct HashMapEqual
	{
		inline bool operator()(const H& a, const H& b) const
		{
			return equals(a, b);
		}
	};
	template<class K, class V> using hashmap = std::unordered_map < K, V, HashMapHasher<K>, HashMapEqual<K> > ;
	template<class V> using hashset = std::unordered_set < V, HashMapHasher<V>, HashMapEqual<V> > ;

	// equals
	template<> inline bool equals(const bool& a, const bool& b)
	{
		return a == b;
	}

	template<> inline bool equals(const int8_t& a, const int8_t& b)
	{
		return a == b;
	}

	template<> inline bool equals(const int16_t& a, const int16_t& b)
	{
		return a == b;
	}

	template<> inline bool equals(const int32_t& a, const int32_t& b)
	{
		return a == b;
	}

	template<> inline bool equals(const int64_t& a, const int64_t& b)
	{
		return a == b;
	}

	template<> inline bool equals(const float& a, const float& b)
	{
		return a == b;
	}

	template<> inline bool equals(const double& a, const double& b)
	{
		return a == b;
	}

	template<> inline bool equals(const std::string& a, const std::string& b)
	{
		if (a.length() != b.length())
			return false;
		else
			return 0 == memcmp(a.c_str(), b.c_str(), a.length());
	}

	template<class C> bool container_equals(const C& a, const C& b);

	template<class C, class E> inline bool container_contains(const C& c, const E& e)
	{
		for (auto it = c.begin(), ite = c.end(); it != ite; ++it)
		{
			if (equals(*it, e))
				return true;
		}
		return false;
	}

	template<class E> inline bool equals(const std::list<E>& a, const std::list<E>& b)
	{
		return container_equals(a, b);
	}

	template<class E> inline bool equals(const std::vector<E>& a, const std::vector<E>& b)
	{
		return container_equals(a, b);
	}

	template<class E> inline bool equals(const hashset<E>& a, const hashset<E>& b)
	{
		return container_equals(a, b);
	}

	template<class F, class S> inline bool equals(const std::pair<F, S>& a, const std::pair<F, S>& b)
	{
		return equals(a.first, b.first) && equals(a.second, b.second);
	}

	template<class F, class S> inline bool equals(const hashmap<F, S>& a, const hashmap<F, S>& b)
	{
		return container_equals(a, b);
	}

	template<class C> inline bool container_equals(const C& a, const C& b)
	{
		if (a.size() != b.size())
			return false;
		for (auto ait = a.begin(), aite = a.end(), bit = b.begin(); ait != aite; ++ait, ++bit)
		{
			if (!equals(*ait, *bit))
				return false;
		}
		return true;
	}

	// hash_code
	template<> inline size_t hash_code<bool>(const bool& h)
	{
		return std::hash<bool>()(h);
	}
	template<> inline size_t hash_code<int8_t>(const int8_t& h)
	{
		return std::hash<int8_t>()(h);
	}
	template<> inline size_t hash_code<int16_t>(const int16_t& h)
	{
		return std::hash<int16_t>()(h);
	}
	template<> inline size_t hash_code<int32_t>(const int32_t& h)
	{
		return std::hash<int32_t>()(h);
	}
	template<> inline size_t hash_code<int64_t>(const int64_t& h)
	{
		return std::hash<int64_t>()(h);
	}
	template<> inline size_t hash_code<float>(const float& h)
	{
		return std::hash<float>()(h);
	}
	template<> inline size_t hash_code<double>(const double& h)
	{
		return std::hash<double>()(h);
	}

	template<> inline size_t hash_code<std::string>(const std::string& h)
	{
		return std::hash<std::string>()(h);
	}

	template<class C> size_t container_hash_code(const C& c);
	template<class E> inline size_t hash_code(const std::list<E> &h)
	{
		return container_hash_code(h);
	}
	template<class E> inline size_t hash_code(const std::vector<E> &h)
	{
		return container_hash_code(h);
	}
	template<class E> inline size_t hash_code(const hashset<E> &h)
	{
		return container_hash_code(h);
	}
	template<class F, class S> inline size_t hash_code(const std::pair<F, S>& h)
	{
		return hash_code(h.first) * 31 + hash_code(h.second);
	}
	template<class F, class S> inline size_t hash_code(const hashmap<F, S>& h)
	{
		return container_hash_code(h);
	}

	template<class C> inline size_t container_hash_code(const C& c)
	{
		size_t hashCode = 1;
		for (const auto& e : c)
			hashCode = 31 * hashCode + hash_code(e);
		return hashCode;
	}

	LIMAX_DLL_EXPORT_API std::string encodeBase64ToString(Octets data);
	LIMAX_DLL_EXPORT_API std::string tostring36(int64_t n);
#pragma endregion
#pragma region class BitSet
	class BitSet
	{
		std::vector<bool> bits;
	private:
		BitSet& operator=(const BitSet&) { return *this; }
	public:
		inline BitSet() {}
		inline ~BitSet() {}
	public:
		inline bool get(size_t pos) const
		{
			if (pos >= bits.size())
				return false;
			else
				return bits.at(pos);
		}
		inline bool set(size_t pos)
		{
			if (pos >= bits.size())
				bits.resize(pos + 1, false);
			bool __old__ = bits[pos];
			bits[pos] = true;
			return __old__;
		}
		inline bool clear(size_t pos)
		{
			if (pos < bits.size())
			{
				bool __old__ = bits[pos];
				bits[pos] = false;
				return __old__;
			}
			return false;
		}
	};
#pragma endregion
#pragma region class MapBitSet
	template<class Key> class MapBitSet
	{
		typedef std::unordered_map<Key, BitSet> Map;
		Map map;
	private:
		MapBitSet(const MapBitSet&) {}
		MapBitSet& operator=(const MapBitSet&) { return *this; }
	public:
		inline MapBitSet() {}
		inline ~MapBitSet() {}
	public:
		inline bool get(Key k, size_t pos) const
		{
			auto it = map.find(k);
			if (it == map.end())
				return false;
			else
				return it->second.get(pos);
		}
		inline bool set(Key k, size_t pos)
		{
			return map[k].set(pos);
		}
		inline bool clear(Key k, size_t pos)
		{
			auto it = map.find(k);
			if (it == map.end())
				return false;
			return it->second.clear(pos);
		}
		inline void remove(Key k)
		{
			map.erase(k);
		}
	};
#pragma endregion
#pragma region class BlockingQueue
	template<typename T> class BlockingQueue
	{
		std::deque<T> q;
		std::mutex mutex;
		std::condition_variable_any cond;
	public:
		explicit BlockingQueue() {}
		BlockingQueue(const BlockingQueue&) = delete;
		BlockingQueue& operator =(const BlockingQueue&) = delete;
		void put(T element)
		{
			std::lock_guard<std::mutex> l(mutex);
			q.push_back(element);
			cond.notify_all();
		}
		T get()
		{
			std::lock_guard<std::mutex> l(mutex);
			while (q.size() == 0)
				cond.wait(mutex);
			T t = q.front();
			q.pop_front();
			return t;
		}
	};
#pragma endregion
	typedef std::function<void(void)> Runnable;
	typedef std::function<void(Runnable)> Executor;
	LIMAX_DLL_EXPORT_API void runOnUiThread(Runnable);
	LIMAX_DLL_EXPORT_API void uiThreadSchedule();
	LIMAX_DLL_EXPORT_API void uiThreadScheduleTime(int);
	LIMAX_DLL_EXPORT_API void uiThreadScheduleCount(size_t);
#pragma region class Resource
	class LIMAX_DLL_EXPORT_API Resource
	{
		struct Impl;
		std::shared_ptr<Impl> impl;
		Resource(Resource& parent, Runnable cleanup);
	public:
		void swap(Resource&);
		Resource();
		Resource(const Resource&);
		Resource(const Resource&&);
		Resource& operator=(const Resource&);
		Resource& operator=(const Resource&&);
		class IllegalStateException : public std::exception {};
		void close();
		static Resource createRoot();
		static Resource create(Resource& parent, Runnable cleanup);
	};
#pragma endregion
#pragma region class trace
	class LIMAX_DLL_EXPORT_API Trace
	{
	public:
		enum Level { Fatal, Error, Warn, Info, Debug };
		typedef std::function<void(const std::string&)> Destination;
	private:
		static Level level;
		static Destination printTo;
	public:
		static inline void open(Destination d, Level l)
		{
			printTo = d;
			level = l;
		}
		static inline bool isDebugEnabled() { return level >= Level::Debug; }
		static inline bool isInfoEnabled() { return level >= Level::Info; }
		static inline bool isWarnEnabled() { return level >= Level::Warn; }
		static inline bool isErrorEnabled() { return level >= Level::Error; }
		static inline bool isFatalEnabled() { return level >= Level::Fatal; }
		static inline void fatal(const std::string& msg)
		{
			if (isFatalEnabled())
				printTo(msg);
		}
		static inline void error(const std::string& msg)
		{
			if (isErrorEnabled())
				printTo(msg);
		}
		static inline void info(const std::string& msg)
		{
			if (isInfoEnabled())
				printTo(msg);
		}
		static inline void debug(const std::string& msg)
		{
			if (isDebugEnabled())
				printTo(msg);
		}
		static inline void warn(const std::string& msg)
		{
			if (isWarnEnabled())
				printTo(msg);
		}
	};
#pragma endregion
} // namespace limax {
