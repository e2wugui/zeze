#pragma once

#include <string>

namespace Zeze
{
	class IModule
	{
	public:
		virtual int GetId() const = 0;
		virtual const char* GetName() const = 0;
		virtual const char* GetFullName() const = 0;
		virtual void UnRegister() = 0;
	};
}