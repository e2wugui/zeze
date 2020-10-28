
#include "Net.h"
#include "Protocol.h"
#include <iostream>

namespace Zeze
{
namespace Net
{
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
			std::cout << e->what() << std::endl;

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
			std::cout << e->what() << std::endl;
	}

	void Service::OnSocketConnected(const std::shared_ptr<Socket>& sender)
	{
		socket = sender;
		OnHandshakeDone(sender);
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
			try
			{
				Protocol::DecodeProtocol(this, sender, input);
			}
			catch (std::exception& ex)
			{
				sender->Close(&ex);
			}
		}
	}

	void Service::DispatchUnknownProtocol(const std::shared_ptr<Socket>& sender, int typeId, Zeze::Serialize::ByteBuffer& data)
	{
	}

	Socket::~Socket()
	{
	}

	void Socket::Close(std::exception* e)
	{
		service->OnSocketClose(std::shared_ptr<Socket>(this), e);
	}

	Socket* Service::Connect(const std::string& host, int port)
	{
		return NULL;
	}

	void Socket::Send(char* data, int offset, int length)
	{

	}

} // namespace Net
} // namespace Zeze
