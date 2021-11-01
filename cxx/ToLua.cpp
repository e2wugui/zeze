
#include "ToLua.h"
#include "Net.h"
#include "Protocol.h"
#include "ToLuaService.h"

namespace Zeze
{
	namespace Net
	{
		int ToLua::ZezeSendProtocol(lua_State* luaState)
        {
            LuaHelper lua(luaState);
            Service* service = lua.ToObject<Service*>(-3);
            //long long sessionId = lua.ToInteger(-2); // 只有一个连接，先不使用这个参数，保留。
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

            Lua.GetField(-1, "ModuleId");
            int ModuleId = (int)Lua.ToInteger(-1);
            Lua.Pop(1);
            Lua.GetField(-1, "ProtocolId");
            int ProtocolId = (int)Lua.ToInteger(-1);
            Lua.Pop(1);

            Lua.GetField(-1, "ResultCode");
            int resultCode = (int)Lua.ToInteger(-1);
            Lua.Pop(1);

			long typeId = (long long)ModuleId << 32 | (ProtocolId & 0xffffffff);
            ProtocolMetasMap::iterator pit = ProtocolMetas.find(typeId);
            if (pit == ProtocolMetas.end())
                throw std::exception("protocol not found in meta for typeid=" + typeId);

            if (pit->second.IsRpc)
            {
                Lua.GetField(-1, "IsRequest");
                bool isRequest = Lua.ToBoolean(-1);
                Lua.Pop(1);
                Lua.GetField(-1, "Sid");
                long long sid = Lua.ToInteger(-1);
                Lua.Pop(1);
                Lua.GetField(-1, "Timeout");
                int timeout = (int)Lua.ToInteger(-1);
                Lua.Pop(1);

                long long argumentBeanTypeId = 0;
                const char* argumentName = NULL;

                if (isRequest)
                {
                    argumentBeanTypeId = pit->second.ArgumentBeanTypeId;
                    argumentName = "Argument";
                }
                else
                {
                    argumentBeanTypeId = pit->second.ResultBeanTypeId;
                    argumentName = "Result";
                }

                // see Rpc.Encode
                Zeze::Serialize::ByteBuffer bb(1024);
                bb.WriteInt4(moduleId);
				bb.WriteInt4(protocolId);
                int outstate;
                bb.BeginWriteWithSize4(outstate);
                bb.WriteBool(isRequest);
                bb.WriteLong(sid);
                bb.WriteInt(resultCode);
                Lua.GetField(-1, argumentName);
                EncodeBean(bb, argumentBeanTypeId);
                Lua.Pop(1);
                bb.EndWriteWithSize4(outstate);
                socket->Send((const char*)bb.Bytes, bb.ReadIndex, bb.Size());

                if (timeout > 0)
                {
                    Zeze::Net::SetTimeout([this, sid]() { return SetRpcTimeout(sid); }, 5);
                }
            }
            else
            {
                // see Protocol.Encode
                Zeze::Serialize::ByteBuffer bb(1024);
                bb.WriteInt4(moduleId);
				bb.WriteInt4(protocolId);
                int outstate;
                bb.BeginWriteWithSize4(outstate);
                bb.WriteInt(resultCode);
                Lua.GetField(-1, "Argument");
                EncodeBean(bb, pit->second.ArgumentBeanTypeId);
                Lua.Pop(1);
                bb.EndWriteWithSize4(outstate);
                socket->Send((const char*)bb.Bytes, bb.ReadIndex, bb.Size());
            }
        }

        void ToLua::Update(Service* service)
        {
            ToLuaHandshakeDoneMap handshakeTmp;
            ToLuaSocketCloseMap socketCloseTmp;;
            std::unordered_set<long long> rpcTimeoutTmp;
            ToLuaBufferMap inputTmp;
            {
                std::lock_guard<std::mutex> lock(mutex);
                handshakeTmp.swap(ToLuaHandshakeDone);
                inputTmp.swap(ToLuaBuffer);
                socketCloseTmp.swap(ToLuaSocketClose);
                rpcTimeoutTmp.swap(ToLuaRpcTimeout);
            }
            for (auto& sid : rpcTimeoutTmp)
            {
                CallRpcTimeout(sid);
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

        void ToLua::RegisterGlobalAndCallback(ToLuaService* service)
        {
            if (Lua.DoString("local Zeze = require 'Zeze'\nreturn Zeze"))
                throw std::exception("load Zeze.lua faild");
            if (false == Lua.IsTable(-1))
                throw std::exception("Zeze.lua not return a table");

            Lua.PushString(std::string("Service") + service->Name);
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

        void ToLua::CallRpcTimeout(long long sid)
        {
            if (LuaHelper::LuaType::Function != Lua.GetGlobal("ZezeDispatchProtocol")) // push func onto stack
            {
                Lua.Pop(1);
                return;
            }
            // see Zeze.lua ：ZezeDispatchProtocol。这里仅设置必要参数。
            Lua.CreateTable(0, 16);

            Lua.PushString("IsRpc");
            Lua.PushBoolean(true);
            Lua.SetTable(-3);

            Lua.PushString("IsRequest");
            Lua.PushBoolean(false);
            Lua.SetTable(-3);

            Lua.PushString("Sid");
            Lua.PushInteger(sid);
            Lua.SetTable(-3);

            Lua.PushString("IsTimeout");
            Lua.PushBoolean(true);
            Lua.SetTable(-3);

            Lua.Call(1, 1);
            Lua.Pop(1);
        }

        bool ToLua::DecodeAndDispatch(Service* service, long long sessionId, int moduleId, int protocolId, Zeze::Serialize::ByteBuffer& _os_)
        {
            if (LuaHelper::LuaType::Function != Lua.GetGlobal("ZezeDispatchProtocol")) // push func onto stack
            {
                Lua.Pop(1);
                return false;
            }
            // 现在不支持 Rpc.但是代码没有检查。
            // 生成的时候报错。
            Lua.CreateTable(0, 16);

            Lua.PushString("Service");
            Lua.PushObject(service);
            Lua.SetTable(-3);

            Lua.PushString("SessionId");
            Lua.PushInteger(sessionId);
            Lua.SetTable(-3);

            Lua.PushString("ModuleId");
            Lua.PushInteger(moduleId);
            Lua.SetTable(-3);

            Lua.PushString("ProtcolId");
            Lua.PushInteger(protocolId);
            Lua.SetTable(-3);

			long long typeId = (long long)moduleId << 32 | (protocolId & 0xffffffff);
            Lua.PushString("TypeId");
            Lua.PushInteger(typeId);
            Lua.SetTable(-3);

            ProtocolMetasMap::iterator pit = ProtocolMetas.find(typeId);
            if (pit == ProtocolMetas.end())
                throw std::exception("protocol not found in meta for typeid=" + typeId);

            if (pit->second.IsRpc)
            {
                bool IsRequest = _os_.ReadBool();
                long long sid = _os_.ReadLong();
                int resultCode = _os_.ReadInt();
                const char * argument;
                if (IsRequest)
                {
                    argument = "Argument";
                }
                else
                {
                    argument = "Result";
                }
                Lua.PushString("IsRpc");
                Lua.PushBoolean(true);
                Lua.SetTable(-3);
                Lua.PushString("IsRequest");
                Lua.PushBoolean(IsRequest);
                Lua.SetTable(-3);
                Lua.PushString("Sid");
                Lua.PushInteger(sid);
                Lua.SetTable(-3);
                Lua.PushString("ResultCode");
                Lua.PushInteger(resultCode);
                Lua.SetTable(-3);
                Lua.PushString(argument);
                DecodeBean(_os_);
                Lua.SetTable(-3);
            }
            else
            {
                Lua.PushString("ResultCode");
                Lua.PushInteger(_os_.ReadInt());
                Lua.SetTable(-3);

                Lua.PushString("Argument");
                DecodeBean(_os_);
                Lua.SetTable(-3);
            }

            Lua.Call(1, 1);
            bool result = false;
            if (false == Lua.IsNil(-1))
                result = Lua.ToBoolean(-1);
            Lua.Pop(1);
            return result;
        }
    }
}