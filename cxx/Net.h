
#pragma once

#include <string>

namespace Zeze
{
namespace Net
{
	class Socket;

	class Service
	{
	public:
		Service();
		virtual ~Service();

		Socket* Connect(const std::string& host, int port);
	};

	class Socket
	{
	public:
		void Send(char* data, int length)
		{
			Send(data, 0, length);
		}

		void Send(char * data, int offset, int length);
	};
} // namespace Net
} // namespace Zeze
