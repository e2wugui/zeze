
#pragma once

#include "LuaHelper.h"
#include "ByteBuffer.h"
#include <unordered_map>
#include <vector>
#include <mutex>

namespace Zeze
{
namespace Net
{
    class Service;
    class Socket;

	class ToLua
    {
        LuaHelper Lua;
    public:
        ToLua()
        {
        }

        ToLua(const ToLua&) = delete;
        ToLua& operator=(const ToLua&) = delete;

    private:
        class VariableMeta
        {
        public:
            int Id = 0;

            int Type = 0;
            long long TypeBeanTypeId = 0;
            int Key = 0;
            long long KeyBeanTypeId = 0;
            int Value = 0;
            long long ValueBeanTypeId = 0;

            VariableMeta()
            {
            }

            VariableMeta(int id, int type, long long typeBeanTypeId)
            {
                Id = id;
                Type = type;
                TypeBeanTypeId = typeBeanTypeId;
            }
        };

        typedef std::unordered_map<long long, std::vector<VariableMeta>> BeanMetasMap;
        typedef std::unordered_map<int, long long> ProtocolMetasMap;

        BeanMetasMap BeanMetas; // Bean.TypeId -> vars
        ProtocolMetasMap ProtocolMetas; // protocol.TypeId -> Bean.TypeId

    public:
        void LoadMeta(lua_State * L)
        {
            Lua.L = L;

            if (Lua.DoString("local Zeze = require 'Zeze'\nreturn Zeze"))
                throw std::exception("load  'Zeze.lua' Error.");

            BeanMetas.clear();
            ProtocolMetas.clear();

            if (Lua.DoString("local meta = require 'ZezeMeta'\nreturn meta"))
                throw std::exception("load ZezeMeta.lua error");
            if (false == Lua.IsTable(-1))
                throw std::exception("ZezeMeta not return a table");
            Lua.GetField(-1, "beans");
            Lua.PushNil();
            while (Lua.Next(-2)) // -1 value of vars(table) -2 key of bean.TypeId
            {
                long long beanTypeId = Lua.ToInteger(-2);
                std::vector<VariableMeta> & vars = BeanMetas[beanTypeId];
                Lua.PushNil();
                while (Lua.Next(-2)) // -1 value of varmeta(table) -2 key of varid
                {
                    VariableMeta var;
                    var.Id = (int)Lua.ToInteger(-2);
                    Lua.PushNil();
                    while (Lua.Next(-2)) // -1 value of typetag -2 key of index
                    {
                        switch (Lua.ToInteger(-2))
                        {
                            case 1: var.Type = (int)Lua.ToInteger(-1); break;
                            case 2: var.TypeBeanTypeId = Lua.ToInteger(-1); break;
                            case 3: var.Key = (int)Lua.ToInteger(-1); break;
                            case 4: var.KeyBeanTypeId = Lua.ToInteger(-1); break;
                            case 5: var.Value = (int)Lua.ToInteger(-1); break;
                            case 6: var.ValueBeanTypeId = Lua.ToInteger(-1); break;
                            default: throw std::exception("error index for typetag");
                        }
                        Lua.Pop(1); // pop value
                    }
                    Lua.Pop(1); // pop value
                    vars.push_back(var);
                }
                Lua.Pop(1); // pop value
            }
            Lua.Pop(1);

            Lua.GetField(-1, "protocols");
            Lua.PushNil();
            while (Lua.Next(-2)) // -1 value of Protocol.Argument.BeanTypeId -2 Protocol.TypeId
            {
                ProtocolMetas[(int)Lua.ToInteger(-2)] = Lua.ToInteger(-1);
                Lua.Pop(1); // pop value
            }
            Lua.Pop(1);
        }

        void CallSocketClose(Service* service, long long socketSessionId)
        {
            if (LuaHelper::LuaType::Function != Lua.GetGlobal("ZezeSocketClose")) // push func onto stack
            {
                Lua.Pop(1);
                return;
            }

            Lua.PushObject(service);
            Lua.PushInteger(socketSessionId);
            Lua.Call(2, 0);
        }

        void CallHandshakeDone(Service * service, long long socketSessionId)
        {
            if (LuaHelper::LuaType::Function != Lua.GetGlobal("ZezeHandshakeDone")) // push func onto stack
            {
                Lua.Pop(1);
                throw std::exception("ZezeHandshakeDone is not a function");
            }

            Lua.PushObject(service);
            Lua.PushInteger(socketSessionId);
            Lua.Call(2, 0);
        }

        static int ZezeSendProtocol(lua_State* luaState);
        static int ZezeUpdate(lua_State* luaState);
        static int ZezeConnect(lua_State* luaState);

        void RegisterGlobalAndCallback(Service* service);
        void SendProtocol(Socket * socket);

        void EncodeBean(Zeze::Serialize::ByteBuffer & bb, long long beanTypeId)
        {
            BeanMetasMap::iterator bit = BeanMetas.find(beanTypeId);
            if (bit == BeanMetas.end())
                throw std::exception("bean not found in meta for beanTypeId=" + beanTypeId);
            if (false == Lua.IsTable(-1))
                throw std::exception("encodebean need a table");

            std::vector<VariableMeta>& vars = bit->second;
            // 先遍历一遍，得到填写了的var的数量
            int varsCount = 0;
            for (auto & v : vars)
            {
                Lua.PushInteger(v.Id);
                Lua.GetTable(-2);
                if (false == Lua.IsNil(-1))
                    ++varsCount;
                Lua.Pop(1);
            }
            bb.WriteInt(varsCount);

            for (auto & v : vars)
            {
                Lua.PushInteger(v.Id);
                Lua.GetTable(-2);
                if (Lua.IsNil(-1)) // allow var not set
                {
                    Lua.Pop(1);
                    continue;
                }
                EncodeVariable(bb, v);
                Lua.Pop(1);
            }
        }

        int EncodeGetTableLength()
        {
            if (false == Lua.IsTable(-1))
                throw std::exception("EncodeGetTableLength: not a table");
            int len = 0;
            Lua.PushNil();
            while (Lua.Next(-2))
            {
                ++len;
                Lua.Pop(1);
            }
            return len;
        }

        void EncodeVariable(Zeze::Serialize::ByteBuffer & _os_, const VariableMeta & v, int index = -1)
        {
            if (v.Id > 0) // 编码容器中项时，Id为0，此时不需要编码 tagid.
                _os_.WriteInt(v.Type | v.Id << Zeze::Serialize::Helper::TAG_SHIFT);

            switch (v.Type)
            {
            case Zeze::Serialize::Helper::BOOL:
                _os_.WriteBool(Lua.ToBoolean(index));
                break;
            case Zeze::Serialize::Helper::BYTE:
                _os_.WriteByte((char)Lua.ToInteger(index));
                break;
            case Zeze::Serialize::Helper::SHORT:
                _os_.WriteShort((short)Lua.ToInteger(index));
                break;
            case Zeze::Serialize::Helper::INT:
                _os_.WriteInt((int)Lua.ToInteger(index));
                break;
            case Zeze::Serialize::Helper::LONG:
                _os_.WriteLong(Lua.ToInteger(index));
                break;
            case Zeze::Serialize::Helper::FLOAT:
                _os_.WriteFloat((float)Lua.ToNumber(index));
                break;
            case Zeze::Serialize::Helper::DOUBLE:
                _os_.WriteDouble(Lua.ToNumber(index));
                break;
            case Zeze::Serialize::Helper::STRING:
                _os_.WriteString(Lua.ToString(index));
                break;
            case Zeze::Serialize::Helper::BYTES:
                _os_.WriteBytes(Lua.ToBuffer(index));
                break;
            case Zeze::Serialize::Helper::LIST:
            {
                if (false == Lua.IsTable(-1))
                    throw std::exception("list must be a table");
                if (v.Id <= 0)
                    throw std::exception("list cannot define in collection");
                int outstate;
                _os_.BeginWriteSegment(outstate);
                _os_.WriteInt(v.Value);
                _os_.WriteInt(EncodeGetTableLength());
                Lua.PushNil();
                while (Lua.Next(-2))
                {
                    EncodeVariable(_os_, VariableMeta(0, v.Value, v.ValueBeanTypeId));
                    Lua.Pop(1);
                }
                _os_.EndWriteSegment(outstate);
                break;
            }
            case Zeze::Serialize::Helper::SET:
            {
                if (false == Lua.IsTable(-1))
                    throw std::exception("set must be a table");
                if (v.Id <= 0)
                    throw std::exception("set cannot define in collection");
                int outstate;
                _os_.BeginWriteSegment(outstate);
                _os_.WriteInt(v.Value);
                _os_.WriteInt(EncodeGetTableLength());
                Lua.PushNil();
                while (Lua.Next(-2))
                {
                    Lua.Pop(1); // set：encode key
                    EncodeVariable(_os_, VariableMeta(0, v.Value, v.ValueBeanTypeId));
                }
                _os_.EndWriteSegment(outstate);
                break;
            }
            case Zeze::Serialize::Helper::MAP:
            {
                if (false == Lua.IsTable(-1))
                    throw std::exception("map must be a table");
                if (v.Id <= 0)
                    throw std::exception("map cannot define in collection");
                int outstate;
                _os_.BeginWriteSegment(outstate);
                _os_.WriteInt(v.Key);
                _os_.WriteInt(v.Value);
                _os_.WriteInt(EncodeGetTableLength());
                Lua.PushNil();
                while (Lua.Next(-2))
                {
                    EncodeVariable(_os_, VariableMeta(0, v.Key, v.KeyBeanTypeId), -2);
                    EncodeVariable(_os_, VariableMeta(0, v.Value, v.ValueBeanTypeId), -1);
                    Lua.Pop(1);
                }
                _os_.EndWriteSegment(outstate);
                break;
            }
            case Zeze::Serialize::Helper::BEAN:
            {
                if (v.Id > 0)
                {
                    int outstate;
                    _os_.BeginWriteSegment(outstate);
                    EncodeBean(_os_, v.TypeBeanTypeId);
                    _os_.EndWriteSegment(outstate);
                }
                else
                {
                    // in collection. direct encode
                    EncodeBean(_os_, v.TypeBeanTypeId);
                }
                break;
            }
            case Zeze::Serialize::Helper::DYNAMIC:
            {
                if (v.Id <= 0)
                    throw std::exception("dynamic cannot define in collection");
                Lua.GetField(-1, "_TypeId_");
                if (Lua.IsNil(-1))
                    throw std::exception("'_TypeId_' not found. dynamic bean needed.");
                long long beanTypeId = Lua.ToInteger(-1);
                Lua.Pop(1);
                _os_.WriteLong8(beanTypeId);
                int outstate;
                _os_.BeginWriteSegment(outstate);
                EncodeBean(_os_, beanTypeId);
                _os_.EndWriteSegment(outstate);
                break;
            }
            default:
                    throw std::exception("Unkown Tag Type");
            }
        }
    public:
        bool DecodeAndDispatch(Service * service, long long sessionId, int typeId, Zeze::Serialize::ByteBuffer & _os_)
        {
            if (LuaHelper::LuaType::Function != Lua.GetGlobal("ZezeDispatchProtocol")) // push func onto stack
            {
                Lua.Pop(1);
                return false;
            }
            // 现在不支持 Rpc.但是代码没有检查。
            // 生成的时候报错。
            Lua.CreateTable(0, 8);

            Lua.PushString("Service");
            Lua.PushObject(service);
            Lua.SetTable(-3);

            Lua.PushString("SessionId");
            Lua.PushInteger(sessionId);
            Lua.SetTable(-3);

            Lua.PushString("ModuleId");
            Lua.PushInteger((typeId >> 16) & 0xffff);
            Lua.SetTable(-3);

            Lua.PushString("ProtcolId");
            Lua.PushInteger(typeId & 0xffff);
            Lua.SetTable(-3);

            Lua.PushString("TypeId");
            Lua.PushInteger(typeId);
            Lua.SetTable(-3);

            Lua.PushString("ResultCode");
            Lua.PushInteger(_os_.ReadInt());
            Lua.SetTable(-3);

            Lua.PushString("Argument");
            DecodeBean(_os_);
            Lua.SetTable(-3);

            Lua.Call(1, 1);
            bool result = false;
            if (false == Lua.IsNil(-1))
                result = Lua.ToBoolean(-1);
            Lua.Pop(1);
            return result;
        }

    private:
        void DecodeBean(Zeze::Serialize::ByteBuffer & _os_)
        {
            Lua.CreateTable(0, 32);
            for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_)
            {
                int _tagid_ = _os_.ReadInt();
                int _varid_ = (_tagid_ >> Zeze::Serialize::Helper::TAG_SHIFT) & Zeze::Serialize::Helper::ID_MASK;
                int _tagType_ = _tagid_ & Zeze::Serialize::Helper::TAG_MASK;
                Lua.PushInteger(_varid_);
                DecodeVariable(_os_, _tagType_);
                Lua.SetTable(-3);
            }
        }

        void DecodeVariable(Zeze::Serialize::ByteBuffer & _os_, int _tagType_, bool inCollection = false)
        {
            switch (_tagType_)
            {
            case Zeze::Serialize::Helper::BOOL:
                Lua.PushBoolean(_os_.ReadBool());
                break;
            case Zeze::Serialize::Helper::BYTE:
                Lua.PushInteger(_os_.ReadByte());
                break;
            case Zeze::Serialize::Helper::SHORT:
                Lua.PushInteger(_os_.ReadShort());
                break;
            case Zeze::Serialize::Helper::INT:
                Lua.PushInteger(_os_.ReadInt());
                break;
            case Zeze::Serialize::Helper::LONG:
                Lua.PushInteger(_os_.ReadLong());
                break;
            case Zeze::Serialize::Helper::FLOAT:
                Lua.PushNumber(_os_.ReadFloat());
                break;
            case Zeze::Serialize::Helper::DOUBLE:
                Lua.PushNumber(_os_.ReadDouble());
                break;
            case Zeze::Serialize::Helper::STRING:
            {
                const char* outstr;
                int outlen;
                _os_.ReadStringNoCopy(outstr, outlen);
                Lua.PushString(outstr, outlen);
                break;
            }
            case Zeze::Serialize::Helper::BYTES:
            {
                const char* outstr;
                int outlen;
                _os_.ReadStringNoCopy(outstr, outlen);
                Lua.PushBuffer(outstr, outlen);
                break;
            }
            case Zeze::Serialize::Helper::LIST:
            {
                int outstate;
                _os_.BeginReadSegment(outstate);
                int _valueTagType_ = _os_.ReadInt();
                Lua.CreateTable(128, 128); // 不知道用哪个参数。
                int i = 1; // 从1开始？
                for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_)
                {
                    Lua.PushInteger(i);
                    DecodeVariable(_os_, _valueTagType_, true);
                    Lua.SetTable(-3);
                    ++i;
                }
                _os_.EndReadSegment(outstate);
                break;
            }
            case Zeze::Serialize::Helper::SET:
            {
                int outstate;
                _os_.BeginReadSegment(outstate);
                int _valueTagType_ = _os_.ReadInt();
                Lua.CreateTable(128, 128); // 不知道用哪个参数。
                int i = 1;
                for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_)
                {
                    DecodeVariable(_os_, _valueTagType_, true);
                    Lua.PushInteger(0);
                    Lua.SetTable(-3);
                    ++i;
                }
                _os_.EndReadSegment(outstate);
                break;
            }
            case Zeze::Serialize::Helper::MAP:
            {
                int outstate;
                _os_.BeginReadSegment(outstate);
                int _keyTagType_ = _os_.ReadInt();
                int _valueTagType_ = _os_.ReadInt();
                Lua.CreateTable(128, 0);
                for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_)
                {
                    DecodeVariable(_os_, _keyTagType_, true);
                    DecodeVariable(_os_, _valueTagType_, true);
                    Lua.SetTable(-3);
                }
                _os_.EndReadSegment(outstate);
                break;
            }
            case Zeze::Serialize::Helper::BEAN:
            {
                if (inCollection)
                {
                    DecodeBean(_os_);
                }
                else
                {
                    int outstate;
                    _os_.BeginReadSegment(outstate);
                    DecodeBean(_os_);
                    _os_.EndReadSegment(outstate);
                }
                break;
            }
            case Zeze::Serialize::Helper::DYNAMIC:
            {
                long long beanTypeId = _os_.ReadLong8();
                if (beanTypeId == 0)
                {
                    // 这个EmptyBean完全没有实现Encode,Decode，没有遵守Bean的系列化协议，所以需要特殊处理一下。
                    int outstate;
                    _os_.BeginReadSegment(outstate);
                    _os_.EndReadSegment(outstate);
                    Lua.CreateTable(0, 0);
                }
                else
                {
                    int outstate;
                    _os_.BeginReadSegment(outstate);
                    DecodeBean(_os_);
                    _os_.EndReadSegment(outstate);
                }
                // 动态bean额外把TypeId加到变量里面。总是使用varid==0表示。程序可以使用这个动态判断是哪个具体的bean。
                Lua.PushInteger(0);
                Lua.PushInteger(beanTypeId);
                Lua.SetTable(-3);
                break;
            }
            default:
                    throw std::exception("Unkown Tag Type");
            }
        }

    private:
        typedef std::unordered_map<long long, std::string> ToLuaBufferMap;
        typedef std::unordered_map<long long, Service*> ToLuaHandshakeDoneMap;
        typedef std::unordered_map<long long, Service*> ToLuaSocketCloseMap;
        ToLuaBufferMap ToLuaBuffer;
        ToLuaHandshakeDoneMap ToLuaHandshakeDone;
        ToLuaSocketCloseMap ToLuaSocketClose;
        std::mutex mutex;

    public:
        void SetHandshakeDone(long long socketSessionId, Service* service)
        {
            std::lock_guard<std::mutex> lock(mutex);
            ToLuaHandshakeDone[socketSessionId] = service;
        }

        void SetSocketClose(long long socketSessionId, Service* service)
        {
            std::lock_guard<std::mutex> lock(mutex);
            ToLuaSocketClose[socketSessionId] = service;
        }

        void AppendInputBuffer(long long socketSessionId, Zeze::Serialize::ByteBuffer& buffer)
        {
            std::lock_guard<std::mutex> lock(mutex);
            ToLuaBuffer[socketSessionId].append((const char*)(buffer.Bytes + buffer.ReadIndex), buffer.Size());
        }

        void Update(Service* service);
    };

} // namespace Net
} // namespace Zeze
