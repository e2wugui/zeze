
#pragma once

#include <string>
#include "ByteBuffer.h"
#include "ToLua.h"
#include <functional>
#include <unordered_map>

namespace Zeze
{
namespace Net
{
	class Protocol;

	class Socket
	{
	public:
		bool IsHandshakeDone;
		long long SessionId;
		Service* service;

		static long long NextSessionId()
		{
			static long long seed = 0;
			static std::mutex mutex;

			std::lock_guard<std::mutex> g(mutex);
			return ++seed;
		}

		Socket(Service* svr) : service(svr)
		{
			IsHandshakeDone = false;
			SessionId = NextSessionId();
		}

		~Socket();
		void Close(std::exception* e);
		void Send(char* data, int length) { Send(data, 0, length); }
		void Send(char* data, int offset, int length);
	};

	class Service
	{
		std::string name;
		std::shared_ptr<Socket> socket;
		ToLua ToLua;
		Helper Helper;
	public:
		Service(const std::string & _name) : name(_name), socket(NULL) { }
		virtual ~Service();
		const std::string & Name() { return name; }
		std::shared_ptr<Socket> GetSocket() { return socket; }
		std::shared_ptr<Socket> GetSocket(long long sessionId)
		{
			if (socket.get() != NULL && socket->SessionId == sessionId)
				return socket;
			return std::shared_ptr<Socket>(NULL);
		}
		void InitializeLua(lua_State* L);
		Socket* Connect(const std::string& host, int port);
		virtual void OnSocketClose(const std::shared_ptr<Socket> & sender, const std::exception* e);
		virtual void OnHandshakeDone(const std::shared_ptr<Socket>& sender);
		virtual void OnSocketConnectError(const std::shared_ptr<Socket>& sender, const std::exception* e);
		virtual void OnSocketConnected(const std::shared_ptr<Socket>& sender);
		virtual void DispatchUnknownProtocol(const std::shared_ptr<Socket>& sender, int typeId, Zeze::Serialize::ByteBuffer& data);
		virtual void DispatchProtocol(Protocol* p);
		virtual void OnSocketProcessInputBuffer(const std::shared_ptr<Socket>& sender, Zeze::Serialize::ByteBuffer& input);

		friend class ToLua;
		friend class Helper;
		friend class Protocol;

		class ProtocolFactoryHandle
		{
		public:
			typedef std::function<Protocol* ()> FuncFactory;
			typedef std::function<int(Protocol*)> FuncHandle;
			FuncFactory Factory;
			FuncHandle Handle;

			ProtocolFactoryHandle(const FuncFactory& factory, const FuncHandle& handle)
				: Factory(factory), Handle(handle)
			{
			}
		};
		void AddProtocolFactory(int typeId, const ProtocolFactoryHandle & func)
		{
			std::pair<ProtocolFactoryMap::iterator, bool> r = ProtocolFactory.insert(std::pair<int, ProtocolFactoryHandle>(typeId, func));
			if (false == r.second)
				throw std::exception("duplicate protocol TypeId");
		}
	private:
		typedef std::unordered_map<int, ProtocolFactoryHandle> ProtocolFactoryMap;
		ProtocolFactoryMap ProtocolFactory;
		Protocol* CreateProtocol(int typeId, Zeze::Serialize::ByteBuffer& os);
	};

} // namespace Net
} // namespace Zeze
