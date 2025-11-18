
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
				ToLua.SetSocketClose(sender->GetSessionId(), this);
				Service::OnSocketClose(sender, e);
			}

			virtual void OnHandshakeDone(const std::shared_ptr<Socket>& sender) override
			{
				Service::OnHandshakeDone(sender);
				ToLua.SetHandshakeDone(sender->GetSessionId(), this);
			}

			virtual void OnSocketProcessInputBuffer(const std::shared_ptr<Socket>& sender, Zeze::ByteBuffer& input) override
			{
				if (sender->IsHandshakeDone())
				{
					ToLua.AppendInputBuffer(sender->GetSessionId(), input);
					input.ReadIndex = input.WriteIndex;
				}
				else
				{
					Protocol::DecodeProtocol(this, sender, input);
				}
			}
		public:
			std::string Name;
			ToLuaService(const std::string & name) : Name(name)
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