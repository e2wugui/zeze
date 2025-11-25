#pragma once

#include "common.h"

#include <mutex>
#include <string>
#include <functional>
#include <unordered_map>
#include <cstdint>
#include "ByteBuffer.h"
#include "dh.h"
#include "codec.h"
#include <unordered_set>
#include <memory>

#ifdef LIMAX_OS_WINDOWS

#include "wepoll.h"

#elif defined(LIMAX_OS_ANDROID) || defined(LIMAX_OS_LINUX)

#include <sys/epoll.h>
#define SOCKET int
#define INVALID_SOCKET (-1)
#define HANDLE int

#elif defined(LIMAX_OS_APPLE_FAMILY)

#include <sys/event.h>
#define SOCKET int
#define INVALID_SOCKET (-1)
#define HANDLE int


#endif


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
	class Selector;

	enum Constant
	{
		eEncryptTypeDisable = 0,
		eEncryptTypeAes = 1,
		eEncryptTypeAesNoSecureIp = 2,

		eCompressTypeDisable = 0, // no compress
		eCompressTypeMppc = 1, // mppc
		//eCompressTypeZstd = 2, // zstd

	};

	class Socket
	{
	protected:
		std::recursive_mutex mutex;
		SOCKET socket = 0;
		uint32_t selectorFlags = 0; // used in Selector
		std::shared_ptr<limax::DHContext> dhContext;
		Selector* selector = nullptr;
		bool client = false;
		bool connectPending = false;

		void SetOutputSecurity(int encryptType, const int8_t* key, int keylen, int compressC2s);
		void SetInputSecurity(int encryptType, const int8_t* key, int keylen, int compressS2c);

		friend class Service;
		friend class Selector;

		std::shared_ptr<BufferedCodec> OutputBuffer;
		std::shared_ptr<BufferedCodec> InputBuffer;

		std::shared_ptr<limax::Codec> OutputCodec;
		std::shared_ptr<limax::Codec> InputCodec;

		virtual void OnSend();
		virtual void OnRecv();

		int64_t activeSendTime;
		int64_t activeRecvTime;

		bool handshakeDone;
		int64_t sessionId;
		Service* service;
		std::shared_ptr<Socket> thisSharedPtr;
		std::string lastAddress;
		std::string lastAddressBytes;

		void finishConnect();
	public:
		std::string GetLocalAddress() const;
		bool IsHandshakeDone() const
		{
			return handshakeDone;
		}
		int64_t GetSessionId() const
		{
			return sessionId;
		}
		Service* GetService() const
		{
			return service;
		}
		const std::shared_ptr<Socket> & GetThisSharedPtr() const
		{
			return thisSharedPtr;
		}
		const std::string & GetLastAddress() const
		{
			return lastAddress;
		}
		const std::string & GetLastAddressBytes() const
		{
			return lastAddressBytes;
		}

		int64_t GetActiveSendTime() const
		{
			return activeSendTime;
		}

		int64_t GetActiveRecvTime() const
		{
			return activeRecvTime;
		}

		static int64_t NextSessionId()
		{
			static int64_t seed = 0;
			static std::mutex mutex;

			std::lock_guard<std::mutex> g(mutex);
			return ++seed;
		}
		Socket(Service* svr);
		Socket(Service* svr, SOCKET so);
		~Socket();
		void Close(const std::exception& e)
		{
			Close(&e);
		}
		void Close(const std::exception* e);
		void Send(const char* data, int length) { Send(data, 0, length); }
		void Send(const char* data, int offset, int length);
		// 成功时，返回成功连接的地址。返回 empty string 表示失败。
		bool Connect(const std::string& host, int port, int timeout);

	};

	class Service
	{
	protected:
		std::recursive_mutex mutex;
		std::unordered_set<int64_t> handshakeProtocols; // 构造的时候初始化，不需要线程保护。
		int keepCheckPeriod = 0;
		int keepSendTimeout = 25;
		int keepRecvTimeout = 60;
		std::unordered_map<int64_t, std::shared_ptr<Socket>> sockets;
		std::unordered_set<std::shared_ptr<Socket>> serverSockets;

	public:
		Service();
		virtual ~Service();
		std::shared_ptr<Socket> GetSocket()
		{
			std::lock_guard<std::recursive_mutex> g(mutex);
			auto it = sockets.begin();
			if (it != sockets.end())
				return it->second;
			return std::shared_ptr<Socket>(nullptr);
		}
		std::shared_ptr<Socket> GetSocket(int64_t sessionId)
		{
			std::lock_guard<std::recursive_mutex> g(mutex);
			auto it = sockets.find(sessionId);
			if (it != sockets.end())
				return it->second;
			return std::shared_ptr<Socket>(nullptr);
		}
		void AddSocket(const std::shared_ptr<Socket>& socket)
		{
			std::lock_guard<std::recursive_mutex> g(mutex);
			sockets[socket->GetSessionId()] = socket;
		}
		void Connect(const std::string& host, int port, int timeout = 5);
		std::string Listen(const std::string& host, int port);

		bool IsHandshakeProtocol(int64_t typeId) const
		{
			return handshakeProtocols.find(typeId) != handshakeProtocols.end();
		}

		int GetKeepCheckPeriod() const
		{
			return keepCheckPeriod;
		}

		int GetKeepSendTimeout() const
		{
			return keepSendTimeout;
		}

		int GetKeepRecvTimeout() const
		{
			return keepRecvTimeout;
		}

		// seconds
		void SetKeepConfig(int period, int sendTimeout = 25, int recvTimeout = 60);

		///////////////////////////////////
		// for ToLua interface
		virtual void Update()
		{
			// ToLuaService 实现
		}
		virtual void SendProtocol(Socket* so)
		{
			// ToLuaService 实现
		}

		class ProtocolFactoryHandle
		{
		public:
			typedef std::function<Protocol* ()> FuncFactory;
			typedef std::function<int64_t(Protocol*)> FuncHandle;
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
		void AddProtocolFactory(int64_t typeId, const ProtocolFactoryHandle& func)
		{
			std::pair<ProtocolFactoryMap::iterator, bool> r = ProtocolFactory.insert(std::pair<int64_t, ProtocolFactoryHandle>(typeId, func));
			if (false == r.second)
				throw std::runtime_error("duplicate protocol TypeId");
		}
		bool FindProtocolFactoryHandle(int64_t typeId, ProtocolFactoryHandle& outFactoryHandle)
		{
			ProtocolFactoryMap::iterator it = ProtocolFactory.find(typeId);
			if (it != ProtocolFactory.end())
			{
				outFactoryHandle = it->second;
				return true;
			}
			return false;
		}

		void SetDhGroup(char _dhGroup)
		{
			this->dhGroup = _dhGroup;
		}

		virtual void OnSocketClose(const std::shared_ptr<Socket>& sender, const std::exception* e);
		virtual void OnHandshakeDone(const std::shared_ptr<Socket>& sender);
		virtual void OnSocketConnectError(const std::shared_ptr<Socket>& sender, const std::exception* e);
		virtual void OnSocketConnected(const std::shared_ptr<Socket>& sender);
		virtual void OnSocketAccept(const std::shared_ptr<Socket>& sender);
		virtual void DispatchUnknownProtocol(const std::shared_ptr<Socket>& sender, int moduleId, int protocolId, ByteBuffer& data);
		virtual void DispatchProtocol(Protocol* p, Service::ProtocolFactoryHandle& factoryHandle);
		virtual void DispatchRpcResponse(Protocol* r, std::function<int(Protocol*)>& responseHandle, Service::ProtocolFactoryHandle& factoryHandle);
		virtual void OnSocketProcessInputBuffer(const std::shared_ptr<Socket>& sender, ByteBuffer& input);

		friend class Protocol;

		int64_t AddRpcContext(Protocol* p)
		{
			std::lock_guard<std::mutex> guard(MutexRpcContexts);
			while (true) {
				++SeedRpcContexts;
				auto pair = RpcContexts.insert(std::unordered_map<int64_t, Protocol*>::value_type(SeedRpcContexts, p));
				if (pair.second)
					return SeedRpcContexts;
			}
		}

		Protocol* RemoveRpcContext(int64_t sid)
		{
			std::lock_guard<std::mutex> guard(MutexRpcContexts);
			auto it = RpcContexts.find(sid);
			if (it == RpcContexts.end())
				return nullptr;
			Protocol* found = it->second;
			RpcContexts.erase(it);
			return found;
		}

		bool RemoveRpcContext(int64_t sid, Protocol* ctx)
		{
			std::lock_guard<std::mutex> guard(MutexRpcContexts);
			auto it = RpcContexts.find(sid);
			if (it == RpcContexts.end())
				return false;
			if (it->second == ctx)
			{
				RpcContexts.erase(it);
				return true;
			}
			return false;
		}

	private:
		typedef std::unordered_map<int64_t, ProtocolFactoryHandle> ProtocolFactoryMap;
		ProtocolFactoryMap ProtocolFactory;
		std::unordered_map<int64_t, Protocol*> RpcContexts;
		int64_t SeedRpcContexts;
		std::mutex MutexRpcContexts;
		void StartConnect(const std::string& host, int port, int timeout);

		// client handshake
		char dhGroup = 1;
		int ProcessSHandshake(Protocol* p);
		int ProcessSHandshake0(Protocol* p);
		void StartHandshake(int encryptType, int compressS2c, int compressC2s, const std::shared_ptr<Socket>& sender);

		// server handshake
		int encryptType = eEncryptTypeDisable;
		int compressS2C = eCompressTypeDisable;
		int compressC2S = eCompressTypeDisable;
		std::string secureIp;

		// OnSocketAccept 发送SHandshake0开始协商
		int ProcessCHandshake(Protocol* _p);
		int ProcessCHandshakeDone(Protocol* p);
		int ProcessKeepAliveRequest(Protocol* _p);
	public:
		// server handshake options
		void SetHandshakeOptions(int encryptType, int compressS2C, int compressC2S);
		void SetSecureIp(const std::string& ipAddress) { secureIp = ipAddress; }
		const std::string& GetSecureIp() const { return secureIp; }
		// handshake keepalive
		void TryStartKeepAliveCheckTimer();
		void CheckKeepAlive();

	protected:
		virtual void OnKeepAliveTimeout(const std::shared_ptr<Socket> & socket);

		/**
		 * 1. 如果你是handshake的service，重载这个方法，按注释发送CKeepAlive即可；
		 * 2. 如果你是其他service子类，重载这个方法，按注释发送CKeepAlive，并且服务器端需要注册这条协议并写一个不需要处理代码的handler。
		 * 3. 如果不发送, 会导致KeepTimerClient时间后再次触发, 也可以调用socket.setActiveSendTime()避免频繁触发
		 *
		 * 4. 实现：CKeepAlive().Send(socket.get());
		 */
		virtual void OnSendKeepAlive(const std::shared_ptr<Socket> & socket);
	};

} // namespace Net
} // namespace Zeze
