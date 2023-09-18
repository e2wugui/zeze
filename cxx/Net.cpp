#include <unordered_map>
#include <iostream>
#include "Net.h"
#include "Rpc.h"
#include "ByteBuffer.h"
#include "security.h"
#include "rfc2118.h"
#include "Bean.h"

namespace Zeze
{
namespace Net
{
	class Constant
	{
	public:
		static const int eEncryptTypeDisable = 0;
		static const int eEncryptTypeAes = 1;

		static const int eCompressTypeDisable = 0; // no compress
		static const int eCompressTypeMppc = 1; // mppc
		static const int eCompressTypeZstd = 2; // zstd

	};

	class SHandshake0Argument : public Serializable
	{
	public:
		int encryptType = 0; // 推荐的加密算法。旧版是boolean
		std::vector<int> supportedEncryptList;
		int compressS2c = 0; // 推荐的压缩算法。
		int compressC2s = 0; // 推荐的压缩算法。
		std::vector<int> supportedCompressList;

		void Encode(ByteBuffer& bb) const override
		{
			bb.WriteInt(encryptType);
			bb.WriteInt((int)supportedEncryptList.size());
			for (auto it = supportedEncryptList.begin(); it != supportedEncryptList.end(); ++it)
				bb.WriteInt(*it);
			bb.WriteInt(compressS2c);
			bb.WriteInt(compressC2s);
			bb.WriteInt((int)supportedCompressList.size());
			for (auto it = supportedCompressList.begin(); it != supportedCompressList.end(); ++it)
				bb.WriteInt(*it);
		}

		void Decode(ByteBuffer& bb) override
		{
			encryptType = bb.ReadInt();
			supportedEncryptList.clear();
			supportedCompressList.clear();

			for (int count = bb.ReadInt(); count > 0; --count)
				supportedEncryptList.push_back(bb.ReadInt());
			compressS2c = bb.ReadInt();
			compressC2s = bb.ReadInt();
			for (int count = bb.ReadInt(); count > 0; --count)
				supportedCompressList.push_back(bb.ReadInt());
		}
	};

	class CHandshakeArgument : public Serializable
	{
	public:
		int encryptType = 0;
		std::string encryptParam;
		int compressS2c = 0;
		int compressC2s = 0;

		void Decode(ByteBuffer& bb) override
		{
			encryptType = bb.ReadInt();
			encryptParam = bb.ReadBytes();
			compressS2c = bb.ReadInt();
			compressC2s = bb.ReadInt();
		}

		void Encode(ByteBuffer& bb) const override
		{
			bb.WriteInt(encryptType);
			bb.WriteBytes(encryptParam);
			bb.WriteInt(compressS2c);
			bb.WriteInt(compressC2s);
		}
	};

	class SHandshakeArgument : public Serializable
	{
	public:
		std::string encryptParam;
		int compressS2c = 0;
		int compressC2s = 0;
		int encryptType = 0;

		void Decode(ByteBuffer& bb) override
		{
			encryptParam = bb.ReadBytes();
			compressS2c = bb.ReadInt();
			compressC2s = bb.ReadInt();
			encryptType = bb.ReadInt();
		}

		void Encode(ByteBuffer& bb) const override
		{
			bb.WriteBytes(encryptParam);
			bb.WriteInt(compressS2c);
			bb.WriteInt(compressC2s);
			bb.WriteInt(encryptType);
		}
	};

	class CHandshake : public ProtocolWithArgument<CHandshakeArgument>
	{
	public:
		virtual int ModuleId() const override { return 0; }
		virtual int ProtocolId() const override { return -554021601; }
	};

	class CHandshakeDone : public ProtocolWithArgument<EmptyBean>
	{
	public:
		virtual int ModuleId() const override { return 0; }
		virtual int ProtocolId() const override { return 1896283174; }
	};

	class KeepAlive : public Rpc<EmptyBean, EmptyBean>
	{
	public:
		virtual int ModuleId() const override { return 0; }
		virtual int ProtocolId() const override { return -183352608; }
	};

	class SHandshake : public ProtocolWithArgument<SHandshakeArgument>
	{
	public:
		virtual int ModuleId() const override { return 0; }
		virtual int ProtocolId() const override { return -723986006; }
	};

	class SHandshake0 : public ProtocolWithArgument<SHandshake0Argument>
	{
	public:
		virtual int ModuleId() const override { return 0; }
		virtual int ProtocolId() const override { return -2018202792; }
	};

	Service::Service()
	{
		SeedRpcContexts = 0;
		autoReconnect = false;
		autoReconnectDelay = 0;

		SHandshake sHandshake;
		AddProtocolFactory(sHandshake.TypeId(), Zeze::Net::Service::ProtocolFactoryHandle(
			[]() { return new SHandshake(); }, std::bind(&Service::ProcessSHandshake, this, std::placeholders::_1)));
		SHandshake0 sHandshake0;
		AddProtocolFactory(sHandshake0.TypeId(), Zeze::Net::Service::ProtocolFactoryHandle(
			[]() { return new SHandshake0(); }, std::bind(&Service::ProcessSHandshake0, this, std::placeholders::_1)));
		KeepAlive keepAlive;
		if (ProtocolFactory.find(keepAlive.TypeId()) == ProtocolFactory.end())
		{
			AddProtocolFactory(keepAlive.TypeId(), Zeze::Net::Service::ProtocolFactoryHandle(
				[]() { return new KeepAlive(); }, std::bind(&Service::ProcessKeepAliveRequest, this, std::placeholders::_1)));
		}

		handshakeProtocols.insert(sHandshake.TypeId());
		handshakeProtocols.insert(sHandshake0.TypeId());
		handshakeProtocols.insert(keepAlive.TypeId());
	}

	/*
	void print(const char* name, const void* data, int size)
	{
		const unsigned char* uc = (const unsigned char*)data;
		std::cout << name << " ";
		for (int i = 0; i < size; ++i)
			std::cout << std::hex << (unsigned int)uc[i] << " ";
		std::cout << std::endl;
	}
	*/

	int ClientCompress(int c) {
		// 客户端检查一下当前版本是否支持推荐的压缩算法。
		// 如果不支持则统一使用最老的。
		// 这样当服务器新增了压缩算法，并且推荐了新的，客户端可以兼容它。
		if (c == Constant::eCompressTypeDisable)
			return c; // 推荐关闭压缩就关闭
		return Constant::eCompressTypeMppc; // 使用最老的压缩。
	}

	void Service::StartHandshake(int encryptType, int compressS2c, int compressC2s, const std::shared_ptr<Socket>& sender)
	{
		sender->dhContext = limax::createDHContext(1);
		const std::vector<unsigned char>& dhResponse = sender->dhContext->generateDHResponse();
		CHandshake hand;
		hand.Argument->encryptType = encryptType;
		if (encryptType == Constant::eEncryptTypeAes)
			hand.Argument->encryptParam = std::string((const char*)&dhResponse[0], dhResponse.size());
		hand.Argument->compressS2c = ClientCompress(compressS2c);
		hand.Argument->compressC2s = ClientCompress(compressC2s);
		hand.Send(sender.get());
	}

	int Service::ProcessKeepAliveRequest(Protocol* _p)
	{
		auto r = (KeepAlive*)_p;
		r->SendResult();
		// 不需要实现代码，OnRecv 已经处理了读取事件，更新了活跃时间。
		return 0;
	}

	int Service::ProcessSHandshake0(Protocol* _p)
	{
		SHandshake0* p = (SHandshake0*)_p;
		if (p->Argument->encryptType != Constant::eEncryptTypeDisable
			|| p->Argument->compressS2c != Constant::eCompressTypeDisable
			|| p->Argument->compressC2s != Constant::eCompressTypeDisable)
		{
			StartHandshake(p->Argument->encryptType, p->Argument->compressS2c, p->Argument->compressC2s, p->Sender);
			//std::cout << "StartHandshake " << p->Argument->encryptType << std::endl;
		}
		else
		{
			CHandshakeDone done;
			done.Send(p->Sender.get());
			OnHandshakeDone(p->Sender);
			//std::cout << "HandshakeDone " << p->Argument->encryptType << std::endl;
		}
		return 0;
	}

	int Service::ProcessSHandshake(Protocol* _p)
	{
		SHandshake* p = (SHandshake*)_p;

		const int8_t* inputKey = nullptr;
		const int8_t* outputKey = nullptr;
		if (p->Argument->encryptType == Constant::eEncryptTypeAes) {
			auto material = p->Sender->dhContext->computeDHKey(
				(unsigned char*)p->Argument->encryptParam.data(), (int)p->Argument->encryptParam.size());

			size_t key_len = p->Sender->LastAddressBytes.size();
			int8_t* key = (int8_t*)p->Sender->LastAddressBytes.data();

			int32_t half = (int32_t)material.size() / 2;
			{
				limax::HmacMD5 hmac(key, 0, (int)key_len);
				hmac.update((int8_t*)&material[0], 0, half);
				outputKey = hmac.digest();
			}
			{
				limax::HmacMD5 hmac(key, 0, (int)key_len);
				hmac.update((int8_t*)&material[0], half, (int32_t)material.size() - half);
				inputKey = hmac.digest();
			}
		}
		p->Sender->SetOutputSecurity(p->Argument->encryptType, outputKey, 16, p->Argument->compressC2s);
		p->Sender->SetInputSecurity(p->Argument->encryptType, inputKey, 16, p->Argument->compressS2c);
		CHandshakeDone done;
		done.Send(p->Sender.get());
		p->Sender->dhContext.reset();
		OnHandshakeDone(p->Sender);
		return 0;
	}

	class BufferedCodec : public limax::Codec
	{
	public:
		std::string buffer;

		BufferedCodec() { }

		virtual void update(int8_t c) override
		{
			buffer.append(1, (char)c);
		}

		virtual void update(int8_t data[], int32_t off, int32_t len) override
		{
			buffer.append((const char*)(data + off), len);
		}

		virtual void flush() override
		{
		}
	};

	void Socket::SetOutputSecurity(int encryptType, const int8_t* key, int keylen, int compressC2s)
	{
		std::shared_ptr<limax::Codec> codec = OutputBuffer;
		switch (encryptType)
		{
		case Constant::eEncryptTypeDisable:
			break;
		case Constant::eEncryptTypeAes:
			codec = std::shared_ptr<limax::Codec>(new limax::Encrypt(codec, (int8_t*)key, keylen));
			break;
			//TODO: 新增加密算法支持这里加case
		default:
			throw new std::exception("SetOutputSecurityCodec: unknown encryptType=");
		}

		switch (compressC2s)
		{
		case Constant::eCompressTypeDisable:
			break;
		case Constant::eCompressTypeMppc:
			codec = std::shared_ptr<limax::Codec>(new limax::RFC2118Encode(codec));
			break;
			//TODO: 新增压缩算法支持这里加case
		default:
			throw new std::exception("SetOutputSecurityCodec: unknown compress=");
		}
		std::lock_guard<std::recursive_mutex> scoped(mutex);
		OutputCodec = codec;
	}

	void Socket::SetInputSecurity(int encryptType, const int8_t* key, int keylen, int compressS2c)
	{
		std::shared_ptr<limax::Codec> codec = InputBuffer;
		switch (compressS2c)
		{
		case Constant::eCompressTypeDisable:
			break;
		case Constant::eCompressTypeMppc:
			codec = std::shared_ptr<limax::Codec>(new limax::RFC2118Decode(codec));
			break;
			// TODO: 新增压缩算法支持这里加case
		default:
			throw new std::exception("SetInputSecurityCodec: unknown compressType=");
		}
		switch (encryptType)
		{
		case Constant::eEncryptTypeDisable:
			break;
		case Constant::eEncryptTypeAes:
			codec = std::shared_ptr<limax::Codec>(new limax::Decrypt(codec, (int8_t*)key, keylen));
			break;
			//TODO: 新增加密算法支持这里加case
		default:
			throw new std::exception("SetInputSecurityCodec: unknown encryptType=");
		}
		std::lock_guard<std::recursive_mutex> scoped(mutex);
		InputCodec = codec;
	}

	Service::~Service()
	{
	}

	void Service::OnSocketClose(const std::shared_ptr<Socket>& sender, const std::exception* e)
	{
		sender;

		if (e)
			std::cout << "OnSocketClose " << e->what() << std::endl;

		if (this->autoReconnect)
		{
			if (0 == autoReconnectDelay)
				autoReconnectDelay = 1000;
			else
			{
				autoReconnectDelay *= 2;
				if (autoReconnectDelay > 30000)
					autoReconnectDelay = 30000;
			}
			StartConnect(this->lastSuccessAddress, this->lastPort, autoReconnectDelay, 5);
		}
	}

	void Service::OnHandshakeDone(const std::shared_ptr<Socket>& sender)
	{
		sender->IsHandshakeDone = true;
	}

	void Service::OnSocketConnectError(const std::shared_ptr<Socket>& sender, const std::exception* e)
	{
		sender;
		if (e)
			std::cout << "OnSocketConnectError " << e->what() << std::endl;
	}

	void Service::OnSocketConnected(const std::shared_ptr<Socket>& sender)
	{
		if (socket.get())
		{
			socket->Close(nullptr);
		}
		socket = sender;
		autoReconnectDelay = 0;
	}

	void Service::DispatchProtocol(Protocol* p, Service::ProtocolFactoryHandle& factoryHandle)
	{
		std::auto_ptr<Protocol> at(p);
		factoryHandle.Handle(p);
	}

	void Service::DispatchRpcResponse(Protocol* r, std::function<int(Protocol*)>& responseHandle, Service::ProtocolFactoryHandle& factoryHandle)
	{
		std::auto_ptr<Protocol> at(r);
		responseHandle(r);
	}

	void Service::OnSocketProcessInputBuffer(const std::shared_ptr<Socket>& sender, ByteBuffer& input)
	{
		Protocol::DecodeProtocol(this, sender, input);
	}

	void Service::DispatchUnknownProtocol(const std::shared_ptr<Socket>& sender, int moduleId, int protocolId, ByteBuffer& data)
	{
		sender; moduleId; protocolId; data;
	}

	void Service::StartConnect(const std::string& host, int port, int delay, int timeoutSecondsPerConnect)
	{
		std::thread([this, host, port, delay, timeoutSecondsPerConnect]
			{
				std::this_thread::sleep_for(std::chrono::milliseconds(delay));
				Socket* sptr = new Socket(this);
				std::shared_ptr<Socket> at = sptr->This;
				try
				{
					if (at->Connect(host, port, lastSuccessAddress, timeoutSecondsPerConnect))
					{
						lastSuccessAddress = at->LastAddress;
						return;
					}
					// 连接失败，内部已经调用Close释放shared_ptr。
				}
				catch (...)
				{
					at->Close(nullptr); // XXX 异常的时候需要手动释放Socket内部的shared_ptr。
				}
			}).detach();
	}

	void Service::Connect(const std::string& host, int port, int timeoutSecondsPerConnect)
	{
		lastSuccessAddress = host;
		lastPort = port;
		StartConnect(host, port, autoReconnectDelay, timeoutSecondsPerConnect);
	}

	Socket::Socket(Service* svr)
		: service(svr)
	{
		This.reset(this);
		IsHandshakeDone = false;
		SessionId = NextSessionId();

		OutputBuffer.reset(new BufferedCodec());
		InputBuffer.reset(new BufferedCodec());
		service->TryStartKeepAliveCheckTimer();
	}

	/// <summary>
	/// 下面使用系统socket-api实现真正的网络操作。尽量使用普通api，平台相关。
	/// </summary>

	class Selector
	{
	public:
		static Selector* Instance;

		static const int OpRead = 1;
		static const int OpWrite = 2;
		static const int OpClose = 4;

		std::mutex mutexPending;
		std::unordered_map<std::shared_ptr<Socket>, int> pending;

		void Select(const std::shared_ptr<Socket>& sock, int add, int remove)
		{
			std::lock_guard<std::mutex> g(mutexPending);
			int oldFlags = sock->selectorFlags;
			int newFlags = (oldFlags & ~remove) | add;
			if (oldFlags != newFlags)
			{
				sock->selectorFlags = newFlags;
				auto rc = pending.insert(std::make_pair(sock, newFlags));
				if (false == rc.second) // 已经存在，更新flags
					rc.first->second = newFlags;
				Wakeup();
			}
		}

		int wakeupfds[2];
		bool loop = true;
		std::thread* worker;
		std::unordered_map<std::shared_ptr<Socket>, int> sockets;

		Selector()
		{
			pipe(wakeupfds);
			worker = new std::thread(std::bind(&Selector::Loop, this));
		}

		~Selector()
		{
			loop = false;
			worker->join();
			delete worker;

			for (auto& socket : sockets)
				socket.first->Close(nullptr);
			sockets.clear();
		}

		void Loop()
		{
			while (loop)
			{
				{
					// apply pending
					std::lock_guard<std::mutex> g(mutexPending);
					for (auto& p : pending)
					{
						if (p.second & OpClose)
							sockets.erase(p.first);
						else
							sockets[p.first] = p.second;
					}
					pending.clear();
				}

				fd_set setwrite, setread;
				FD_ZERO(&setwrite);
				FD_ZERO(&setread);

				int maxfd = 0;
				for (auto& socket : sockets)
				{
					if (socket.first->socket > maxfd)
						maxfd = socket.first->socket;
					if (socket.second & OpRead)
						FD_SET(socket.first->socket, &setread);
					if (socket.second & OpWrite)
						FD_SET(socket.first->socket, &setwrite);
				}
				FD_SET(wakeupfds[0], &setread); // wakeup fd

				struct timeval timeout = { 0 };
				timeout.tv_sec = 1;
				timeout.tv_usec = 0;

				if (::select(maxfd + 1, &setread, &setwrite, nullptr, &timeout) > 0)
				{
					if (FD_ISSET(wakeupfds[0], &setread))
					{
						char buf[256];
						while (true) // 确保读完所有的wakeup消息。
						{
							if (::recv(wakeupfds[0], buf, sizeof(buf), 0) < sizeof(buf))
								break;
						}
					}
					for (auto& socket : sockets)
					{
						try
						{
							if (FD_ISSET(socket.first->socket, &setread))
								socket.first->OnRecv();
							if (FD_ISSET(socket.first->socket, &setwrite))
								socket.first->OnSend();
						}
						catch (std::exception& ex)
						{
							std::cout << "Selector Dispatch " << ex.what() << std::endl;
							socket.first->Close(&ex);
						}
					}
				}
				{
					int64_t now = time(0);
					timeouts_t::iterator it = timeouts.begin();
					while (it != timeouts.end())
					{
						if (now >= it->second)
						{
							try
							{
								it->first();
							}
							catch (std::exception& ex)
							{
								std::cout << "Selector Timeout " << ex.what() << std::endl;
							}
							timeouts.erase(it++);
						}
						else
							++it;
					}
				}
			}
		}
		typedef std::list<std::pair<std::function<void()>, int64_t>> timeouts_t;
		timeouts_t timeouts;

		void SetTimeout(const std::function<void()>& func, int timeout)
		{
			timeouts.push_back(std::make_pair(func, time(0) + timeout));
		}

		void Wakeup()
		{
			::send(wakeupfds[1], " ", 1, 0);
		}

		// 客户端不需要大量连接，先实现一个总是使用select的版本。
		// 看需要再实现其他版本。
#ifdef LIMAX_OS_WINDOWS
		int pipe(int fildes[2])
		{
			sockaddr_in name;
			memset(&name, 0, sizeof(name));
			name.sin_family = AF_INET;
			name.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
			int namelen = sizeof(name);
			SOCKET tcp1 = -1, tcp2 = -1;
			SOCKET tcp = socket(AF_INET, SOCK_STREAM, 0);
			if (tcp == -1) {
				goto clean;
			}
			if (bind(tcp, (sockaddr*)&name, namelen) == -1) {
				goto clean;
			}
			if (listen(tcp, 5) == -1) {
				goto clean;
			}
			if (getsockname(tcp, (sockaddr*)&name, &namelen) == -1) {
				goto clean;
			}
			tcp1 = socket(AF_INET, SOCK_STREAM, 0);
			if (tcp1 == -1) {
				goto clean;
			}
			if (-1 == connect(tcp1, (sockaddr*)&name, namelen)) {
				goto clean;
			}
			tcp2 = accept(tcp, (sockaddr*)&name, &namelen);
			if (tcp2 == -1) {
				goto clean;
			}
			if (closesocket(tcp) == -1) {
				goto clean;
			}
			fildes[0] = (int)tcp1;
			fildes[1] = (int)tcp2;
			return 0;
		clean:
			if (tcp != -1) {
				closesocket(tcp);
			}
			if (tcp2 != -1) {
				closesocket(tcp2);
			}
			if (tcp1 != -1) {
				closesocket(tcp1);
			}
			return -1;
	}
#endif
	};

	Selector* Selector::Instance = nullptr;
	void SetTimeout(const std::function<void()>& func, int timeout)
	{
		Selector::Instance->SetTimeout(func, timeout);
	}

	bool Startup()
	{
		bool sysresult = false;
#ifdef LIMAX_OS_WINDOWS
		WSADATA wData;
		sysresult = (0 == ::WSAStartup(MAKEWORD(2, 2), &wData));
#endif
		if (sysresult)
		{
			Selector::Instance = new Selector();
		}
		return true;
	}

	void Cleanup()
	{
		delete Selector::Instance;
		Selector::Instance = nullptr;

#ifdef LIMAX_OS_WINDOWS
		::WSACleanup();
#endif
	}

	void Socket::Close(std::exception* e)
	{
		if (Selector::Instance)
		{
			service->OnSocketClose(This, e);
			Selector::Instance->Select(This, Selector::OpClose, 0);
		}
		This.reset();
	}

	inline void platform_close_socket(int& so)
	{
#ifdef LIMAX_OS_WINDOWS
		::closesocket(so);
#else
		::close(so);
#endif
		so = 0;
	}

	inline bool platform_ignore_error_for_send()
	{
#ifdef LIMAX_OS_WINDOWS
		return ::WSAGetLastError() == WSAEWOULDBLOCK;
#else
		return errno = EWOULDBLOCK;
#endif
	}

	Socket::~Socket()
	{
		std::cout << "~Socket" << std::endl;
		platform_close_socket(socket);
	}

	class Buffer
	{
	public:
		char* data;
		int capacity;
		Buffer()
		{
			capacity = 16 * 1024;
			data = new char[capacity];
		}
		~Buffer()
		{
			delete[] data;
		}
	};

	void Socket::OnRecv()
	{
		std::lock_guard<std::recursive_mutex> g(mutex);
		activeRecvTime = time(0);

		Buffer recvbuf;
		int rc = ::recv(socket, recvbuf.data, recvbuf.capacity, 0);
		if (-1 == rc)
		{
			if (false == platform_ignore_error_for_send())
			{
				std::exception senderr("onsend error");
				this->Close(&senderr);
				return;
			}
			return;
		}
		if (0 == rc)
		{
			this->Close(nullptr);
			return;
		}

		if (InputCodec.get())
		{
			InputCodec->update((int8_t*)recvbuf.data, 0, rc);
			InputCodec->flush();
			ByteBuffer bb((unsigned char*)InputBuffer->buffer.data(), 0, (int)InputBuffer->buffer.size());
			service->OnSocketProcessInputBuffer(This, bb);
			InputBuffer->buffer.erase(0, bb.ReadIndex);
		}
		else if (InputBuffer->buffer.size() > 0)
		{
			InputBuffer->buffer.append(recvbuf.data, rc);
			ByteBuffer bb((unsigned char*)InputBuffer->buffer.data(), 0, (int)InputBuffer->buffer.size());
			service->OnSocketProcessInputBuffer(This, bb);
			InputBuffer->buffer.erase(0, bb.ReadIndex);
		}
		else
		{
			ByteBuffer bb((unsigned char*)recvbuf.data, 0, rc);
			service->OnSocketProcessInputBuffer(This, bb);
			if (bb.Size() > 0)
				InputBuffer->buffer.append((const char *)(bb.Bytes + bb.ReadIndex), bb.Size());
		}

		if (InputBuffer->buffer.empty())
			InputBuffer->buffer = std::string(); // XXX release memory if empty
	}

	void Socket::OnSend()
	{
		std::lock_guard<std::recursive_mutex> g(mutex);

		int rc = ::send(socket, OutputBuffer->buffer.data(), (int)OutputBuffer->buffer.size(), 0);
		if (-1 == rc)
		{
			if (false == platform_ignore_error_for_send())
			{
				std::exception senderr("onsend error");
				this->Close(&senderr);
				return;
			}
			rc = 0;
		}
		OutputBuffer->buffer.erase(0, rc);
		if (OutputBuffer->buffer.empty())
			Selector::Instance->Select(This, 0, Selector::OpWrite);
	}

	void Socket::Send(const char* data, int offset, int length)
	{
		std::lock_guard<std::recursive_mutex> g(mutex);
		activeSendTime = time(0);

		bool noPendingSend = OutputBuffer->buffer.empty();
		bool hasCodec = false;
		if (OutputCodec.get())
		{
			OutputCodec->update((int8_t*)data, offset, length);
			OutputCodec->flush();
			data = OutputBuffer->buffer.data();
			offset = 0;
			length = (int)OutputBuffer->buffer.size();
			hasCodec = true;
		}

		if (noPendingSend)
		{
			// try send direct
			int rc = ::send(socket, data + offset, length, 0);
			if (rc == -1)
			{
				if (false == platform_ignore_error_for_send())
				{
					std::exception senderr("send error");
					this->Close(&senderr);
					return;
				}
				rc = 0;
			}

			if (hasCodec)
			{
				OutputBuffer->buffer.erase(0, rc);
				if (false == OutputBuffer->buffer.empty())
					Selector::Instance->Select(This, Selector::OpWrite, 0);
				return;
			}
			if (rc >= length)
			{
				return; // all send and hasn't Codec
			}
			// part send
			offset += rc;
			length -= rc;
			OutputBuffer->buffer.append(data + offset, length);
			Selector::Instance->Select(This, Selector::OpWrite, 0);
			return;
		}
		// in sending
		if (false == hasCodec) // 如果有Codec，那么将要发送的数据已经被处理(update)到buffer中，不需要再次添加。
			OutputBuffer->buffer.append(data + offset, length);
	}

	inline void AssignAddressBytes(struct addrinfo* ai, std::string& out)
	{
		switch (ai->ai_family)
		{
		case AF_INET:
		{
			sockaddr_in* sav4 = (sockaddr_in*)ai->ai_addr;
			out.assign((const char*)&(sav4->sin_addr), sizeof(in_addr));
			std::cout << "ipv4 " << sizeof(in_addr) << std::endl;
			break;
		}
		case AF_INET6:
		{
			sockaddr_in6* sav6 = (sockaddr_in6*)ai->ai_addr;
			out.assign((const char*)&(sav6->sin6_addr), sizeof(in6_addr));
			std::cout << "ipv6 " << sizeof(in6_addr) << std::endl;
			break;
		}
		}
	}

	bool Socket::Connect(const std::string& host, int _port, const std::string& lastSuccessAddress, int timeoutSecondsPerConnect)
	{
		struct addrinfo hints, *res;

		memset(&hints, 0, sizeof(hints));
		hints.ai_family = AF_UNSPEC;
		hints.ai_socktype = SOCK_STREAM;
		hints.ai_protocol = IPPROTO_TCP;
		//hints.ai_flags = AI_V4MAPPED | AI_ALL;

		std::stringstream sport;
		sport << _port;
		std::string port(sport.str());

		if (0 != ::getaddrinfo(host.c_str(), port.c_str(), &hints, &res))
		{
			if (lastSuccessAddress.empty() || 0 != ::getaddrinfo(lastSuccessAddress.c_str(), port.c_str(), &hints, &res))
			{
				std::exception dnsfail("dns query fail");
				this->Close(&dnsfail);
				return false;
			}
		}

		int so = 0;
		for (struct addrinfo* ai = res; ai != nullptr; ai = ai->ai_next)
		{
			so = (int)::socket(ai->ai_family, ai->ai_socktype, ai->ai_protocol);
			if (so == 0)
				continue;

			// 设置异步模式
#ifdef LIMAX_OS_WINDOWS
			u_long ul = 1;
			if (SOCKET_ERROR == ::ioctlsocket(so, FIONBIO, &ul))
#else
			if (-1 == fcntl(so, F_SETFL, fcntl(sock, F_GETFL) | O_NONBLOCK))
#endif
			{
				platform_close_socket(so);
				continue;
			}

			int ret = ::connect(so, ai->ai_addr, static_cast<int>(ai->ai_addrlen));
			if (ret != -1) // 连接马上成功，异步socket，windows应该不会立即返回成功。保险写法
			{
				AssignAddressBytes(ai, LastAddressBytes);
				char addrName[256];
				if (::getnameinfo(ai->ai_addr, static_cast<socklen_t>(ai->ai_addrlen), addrName, sizeof(addrName), nullptr, 0, NI_NUMERICHOST) == 0)
					LastAddress = addrName; // 设置成功连接的地址
				break;
			}
#ifdef LIMAX_OS_WINDOWS
			if (::WSAGetLastError() == WSAEWOULDBLOCK) // 连接处理中。。。
#else
			if (errno == EINPROGRESS)
#endif
			{
				fd_set setw;
				FD_ZERO(&setw);
				FD_SET(so, &setw);
				struct timeval timeout = { 0 };
				timeout.tv_sec = timeoutSecondsPerConnect;
				timeout.tv_usec = 0;
				ret = ::select(so + 1, nullptr, &setw, nullptr, &timeout);
				if (ret <= 0)
				{
					// 错误或者超时
					platform_close_socket(so);
					continue;
				}
				int err = -1;
				socklen_t socklen = sizeof(err);
				if (0 == ::getsockopt(so, SOL_SOCKET, SO_ERROR, (char*)&err, &socklen))
				{
					if (err == 0)
					{
						AssignAddressBytes(ai, LastAddressBytes);
						char addrName[256];
						if (::getnameinfo(ai->ai_addr, static_cast<socklen_t>(ai->ai_addrlen), addrName, sizeof(addrName), nullptr, 0, NI_NUMERICHOST) == 0)
							LastAddress = addrName; // 设置成功连接的地址
						break;
					}
				}
			}
			platform_close_socket(so);
		}
		::freeaddrinfo(res);

		if (0 == so)
		{
			std::exception connfail("connect fail");
			this->Close(&connfail);
			return false;
		}
		this->socket = so;
		Selector::Instance->Select(This, Selector::OpRead, 0);
		service->OnSocketConnected(This);
		return true;
	}

	void Service::SetKeepConfig(int period, int sendTimeout, int recvTimeout)
	{
		keepCheckPeriod = period;
		keepSendTimeout = sendTimeout;
		keepRecvTimeout = recvTimeout;
		TryStartKeepAliveCheckTimer();
	}

	void Service::TryStartKeepAliveCheckTimer()
	{
		auto period = GetKeepCheckPeriod();
		if (period > 0)
			SetTimeout(std::bind(&Service::CheckKeepAlive, this), period);
	}

	void Service::CheckKeepAlive()
	{
		auto keepRecvTimeout = GetKeepRecvTimeout();
		if (keepRecvTimeout <= 0)
			keepRecvTimeout = 0x7fffffff;
		auto keepSendTimeout = GetKeepSendTimeout();
		if (keepSendTimeout <= 0)
			keepSendTimeout = 0x7fffffff;
		int64_t now = time(0);

		// c++ Service 只维护一个连接。
		if (now - socket->GetActiveRecvTime() > keepRecvTimeout)
		{
			try
			{
				OnKeepAliveTimeout(socket);
			}
			catch (std::exception& e)
			{
				std::cout << "onKeepAliveTimeout exception:" << e.what() << std::endl;
			}
		}
		// c++ Socket MUST BE client
		if (now - socket->GetActiveSendTime() > keepSendTimeout)
		{
			try
			{
				OnSendKeepAlive(socket);
			}
			catch (std::exception& e)
			{
				std::cout << "onSendKeepAlive exception:" << e.what() << std::endl;
			}
		}
		TryStartKeepAliveCheckTimer();
	}

	void Service::OnKeepAliveTimeout(const std::shared_ptr<Socket>& socket)
	{
		std::cout << "socket keep alive timeout " << socket->LastAddress << std::endl;
		socket->Close(NULL);
	}

	void Service::OnSendKeepAlive(const std::shared_ptr<Socket> & socket)
	{
		// CKeepAlive().Send(socket.get());
	}
} // namespace Net
} // namespace Zeze
