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
		// client protocol
		SHandshake0 sHandshake0;
		AddProtocolFactory(sHandshake0.TypeId(), Zeze::Net::Service::ProtocolFactoryHandle(
			[]() { return new SHandshake0(); }, std::bind(&Service::ProcessSHandshake0, this, std::placeholders::_1)));
		SHandshake sHandshake;
		AddProtocolFactory(sHandshake.TypeId(), Zeze::Net::Service::ProtocolFactoryHandle(
			[]() { return new SHandshake(); }, std::bind(&Service::ProcessSHandshake, this, std::placeholders::_1)));

		// server protocol
		CHandshake cHandshake;
		AddProtocolFactory(cHandshake.TypeId(), Zeze::Net::Service::ProtocolFactoryHandle(
			[]() { return new CHandshake(); }, std::bind(&Service::ProcessCHandshake, this, std::placeholders::_1)));
		CHandshakeDone done;
		AddProtocolFactory(done.TypeId(), Zeze::Net::Service::ProtocolFactoryHandle(
			[]() { return new CHandshakeDone(); }, std::bind(&Service::ProcessCHandshakeDone, this, std::placeholders::_1)));

		KeepAlive keepAlive;
		if (ProtocolFactory.find(keepAlive.TypeId()) == ProtocolFactory.end())
		{
			AddProtocolFactory(keepAlive.TypeId(), Zeze::Net::Service::ProtocolFactoryHandle(
				[]() { return new KeepAlive(); }, std::bind(&Service::ProcessKeepAliveRequest, this, std::placeholders::_1)));
		}

		handshakeProtocols.insert(sHandshake0.TypeId());
		handshakeProtocols.insert(sHandshake.TypeId());
		handshakeProtocols.insert(cHandshake.TypeId());
		handshakeProtocols.insert(done.TypeId());
		handshakeProtocols.insert(keepAlive.TypeId());

		TryStartKeepAliveCheckTimer();
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

	void Service::SetHandshakeOptions(int encryptType, int compressS2C, int compressC2S)
	{
		std::lock_guard<std::recursive_mutex> g(mutex);
		this->encryptType = encryptType;
		this->compressS2C = compressS2C;
		this->compressC2S = compressC2S;
	}

	int ClientCompress(int c)
	{
		// 客户端检查一下当前版本是否支持推荐的压缩算法。
		// 如果不支持则统一使用最老的。
		// 这样当服务器新增了压缩算法，并且推荐了新的，客户端可以兼容它。
		if (c == Constant::eCompressTypeDisable)
			return c; // 推荐关闭压缩就关闭
		return Constant::eCompressTypeMppc; // 使用最老的压缩。
	}

	int ClientEncrypt(int e)
	{
		switch (e)
		{
			case Constant::eEncryptTypeDisable:
			case Constant::eEncryptTypeAes:
			case Constant::eEncryptTypeAesNoSecureIp:
				return e;
		}
		return Constant::eEncryptTypeAesNoSecureIp; // 保底。
	}

	void Service::StartHandshake(int encryptType, int compressS2c, int compressC2s, const std::shared_ptr<Socket>& sender)
	{
		sender->dhContext = limax::createDHContext(1);
		const std::vector<unsigned char>& dhResponse = sender->dhContext->generateDHResponse();
		CHandshake hand;
		hand.Argument->encryptType = ClientEncrypt(encryptType);
		if (encryptType == Constant::eEncryptTypeAes || encryptType == Constant::eEncryptTypeAesNoSecureIp)
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
		int inputKeyLen = 0;
		const int8_t* outputKey = nullptr;
		int outputKeyLen = 0;
		std::auto_ptr<limax::HmacMD5> outputHmacMD5;
		std::auto_ptr<limax::HmacMD5> inputHmacMD5;

		switch (p->Argument->encryptType)
		{
			case Constant::eEncryptTypeAes:
			{
				auto material = p->Sender->dhContext->computeDHKey(
					(unsigned char*)p->Argument->encryptParam.data(), (int)p->Argument->encryptParam.size());

				size_t key_len = p->Sender->GetLastAddressBytes().size();
				int8_t* key = (int8_t*)p->Sender->GetLastAddressBytes().data();

				int32_t half = (int32_t)material.size() / 2;
				{
					outputHmacMD5.reset(new limax::HmacMD5(key, 0, (int)key_len));
					outputHmacMD5->update((int8_t*)&material[0], 0, half);
					outputKey = outputHmacMD5->digest();
					outputKeyLen = 16;
				}
				{
					inputHmacMD5.reset(new limax::HmacMD5(key, 0, (int)key_len));
					inputHmacMD5->update((int8_t*)&material[0], half, (int32_t)material.size() - half);
					inputKey = inputHmacMD5->digest();
					inputKeyLen = 16;
				}
			}
			break;

			case Constant::eEncryptTypeAesNoSecureIp:
			{
				// material 是const std::vector<unsigned char>&，不用释放。
				auto material = p->Sender->dhContext->computeDHKey(
					(unsigned char*)p->Argument->encryptParam.data(), (int)p->Argument->encryptParam.size());

				int32_t half = (int32_t)material.size() / 2;
				outputKey = (const int8_t*)&material[0];
				outputKeyLen = half;
				inputKey = (const int8_t*)&material[half];
				inputKeyLen = (int)(material.size() - half);
			}
			break;
		}
		p->Sender->SetOutputSecurity(p->Argument->encryptType, outputKey, outputKeyLen, p->Argument->compressC2s);
		p->Sender->SetInputSecurity(p->Argument->encryptType, inputKey, inputKeyLen, p->Argument->compressS2c);
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

	void Socket::SetOutputSecurity(int encryptType, const int8_t* key, int keylen, int compress)
	{
		std::cout << "SetOutputSecurity encrypt=" << encryptType << " keyLen=" << keylen << " compress=" << compress << std::endl;
		std::shared_ptr<limax::Codec> codec = OutputBuffer;
		switch (encryptType)
		{
		case Constant::eEncryptTypeDisable:
			break;
		case Constant::eEncryptTypeAes:
		case Constant::eEncryptTypeAesNoSecureIp:
			codec = std::shared_ptr<limax::Codec>(new limax::Encrypt(codec, (int8_t*)key, keylen));
			break;
			//TODO: 新增加密算法支持这里加case
		default:
			throw new std::exception("SetOutputSecurityCodec: unknown encryptType=");
		}

		switch (compress)
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

	void Socket::SetInputSecurity(int encryptType, const int8_t* key, int keylen, int compress)
	{
		std::cout << "SetInputSecurity encrypt=" << encryptType << " keyLen=" << keylen << " compress=" << compress << std::endl;
		std::shared_ptr<limax::Codec> codec = InputBuffer;
		switch (compress)
		{
		case Constant::eCompressTypeDisable:
			break;
		case Constant::eCompressTypeMppc:
		case Constant::eEncryptTypeAesNoSecureIp:
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
		case Constant::eEncryptTypeAesNoSecureIp:
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
		std::lock_guard<std::recursive_mutex> g(mutex);
		sockets.erase(sender->GetSessionId());
		if (e)
			std::cout << "OnSocketClose " << e->what() << std::endl;
	}

	void Service::OnHandshakeDone(const std::shared_ptr<Socket>& sender)
	{
		sender->handshakeDone = true;
	}

	void Service::OnSocketConnectError(const std::shared_ptr<Socket>& sender, const std::exception* e)
	{
		std::lock_guard<std::recursive_mutex> g(mutex);
		sockets.erase(sender->GetSessionId());
		if (e)
			std::cout << "OnSocketConnectError " << e->what() << std::endl;
	}

	void Service::OnSocketConnected(const std::shared_ptr<Socket>& sender)
	{
		AddSocket(sender);
	}

	void Service::OnSocketAccept(const std::shared_ptr<Socket>& sender)
	{
		AddSocket(sender);

		SHandshake0 hand0;
		hand0.Argument->encryptType = this->encryptType;
		hand0.Argument->supportedEncryptList.push_back(eEncryptTypeAes);
		hand0.Argument->supportedEncryptList.push_back(eEncryptTypeAesNoSecureIp);
		hand0.Argument->compressS2c = this->compressS2C;
		hand0.Argument->compressC2s = this->compressC2S;
		hand0.Argument->supportedCompressList.push_back(eCompressTypeMppc);
		hand0.Send(sender.get());
	}

	int Service::ProcessCHandshakeDone(Protocol* p)
	{
		OnHandshakeDone(p->Sender);
		return 0;
	}

	int ServerCompressS2c(int s2cHint) {
		switch (s2cHint)
		{
		case eCompressTypeDisable:
		case eCompressTypeMppc:
			return s2cHint;
		}
		return eCompressTypeMppc; // 跟客户端选择的不兼容,就强制以MPPC作为兜底的压缩,客户端如果还不支持就无法继续通信了
	}

	int ServerCompressC2s(int c2sHint) {
		switch (c2sHint)
		{
		case eCompressTypeDisable:
		case eCompressTypeMppc:
			return c2sHint;
		}
		return eCompressTypeMppc; // 跟客户端选择的不兼容,就强制以MPPC作为兜底的压缩,客户端如果还不支持就无法继续通信了
	}

	int Service::ProcessCHandshake(Protocol* _p)
	{
		auto p = (CHandshake*)_p;
		const int8_t* inputKey = nullptr;
		int inputKeyLen = 0;
		const int8_t* outputKey = nullptr;
		int outputKeyLen = 0;
		std::string response;
		int group = 1;

		std::auto_ptr<limax::HmacMD5> inputHmacMD5;
		std::auto_ptr<limax::HmacMD5> outputHmacMD5;

		switch (p->Argument->encryptType) {
		case eEncryptTypeAes: {
			std::string & data = p->Argument->encryptParam;
			auto context = limax::createDHContext(group);
			auto material = context->computeDHKey((unsigned char*)data.data(), (int)data.size());

			std::string keyStr = GetSecureIp();
			if (keyStr.empty())
				keyStr = p->Sender->GetLocalAddress();
			int8_t* key = (int8_t*)keyStr.data();
			int keyLen = (int)keyStr.size();

			int half = (int)material.size() / 2;

			inputHmacMD5.reset(new limax::HmacMD5(key, 0, keyLen));
			inputHmacMD5->update((int8_t*) & material[0], 0, half);
			inputKey = inputHmacMD5->digest();
			inputKeyLen = 16;

			auto _response = context->generateDHResponse();
			response.assign((char *) & _response[0], _response.size());

			outputHmacMD5.reset(new limax::HmacMD5(key, 0, keyLen));
			outputHmacMD5->update((int8_t*) & material[0], half, (int)material.size() - half);
			outputKey = outputHmacMD5->digest();
			outputKeyLen = 16;
			break;
		}
		case eEncryptTypeAesNoSecureIp: {
			std::string& data = p->Argument->encryptParam;
			auto context = limax::createDHContext(group);
			auto material = context->computeDHKey((unsigned char*)data.data(), (int)data.size());
			int half = (int)material.size() / 2;

			inputKey = (const int8_t*) & material[0];
			inputKeyLen = half;
			auto _response = context->generateDHResponse();
			response.assign((char*)&_response[0], _response.size());
			outputKey = (const int8_t*)&material[half];
			outputKeyLen = (int)material.size() - half;
			break;
		}
		}
		auto s2c = ServerCompressS2c(p->Argument->compressS2c);
		auto c2s = ServerCompressC2s(p->Argument->compressC2s);
		p->Sender->SetInputSecurity(p->Argument->encryptType, inputKey, inputKeyLen, c2s);

		SHandshake sHandshake;
		sHandshake.Argument->encryptParam = response;
		sHandshake.Argument->compressS2c = s2c;
		sHandshake.Argument->compressC2s = c2s;
		sHandshake.Argument->encryptType = p->Argument->encryptType;
		sHandshake.Send(p->Sender.get());
		p->Sender->SetOutputSecurity(p->Argument->encryptType, outputKey, outputKeyLen, s2c);

		// 为了防止服务器在Handshake以后马上发送数据，
		// 导致未加密数据和加密数据一起到达Client，这种情况很难处理。
		// 这个本质上是协议相关的问题：就是前面一个协议的处理结果影响后面数据处理。
		// 所以增加CHandshakeDone协议，在Client进入加密以后发送给Server。
		// OnHandshakeDone(p.Sender);
		return 0;
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

	void Service::StartConnect(const std::string& host, int port, int timeout)
	{
		Socket* ss = new Socket(this);
		try
		{
			ss->Connect(host, port, timeout);
		}
		catch (std::exception&)
		{
			// 异常必须主动关闭。
			ss->Close(nullptr);
		}
	}

	void Service::Connect(const std::string& host, int port, int timeout)
	{
		StartConnect(host, port, timeout);
	}

	Socket::Socket(Service* svr)
	{
		service = svr;
		thisSharedPtr.reset(this);
		handshakeDone = false;
		sessionId = NextSessionId();

		OutputBuffer.reset(new BufferedCodec());
		InputBuffer.reset(new BufferedCodec());
	}

	/// <summary>
	/// 下面使用系统socket-api实现真正的网络操作。尽量使用普通api，平台相关。
	/// </summary>

	inline void platform_close_socket(SOCKET so)
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

	class Selector
	{
	public:
		static Selector* Instance;

		void Add(SOCKET sock, uint32_t events)
		{
			epoll_event event;
			event.data.ptr = nullptr;
			event.events = events;
			epoll_ctl(epollHandle, EPOLL_CTL_ADD, sock, &event);
		}

		void Add(const std::shared_ptr<Socket>& sock, uint32_t events)
		{
			//std::cout << "add " << sock->socket << " " << events << std::endl;
			epoll_event event;
			event.data.ptr = sock.get();
			event.events = events;
			epoll_ctl(epollHandle, EPOLL_CTL_ADD, sock->socket, &event);
		}

		void Mod(const std::shared_ptr<Socket>& sock, uint32_t events)
		{
			//std::cout << "mod " << sock->socket << " " << events << std::endl;
			epoll_event event;
			event.data.ptr = sock.get();
			event.events = events;
			epoll_ctl(epollHandle, EPOLL_CTL_MOD, sock->socket, &event);
		}

		void Del(const std::shared_ptr<Socket>& sock)
		{
			epoll_event event;
			event.data.ptr = sock.get();
			event.events = 0;
			epoll_ctl(epollHandle, EPOLL_CTL_DEL, sock->socket, &event);
		}

		void Select(const std::shared_ptr<Socket>& sock, uint32_t add, uint32_t remove)
		{
			std::lock_guard<std::recursive_mutex> g(sock->mutex);
			uint32_t oldFlags = sock->selectorFlags;
			uint32_t newFlags = (oldFlags & ~remove) | add;
			if (oldFlags != newFlags)
			{
				if (sock->selector)
				{
					if (sock->selector != this)
						throw new std::exception("selector not null and not this.");
					Mod(sock, newFlags);
				}
				else
				{
					Add(sock, newFlags);
					sock->selector = this;
				}
				sock->selectorFlags = newFlags;
				Wakeup();
			}
		}

		SOCKET wakeupfds[2];
		bool loop = true;
		std::thread* worker;
		HANDLE epollHandle;

		Selector()
		{
			epollHandle = epoll_create(1);
			if (!epollHandle)
				throw new std::exception("epoll_create");
			pipe(wakeupfds);
			worker = new std::thread(std::bind(&Selector::Loop, this));
		}

		~Selector()
		{
			loop = false;
			worker->join();
			delete worker;
		}

		void Loop()
		{
			while (loop)
			{
				int timeout = 1000;
				epoll_event events[200];
				int rc = epoll_wait(epollHandle, events, 200, timeout);
				if (rc == -1 && errno != EINTR)
					return; // 内部错误。
				for (int i = 0; i < rc; ++i)
				{
					epoll_event& e = events[i];

					if (e.data.ptr == nullptr)
					{
						Buffer buf; // 大一些，确保读完，如果实在读不完，那就再醒一次，等下次读。
						recv(wakeupfds[0], buf.data, buf.capacity, 0);
						continue;
					}

					try
					{
						if (e.events & EPOLLIN)
							((Socket*)(e.data.ptr))->OnRecv();
						else if (e.events & EPOLLOUT)
							((Socket*)(e.data.ptr))->OnSend();
						else if (e.events & EPOLLERR)
							((Socket*)(e.data.ptr))->Close(std::exception("err"));
						else if (e.events & EPOLLHUP)
							((Socket*)(e.data.ptr))->Close(std::exception("hup"));
					}
					catch (std::exception& ex)
					{
						((Socket*)(e.data.ptr))->Close(&ex);
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

		int pipe(SOCKET fildes[2])
		{
			sockaddr_in name;
			memset(&name, 0, sizeof(name));
			name.sin_family = AF_INET;
			name.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
			int namelen = sizeof(name);
			SOCKET tcp1 = -1, tcp2 = -1;
			SOCKET tcp = socket(AF_INET, SOCK_STREAM, 0);
			if (tcp == INVALID_SOCKET) {
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
			if (tcp1 == INVALID_SOCKET) {
				goto clean;
			}
			if (-1 == connect(tcp1, (sockaddr*)&name, namelen)) {
				goto clean;
			}
			tcp2 = accept(tcp, (sockaddr*)&name, &namelen);
			if (tcp2 == -1) {
				goto clean;
			}
			platform_close_socket(tcp);
			fildes[0] = tcp1;
			fildes[1] = tcp2;
			Add(fildes[0], EPOLLIN);
			return 0;
		clean:
			if (tcp != INVALID_SOCKET) {
				platform_close_socket(tcp);
			}
			if (tcp2 != INVALID_SOCKET) {
				platform_close_socket(tcp2);
			}
			if (tcp1 != INVALID_SOCKET) {
				platform_close_socket(tcp1);
			}
			return -1;
		}
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

	void Socket::Close(const std::exception* e)
	{
		if (Selector::Instance)
		{
			service->OnSocketClose(thisSharedPtr, e);
			Selector::Instance->Del(thisSharedPtr);
		}
		thisSharedPtr.reset();
	}

	Socket::Socket(Service* svr, SOCKET so)
	{
		socket = so;
		service = svr;
		thisSharedPtr.reset(this);
		handshakeDone = false;
		sessionId = NextSessionId();

		OutputBuffer.reset(new BufferedCodec());
		InputBuffer.reset(new BufferedCodec());
		Selector::Instance->Select(GetThisSharedPtr(), EPOLLIN, 0);
	}

	Socket::~Socket()
	{
		//std::cout << "~Socket" << std::endl;
		platform_close_socket(socket);
	}

	void Socket::OnRecv()
	{
		auto ref = thisSharedPtr;
		std::lock_guard<std::recursive_mutex> g(mutex);
		activeRecvTime = time(0);

		Buffer recvbuf;
		int rc = ::recv(socket, recvbuf.data, recvbuf.capacity, 0);
		if (-1 == rc)
		{
			if (false == platform_ignore_error_for_send())
			{
				std::exception senderr("onrecv error");
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
			service->OnSocketProcessInputBuffer(thisSharedPtr, bb);
			InputBuffer->buffer.erase(0, bb.ReadIndex);
		}
		else if (InputBuffer->buffer.size() > 0)
		{
			InputBuffer->buffer.append(recvbuf.data, rc);
			ByteBuffer bb((unsigned char*)InputBuffer->buffer.data(), 0, (int)InputBuffer->buffer.size());
			service->OnSocketProcessInputBuffer(thisSharedPtr, bb);
			InputBuffer->buffer.erase(0, bb.ReadIndex);
		}
		else
		{
			ByteBuffer bb((unsigned char*)recvbuf.data, 0, rc);
			service->OnSocketProcessInputBuffer(thisSharedPtr, bb);
			if (bb.Size() > 0)
				InputBuffer->buffer.append((const char *)(bb.Bytes + bb.ReadIndex), bb.Size());
		}

		if (InputBuffer->buffer.empty())
			InputBuffer->buffer = std::string(); // XXX release memory if empty
	}

	void Socket::OnSend()
	{
		auto ref = thisSharedPtr;
		std::lock_guard<std::recursive_mutex> g(mutex);

		if (connectPending)
		{
			try
			{
				connectPending = false; // one shot!
				finishConnect();
				Selector::Instance->Select(thisSharedPtr, EPOLLIN, EPOLLOUT);
				service->OnSocketConnected(thisSharedPtr);
			}
			catch (std::exception& ex)
			{
				service->OnSocketConnectError(thisSharedPtr, &ex);
			}
			return;
		}

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
			Selector::Instance->Select(thisSharedPtr, 0, EPOLLOUT);
	}

	void Socket::Send(const char* data, int offset, int length)
	{
		auto ref = thisSharedPtr;
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
					Selector::Instance->Select(thisSharedPtr, EPOLLOUT, 0);
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
			Selector::Instance->Select(thisSharedPtr, EPOLLOUT, 0);
			return;
		}
		// in sending
		if (false == hasCodec) // 如果有Codec，那么将要发送的数据已经被处理(update)到buffer中，不需要再次添加。
			OutputBuffer->buffer.append(data + offset, length);
	}

	inline void AssignAddressBytesAi(addrinfo* ai, std::string& addrbytes, std::string &address)
	{
		char ip_str[INET6_ADDRSTRLEN];
		switch (ai->ai_family)
		{
		case AF_INET:
		{
			sockaddr_in* sav4 = (sockaddr_in*)ai->ai_addr;
			addrbytes.assign((const char*)&(sav4->sin_addr), sizeof(in_addr));
			if (inet_ntop(AF_INET, &(sav4->sin_addr), ip_str, INET_ADDRSTRLEN) != NULL)
				address.assign(ip_str);
			break;
		}
		case AF_INET6:
		{
			sockaddr_in6* sav6 = (sockaddr_in6*)ai->ai_addr;
			addrbytes.assign((const char*)&(sav6->sin6_addr), sizeof(in6_addr));
			if (inet_ntop(AF_INET6, &(sav6->sin6_addr), ip_str, INET6_ADDRSTRLEN) != NULL)
				address.assign(ip_str);
			break;
		}
		}
	}

	inline void AssignAddressBytes(sockaddr* ai, std::string& addrbytes, std::string & address)
	{
		char ip_str[INET6_ADDRSTRLEN];
		switch (ai->sa_family)
		{
		case AF_INET:
		{
			sockaddr_in* sav4 = (sockaddr_in*)ai;
			addrbytes.assign((const char*)&(sav4->sin_addr), sizeof(in_addr));
			if (inet_ntop(AF_INET, &(sav4->sin_addr), ip_str, INET_ADDRSTRLEN) != NULL)
				address.assign(ip_str);
			break;
		}
		case AF_INET6:
		{
			sockaddr_in6* sav6 = (sockaddr_in6*)ai;
			addrbytes.assign((const char*)&(sav6->sin6_addr), sizeof(in6_addr));
			if (inet_ntop(AF_INET6, &(sav6->sin6_addr), ip_str, INET6_ADDRSTRLEN) != NULL)
				address.assign(ip_str);
			break;
		}
		}
	}

	class ServerSocket : public Socket
	{
		int family;
	public:
		ServerSocket(Service* service, SOCKET fd, int family)
			: Socket(service, fd)
		{
			this->family = family;
		}

		virtual void OnRecv()
		{
			SOCKET acceptedso = -1;
			switch (family)
			{
			case AF_INET:
			{
				struct sockaddr_in addr;
				int addrlen = sizeof(addr);
				acceptedso = accept(socket, (sockaddr*)&addr, &addrlen);
				if (acceptedso == INVALID_SOCKET)
					return;
				AssignAddressBytes((sockaddr*)&addr, lastAddressBytes, lastAddress);
				break;
			}
			case AF_INET6:
			{
				struct sockaddr_in6 addr;
				int addrlen = sizeof(addr);
				acceptedso = accept(socket, (sockaddr*)&addr, &addrlen);
				if (acceptedso == INVALID_SOCKET)
					return;
				AssignAddressBytes((sockaddr*)&addr, lastAddressBytes, lastAddress);
				break;
			}
			}
			auto ss = new Socket(GetService(), acceptedso);
			GetService()->OnSocketAccept(ss->GetThisSharedPtr());
		}
	};

	std::string Service::Listen(const std::string& host, int port)
	{
		bool use_ipv6 = (host.find(':') != std::string::npos);

		struct sockaddr* addr;
		int addrlen;

		struct sockaddr_in6 address6;
		struct sockaddr_in address;

		if (use_ipv6)
		{
			memset(&address6, 0, sizeof(address6));
			address6.sin6_family = AF_INET6;
			address6.sin6_port = htons(port);
			if (host == "::" || host.empty())
				address6.sin6_addr = in6addr_any;  // 监听所有IPv6接口
			else if (inet_pton(AF_INET6, host.c_str(), &address6.sin6_addr) <= 0)
				return "inet_pton";

			addr = (sockaddr*)&address6;
			addrlen = sizeof(address6);
		}
		else
		{
			memset(&address, 0, sizeof(address));
			address.sin_family = AF_INET;
			address.sin_port = htons(port);
			if (host == "0.0.0.0" || host.empty())
				address.sin_addr.s_addr = INADDR_ANY; // 监听所有IPv4接口
			else if (inet_pton(AF_INET, host.c_str(), &address.sin_addr) <= 0)
				return "inet_pton";

			addr = (sockaddr*)&address;
			addrlen = sizeof(address);
		}

		int domain = use_ipv6 ? AF_INET6 : AF_INET;
		SOCKET serverfd = socket(domain, SOCK_STREAM, 0);
		if (serverfd == INVALID_SOCKET)
			return "create socket";

		int opt = 1;
		if (setsockopt(serverfd, SOL_SOCKET, SO_REUSEADDR, (const char*)&opt, sizeof(opt)))
		{
			platform_close_socket(serverfd);
			return "setsockopt";
		}
		if (bind(serverfd, addr, addrlen) < 0)
		{
			platform_close_socket(serverfd);
			return "bind";
		}
		if (listen(serverfd, SOMAXCONN) < 0)
		{
			platform_close_socket(serverfd);
			return "listen";
		}
		serverSockets.insert((new ServerSocket(this, serverfd, domain))->GetThisSharedPtr());
		return "";
	}

	bool Socket::Connect(const std::string& host, int _port, int timeout)
	{
		client = true;
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
			std::exception dnsfail("dns query fail");
			this->Close(&dnsfail);
			return false;
		}

		SOCKET so = INVALID_SOCKET;
		for (struct addrinfo* ai = res; ai != nullptr; ai = ai->ai_next)
		{
			so = ::socket(ai->ai_family, ai->ai_socktype, ai->ai_protocol);
			if (so == INVALID_SOCKET)
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
			if (ret == 0) // 连接马上成功，异步socket，windows应该不会立即返回成功。保险写法
			{
				AssignAddressBytesAi(ai, lastAddressBytes, lastAddress);
				break;
			}
#ifdef LIMAX_OS_WINDOWS
			if (::WSAGetLastError() == WSAEWOULDBLOCK) // 连接处理中。。。
#else
			if (errno == EINPROGRESS)
#endif
			{
				this->socket = so;
				this->connectPending = true;
				Selector::Instance->Select(thisSharedPtr, EPOLLOUT, 0);
				::freeaddrinfo(res);
				return true; // connect pending
			}
			platform_close_socket(so);
			so = INVALID_SOCKET;
		}
		::freeaddrinfo(res);

		if (INVALID_SOCKET == so)
		{
			std::exception connfail("connect fail");
			this->Close(&connfail);
			return false;
		}
		this->socket = so;
		Selector::Instance->Select(thisSharedPtr, EPOLLIN, 0);
		service->OnSocketConnected(thisSharedPtr);
		return true;
	}

	std::string Socket::GetLocalAddress() const
	{
		struct sockaddr_storage addr;
		socklen_t addr_len = sizeof(addr);

		if (getsockname(socket, (struct sockaddr*)&addr, &addr_len) < 0)
			return "";

		struct sockaddr* a = (sockaddr*)&addr;
		if (a->sa_family == AF_INET) {
			struct sockaddr_in* addr4 = (struct sockaddr_in*)a;
			return std::string((const char*)&addr4->sin_addr, INET_ADDRSTRLEN);
		}
		if (a->sa_family == AF_INET6) {
			struct sockaddr_in6* addr6 = (struct sockaddr_in6*)a;
			return std::string((const char*)&addr6->sin6_addr, INET6_ADDRSTRLEN);
		}
		return "";
	}

	void Socket::finishConnect()
	{
		int err = -1;
		socklen_t socklen = sizeof(err);
		if (0 == ::getsockopt(socket, SOL_SOCKET, SO_ERROR, (char*)&err, &socklen))
		{
			if (err == 0)
			{
				struct sockaddr_storage addr;
				int addrlen = sizeof(addr);
				if (0 == getpeername(socket, (sockaddr*)&addr, &addrlen))
				{
					AssignAddressBytes((sockaddr*) & addr, lastAddressBytes, lastAddress);
				}
				return;
			}
		}
		throw std::exception("finishConnect");
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
		{
			std::unordered_map<int64_t, std::shared_ptr<Socket>> tmp;
			{
				std::lock_guard<std::recursive_mutex> g(mutex);
				tmp = sockets;
			}
			for (auto it = tmp.begin(); it != tmp.end(); ++it)
			{
				std::shared_ptr<Socket>& socket = it->second;
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
				if (socket->client && now - socket->GetActiveSendTime() > keepSendTimeout)
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
			}
		}
		TryStartKeepAliveCheckTimer();
	}

	void Service::OnKeepAliveTimeout(const std::shared_ptr<Socket>& socket)
	{
		std::cout << "socket keep alive timeout " << socket->GetLastAddress() << std::endl;
		socket->Close(NULL);
	}

	int NullRpcResponseHandle(Protocol* p)
	{
		return 0;
	}

	void Service::OnSendKeepAlive(const std::shared_ptr<Socket> & socket)
	{
		KeepAlive().SendAsync(socket.get(), NullRpcResponseHandle, 5000);
	}
} // namespace Net
} // namespace Zeze
