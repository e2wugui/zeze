#pragma once

#include <future>

namespace Zeze
{
template<class T>
class TaskCompletionSource
{
	std::promise<T> Promise;
	std::future<T> Future;

public:
	TaskCompletionSource()
	{
		Future = Promise.get_future();
	}

	void TrySetException(std::exception* ex)
	{
		Promise.set_exception(ex);
	}

	void SetResult(const T& value)
	{
		Promise.set_value(value);
	}

	void Wait()
	{
		Future.wait();
	}
	
	T Get()
	{
		return Future.get();
	}
};
}
