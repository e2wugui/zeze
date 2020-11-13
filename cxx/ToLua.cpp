
#include "ToLua.h"
#include "Net.h"
#include "Protocol.h"

namespace Zeze
{
	namespace Net
	{
		int ToLua::ZezeSendProtocol(lua_State* luaState)
        {
            LuaHelper lua(luaState);
            Service* service = lua.ToObject<Service*>(-3);
            long long sessionId = lua.ToInteger(-2);
            std::shared_ptr<Socket> socket = service->GetSocket();
            if (NULL == socket.get())
                return 0;
            service->SendProtocol(socket.get());
            return 0;
        }

        int ToLua::ZezeUpdate(lua_State* luaState)
        {
            LuaHelper lua(luaState);
            Service* service = lua.ToObject<Service*>(-1);
            service->Update();
            return 0;
        }

        // connect(service, host, port, timeout)
        int ToLua::ZezeConnect(lua_State* luaState)
        {
            LuaHelper lua(luaState);
            Service* service = lua.ToObject<Service*>(-4);
            std::string host = lua.ToString(-3);
            int port = (int)lua.ToInteger(-2);
            bool autoReconnect = lua.ToBoolean(-1);
            service->SetAutoConnect(autoReconnect);
            service->Connect(host, port);
            return 0;
        }

        void ToLua::SendProtocol(Socket* socket)
        {
            if (false == Lua.IsTable(-1))
                throw std::exception("SendProtocol param is not a table.");

            Lua.GetField(-1, "TypeId");
            int typeId = (int)Lua.ToInteger(-1);
            Lua.Pop(1);
            Lua.GetField(-1, "ResultCode");
            int resultCode = (int)Lua.ToInteger(-1);
            Lua.Pop(1);

            ProtocolMetasMap::iterator pit = ProtocolMetas.find(typeId);
            if (pit == ProtocolMetas.end())
                throw std::exception("protocol not found in meta for typeid=" + typeId);
            long long argumentBeanTypeId = pit->second;

            // see Protocol.Encode
            Zeze::Serialize::ByteBuffer bb(1024);
            bb.WriteInt4(typeId);
            int outstate;
            bb.BeginWriteWithSize4(outstate);
            bb.WriteInt(resultCode);
            Lua.GetField(-1, "Argument");
            EncodeBean(bb, argumentBeanTypeId);
            Lua.Pop(1);
            bb.EndWriteWithSize4(outstate);
            socket->Send((const char *)bb.Bytes, bb.ReadIndex, bb.Size());
        }

        void ToLua::Update(Service* service)
        {
            ToLuaHandshakeDoneMap handshakeTmp;
            ToLuaSocketCloseMap socketCloseTmp;;
            ToLuaBufferMap inputTmp;
            {
                std::lock_guard<std::mutex> lock(mutex);
                handshakeTmp.swap(ToLuaHandshakeDone);
                inputTmp.swap(ToLuaBuffer);
                socketCloseTmp.swap(ToLuaSocketClose);
            }

            for (auto& e : socketCloseTmp)
            {
                CallSocketClose(e.second, e.first);
            }
            for (auto& e : handshakeTmp)
            {
                CallHandshakeDone(e.second, e.first);
            }

            for (auto& e : inputTmp)
            {
                std::shared_ptr<Socket> sender = service->GetSocket(e.first);
                if (NULL == sender.get())
                    continue;
                Zeze::Serialize::ByteBuffer bb((unsigned char *)e.second.data(), 0, e.second.size());
                Protocol::DecodeProtocol(service, sender, bb, this);
                e.second.erase(0, bb.ReadIndex);
            }

            {
                std::lock_guard<std::mutex> lock(mutex);
                for (auto & e : inputTmp)
                {
                    if (e.second.empty())
                        continue; // 数据全部处理完成。

                    ToLuaBufferMap::iterator bit = ToLuaBuffer.find(e.first);
                    if (bit != ToLuaBuffer.end())
                    {
                        // 处理过程中有新数据到来，加到当前剩余数据后面，然后覆盖掉buffer。
                        e.second.append(bit->second);
                        ToLuaBuffer[e.first] = e.second;
                    }
                    else
                    {
                        // 没有新数据到来，有剩余，加回去。下一次update再处理。
                        ToLuaBuffer[e.first] = e.second;
                    }
                }
            }
        }

        void ToLua::RegisterGlobalAndCallback(Service * service)
        {
            if (Lua.DoString("local Zeze = require 'Zeze'\nreturn Zeze"))
                throw std::exception("load Zeze.lua faild");
            if (false == Lua.IsTable(-1))
                throw std::exception("Zeze.lua not return a table");

            Lua.PushString(std::string("Service") + service->Name());
            Lua.PushObject(service);
            Lua.SetTable(-3);
            Lua.PushString("CurrentService");
            Lua.PushObject(service);
            Lua.SetTable(-3); // 当存在多个service时，这里保存最后一个。

            static bool registerCallback = false; // 简单保护一下。
            if (registerCallback == false)
            {
                registerCallback = true;
                Lua.Register("ZezeUpdate", ZezeUpdate);
                Lua.Register("ZezeSendProtocol", ZezeSendProtocol);
                Lua.Register("ZezeConnect", ZezeConnect);
            }
        }
    }
}