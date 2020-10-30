
#include "common.h"
#include "Net.h"
#include "Protocol.h"
#include <iostream>
#include "ByteBuffer.h"
#include "security.h"
#include "rfc2118.h"
#include <unordered_map>

namespace Zeze
{
namespace Net
{
	class CHandshakeArgument : public Zeze::Serialize::Serializable
	{
	public:
		char dh_group = 0;
		std::string dh_data;

		void Decode(Zeze::Serialize::ByteBuffer& bb) override
		{
			dh_group = bb.ReadByte();
			dh_data = bb.ReadBytes();
		}

		void Encode(Zeze::Serialize::ByteBuffer& bb) override
		{
			bb.WriteByte(dh_group);
			bb.WriteBytes(dh_data);
		}
	};

	class SHandshakeArgument : public Zeze::Serialize::Serializable
	{
	public:
		std::string dh_data;
		bool s2cneedcompress = true;
		bool c2sneedcompress = true;

		void Decode(Zeze::Serialize::ByteBuffer& bb) override
		{
			dh_data = bb.ReadBytes();
			s2cneedcompress = bb.ReadBool();
			c2sneedcompress = bb.ReadBool();
		}

		void Encode(Zeze::Serialize::ByteBuffer& bb) override
		{
			bb.WriteBytes(dh_data);
			bb.WriteBool(s2cneedcompress);
			bb.WriteBool(c2sneedcompress);
		}
	};

	class CHandshake : public ProtocolWithArgument<CHandshakeArgument>
	{
	public:
		int ModuleId() override { return 0; }
		int ProtocolId() override { return 1; }

		CHandshake()
		{
		}

		CHandshake(char dh_group, const std::string& dh_data)
		{
			Argument.dh_group = dh_group;
			Argument.dh_data = dh_data;
		}
	};

	class SHandshake : public ProtocolWithArgument<SHandshakeArgument>
	{
	public:
		int ModuleId() override { return 0; }
		int ProtocolId() override { return 2; }

		SHandshake()
		{
		}

		SHandshake(const std::string& dh_data, bool s2cneedcompress, bool c2sneedcompress)
		{
			Argument.dh_data = dh_data;
			Argument.s2cneedcompress = s2cneedcompress;
			Argument.c2sneedcompress = c2sneedcompress;
		}
	};

	Service::Service(const std::string& _name)
		: name(_name), socket(NULL)
	{
		SHandshake forTypeId;
		AddProtocolFactory(forTypeId.TypeId(), Zeze::Net::Service::ProtocolFactoryHandle(
			[]() { return new SHandshake(); }, std::bind(&Service::ProcessSHandshake, this, std::placeholders::_1)));
	}

	int Service::ProcessSHandshake(Protocol* _p)
	{
		SHandshake* p = (SHandshake*)_p;

		const std::vector<unsigned char> material = p->Sender->dhContext->computeDHKey((unsigned char*)p->Argument.dh_data.data(), (int32_t)p->Argument.dh_data.size());
		socklen_t key_len = p->Sender->LastAddressBytes.size();
		int8_t* key = (int8_t*)p->Sender->LastAddressBytes.data();
		int32_t half = (int32_t)material.size() / 2;
		{
			limax::HmacMD5 hmac(key, 0, key_len);
			hmac.update((int8_t*)&material[0], 0, half);
			const int8_t* skey = hmac.digest();
			std::cout << "output ";
			for (int i = 0; i < 16; ++i)
				std::cout << std::hex << (unsigned int)(unsigned char)skey[i] << " ";
			std::cout << std::endl;
			p->Sender->SetOutputSecurity(p->Argument.c2sneedcompress, skey, 16);
		}
		{
			limax::HmacMD5 hmac(key, 0, key_len);
			hmac.update((int8_t*)&material[0], half, (int32_t)material.size() - half);
			const int8_t* skey = hmac.digest();
			std::cout << "input ";
			for (int i = 0; i < 16; ++i)
				std::cout << std::hex << (unsigned int)(unsigned char)skey[i] << " ";
			std::cout << std::endl;
			p->Sender->SetInputSecurity(p->Argument.s2cneedcompress, skey, 16);
		}
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

	void Socket::SetOutputSecurity(bool c2sneedcompress, const int8_t* key, int keylen)
	{
		std::shared_ptr<limax::Codec> codec = OutputBuffer;
		if (keylen > 0)
		{
			codec = std::shared_ptr<limax::Codec>(new limax::Encrypt(codec, (int8_t*)key, (int32_t)keylen));
		}
		if (c2sneedcompress)
		{
			codec = std::shared_ptr<limax::Codec>(new limax::RFC2118Encode(codec));
		}
		std::lock_guard<std::recursive_mutex> scoped(mutex);
		OutputCodec = codec;
	}

	void Socket::SetInputSecurity(bool s2cneedcompress, const int8_t* key, int keylen)
	{
		std::shared_ptr<limax::Codec> codec = InputBuffer;
		if (s2cneedcompress)
		{
			codec = std::shared_ptr<limax::Codec>(new limax::RFC2118Decode(codec));
		}
		if (keylen > 0)
		{
			codec = std::shared_ptr<limax::Codec>(new limax::Decrypt(codec, (int8_t*)key, (int32_t)keylen));
		}
		std::lock_guard<std::recursive_mutex> scoped(mutex);
		InputCodec = codec;
	}

	Service::~Service()
	{
	}

	void Service::InitializeLua(lua_State* L)
	{
		ToLua.LoadMeta(L);
		ToLua.RegisterGlobalAndCallback(this);
	}

	void Service::OnSocketClose(const std::shared_ptr<Socket>& sender, const std::exception* e)
	{
		if (e)
			std::cout << "OnSocketClose " << e->what() << std::endl;

		if (sender.get() == socket.get())
		{
			socket.reset();
		}
	}

	void Service::OnHandshakeDone(const std::shared_ptr<Socket>& sender)
	{
		sender->IsHandshakeDone = true;
		Helper.SetHandshakeDone(sender->SessionId, this);
	}

	void Service::OnSocketConnectError(const std::shared_ptr<Socket>& sender, const std::exception* e)
	{
		if (e)
			std::cout << "OnSocketConnectError " << e->what() << std::endl;
	}

	void Service::OnSocketConnected(const std::shared_ptr<Socket>& sender)
	{
		if (socket.get())
		{
			socket->Close(NULL);
		}
		socket = sender;
		sender->dhContext = limax::createDHContext(dhGroup);
		const std::vector<unsigned char> dhResponse = sender->dhContext->generateDHResponse();
		std::cout << "dhResponse ";
		for (int i = 0; i < dhResponse.size(); ++i)
			std::cout << (unsigned int)dhResponse[i] << " ";
		std::cout << std::endl;
		CHandshake hand(dhGroup, std::string((const char *)&dhResponse[0], dhResponse.size()));
		hand.Send(sender.get());
	}

	Protocol* Service::CreateProtocol(int typeId, Zeze::Serialize::ByteBuffer& os)
	{
		ProtocolFactoryMap::iterator it = ProtocolFactory.find(typeId);
		if (it != ProtocolFactory.end())
		{
			std::auto_ptr<Protocol> p(it->second.Factory());
			p->Decode(os);
			return p.release();
		}
		return NULL;
	}

	void Service::DispatchProtocol(Protocol* p)
	{
		std::auto_ptr<Protocol> at(p);

		ProtocolFactoryMap::iterator it = ProtocolFactory.find(p->TypeId());
		if (it != ProtocolFactory.end())
		{
			it->second.Handle(p);
		}
	}

	void Service::OnSocketProcessInputBuffer(const std::shared_ptr<Socket>& sender, Zeze::Serialize::ByteBuffer& input)
	{
		if (sender->IsHandshakeDone)
		{
			Helper.AppendInputBuffer(sender->SessionId, input);
			input.ReadIndex = input.WriteIndex;
		}
		else
		{
			Protocol::DecodeProtocol(this, sender, input);
		}
	}

	void Service::DispatchUnknownProtocol(const std::shared_ptr<Socket>& sender, int typeId, Zeze::Serialize::ByteBuffer& data)
	{
	}

	void Service::Connect(const std::string& host, int port, int timeoutSecondsPerConnect)
	{
		std::thread([this, host, port, timeoutSecondsPerConnect]
			{
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
					at->Close(NULL); // XXX 异常的时候需要手动释放Socket内部的shared_ptr。
				}
			}).detach();
	}

	Socket::Socket(Service* svr)
		: service(svr), This(this)
	{
		IsHandshakeDone = false;
		SessionId = NextSessionId();

		OutputBuffer.reset(new BufferedCodec());
		InputBuffer.reset(new BufferedCodec());
	}

	/// <summary>
	/// 下面使用系统socket-api实现真正的网络操作。尽量使用普通api平台相关。
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
		std::thread * worker;
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
				socket.first->Close(NULL);
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

				if (::select(maxfd + 1, &setread, &setwrite, NULL, &timeout) > 0)
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
			}
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
			int tcp1, tcp2;
			sockaddr_in name;
			memset(&name, 0, sizeof(name));
			name.sin_family = AF_INET;
			name.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
			int namelen = sizeof(name);
			tcp1 = tcp2 = -1;
			int tcp = socket(AF_INET, SOCK_STREAM, 0);
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
			fildes[0] = tcp1;
			fildes[1] = tcp2;
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

	Selector* Selector::Instance = NULL;

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
		Selector::Instance = NULL;

#ifdef LIMAX_OS_WINDOWS
		::WSACleanup();
#endif
	}

	void Socket::Close(std::exception* e)
	{
		service->OnSocketClose(This, e);
		Selector::Instance->Select(This, Selector::OpClose, 0);
		This.reset();
	}

	inline void platform_close_socket(int & so)
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
			this->Close(NULL);
			return;
		}

		if (InputCodec.get())
		{
			InputCodec->update((int8_t*)recvbuf.data, 0, rc);
			InputCodec->flush();
			Zeze::Serialize::ByteBuffer bb((char*)InputBuffer->buffer.data(), 0, InputBuffer->buffer.size());
			service->OnSocketProcessInputBuffer(This, bb);
			InputBuffer->buffer.erase(0, bb.ReadIndex);
		}
		else if (InputBuffer->buffer.size() > 0)
		{
			InputBuffer->buffer.append(recvbuf.data, rc);
			Zeze::Serialize::ByteBuffer bb((char*)InputBuffer->buffer.data(), 0, InputBuffer->buffer.size());
			service->OnSocketProcessInputBuffer(This, bb);
			InputBuffer->buffer.erase(0, bb.ReadIndex);
		}
		else
		{
			Zeze::Serialize::ByteBuffer bb(recvbuf.data, 0, rc);
			service->OnSocketProcessInputBuffer(This, bb);
			if (bb.Size() > 0)
				InputBuffer->buffer.append(bb.Bytes + bb.ReadIndex, bb.Size());
		}

		if (InputBuffer->buffer.empty())
			InputBuffer->buffer = std::string(); // XXX release memory if empty
	}

	void Socket::OnSend()
	{
		std::lock_guard<std::recursive_mutex> g(mutex);

		int rc = ::send(socket, OutputBuffer->buffer.data(), OutputBuffer->buffer.size(), 0);
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

		bool noPendingSend = OutputBuffer->buffer.empty();
		bool hasCodec = false;
		if (OutputCodec.get())
		{
			OutputCodec->update((int8_t*)data, offset, length);
			OutputCodec->flush();
			data = OutputBuffer->buffer.data();
			offset = 0;
			length = OutputBuffer->buffer.size();
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
			//std::cout << "ipv4 " << sizeof(in_addr) << std::endl;
			break;
		}
		case AF_INET6:
		{
			sockaddr_in6* sav6 = (sockaddr_in6*)ai->ai_addr;
			out.assign((const char*)&(sav6->sin6_addr), sizeof(in6_addr));
			//std::cout << "ipv6 " << sizeof(in6_addr) << std::endl;
			break;
		}
		}
	}

	bool Socket::Connect(const std::string& host, int _port, const std::string& lastSuccessAddress, int timeoutSecondsPerConnect)
	{
		struct addrinfo hints, * res;

		memset(&hints, 0, sizeof(hints));
		hints.ai_family = AF_UNSPEC;
		hints.ai_socktype = SOCK_STREAM;

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
		for (struct addrinfo* ai = res; ai != NULL; ai = ai->ai_next)
		{
			so = ::socket(ai->ai_family, ai->ai_socktype, ai->ai_protocol);
			if (so == 0)
				continue;

			// 设置异步模式
#ifdef LIMAX_OS_WINDOWS
			unsigned long ul = 1;
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
				if (::getnameinfo(ai->ai_addr, static_cast<socklen_t>(ai->ai_addrlen), addrName, sizeof(addrName), NULL, 0, NI_NUMERICHOST) == 0)
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
				ret = ::select(so + 1, NULL, &setw, NULL, &timeout);
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
						if (::getnameinfo(ai->ai_addr, static_cast<socklen_t>(ai->ai_addrlen), addrName, sizeof(addrName), NULL, 0, NI_NUMERICHOST) == 0)
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

} // namespace Net
} // namespace Zeze
