
#pragma once

#include <mutex>
#include <string>
#include <functional>
#include <unordered_map>

#include "ByteBuffer.h"
#include "dh.h"
#include "codec.h"

namespace Zeze
{
namespace Net
{
	bool Startup();
	void Cleanup();
	void SetTimeout(const std::function<void()> &func, int timeout);

	class Protocol;
	class BufferedCodec;
	class Service;

	class Socket
	{
		std::recursive_mutex mutex;
		int socket = 0;
		int selectorFlags = 0; // used in Selector
		std::shared_ptr<limax::DHContext> dhContext;

		void SetOutputSecurity(bool c2sneedcompress, const int8_t* key, int keylen);
		void SetInputSecurity(bool s2cneedcompress, const int8_t* key, int keylen);

		friend class Service;
		friend class Selector;

		std::shared_ptr<BufferedCodec> OutputBuffer;
		std::shared_ptr<BufferedCodec> InputBuffer;

		std::shared_ptr<limax::Codec> OutputCodec;
		std::shared_ptr<limax::Codec> InputCodec;

		void OnSend();
		void OnRecv();

	public:
		bool IsHandshakeDone;
		long long SessionId;
		Service* service;
		std::shared_ptr<Socket> This;
		std::string LastAddress;
		std::string LastAddressBytes;

		static long long NextSessionId()
		{
			static long long seed = 0;
			static std::mutex mutex;

			std::lock_guard<std::mutex> g(mutex);
			return ++seed;
		}
		Socket(Service* svr);
		~Socket();
		void Close(std::exception* e);
		void Send(const char* data, int length) { Send(data, 0, length); }
		void Send(const char* data, int offset, int length);
		// 成功时，返回成功连接的地址。返回 empty string 表示失败。
		bool Connect(const std::string& host, int port, const std::string& lastSuccessAddress, int timeoutSecondsPerConnect);
	};

	class Service
	{
		std::string name;
		std::string lastSuccessAddress;
		int lastPort;
		bool autoReconnect;
		int autoReconnectDelay;

	protected:
		std::shared_ptr<Socket> socket;

	public:
		Service(const std::string& _name);
		virtual ~Service();
		const std::string & Name() { return name; }
		std::shared_ptr<Socket> GetSocket() { return socket; }
		std::shared_ptr<Socket> GetSocket(long long sessionId)
		{
			if (socket.get() != NULL && socket->SessionId == sessionId)
				return socket;
			return std::shared_ptr<Socket>(NULL);
		}
		void Connect(const std::string& host, int port, int timeoutSecondsPerConnect = 5);

		///////////////////////////////////
		// for ToLua interface
		virtual void Update()
		{
			// ToLuaService 实现
		}
		virtual void SendProtocol(Socket * so)
		{
			// ToLuaService 实现
		}

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
			ProtocolFactoryHandle()
			{
			}
		};
		void AddProtocolFactory(int typeId, const ProtocolFactoryHandle& func)
		{
			std::pair<ProtocolFactoryMap::iterator, bool> r = ProtocolFactory.insert(std::pair<int, ProtocolFactoryHandle>(typeId, func));
			if (false == r.second)
				throw std::exception("duplicate protocol TypeId");
		}
		bool FindProtocolFactoryHandle(int typeId, ProtocolFactoryHandle& outFactoryHandle)
		{
			ProtocolFactoryMap::iterator it = ProtocolFactory.find(typeId);
			if (it != ProtocolFactory.end())
			{
				outFactoryHandle = it->second;
				return true;
			}
			return false;
		}

		void SetDhGroup(char dhGroup)
		{
			this->dhGroup = dhGroup;
		}

		void SetAutoConnect(bool bAuto)
		{
			this->autoReconnect = bAuto;
		}

		virtual void OnSocketClose(const std::shared_ptr<Socket> & sender, const std::exception* e);
		virtual void OnHandshakeDone(const std::shared_ptr<Socket>& sender);
		virtual void OnSocketConnectError(const std::shared_ptr<Socket>& sender, const std::exception* e);
		virtual void OnSocketConnected(const std::shared_ptr<Socket>& sender);
		virtual void DispatchUnknownProtocol(const std::shared_ptr<Socket>& sender, int typeId, Zeze::Serialize::ByteBuffer& data);
		virtual void DispatchProtocol(Protocol* p, Service::ProtocolFactoryHandle& factoryHandle);
		virtual void OnSocketProcessInputBuffer(const std::shared_ptr<Socket>& sender, Zeze::Serialize::ByteBuffer& input);

		friend class Protocol;

	private:
		typedef std::unordered_map<int, ProtocolFactoryHandle> ProtocolFactoryMap;
		ProtocolFactoryMap ProtocolFactory;
		void StartConnect(const std::string& host, int port, int delay, int timeoutSecondsPerConnect);

		char dhGroup = 1;
		int ProcessSHandshake(Protocol* p);
	};

} // namespace Net
} // namespace Zeze
