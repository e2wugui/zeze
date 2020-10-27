
#pragma once

#include <string>
#include "ByteBuffer.h"
#include "ToLua.h"

namespace Zeze
{
namespace Net
{
	class Socket;

	class Service
	{
		std::string name;
		Socket* socket;

	public:
		ToLua ToLua;
		Helper Helper;

		Service(const std::string & _name)
			: name(_name)
		{
		}

		const std::string & Name()
		{
			return name;
		}

		virtual ~Service();

		virtual Socket* GetSocket()
		{
			return NULL;
		}


		virtual Socket* GetSocket(long long sessionId)
		{
			return NULL;
		}

		Socket* Connect(const std::string& host, int port);

		virtual void OnSocketClose(Socket * so, const std::exception * e)
		{
		}

		virtual void OnHandshakeDone(Socket * sender)
		{
			sender->IsHandshakeDone = true;
		}

		virtual void OnSocketConnectError(Socket * so, const std::exception * e)
		{
		}

		virtual void OnSocketConnected(Socket * so)
		{
			OnHandshakeDone(so);
		}

		virtual void OnSocketProcessInputBuffer(Socket * so, Zeze::Serialize::ByteBuffer & input)
		{
			try
			{
				Protocol.Decode(this, so, input);
			}
			catch (std::exception & ex)
			{
				so->Close(&ex);
			}
		}

	};

	class Socket
	{
	public:
		bool IsHandshakeDone;

		Socket()
		{
			IsHandshakeDone = false;
		}

		void Send(char* data, int length)
		{
			Send(data, 0, length);
		}

		void Send(char * data, int offset, int length);
	};

} // namespace Net
} // namespace Zeze
