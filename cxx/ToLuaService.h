
#pragma once

#include "Net.h"
#include "ToLua.h"
#include "Protocol.h"

namespace Zeze
{
	namespace Net
	{
		class ToLuaService : public Service
		{
			ToLua ToLua;

			virtual void Update() override
			{
				ToLua.Update(this);
			}
			virtual void SendProtocol(Socket* so) override
			{
				ToLua.SendProtocol(so);
			}

			virtual void OnSocketClose(const std::shared_ptr<Socket>& sender, const std::exception* e) override
			{
				if (sender.get() == socket.get())
				{
					ToLua.SetSocketClose(sender->SessionId, this);
					socket.reset();
				}
				Service::OnSocketClose(sender, e);
			}

			virtual void OnHandshakeDone(const std::shared_ptr<Socket>& sender) override
			{
				Service::OnHandshakeDone(sender);
				ToLua.SetHandshakeDone(sender->SessionId, this);
			}

			virtual void OnSocketProcessInputBuffer(const std::shared_ptr<Socket>& sender, Zeze::Serialize::ByteBuffer& input) override
			{
				if (sender->IsHandshakeDone)
				{
					ToLua.AppendInputBuffer(sender->SessionId, input);
					input.ReadIndex = input.WriteIndex;
				}
				else
				{
					Protocol::DecodeProtocol(this, sender, input);
				}
			}
		public:
			ToLuaService(const std::string & name) : Service(name)
			{

			}

			void InitializeLua(lua_State* L)
			{
				ToLua.LoadMeta(L);
				ToLua.RegisterGlobalAndCallback(this);
			}
		};
	}
}