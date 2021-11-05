
using System;
using System.Collections.Generic;
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
    }

    public class ToLuaServiceClient : HandshakeClient, FromLua
    {
        public ToLuaService.ToLua ToLua { get; private set; } = new ToLuaService.ToLua();
        public Net.Service Service => this;

        public ToLuaServiceClient(string name, Zeze.Application app) : base(name, app)
        {
        }

        public void InitializeLua(ToLuaService.ILua iLua)
        {
            ToLua.InitializeLua(iLua);
            ToLua.RegisterGlobalAndCallback(this);
        }

        public override void OnHandshakeDone(AsyncSocket sender)
        {
            sender.IsHandshakeDone = true;
            ToLua.SetHandshakeDone(sender.SessionId, this);
        }

        public override void OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input)
        {
            if (so.IsHandshakeDone)
            {
                ToLua.AppendInputBuffer(so.SessionId, input);
                input.ReadIndex = input.WriteIndex;
            }
            else
            {
                base.OnSocketProcessInputBuffer(so, input);
            }
        }

        public override void OnSocketClose(AsyncSocket so, Exception e)
        {
            ToLua.SetSocketClose(so.SessionId, this);
            base.OnSocketClose(so, e);
        }
    }

    // 完全 ToLuaServiceClient，由于 c# 无法写 class S<T> : T where T : Net.Service，复制一份.
    public class ToLuaServiceServer : HandshakeServer, FromLua
    {
        public ToLuaService.ToLua ToLua { get; private set; } = new ToLuaService.ToLua();
        public Net.Service Service => this;

        public ToLuaServiceServer(string name, Zeze.Application app) : base(name, app)
        {
        }

        public void InitializeLua(ToLuaService.ILua iLua)
        {
            ToLua.InitializeLua(iLua);
            ToLua.RegisterGlobalAndCallback(this);
        }

        public override void OnHandshakeDone(AsyncSocket sender)
        {
            sender.IsHandshakeDone = true;
            ToLua.SetHandshakeDone(sender.SessionId, this);
        }

        public override void OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input)
        {
            if (so.IsHandshakeDone)
            {
                ToLua.AppendInputBuffer(so.SessionId, input);
                input.ReadIndex = input.WriteIndex;
            }
            else
            {
                base.OnSocketProcessInputBuffer(so, input);
            }
        }
    }
}

namespace Zeze.Services.ToLuaService
{
    public enum LuaType
    {
        None = -1,
        Nil = 0,
        Boolean = 1,
        LightUserData = 2,
        Number = 3,
        String = 4,
        Table = 5,
        Function = 6,
        UserData = 7,
        Thread = 8
    }
    public interface ILua
    {
        public bool DoString(string str);
        public void GetField(int index, string name);
        public void PushNil();
        public bool IsTable(int index);
        public bool Next(int index);
        public long ToInteger(int index);
        public void Pop(int n);
        public LuaType GetGlobal(string name);
        public void PushObject(object obj);
        public void PushInteger(long l);
        public void Call(int arguments, int results);
        public void PushString(string str);
        public void SetTable(int index);
        public void GetTable(int index);
        public bool IsNil(int index);
        public double ToNumber(int index);
        public string ToString(int index);
        public byte[] ToBuffer(int index);
        public bool ToBoolean(int index);
        public void CreateTable(int elements, int records);
        public void PushBoolean(bool v);
        public void PushNumber(double number);
        public void PushBuffer(byte[] buffer);
        public T ToObject<T>(int index);
#if USE_KERA_LUA
        public void Register(string name, KeraLua.LuaFunction func);
#endif // end USE_KERA_LUA
    }

#if USE_KERA_LUA
    public class Kera : ILua
    {
        KeraLua.Lua Lua;
        public Kera(KeraLua.Lua Lua)
        {
            this.Lua = Lua;
        }
        void ILua.Call(int arguments, int results) { Lua.Call(arguments, results); }
        void ILua.CreateTable(int elements, int records) { Lua.CreateTable(elements, records); }
        bool ILua.DoString(string str) { return Lua.DoString(str); }
        void ILua.GetField(int index, string name) { Lua.GetField(index, name); }
        LuaType ILua.GetGlobal(string name) { return (LuaType)Lua.GetGlobal(name); }
        void ILua.GetTable(int index) { Lua.GetTable(index); }
        bool ILua.IsNil(int index) { return Lua.IsNil(index); }
        bool ILua.IsTable(int index) { return Lua.IsTable(index); }
        bool ILua.Next(int index) { return Lua.Next(index); }
        void ILua.Pop(int n) { Lua.Pop(n); }
        void ILua.PushBoolean(bool v) { Lua.PushBoolean(v); }
        void ILua.PushBuffer(byte[] buffer) { Lua.PushBuffer(buffer); }
        void ILua.PushInteger(long l) { Lua.PushInteger(l); }
        void ILua.PushNil() { Lua.PushNil(); }
        void ILua.PushNumber(double number) { Lua.PushNumber(number); }
        void ILua.PushObject(object obj) { Lua.PushObject(obj); }
        void ILua.PushString(string str) { Lua.PushString(str); }
        void ILua.SetTable(int index) { Lua.SetTable(index); }
        bool ILua.ToBoolean(int index) { return Lua.ToBoolean(index); }
        byte[] ILua.ToBuffer(int index) { return Lua.ToBuffer(index); }
        long ILua.ToInteger(int index) { return Lua.ToInteger(index); }
        double ILua.ToNumber(int index) { return Lua.ToNumber(index); }
        string ILua.ToString(int index) { return Lua.ToString(index); }
        public T ToObject<T>(int index) { return Lua.ToObject<T>(index, false); }
        public void Register(string name, KeraLua.LuaFunction func) { Lua.Register(name, func); }
    }
#endif // end USE_KERA_LUA

    public class ToLua
    {
        public ILua Lua { get; private set; }

        public ToLua()
        {
        }

        public void InitializeLua(ILua lua)
        {
            this.Lua = lua;
            if (this.Lua.DoString("local Zeze = require 'Zeze'\nreturn Zeze"))
                throw new Exception("load  'Zeze.lua' Error.");
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
            public string Name { get; set; }

            public BeanMeta BeanMeta { get; set; }

            public override string ToString()
            {
                return $"{{[{Id}]={{{Type},{TypeBeanTypeId},{Key},{KeyBeanTypeId},{Value},{ValueBeanTypeId},\"{Name}\"}}}}";
            }
        }

        class ProtocolArgument
        {
            public long ArgumentBeanTypeId { get; set; }
            public long ResultBeanTypeId { get; set; }
            public bool IsRpc { get; set; } = false;
        };

        class BeanMeta
        {
            public string Name { get; set; }
            public List<VariableMeta> Variables { get; } = new List<VariableMeta>();
        }

        private readonly Dictionary<long, BeanMeta> BeanMetas
            = new Dictionary<long, BeanMeta>(); // Bean.TypeId -> vars

        private readonly Dictionary<long, ProtocolArgument> ProtocolMetas
            = new Dictionary<long, ProtocolArgument>(); // protocol.TypeId -> Bean.TypeId

        public void LoadMeta()
        {
            BeanMetas.Clear();
            ProtocolMetas.Clear();

            if (Lua.DoString("local meta = require 'ZezeMeta'\nreturn meta"))
                throw new Exception("load ZezeMeta.lua error");
            if (false == Lua.IsTable(-1))
                throw new Exception("ZezeMeta not return a table");
            Lua.GetField(-1, "beans");
            Lua.PushNil();
            while (Lua.Next(-2)) // -1 value of vars(table) -2 key of bean.TypeId
            {
                long beanTypeId = Lua.ToInteger(-2);
                var beanMeta = new BeanMeta();
                Lua.PushNil();
                while (Lua.Next(-2)) // -1 value of varmeta(table) -2 key of varid
                {
                    var varId = (int)Lua.ToInteger(-2);
                    if (0 == varId)
                    {
                        // bean full name
                        beanMeta.Name = Lua.ToString(-1);
                        Lua.Pop(1); // pop value XXX 忘了这里要不要Pop一次了。
                        continue;
                    }
                    VariableMeta var = new VariableMeta() { BeanMeta = beanMeta };
                    var.Id = varId;
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
                            case 7: var.Name = Lua.ToString(-1); break;
                            default: throw new Exception("error index for typetag");
                        }
                        Lua.Pop(1); // pop value
                    }
                    Lua.Pop(1); // pop value
                    beanMeta.Variables.Add(var);
                }
                BeanMetas.Add(beanTypeId, beanMeta);
                Lua.Pop(1); // pop value
            }
            Lua.Pop(1);

            Lua.GetField(-1, "protocols");
            Lua.PushNil();
            while (Lua.Next(-2)) // -1 value of Protocol.Argument.BeanTypeId -2 Protocol.TypeId
            {
                ProtocolArgument pa = new ProtocolArgument();
                Lua.PushNil();
                while (Lua.Next(-2)) // -1 value of beantypeid -2 key of index
                {
                    switch (Lua.ToInteger(-2))
                    {
                        case 1:
                            pa.ArgumentBeanTypeId = Lua.ToInteger(-1);
                            pa.IsRpc = false;
                            break;

                        case 2:
                            pa.ResultBeanTypeId = Lua.ToInteger(-1);
                            pa.IsRpc = true;
                            break;

                        default:
                            throw new Exception("error index for protocol argument bean typeid");
                    }
                    Lua.Pop(1);
                }
                ProtocolMetas.Add(Lua.ToInteger(-2), pa);
                Lua.Pop(1); // pop value
            }
            Lua.Pop(1);
        }

        internal void CallSocketClose(FromLua service, long socketSessionId)
        {
            if (LuaType.Function != Lua.GetGlobal("ZezeSocketClose")) // push func onto stack
            {
                Lua.Pop(1);
                return;
            }

            Lua.PushObject(service);
            Lua.PushInteger(socketSessionId);
            Lua.Call(2, 0);
        }

        internal void CallHandshakeDone(FromLua service, long socketSessionId)
        {
            // void OnHandshakeDone(service, long sessionId)
            if (LuaType.Function != this.Lua.GetGlobal("ZezeHandshakeDone")) // push func onto stack
            {
                Lua.Pop(1);
                throw new Exception("ZezeHandshakeDone is not a function");
            }

            Lua.PushObject(service);
            Lua.PushInteger(socketSessionId);
            Lua.Call(2, 0);
        }

        private int ZezeSendProtocol(IntPtr luaState)
        {
            //KeraLua.Lua lua = KeraLua.Lua.FromIntPtr(luaState);
            FromLua callback = Lua.ToObject<FromLua>(-3);
            long sessionId = Lua.ToInteger(-2);
            AsyncSocket socket = callback.Service.GetSocket(sessionId);
            if (null == socket)
                return 0;
            callback.ToLua.SendProtocol(socket);
            return 0;
        }

        private int ZezeUpdate(IntPtr luaState)
        {
            //KeraLua.Lua lua = KeraLua.Lua.FromIntPtr(luaState);
            FromLua callback = Lua.ToObject<FromLua>(-1);
            callback.ToLua.Update(callback.Service);
            return 0;
        }

        private int ZezeConnect(IntPtr luaState)
        {
            FromLua service = Lua.ToObject<FromLua>(-4);
            string host = Lua.ToString(-3);
            int port = (int)Lua.ToInteger(-2);
            bool autoReconnect = Lua.ToBoolean(-1);
            if (service.Service is HandshakeClient client)
                client.Connect(host, port, autoReconnect);
            return 0;
        }

        // 使用静态变量，防止垃圾回收。
#if USE_KERA_LUA
        private static KeraLua.LuaFunction ZezeUpdateFunction;
        private static KeraLua.LuaFunction ZezeSendProtocolFunction;
        private static KeraLua.LuaFunction ZezeConnectFunction;
#endif // USE_KERA_LUA
        private static object RegisterCallbackLock = new object();

        internal void RegisterGlobalAndCallback(FromLua callback)
        {
            if (Lua.DoString("local Zeze = require 'Zeze'\nreturn Zeze"))
                throw new Exception("load Zeze.lua faild");
            if (false == Lua.IsTable(-1))
                throw new Exception("Zeze.lua not return a table");

            Lua.PushString("Service" + callback.Name);
            Lua.PushObject(callback);
            Lua.SetTable(-3);
            Lua.PushString("CurrentService");
            Lua.PushObject(callback);
            Lua.SetTable(-3); // 当存在多个service时，这里保存最后一个。

            lock (RegisterCallbackLock)
            {
                // 所有的ToLua实例共享回调函数。
#if USE_KERA_LUA
                if (null == ZezeUpdateFunction)
                {
                    ZezeUpdateFunction = ZezeUpdate;
                    ZezeSendProtocolFunction = ZezeSendProtocol;
                    ZezeConnectFunction = ZezeConnect;

                    Lua.Register("ZezeUpdate", ZezeUpdateFunction);
                    Lua.Register("ZezeSendProtocol", ZezeSendProtocolFunction);
                    Lua.Register("ZezeConnect", ZezeConnectFunction);
                }
#endif // USE_KERA_LUA
            }
        }

        public void SendProtocol(AsyncSocket socket)
        {
            if (false == Lua.IsTable(-1))
                throw new Exception("SendProtocol param is not a table.");

            Lua.GetField(-1, "ModuleId");
            int ModuleId = (int)Lua.ToInteger(-1);
            Lua.Pop(1);
            Lua.GetField(-1, "ProtocolId");
            int ProtocolId = (int)Lua.ToInteger(-1);
            Lua.Pop(1);
            Lua.GetField(-1, "ResultCode");
            long resultCode = Lua.ToInteger(-1);
            Lua.Pop(1);

            long type = Protocol.MakeTypeId(ModuleId, ProtocolId);
            if (false == ProtocolMetas.TryGetValue(type, out var pa))
                throw new Exception($"protocol not found in meta. ({ModuleId},{ProtocolId})");

            if (pa.IsRpc)
            {
                Lua.GetField(-1, "IsRequest");
                bool isRequest = Lua.ToBoolean(-1);
                Lua.Pop(1);
                Lua.GetField(-1, "Sid");
                long sid = Lua.ToInteger(-1);
                Lua.Pop(1);
                Lua.GetField(-1, "Timeout");
                int timeout = (int)Lua.ToInteger(-1);
                Lua.Pop(1);

                long argumentBeanTypeId;
                string argumentName;
                if (isRequest)
                {
                    argumentBeanTypeId = pa.ArgumentBeanTypeId;
                    argumentName = "Argument";
                }
                else
                {
                    argumentBeanTypeId = pa.ResultBeanTypeId;
                    argumentName = "Result";
                }

                // see Rpc.Encode
                ByteBuffer bb = ByteBuffer.Allocate();
                bb.WriteInt4(ModuleId);
                bb.WriteInt4(ProtocolId);
                bb.BeginWriteWithSize4(out var outstate);
                bb.WriteBool(isRequest);
                bb.WriteLong(sid);
                bb.WriteLong(resultCode);
                Lua.GetField(-1, argumentName);
                EncodeBean(bb, argumentBeanTypeId);
                Lua.Pop(1);
                bb.EndWriteWithSize4(outstate);
                socket.Send(bb);

                if (timeout > 0)
                    Util.Scheduler.Instance.Schedule((ThisTask) => { SetRpcTimeout(sid); }, timeout);
            }
            else
            {
                // see Protocol.Encode
                ByteBuffer bb = ByteBuffer.Allocate();
                bb.WriteInt4(ModuleId);
                bb.WriteInt4(ProtocolId);
                bb.BeginWriteWithSize4(out var state);
                bb.WriteLong(resultCode);
                Lua.GetField(-1, "Argument");
                EncodeBean(bb, pa.ArgumentBeanTypeId);
                Lua.Pop(1);
                bb.EndWriteWithSize4(state);
                socket.Send(bb);
            }
        }

        private void EncodeBean(ByteBuffer bb, long beanTypeId)
        {
            if (false == Lua.IsTable(-1))
                throw new Exception("encodebean need a table");

            if (beanTypeId == Zeze.Transaction.EmptyBean.TYPEID)
            {
                bb.WriteInt(0);
                return;
            }
            if (false == BeanMetas.TryGetValue(beanTypeId, out var beanMeta))
                throw new Exception("bean not found in meta for beanTypeId=" + beanTypeId);

            // 先遍历一遍，得到填写了的var的数量
            int varsCount = 0;
            foreach (var v in beanMeta.Variables)
            {
                Lua.PushInteger(v.Id);
                Lua.GetTable(-2);
                if (false == Lua.IsNil(-1))
                    ++varsCount;
                Lua.Pop(1);
            }
            bb.WriteInt(varsCount);

            foreach (var v in beanMeta.Variables)
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
                _os_.WriteInt(v.Type | v.Id << Zeze.Serialize.ByteBuffer.TAG_SHIFT);

            switch (v.Type)
            {
                case Zeze.Serialize.ByteBuffer.BOOL:
                    _os_.WriteBool(Lua.ToBoolean(index));
                    break;
                case Zeze.Serialize.ByteBuffer.BYTE:
                    _os_.WriteByte((byte)Lua.ToInteger(index));
                    break;
                case Zeze.Serialize.ByteBuffer.SHORT:
                    _os_.WriteShort((short)Lua.ToInteger(index));
                    break;
                case Zeze.Serialize.ByteBuffer.INT:
                    _os_.WriteInt((int)Lua.ToInteger(index));
                    break;
                case Zeze.Serialize.ByteBuffer.LONG:
                    _os_.WriteLong(Lua.ToInteger(index));
                    break;
                case Zeze.Serialize.ByteBuffer.FLOAT:
                    _os_.WriteFloat((float)Lua.ToNumber(index));
                    break;
                case Zeze.Serialize.ByteBuffer.DOUBLE:
                    _os_.WriteDouble(Lua.ToNumber(index));
                    break;
                case Zeze.Serialize.ByteBuffer.STRING:
                    _os_.WriteString(Lua.ToString(index));
                    break;
                case Zeze.Serialize.ByteBuffer.BYTES:
                    _os_.WriteBytes(Lua.ToBuffer(index));
                    break;
                case Zeze.Serialize.ByteBuffer.LIST:
                    {
                        if (false == Lua.IsTable(-1))
                            throw new Exception("list must be a table");
                        if (v.Id <= 0)
                            throw new Exception("list cannot define in collection");
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
                case Zeze.Serialize.ByteBuffer.SET:
                    {
                        if (false == Lua.IsTable(-1))
                            throw new Exception("set must be a table");
                        if (v.Id <= 0)
                            throw new Exception("set cannot define in collection");
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
                case Zeze.Serialize.ByteBuffer.MAP:
                    {
                        if (false == Lua.IsTable(-1))
                            throw new Exception("map must be a table");
                        if (v.Id <= 0)
                            throw new Exception("map cannot define in collection");
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
                case Zeze.Serialize.ByteBuffer.BEAN:
                    {
                        if (v.Id > 0)
                        {
                            _os_.BeginWriteSegment(out var _state_);
                            EncodeBean(_os_, v.TypeBeanTypeId);
                            _os_.EndWriteSegment(_state_);
                        }
                        else
                        {
                            // in collection. direct encode
                            EncodeBean(_os_, v.TypeBeanTypeId);
                        }
                    }
                    break;
                case Zeze.Serialize.ByteBuffer.DYNAMIC:
                    {
                        if (v.Id <= 0)
                            throw new Exception("dynamic cannot define in collection");
                        Lua.GetField(-1, "_TypeId_");
                        if (Lua.IsNil(-1))
                            throw new Exception("'_TypeId_' not found. dynamic bean needed.");
                        long beanTypeId = Lua.ToInteger(-1);
                        Lua.Pop(1);

                        var funcName = $"Zeze_GetRealBeanTypeIdFromSpecial_{v.BeanMeta.Name}_{v.Name}";
                        if (LuaType.Function != this.Lua.GetGlobal(funcName)) // push func onto stack
                        {
                            Lua.Pop(1);
                            throw new Exception($"{funcName} is not a function");
                        }
                        Lua.PushInteger(beanTypeId);
                        Lua.Call(1, 1);
                        var realBeanTypeId = Lua.ToInteger(-1);
                        Lua.Pop(1);

                        _os_.WriteLong8(beanTypeId);
                        _os_.BeginWriteSegment(out var _state_);
                        EncodeBean(_os_, realBeanTypeId);
                        _os_.EndWriteSegment(_state_);
                    }
                    break;
                default:
                    throw new Exception("Unkown Tag Type");
            }
        }

        private void CallRpcTimeout(long sid)
        {
            if (LuaType.Function != this.Lua.GetGlobal("ZezeDispatchProtocol")) // push func onto stack
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

        public bool DecodeAndDispatch(Net.Service service, long sessionId, long typeId, ByteBuffer _os_)
        {
            if (LuaType.Function != this.Lua.GetGlobal("ZezeDispatchProtocol")) // push func onto stack
            {
                Lua.Pop(1);
                return false;
            }

            if (false == ProtocolMetas.TryGetValue(typeId, out var pa))
                throw new Exception("protocol not found in meta for typeid=" + typeId);

            // 现在不支持 Rpc.但是代码没有检查。
            // 生成的时候报错。
            Lua.CreateTable(0, 16);

            if (service is FromLua fromLua) // 必须是，不报错了。
            {
                Lua.PushString("Service");
                Lua.PushObject(fromLua);
                Lua.SetTable(-3);
            }

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

            if (pa.IsRpc)
            {
                bool IsRequest = _os_.ReadBool();
                long sid = _os_.ReadLong();
                long resultCode = _os_.ReadLong();
                string argument;
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
                Lua.PushInteger(_os_.ReadLong());
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

        private void DecodeBean(ByteBuffer _os_)
        {
            Lua.CreateTable(0, 32);
            for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_)
            {
                int _tagid_ = _os_.ReadInt();
                int _varid_ = (_tagid_ >> Zeze.Serialize.ByteBuffer.TAG_SHIFT) & Zeze.Serialize.ByteBuffer.ID_MASK;
                int _tagType_ = _tagid_ & Zeze.Serialize.ByteBuffer.TAG_MASK;
                Lua.PushInteger(_varid_);
                DecodeVariable(_os_, _tagType_);
                Lua.SetTable(-3);
            }
        }

        private void DecodeVariable(ByteBuffer _os_, int _tagType_, bool inCollection = false)
        {
            switch (_tagType_)
            {
                case Zeze.Serialize.ByteBuffer.BOOL:
                    Lua.PushBoolean(_os_.ReadBool());
                    break;
                case Zeze.Serialize.ByteBuffer.BYTE:
                    Lua.PushInteger(_os_.ReadByte());
                    break;
                case Zeze.Serialize.ByteBuffer.SHORT:
                    Lua.PushInteger(_os_.ReadShort());
                    break;
                case Zeze.Serialize.ByteBuffer.INT:
                    Lua.PushInteger(_os_.ReadInt());
                    break;
                case Zeze.Serialize.ByteBuffer.LONG:
                    Lua.PushInteger(_os_.ReadLong());
                    break;
                case Zeze.Serialize.ByteBuffer.FLOAT:
                    Lua.PushNumber(_os_.ReadFloat());
                    break;
                case Zeze.Serialize.ByteBuffer.DOUBLE:
                    Lua.PushNumber(_os_.ReadDouble());
                    break;
                case Zeze.Serialize.ByteBuffer.STRING:
                    Lua.PushString(_os_.ReadString());
                    break;
                case Zeze.Serialize.ByteBuffer.BYTES:
                    Lua.PushBuffer(_os_.ReadBytes());
                    break;
                case Zeze.Serialize.ByteBuffer.LIST:
                    {
                        _os_.BeginReadSegment(out var _state_);
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
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case Zeze.Serialize.ByteBuffer.SET:
                    {
                        _os_.BeginReadSegment(out var _state_);
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
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case Zeze.Serialize.ByteBuffer.MAP:
                    {
                        _os_.BeginReadSegment(out var _state_);
                        int _keyTagType_ = _os_.ReadInt();
                        int _valueTagType_ = _os_.ReadInt();
                        Lua.CreateTable(128, 0);
                        for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_)
                        {
                            DecodeVariable(_os_, _keyTagType_, true);
                            DecodeVariable(_os_, _valueTagType_, true);
                            Lua.SetTable(-3);
                        }
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case Zeze.Serialize.ByteBuffer.BEAN:
                    {
                        if (inCollection)
                        {
                            DecodeBean(_os_);
                        }
                        else
                        {
                            _os_.BeginReadSegment(out var _state_);
                            DecodeBean(_os_);
                            _os_.EndReadSegment(_state_);
                        }
                    }
                    break;
                case Zeze.Serialize.ByteBuffer.DYNAMIC:
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

        private Dictionary<long, ByteBuffer> ToLuaBuffer = new Dictionary<long, ByteBuffer>();
        private Dictionary<long, FromLua> ToLuaHandshakeDone = new Dictionary<long, FromLua>();
        private Dictionary<long, FromLua> ToLuaSocketClose = new Dictionary<long, FromLua>();
        private HashSet<long> ToLuaRpcTimeout = new HashSet<long>();

        internal void SetRpcTimeout(long sid)
        {
            lock (this)
            {
                ToLuaRpcTimeout.Add(sid);
            }
        }

        internal void SetHandshakeDone(long socketSessionId, FromLua service)
        {
            lock (this)
            {
                ToLuaHandshakeDone[socketSessionId] = service;
            }
        }

        internal void SetSocketClose(long socketSessionId, FromLua service)
        {
            lock (this)
            {
                ToLuaSocketClose[socketSessionId] = service;
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

        public void Update(Net.Service service)
        {
            Dictionary<long, FromLua> handshakeTmp;
            Dictionary<long, FromLua> socketCloseTmp;
            Dictionary<long, Serialize.ByteBuffer> inputTmp;
            HashSet<long> rpcTimeout;
            lock (this)
            {
                handshakeTmp = ToLuaHandshakeDone;
                socketCloseTmp = ToLuaSocketClose;
                inputTmp = ToLuaBuffer;
                rpcTimeout = ToLuaRpcTimeout;
                ToLuaBuffer = new Dictionary<long, ByteBuffer>();
                ToLuaHandshakeDone = new Dictionary<long, FromLua>();
                ToLuaSocketClose = new Dictionary<long, FromLua>();
                ToLuaRpcTimeout = new HashSet<long>();
            }

            foreach (var e in socketCloseTmp)
            {
                this.CallSocketClose(e.Value, e.Key);
            }

            foreach (var e in handshakeTmp)
            {
                this.CallHandshakeDone(e.Value, e.Key);
            }

            foreach (var sid in rpcTimeout)
            {
                this.CallRpcTimeout(sid);
            }

            foreach (var e in inputTmp)
            {
                AsyncSocket sender = service.GetSocket(e.Key);
                if (null == sender)
                    continue;

                Net.Protocol.Decode(service, sender, e.Value, this);
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
