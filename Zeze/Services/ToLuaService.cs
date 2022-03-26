using System;
using System.Collections.Generic;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;

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
    interface IFromLua
    {
        public string Name { get; } // Service Name
        public Service Service { get; }

        public ToLuaService.ToLua ToLua { get; }
    }

    public class ToLuaServiceClient : HandshakeClient, IFromLua
    {
        public ToLuaService.ToLua ToLua { get; private set; } = new ToLuaService.ToLua();
        public Service Service => this;

        public ToLuaServiceClient(string name, Application app) : base(name, app)
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
                base.OnSocketProcessInputBuffer(so, input);
        }

        public override void OnSocketClose(AsyncSocket so, Exception e)
        {
            ToLua.SetSocketClose(so.SessionId, this);
            base.OnSocketClose(so, e);
        }
    }

    // 完全 ToLuaServiceClient，由于 c# 无法写 class S<T> : T where T : Service，复制一份.
    public class ToLuaServiceServer : HandshakeServer, IFromLua
    {
        public ToLuaService.ToLua ToLua { get; private set; } = new ToLuaService.ToLua();
        public Service Service => this;

        public ToLuaServiceServer(string name, Application app) : base(name, app)
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
                base.OnSocketProcessInputBuffer(so, input);
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
        public Kera(KeraLua.Lua lua)
        {
            Lua = lua;
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
            Lua = lua;
            if (lua.DoString("return (require 'Zeze')"))
                throw new Exception("require 'Zeze' failed");
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
            public bool IsRpc { get; set; }
        };

        class BeanMeta
        {
            public string Name { get; set; }
            public List<VariableMeta> Variables { get; } = new List<VariableMeta>();
        }

        readonly Dictionary<long, BeanMeta> BeanMetas = new(); // Bean.TypeId -> vars
        readonly Dictionary<long, ProtocolArgument> ProtocolMetas = new(); // protocol.TypeId -> Bean.TypeId

        public void LoadMeta()
        {
            BeanMetas.Clear();
            ProtocolMetas.Clear();

            if (Lua.DoString("return (require 'ZezeMeta')"))
                throw new Exception("require 'ZezeMeta' failed");
            if (!Lua.IsTable(-1))
                throw new Exception("require 'ZezeMeta' not return a table");
            Lua.GetField(-1, "beans");
            for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1)) // -1 value of vars(table) -2 key of bean.TypeId
            {
                long beanTypeId = Lua.ToInteger(-2);
                var beanMeta = new BeanMeta();
                for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1)) // -1 value of varmeta(table) -2 key of varid
                {
                    int varId = (int)Lua.ToInteger(-2);
                    if (varId == 0)
                    {
                        beanMeta.Name = Lua.ToString(-1); // bean full name
                        continue;
                    }
                    var var = new VariableMeta() { BeanMeta = beanMeta };
                    var.Id = varId;
                    for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1)) // -1 value of typetag -2 key of index
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
                    }
                    beanMeta.Variables.Add(var);
                }
                beanMeta.Variables.Sort((a, b) => a.Id - b.Id);
                BeanMetas.Add(beanTypeId, beanMeta);
            }
            Lua.Pop(1);

            Lua.GetField(-1, "protocols");
            for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1)) // -1 value of Protocol.Argument.BeanTypeId -2 Protocol.TypeId
            {
                var pa = new ProtocolArgument();
                for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1)) // -1 value of beantypeid -2 key of index
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
                }
                ProtocolMetas.Add(Lua.ToInteger(-2), pa);
            }
            Lua.Pop(1);
        }

        internal void CallSocketClose(IFromLua service, long socketSessionId)
        {
            if (Lua.GetGlobal("ZezeSocketClose") != LuaType.Function) // push func onto stack
            {
                Lua.Pop(1);
                return;
            }

            Lua.PushObject(service);
            Lua.PushInteger(socketSessionId);
            Lua.Call(2, 0);
        }

        internal void CallHandshakeDone(IFromLua service, long socketSessionId)
        {
            // void OnHandshakeDone(service, long sessionId)
            if (Lua.GetGlobal("ZezeHandshakeDone") != LuaType.Function) // push func onto stack
            {
                Lua.Pop(1);
                throw new Exception("ZezeHandshakeDone is not a function");
            }

            Lua.PushObject(service);
            Lua.PushInteger(socketSessionId);
            Lua.Call(2, 0);
        }

        int ZezeSendProtocol(IntPtr luaState)
        {
            //KeraLua.Lua lua = KeraLua.Lua.FromIntPtr(luaState);
            IFromLua callback = Lua.ToObject<IFromLua>(-3);
            long sessionId = Lua.ToInteger(-2);
            AsyncSocket socket = callback.Service.GetSocket(sessionId);
            if (null == socket)
                return 0;
            callback.ToLua.SendProtocol(socket);
            return 0;
        }

        int ZezeUpdate(IntPtr luaState)
        {
            //KeraLua.Lua lua = KeraLua.Lua.FromIntPtr(luaState);
            IFromLua callback = Lua.ToObject<IFromLua>(-1);
            callback.ToLua.Update(callback.Service);
            return 0;
        }

        int ZezeConnect(IntPtr luaState)
        {
            IFromLua service = Lua.ToObject<IFromLua>(-4);
            string host = Lua.ToString(-3);
            int port = (int)Lua.ToInteger(-2);
            bool autoReconnect = Lua.ToBoolean(-1);
            if (service.Service is HandshakeClient client)
                client.Connect(host, port, autoReconnect);
            return 0;
        }

        // 使用静态变量，防止垃圾回收。
#if USE_KERA_LUA
        static KeraLua.LuaFunction ZezeUpdateFunction;
        static KeraLua.LuaFunction ZezeSendProtocolFunction;
        static KeraLua.LuaFunction ZezeConnectFunction;
#endif // USE_KERA_LUA
        static readonly object RegisterCallbackLock = new ();

        internal void RegisterGlobalAndCallback(IFromLua callback)
        {
            if (Lua.DoString("return (require 'Zeze')"))
                throw new Exception("require 'Zeze' failed");
            if (!Lua.IsTable(-1))
                throw new Exception("require 'Zeze' not return a table");

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
            if (!Lua.IsTable(-1))
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
            if (!ProtocolMetas.TryGetValue(type, out var pa))
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
                    Util.Scheduler.Schedule((ThisTask) => SetRpcTimeout(sid), timeout);
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

        void EncodeBean(ByteBuffer bb, long beanTypeId)
        {
            if (!Lua.IsTable(-1))
                throw new Exception("encodebean need a table");
            if (beanTypeId != EmptyBean.TYPEID)
            {
                if (!BeanMetas.TryGetValue(beanTypeId, out var beanMeta))
                    throw new Exception("bean not found in meta for beanTypeId=" + beanTypeId);
                int lastId = 0;
                foreach (var v in beanMeta.Variables)
                {
                    Lua.PushInteger(v.Id);
                    Lua.GetTable(-2);
                    if (!Lua.IsNil(-1)) // allow var not set
                        lastId = EncodeVariable(bb, v, lastId, -1);
                    Lua.Pop(1);
                }
            }
            bb.WriteByte(0);
        }

        int EncodeGetTableLength()
        {
            if (!Lua.IsTable(-1))
                throw new Exception("EncodeGetTableLength: not a table");
            int len = 0;
            for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1))
                len++;
            return len;
        }

        int EncodeVariable(ByteBuffer bb, VariableMeta v, int lastId, int index)
        {
            int id = v.Id;
            switch (v.Type)
            {
                case ByteBuffer.LUA_BOOL:
                    bool vb = Lua.ToBoolean(index);
                    if (vb || id <= 0)
                    {
                        if (id > 0)
                            lastId = bb.WriteTag(lastId, id, ByteBuffer.INTEGER);
                        bb.WriteBool(vb);
                    }
                    break;
                case ByteBuffer.INTEGER:
                    long vi = Lua.ToInteger(index);
                    if (vi != 0 || id <= 0)
                    {
                        if (id > 0)
                            lastId = bb.WriteTag(lastId, id, ByteBuffer.INTEGER);
                        bb.WriteLong(vi);
                    }
                    break;
                case ByteBuffer.FLOAT:
                    float vf = (float)Lua.ToNumber(index);
                    if (vf != 0 || id <= 0)
                    {
                        if (id > 0)
                            lastId = bb.WriteTag(lastId, id, ByteBuffer.FLOAT);
                        bb.WriteFloat(vf);
                    }
                    break;
                case ByteBuffer.DOUBLE:
                    double vd = Lua.ToNumber(index);
                    if (vd != 0 || id <= 0)
                    {
                        if (id > 0)
                            lastId = bb.WriteTag(lastId, id, ByteBuffer.DOUBLE);
                        bb.WriteDouble(vd);
                    }
                    break;
                case ByteBuffer.BYTES:
                    byte[] vbs = Lua.ToBuffer(index);
                    if (vbs.Length != 0 || id <= 0)
                    {
                        if (id > 0)
                            lastId = bb.WriteTag(lastId, id, ByteBuffer.BYTES);
                        bb.WriteBytes(vbs);
                    }
                    break;
                case ByteBuffer.LIST:
                    if (!Lua.IsTable(-1))
                        throw new Exception("list must be a table");
                    if (id <= 0)
                        throw new Exception("list cannot be defined in container");
                    int n = EncodeGetTableLength();
                    if (n > 0)
                    {
                        lastId = bb.WriteTag(lastId, id, ByteBuffer.LIST);
                        bb.WriteListType(n, v.Value & ByteBuffer.TAG_MASK);
                        for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1))
                            EncodeVariable(bb, new VariableMeta() { Id = 0, Type = v.Value, TypeBeanTypeId = v.ValueBeanTypeId }, 0, -1);
                    }
                    break;
                case ByteBuffer.LUA_SET:
                    if (!Lua.IsTable(-1))
                        throw new Exception("set must be a table");
                    if (id <= 0)
                        throw new Exception("set cannot be defined in container");
                    n = EncodeGetTableLength();
                    if (n > 0)
                    {
                        lastId = bb.WriteTag(lastId, id, ByteBuffer.LIST);
                        bb.WriteListType(n, v.Value & ByteBuffer.TAG_MASK);
                        for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1))
                            EncodeVariable(bb, new VariableMeta() { Id = 0, Type = v.Value, TypeBeanTypeId = v.ValueBeanTypeId }, 0, -2); // set：encode key
                    }
                    break;
                case ByteBuffer.MAP:
                    if (!Lua.IsTable(-1))
                        throw new Exception("map must be a table");
                    if (id <= 0)
                        throw new Exception("map cannot be defined in container");
                    n = EncodeGetTableLength();
                    if (n > 0)
                    {
                        lastId = bb.WriteTag(lastId, id, ByteBuffer.MAP);
                        bb.WriteMapType(n, v.Key & ByteBuffer.TAG_MASK, v.Value & ByteBuffer.TAG_MASK);
                        for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1))
                        {
                            EncodeVariable(bb, new VariableMeta() { Id = 0, Type = v.Key, TypeBeanTypeId = v.KeyBeanTypeId }, 0, -2);
                            EncodeVariable(bb, new VariableMeta() { Id = 0, Type = v.Value, TypeBeanTypeId = v.ValueBeanTypeId }, 0, -1);
                        }
                    }
                    break;
                case ByteBuffer.BEAN:
                    if (id > 0)
                    {
                        int a = bb.WriteIndex;
                        int j = bb.WriteTag(lastId, id, ByteBuffer.BEAN);
                        int b = bb.WriteIndex;
                        EncodeBean(bb, v.TypeBeanTypeId);
                        if (b + 1 == bb.WriteIndex) // only bean end mark
                            bb.WriteIndex = a;
                        else
                            lastId = j;
                    }
                    else
                        EncodeBean(bb, v.TypeBeanTypeId);
                    break;
                case ByteBuffer.DYNAMIC:
                    if (id <= 0)
                        throw new Exception("dynamic cannot be defined in container");
                    Lua.GetField(-1, "_TypeId_");
                    if (Lua.IsNil(-1))
                        throw new Exception("'_TypeId_' not found. dynamic bean needed.");
                    long beanTypeId = Lua.ToInteger(-1);
                    Lua.Pop(1);

                    string funcName = $"Zeze_GetRealBeanTypeIdFromSpecial_{v.BeanMeta.Name}_{v.Name}";
                    if (Lua.GetGlobal(funcName) != LuaType.Function) // push func onto stack
                    {
                        Lua.Pop(1);
                        throw new Exception($"{funcName} is not a function");
                    }
                    Lua.PushInteger(beanTypeId);
                    Lua.Call(1, 1);
                    long realBeanTypeId = Lua.ToInteger(-1);
                    Lua.Pop(1);

                    if (id > 0)
                        lastId = bb.WriteTag(lastId, id, ByteBuffer.DYNAMIC);
                    bb.WriteLong(beanTypeId);
                    EncodeBean(bb, realBeanTypeId);
                    break;
                default:
                    throw new Exception("Unknown Tag Type");
            }
            return lastId;
        }

        void CallRpcTimeout(long sid)
        {
            if (Lua.GetGlobal("ZezeDispatchProtocol") != LuaType.Function) // push func onto stack
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

        public bool DecodeAndDispatch(Service service, long sessionId, long typeId, ByteBuffer _os_)
        {
            if (Lua.GetGlobal("ZezeDispatchProtocol") != LuaType.Function) // push func onto stack
            {
                Lua.Pop(1);
                return false;
            }

            if (!ProtocolMetas.TryGetValue(typeId, out var pa))
                throw new Exception("protocol not found in meta for typeid=" + typeId);

            // 现在不支持 Rpc.但是代码没有检查。
            // 生成的时候报错。
            Lua.CreateTable(0, 16);

            if (service is IFromLua fromLua) // 必须是，不报错了。
            {
                Lua.PushString("Service");
                Lua.PushObject(fromLua);
                Lua.SetTable(-3);
            }

            Lua.PushString("SessionId");
            Lua.PushInteger(sessionId);
            Lua.SetTable(-3);

            Lua.PushString("ModuleId");
            Lua.PushInteger(typeId >> 32);
            Lua.SetTable(-3);

            Lua.PushString("ProtcolId");
            Lua.PushInteger((int)typeId);
            Lua.SetTable(-3);

            Lua.PushString("TypeId");
            Lua.PushInteger(typeId);
            Lua.SetTable(-3);

            if (pa.IsRpc)
            {
                bool IsRequest = _os_.ReadBool();
                long sid = _os_.ReadLong();
                long resultCode = _os_.ReadLong();
                long argumentBeanTypeId;
                string argument;
                if (IsRequest)
                {
                    argumentBeanTypeId = pa.ArgumentBeanTypeId;
                    argument = "Argument";
                }
                else
                {
                    argumentBeanTypeId = pa.ResultBeanTypeId;
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
                DecodeBean(_os_, argumentBeanTypeId);
                Lua.SetTable(-3);
            }
            else
            {
                Lua.PushString("ResultCode");
                Lua.PushInteger(_os_.ReadLong());
                Lua.SetTable(-3);
                Lua.PushString("Argument");
                DecodeBean(_os_, pa.ArgumentBeanTypeId);
                Lua.SetTable(-3);
            }

            Lua.Call(1, 1);
            bool result = false;
            if (!Lua.IsNil(-1))
                result = Lua.ToBoolean(-1);
            Lua.Pop(1);
            return result;
        }

        void DecodeBean(ByteBuffer bb, long typeId)
        {
            BeanMetas.TryGetValue(typeId, out var beanMeta);
            Lua.CreateTable(0, 32);
            for (int id = 0;;)
            {
                int t = bb.ReadByte();
                if (t == 0)
                    return;
                id += bb.ReadTagSize(t);
                Lua.PushInteger(id);
                DecodeVariable(bb, id, t, beanMeta);
                Lua.SetTable(-3);
            }
        }

        void DecodeVariable(ByteBuffer bb, int id, int type, BeanMeta beanMeta)
        {
            switch (type)
            {
                case ByteBuffer.INTEGER:
                    VariableMeta varMeta = beanMeta?.Variables.Find(x => x.Id == id);
                    if (varMeta != null && varMeta.Type == ByteBuffer.LUA_BOOL)
                        Lua.PushBoolean(bb.ReadBool());
                    else
                        Lua.PushInteger(bb.ReadLong());
                    break;
                case ByteBuffer.FLOAT:
                    Lua.PushNumber(bb.ReadFloat());
                    break;
                case ByteBuffer.DOUBLE:
                    Lua.PushNumber(bb.ReadDouble());
                    break;
                case ByteBuffer.BYTES:
                    Lua.PushBuffer(bb.ReadBytes());
                    break;
                case ByteBuffer.LIST:
                    int t = bb.ReadByte();
                    int n = bb.ReadTagSize(t);
                    t &= ByteBuffer.TAG_MASK;
                    varMeta = beanMeta?.Variables.Find(x => x.Id == id);
                    if (varMeta != null && varMeta.Type == ByteBuffer.LUA_SET)
                    {
                        Lua.CreateTable(0, Math.Min(n, 1000));
                        for (; n > 0; n--)
                        {
                            DecodeVariable(bb, 0, t, null);
                            Lua.PushInteger(0);
                            Lua.SetTable(-3);
                        }
                    }
                    else
                    {
                        Lua.CreateTable(Math.Min(n, 1000), 0);
                        for (int i = 1; i <= n; i++) // 从1开始？
                        {
                            Lua.PushInteger(i);
                            DecodeVariable(bb, 0, t, null);
                            Lua.SetTable(-3);
                        }
                    }
                    break;
                case ByteBuffer.MAP:
                    t = bb.ReadByte();
                    int s = t >> ByteBuffer.TAG_SHIFT;
                    t &= ByteBuffer.TAG_MASK;
                    n = bb.ReadUInt();
                    Lua.CreateTable(0, Math.Min(n, 1000));
                    for (; n > 0; n--)
                    {
                        DecodeVariable(bb, 0, s, null);
                        DecodeVariable(bb, 0, t, null);
                        Lua.SetTable(-3);
                    }
                    break;
                case ByteBuffer.BEAN:
                    varMeta = beanMeta?.Variables.Find(x => x.Id == id);
                    DecodeBean(bb, varMeta != null ? varMeta.TypeBeanTypeId : 0);
                    break;
                case ByteBuffer.DYNAMIC:
                    long beanTypeId = bb.ReadLong();
                    DecodeBean(bb, beanTypeId);
                    // 动态bean额外把TypeId加到变量里面。总是使用varid==0表示。程序可以使用这个动态判断是哪个具体的bean。
                    Lua.PushInteger(0);
                    Lua.PushInteger(beanTypeId);
                    Lua.SetTable(-3);
                    break;
                default:
                    throw new Exception("Unknown Tag Type");
            }
        }

        Dictionary<long, ByteBuffer> ToLuaBuffer = new();
        Dictionary<long, IFromLua> ToLuaHandshakeDone = new();
        Dictionary<long, IFromLua> ToLuaSocketClose = new();
        HashSet<long> ToLuaRpcTimeout = new();

        internal void SetRpcTimeout(long sid)
        {
            lock (this)
            {
                ToLuaRpcTimeout.Add(sid);
            }
        }

        internal void SetHandshakeDone(long socketSessionId, IFromLua service)
        {
            lock (this)
            {
                ToLuaHandshakeDone[socketSessionId] = service;
            }
        }

        internal void SetSocketClose(long socketSessionId, IFromLua service)
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

        public void Update(Service service)
        {
            Dictionary<long, IFromLua> handshakeTmp;
            Dictionary<long, IFromLua> socketCloseTmp;
            Dictionary<long, ByteBuffer> inputTmp;
            HashSet<long> rpcTimeout;
            lock (this)
            {
                handshakeTmp = ToLuaHandshakeDone;
                socketCloseTmp = ToLuaSocketClose;
                inputTmp = ToLuaBuffer;
                rpcTimeout = ToLuaRpcTimeout;
                ToLuaBuffer = new Dictionary<long, ByteBuffer>();
                ToLuaHandshakeDone = new Dictionary<long, IFromLua>();
                ToLuaSocketClose = new Dictionary<long, IFromLua>();
                ToLuaRpcTimeout = new HashSet<long>();
            }

            foreach (var e in socketCloseTmp)
                CallSocketClose(e.Value, e.Key);

            foreach (var e in handshakeTmp)
                CallHandshakeDone(e.Value, e.Key);

            foreach (var sid in rpcTimeout)
                CallRpcTimeout(sid);

            foreach (var e in inputTmp)
            {
                AsyncSocket sender = service.GetSocket(e.Key);
                if (null == sender)
                    continue;

                Protocol.Decode(service, sender, e.Value, this);
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
