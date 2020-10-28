
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
            service->ToLua.SendProtocol(socket.get());
            return 0;
        }

        int ToLua::ZezeUpdate(lua_State* luaState)
        {
            LuaHelper lua(luaState);
            Service* service = lua.ToObject<Service*>(-1);
            service->Helper.Update(service, service->ToLua);
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
            socket->Send(bb.Bytes, bb.ReadIndex, bb.Size());
        }

        void Helper::Update(Service* service, ToLua& toLua)
        {
            ToLuaHandshakeDoneMap handshakeTmp;
            ToLuaBufferMap inputTmp;
            {
                std::lock_guard<std::mutex> lock(mutex);
                handshakeTmp.swap(ToLuaHandshakeDone);
                inputTmp.swap(ToLuaBuffer);
            }

            for (auto& e : handshakeTmp)
            {
                toLua.CallHandshakeDone(e.second, e.first);
            }

            for (auto& e : inputTmp)
            {
                std::shared_ptr<Socket> sender = service->GetSocket(e.first);
                if (NULL == sender.get())
                    continue;
                Zeze::Serialize::ByteBuffer bb((char *)e.second.data(), 0, e.second.size());
                Protocol::DecodeProtocol(service, sender, bb, &toLua);
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

            // 第一个参数是Service的全局变量。在上面一行注册进去的。
            // void ZezeUpdate(ZezeService##Name)
            Lua.Register("ZezeUpdate", ZezeUpdate);
            // 由 Protocol 的 lua 生成代码调用，其中 sesionId 从全局变量 ZezeCurrentSessionId 中读取，
            // 对于客户端，连接 HandshakeDone 以后保存 sessionId 到 ZezeCurrentSessionId 中，以后不用重新设置。
            // 对于服务器，连接 HandshakeDone 以后保存 sessionId 自己的结构中，发送前需要把当前连接设置到 ZezeCurrentSessionId 中。 
            // void ZezeSendProtocol(ZezeService##Name, sessionId, protocol)
            Lua.Register("ZezeSendProtocol", ZezeSendProtocol);
        }
    }
}