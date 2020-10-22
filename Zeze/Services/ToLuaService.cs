
#define USE_KERA_LUA

using System;
using System.Collections.Generic;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Text;
using Zeze.Net;
using Zeze.Serialize;

/// <summary>
/// 在lua线程中调用，一般实现：
/// 0 创建lua线程，
/// 1 创建 ToLua 实例。
/// 2 调用具体 Service.InitializeLua 初始化
/// 3 调用lua.main进入lua代码
/// 4 在lua.main中回调每个 Service.InitializeLua 中注册的方法回调
/// * lua 热更的话需要建议重新创建 ToLua ，并且重新初始化（InitializeLua）。重用 ToLua 的话，需要调用一次 ToLua.LoadMeta();
/// </summary>
namespace Zeze.Services
{
    interface FromLua
    {
        public string Name { get; } // Service Name
        public Net.Service Service { get; }

        public ToLuaService.ToLua ToLua { get; }
        public ToLuaService.Helper Helper { get; }
    }

    public class ToLuaServiceClient : HandshakeClient, FromLua
    {
        public ToLuaService.ToLua ToLua { get; private set; }
        public ToLuaService.Helper Helper { get; } = new ToLuaService.Helper();

        public ToLuaServiceClient(string name, Zeze.Application zeze) : base(name, zeze)
        {
        }

        public Net.Service Service => this;

        public void InitializeLua(ToLuaService.ToLua toLua)
        {
            ToLua = toLua;
            toLua.RegisterGlobalAndCallback(this);
        }

        public override void OnHandshakeDone(AsyncSocket sender)
        {
            Helper.SetHandshakeDone(sender.SessionId, this);
        }

        public override void OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input)
        {
            Helper.AppendInputBuffer(so.SessionId, input);
            input.ReadIndex = input.WriteIndex;
        }
    }

    // 完全 ToLuaServiceClient，由于 c# 无法写 class S<T> : T where T : Net.Service，复制一份.
    public class ToLuaServiceServer : HandshakeServer, FromLua
    {
        public ToLuaService.ToLua ToLua { get; private set; }
        public ToLuaService.Helper Helper { get; } = new ToLuaService.Helper();
        public Net.Service Service => this;

        public ToLuaServiceServer(string name, Zeze.Application zeze) : base(name, zeze)
        {
        }

        public void InitializeLua(ToLuaService.ToLua toLua)
        {
            ToLua = toLua;
            toLua.RegisterGlobalAndCallback(this);
        }

        public override void OnHandshakeDone(AsyncSocket sender)
        {
            Helper.SetHandshakeDone(sender.SessionId, this);
        }

        public override void OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input)
        {
            Helper.AppendInputBuffer(so.SessionId, input);
            input.ReadIndex = input.WriteIndex;
        }
    }
}

namespace Zeze.Services.ToLuaService
{
    public class ToLua
    {
#if USE_KERA_LUA
        public KeraLua.Lua Lua { get; }

        public ToLua(KeraLua.Lua lua)
        {
            this.Lua = lua;
            if (this.Lua.DoString("require  'Zeze'"))
                throw new Exception("require  'Zeze' Error.");
            LoadMeta();
        }

        class VariableMeta
        {
            public int Id { get; set; }

            public int Type { get; set; }
            public long TypeBeanTypeId { get; set; }
            public int Key { get; set; }
            public long KeyBeanTypeId { get; set; }
            public int Value { get; set; }
            public long ValueBeanTypeId { get; set; }

            public override string ToString()
            {
                return $"{{[{Id}]={{{Type},{TypeBeanTypeId},{Key},{KeyBeanTypeId},{Value},{ValueBeanTypeId}}}}}";
            }
        }

        private readonly Dictionary<long, List<VariableMeta>> BeanMetas = new Dictionary<long, List<VariableMeta>>(); // Bean.TypeId -> vars
        private readonly Dictionary<int, long> ProtocolMetas = new Dictionary<int, long>(); // protocol.TypeId -> Bean.TypeId

        public void LoadMeta()
        {
            BeanMetas.Clear();
            ProtocolMetas.Clear();

            Lua.DoFile("ZezeMeta.lua");

            Lua.GetField(-1, "beans");
            Lua.PushNil();
            while (Lua.Next(-2)) // -1 value of vars(table) -2 key of bean.TypeId
            {
                long beanTypeId = Lua.ToInteger(-2);
                List<VariableMeta> vars = new List<VariableMeta>();
                Lua.PushNil();
                while (Lua.Next(-2)) // -1 value of varmeta(table) -2 key of varid
                {
                    VariableMeta var = new VariableMeta();
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
                            default: throw new Exception("error index for typetag");
                        }
                        Lua.Pop(1); // pop value
                    }
                    Lua.Pop(1); // pop value
                    vars.Add(var);
                }
                BeanMetas.Add(beanTypeId, vars);
                Lua.Pop(1); // pop value
            }
            Lua.Pop(1);

            Lua.GetField(-1, "protocols");
            Lua.PushNil();
            while (Lua.Next(-2)) // -1 value of Protocol.Argument.BeanTypeId -2 Protocol.TypeId
            {
                ProtocolMetas.Add((int)Lua.ToInteger(-2), Lua.ToInteger(-1));
                Lua.Pop(1); // pop value
            }
            Lua.Pop(1);
        }

        public void RegisterFunction(string name, KeraLua.LuaFunction func)
        {
            Lua.Register(name, func);
        }

        internal void CallHandshakeDone(FromLua service, long socketSessionId)
        {
            // void OnHandshakeDone(long sessionId)
            this.Lua.GetGlobal("ZezeHandshakeDone"); // push func onto stack
            Lua.PushObject(service);
            Lua.PushInteger(socketSessionId);
            Lua.Call(2, 0);
        }

        private static int ZezeSendProtocol(IntPtr luaState)
        {
            KeraLua.Lua lua = KeraLua.Lua.FromIntPtr(luaState);
            FromLua callback = lua.ToObject<FromLua>(-3);
            long sessionId = lua.ToInteger(-2);
            AsyncSocket socket = callback.Service.GetSocket(sessionId);
            if (null == socket)
                return 0;
            callback.ToLua.SendProtocol(socket);
            return 0;
        }

        private static int ZezeUpdate(IntPtr luaState)
        {
            KeraLua.Lua lua = KeraLua.Lua.FromIntPtr(luaState);
            FromLua callback = lua.ToObject<FromLua>(-1);
            callback.Helper.Update(callback.Service, callback.ToLua);
            return 0;
        }

        internal void RegisterGlobalAndCallback(FromLua callback)
        {
            Lua.PushObject(callback);
            Lua.SetGlobal("ZezeService" + callback.Name);
            Lua.PushObject(callback);
            Lua.SetGlobal("ZezeCurrentService"); // 当存在多个service时，这里保存最后一个。

            // 第一个参数是Service的全局变量。在上面一行注册进去的。
            // void ZezeUpdate(ZezeService##Name)
            RegisterFunction("ZezeUpdate", ZezeUpdate);
            // 由 Protocol 的 lua 生成代码调用，其中 sesionId 从全局变量 ZezeCurrentSessionId 中读取，
            // 对于客户端，连接 HandshakeDone 以后保存 sessionId 到 ZezeCurrentSessionId 中，以后不用重新设置。
            // 对于服务器，连接 HandshakeDone 以后保存 sessionId 自己的结构中，发送前需要把当前连接设置到 ZezeCurrentSessionId 中。 
            // void ZezeSendProtocol(ZezeService##Name, sessionId, protocol)
            RegisterFunction("ZezeSendProtocol", ZezeSendProtocol);
        }

        public void SendProtocol(AsyncSocket socket)
        {
            if (false == Lua.IsTable(-1))
                throw new Exception("SendProtocol param is not a table.");

            Lua.GetField(-1, "TypeId");
            int typeId = (int)Lua.ToInteger(-1);
            Lua.Pop(1);
            Lua.GetField(-1, "ResultCode");
            int resultCode = (int)Lua.ToInteger(-1);
            Lua.Pop(1);

            if (false == ProtocolMetas.TryGetValue(typeId, out var argumentBeanTypeId))
                throw new Exception("protocol not found in meta for typeid=" + typeId);

            // see Protocol.Encode
            ByteBuffer bb = ByteBuffer.Allocate();
            bb.WriteInt(typeId);
            bb.BeginWriteWithSize4(out var state);
            bb.WriteInt(resultCode);
            Lua.GetField(-1, "Argument");
            EncodeBean(bb, argumentBeanTypeId);
            Lua.Pop(1);
            bb.EndWriteWithSize4(state);
            socket.Send(bb);            
        }

        private void EncodeBean(ByteBuffer bb, long beanTypeId)
        {
            if (false == BeanMetas.TryGetValue(beanTypeId, out var vars))
                throw new Exception("bean not found in meta for beanTypeId=" + beanTypeId);

            bb.WriteInt(vars.Count);
            foreach (var v in vars)
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

        private int EncodeGetTableLength()
        {
            if (false == Lua.IsTable(-1))
                throw new Exception("EncodeGetTableLength: not a table");
            int len = 0;
            Lua.PushNil();
            while (Lua.Next(-2))
            {
                ++len;
                Lua.Pop(1);
            }
            return len;
        }

        private void EncodeVariable(ByteBuffer _os_, VariableMeta v, int index = -1)
        {
            if (v.Id > 0) // 编码容器中项时，Id为0，此时不需要编码 tagid.
                _os_.WriteInt(v.Type | v.Id << Zeze.Serialize.Helper.TAG_SHIFT);

            switch (v.Type)
            {
                case Zeze.Serialize.Helper.BOOL:
                    _os_.WriteBool(Lua.ToBoolean(index));
                    break;
                case Zeze.Serialize.Helper.BYTE:
                    _os_.WriteByte((byte)Lua.ToInteger(index));
                    break;
                case Zeze.Serialize.Helper.SHORT:
                    _os_.WriteShort((short)Lua.ToInteger(index));
                    break;
                case Zeze.Serialize.Helper.INT:
                    _os_.WriteInt((int)Lua.ToInteger(index));
                    break;
                case Zeze.Serialize.Helper.LONG:
                    _os_.WriteLong(Lua.ToInteger(index));
                    break;
                case Zeze.Serialize.Helper.FLOAT:
                    _os_.WriteFloat((float)Lua.ToNumber(index));
                    break;
                case Zeze.Serialize.Helper.DOUBLE:
                    _os_.WriteDouble(Lua.ToNumber(index));
                    break;
                case Zeze.Serialize.Helper.STRING:
                    _os_.WriteString(Lua.ToString(index));
                    break;
                case Zeze.Serialize.Helper.BYTES:
                    _os_.WriteBytes(Lua.ToBuffer(index));
                    break;
                case Zeze.Serialize.Helper.LIST:
                    {
                        _os_.BeginWriteSegment(out var _state_);
                        _os_.WriteInt(v.Value);
                        _os_.WriteInt(EncodeGetTableLength());
                        Lua.PushNil();
                        while (Lua.Next(-2))
                        { 
                            EncodeVariable(_os_, new VariableMeta() { Id = 0, Type = v.Value, TypeBeanTypeId = v.ValueBeanTypeId });
                            Lua.Pop(1);
                        }
                        _os_.EndWriteSegment(_state_);
                    }
                    break;
                case Zeze.Serialize.Helper.SET:
                    {
                        _os_.BeginWriteSegment(out var _state_);
                        _os_.WriteInt(v.Value);
                        _os_.WriteInt(EncodeGetTableLength());
                        Lua.PushNil();
                        while (Lua.Next(-2))
                        {
                            Lua.Pop(1); // set：encode key
                            EncodeVariable(_os_, new VariableMeta() { Id = 0, Type = v.Value, TypeBeanTypeId = v.ValueBeanTypeId });
                        }
                        _os_.EndWriteSegment(_state_);
                    }
                    break;
                case Zeze.Serialize.Helper.MAP:
                    {
                        _os_.BeginWriteSegment(out var _state_);
                        _os_.WriteInt(v.Key);
                        _os_.WriteInt(v.Value);
                        _os_.WriteInt(EncodeGetTableLength());
                        Lua.PushNil();
                        while (Lua.Next(-2))
                        {
                            EncodeVariable(_os_, new VariableMeta() { Id = 0, Type = v.Key, TypeBeanTypeId = v.KeyBeanTypeId }, -2);
                            EncodeVariable(_os_, new VariableMeta() { Id = 0, Type = v.Value, TypeBeanTypeId = v.ValueBeanTypeId });
                            Lua.Pop(1);
                        }
                        _os_.EndWriteSegment(_state_);
                    }
                    break;
                case Zeze.Serialize.Helper.BEAN:
                    {
                        _os_.BeginWriteSegment(out var _state_);
                        EncodeBean(_os_, v.TypeBeanTypeId);
                        _os_.EndWriteSegment(_state_);
                    }
                    break;
                case Zeze.Serialize.Helper.DYNAMIC:
                    {
                        Lua.GetField(-1, "_TypeId_");
                        if (Lua.IsNil(-1))
                            throw new Exception("'_TypeId_' not found. dynamic bean needed.");
                        long beanTypeId = Lua.ToInteger(-1);
                        Lua.Pop(1);
                        _os_.WriteLong8(beanTypeId);
                        _os_.BeginWriteSegment(out var _state_);
                        EncodeBean(_os_, beanTypeId);
                        _os_.EndWriteSegment(_state_);
                    }
                    break;
                default:
                    throw new Exception("Unkown Tag Type");
            }
        }

        public bool DecodeAndDispatch(long sessionId, int typeId, ByteBuffer _os_)
        {
            this.Lua.GetGlobal("ZezeDispatchProtocol"); // push func onto stack
            if (false == Lua.IsFunction(-1))
            {
                Lua.Pop(1);
                return false;
            }
            // 现在不支持 Rpc.但是代码没有检查。
            // 生成的时候报错。
            Lua.CreateTable(0, 8);

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

        private void DecodeBean(ByteBuffer _os_)
        {
            Lua.CreateTable(0, 32);
            for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_)
            {
                int _tagid_ = _os_.ReadInt();
                int _varid_ = (_tagid_ >> Zeze.Serialize.Helper.TAG_SHIFT) & Zeze.Serialize.Helper.ID_MASK;
                int _tagType_ = _tagid_ & Zeze.Serialize.Helper.TAG_MASK;
                Lua.PushInteger(_varid_);
                DecodeVariable(_os_, _tagType_);
                Lua.SetTable(-3);
            }
        }

        private void DecodeVariable(ByteBuffer _os_, int _tagType_)
        {
            switch (_tagType_)
            {
                case Zeze.Serialize.Helper.BOOL:
                    Lua.PushBoolean(_os_.ReadBool());
                    break;
                case Zeze.Serialize.Helper.BYTE:
                    Lua.PushInteger(_os_.ReadByte());
                    break;
                case Zeze.Serialize.Helper.SHORT:
                    Lua.PushInteger(_os_.ReadShort());
                    break;
                case Zeze.Serialize.Helper.INT:
                    Lua.PushInteger(_os_.ReadInt());
                    break;
                case Zeze.Serialize.Helper.LONG:
                    Lua.PushInteger(_os_.ReadLong());
                    break;
                case Zeze.Serialize.Helper.FLOAT:
                    Lua.PushNumber(_os_.ReadFloat());
                    break;
                case Zeze.Serialize.Helper.DOUBLE:
                    Lua.PushNumber(_os_.ReadDouble());
                    break;
                case Zeze.Serialize.Helper.STRING:
                    Lua.PushString(_os_.ReadString());
                    break;
                case Zeze.Serialize.Helper.BYTES:
                    Lua.PushBuffer(_os_.ReadBytes());
                    break;
                case Zeze.Serialize.Helper.LIST:
                    {
                        _os_.BeginReadSegment(out var _state_);
                        int _valueTagType_ = _os_.ReadInt();
                        Lua.CreateTable(128, 128); // 不知道用哪个参数。
                        int i = 1; // 从1开始？
                        for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_)
                        {
                            Lua.PushInteger(i);
                            DecodeVariable(_os_, _valueTagType_);
                            Lua.SetTable(-3);
                            ++i;
                        }
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case Zeze.Serialize.Helper.SET:
                    {
                        _os_.BeginReadSegment(out var _state_);
                        int _valueTagType_ = _os_.ReadInt();
                        Lua.CreateTable(128, 128); // 不知道用哪个参数。
                        int i = 1;
                        for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_)
                        {
                            DecodeVariable(_os_, _valueTagType_);
                            Lua.PushNil();
                            Lua.SetTable(-3);
                            ++i;
                        }
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case Zeze.Serialize.Helper.MAP:
                    {
                        _os_.BeginReadSegment(out var _state_);
                        int _keyTagType_ = _os_.ReadInt();
                        int _valueTagType_ = _os_.ReadInt();
                        Lua.CreateTable(128, 0);
                        for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_)
                        {
                            DecodeVariable(_os_, _keyTagType_);
                            DecodeVariable(_os_, _valueTagType_);
                            Lua.SetTable(-3);
                        }
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case Zeze.Serialize.Helper.BEAN:
                    {
                        _os_.BeginReadSegment(out var _state_);
                        DecodeBean(_os_);
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case Zeze.Serialize.Helper.DYNAMIC:
                    {
                        long beanTypeId = _os_.ReadLong8();
                        if (beanTypeId == Transaction.EmptyBean.TYPEID)
                        {
                            // 这个EmptyBean完全没有实现Encode,Decode，没有遵守Bean的系列化协议，所以需要特殊处理一下。
                            _os_.BeginReadSegment(out var _state_);
                            _os_.EndReadSegment(_state_);
                            Lua.CreateTable(0, 0);
                        }
                        else
                        {
                            _os_.BeginReadSegment(out var _state_);
                            DecodeBean(_os_);
                            _os_.EndReadSegment(_state_);
                        }
                        // 动态bean额外把TypeId加到变量里面。总是使用varid==0表示。程序可以使用这个动态判断是哪个具体的bean。
                        Lua.PushInteger(0);
                        Lua.PushInteger(beanTypeId);
                        Lua.SetTable(-3);
                    }
                    break;
                default:
                    throw new Exception("Unkown Tag Type");
            }
        }
#else
        // 不使用 lua 也需要定义这个函数，Protocol.Decode 编译需要。其他函数核心代码都不调用，先不定义。
        public bool DecodeAndDispatch(long sessionId, int typeId, ByteBuffer _os_)
        {
            return false;
        }

#endif // end USE_KERA_LUA
    }

    public class Helper
    {

        private Dictionary<long, ByteBuffer> ToLuaBuffer = new Dictionary<long, ByteBuffer>();
        private Dictionary<long, FromLua> ToLuaHandshakeDone = new Dictionary<long, FromLua>();

        internal void SetHandshakeDone(long socketSessionId, FromLua service)
        {
            lock (this)
            {
                ToLuaHandshakeDone[socketSessionId] = service;
            }
        }

        public void AppendInputBuffer(long socketSessionId, ByteBuffer buffer)
        {
            lock (this)
            {
                if (ToLuaBuffer.TryGetValue(socketSessionId, out var exist))
                {
                    exist.Append(buffer.Bytes, buffer.ReadIndex, buffer.Size);
                    return;
                }
                ByteBuffer newBuffer = ByteBuffer.Allocate();
                ToLuaBuffer.Add(socketSessionId, newBuffer);
                newBuffer.Append(buffer.Bytes, buffer.ReadIndex, buffer.Size);
            }
        }

        public void Update(Net.Service service, ToLua toLua)
        {
            Dictionary<long, FromLua> handshakeTmp;
            Dictionary<long, Serialize.ByteBuffer> inputTmp;
            lock (this)
            {
                handshakeTmp = ToLuaHandshakeDone;
                inputTmp = ToLuaBuffer;
                ToLuaBuffer = new Dictionary<long, ByteBuffer>();
                ToLuaHandshakeDone = new Dictionary<long, FromLua>();
            }

            foreach (var e in handshakeTmp)
            {
                toLua.CallHandshakeDone(e.Value, e.Key);
            }

            foreach (var e in inputTmp)
            {
                AsyncSocket sender = service.GetSocket(e.Key);
                if (null == sender)
                    continue;

                Net.Protocol.Decode(service, sender, e.Value, toLua);
            }

            lock (this)
            {
                foreach (var e in inputTmp)
                {
                    if (e.Value.Size <= 0)
                        continue; // 数据全部处理完成。

                    e.Value.Campact();
                    if (ToLuaBuffer.TryGetValue(e.Key, out var exist))
                    {
                        // 处理过程中有新数据到来，加到当前剩余数据后面，然后覆盖掉buffer。
                        e.Value.Append(exist.Bytes, exist.ReadIndex, exist.Size);
                        ToLuaBuffer[e.Key] = e.Value;
                    }
                    else
                    {
                        // 没有新数据到来，有剩余，加回去。下一次update再处理。
                        ToLuaBuffer.Add(e.Key, e.Value);
                    }
                }
            }
        }
    }
}
