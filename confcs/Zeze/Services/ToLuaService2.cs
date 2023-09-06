using System;
using System.Collections.Generic;
using System.Threading;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Services.ToLuaService2;

// <summary>
// 在lua线程中调用，一般实现：
// 0 创建lua线程，
// 1 创建 ToLua 实例。
// 2 调用具体 Service.InitializeLua 初始化
// 3 调用lua.main进入lua代码
// 4 在lua.main中回调每个 Service.InitializeLua 中注册的方法回调
// * lua 热更的话需要建议重新创建 ToLua ，并且重新初始化（InitializeLua）。重用 ToLua 的话，需要调用一次 ToLua.LoadMeta();
// </summary>
namespace Zeze.Services
{
    interface IFromLua2
    {
        string Name { get; } // Service Name
        Service Service { get; }
        ToLua ToLua { get; }
    }

    public class ToLuaServiceClient2 : HandshakeClient, IFromLua2
    {
        public Service Service => this;
        public ToLua ToLua { get; } = new ToLua();
        readonly bool clientFirst;

        public ToLuaServiceClient2(string name, Config config, bool clientFirst = true) : base(name, config)
        {
            this.clientFirst = clientFirst;
        }

        public void InitializeLua(ILua iLua)
        {
            ToLua.InitializeLua(iLua);
        }

        public override void OnSocketConnected(AsyncSocket so)
        {
            base.OnSocketConnected(so);

            if (clientFirst)
            {
                // todo 这里先让客户端发起流程，不适用SHandShake0 触发
                var p = new Handshake.SHandshake0Argument
                {
                    EncryptType = Constant.eEncryptTypeAes,
                    CompressS2c = Constant.eCompressTypeMppc,
                    CompressC2s = Constant.eCompressTypeMppc
                };
                StartHandshake(p, so);
            }
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
            if (ToLua.callThreadId == Thread.CurrentThread.ManagedThreadId && ToLua.UpdateLuaState != default) // 在主线程中主动关闭网络，直接同步返回回调
                ToLua.CallSocketClose(ToLua.UpdateLuaState, this, so.SessionId);
            else
                ToLua.SetSocketClose(so.SessionId, this);
            base.OnSocketClose(so, e);
        }
    }

    // 完全 ToLuaServiceClient，由于 c# 无法写 class S<T> : T where T : Net.Service，复制一份.
    public class ToLuaServiceServer2 : HandshakeServer, IFromLua2
    {
        public Service Service => this;
        public ToLua ToLua { get; } = new ToLua();

        public ToLuaServiceServer2(string name, Config config) : base(name, config)
        {
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

namespace Zeze.Services.ToLuaService2
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

    public enum LuaDefine
    {
        RefNil = -1,
        NoRef = -2
    }

    public interface ILua
    {
        void PushNil(IntPtr luaState);
        void PushBoolean(IntPtr luaState, bool v);
        void PushInteger(IntPtr luaState, long l);
        void PushLong(IntPtr luaState, long l);
        void PushNumber(IntPtr luaState, double number);
        void PushString(IntPtr luaState, string str);
        void PushBuffer(IntPtr luaState, byte[] buffer);
        void PushObject(IntPtr luaState, object obj);
        void PushValue(IntPtr luaState, int index);
        void Pop(IntPtr luaState, int n);
        void Replace(IntPtr luaState, int index);
        int GetTop(IntPtr luaState);
        bool CheckStack(IntPtr luaState, int extra);

        LuaType GetType(IntPtr luaState, int index);
        bool IsNil(IntPtr luaState, int index);
        bool IsTable(IntPtr luaState, int index);
        bool ToBoolean(IntPtr luaState, int index);
        long ToInteger(IntPtr luaState, int index);
        long ToLong(IntPtr luaState, int index);
        double ToNumber(IntPtr luaState, int index);
        string ToString(IntPtr luaState, int index);
        IntPtr ToLString(IntPtr luaState, int index, out IntPtr strLen);
        byte[] ToBuffer(IntPtr luaState, int index);
        T ToObject<T>(IntPtr luaState, int index);

        void CreateTable(IntPtr luaState, int elements, int records);
        void GetTable(IntPtr luaState, int index);
        void SetTable(IntPtr luaState, int index);
        int GetMetatable(IntPtr luaState, int index);
        void SetMetatable(IntPtr luaState, int index);
        void GetField(IntPtr luaState, int index, string name);
        void RawGet(IntPtr luaState, int index);
        void RawGetI(IntPtr luaState, int index, int n);
        bool Next(IntPtr luaState, int index);
        LuaType GetGlobal(IntPtr luaState, string name);

        int LuaRegistryIndex { get; }
        int LuaL_ref(IntPtr luaState, int t); // [-1, +0, m]
        void LuaL_unref(IntPtr luaState, int t, int reference); // [-0, +0, -]

        int PCallPrepare(IntPtr luaState, int funcRef);
        void PCall(IntPtr luaState, int arguments, int results, int errFunc);
        bool DoString(IntPtr luaState, string str);
    }

    public class ToLua : Protocol.IDecodeAndDispatch
    {
        class DynamicMeta
        {
            public readonly Dictionary<long, BeanMeta> SpecialTypeIdToBean = new Dictionary<long, BeanMeta>();
            public readonly Dictionary<long, long> BeanToSpecialTypeId = new Dictionary<long, long>();
        }

        class VariableMeta
        {
            public int Id;
            public string Name;
            public int Type;
            public long TypeBeanTypeId;
            public int KeyType;
            public long KeyBeanTypeId;
            public int ValueType;
            public long ValueBeanTypeId;
            public DynamicMeta DynamicMeta;

            public override string ToString() => $"{{[{Id}]={{{Type},{TypeBeanTypeId},{KeyType},{KeyBeanTypeId}," +
                                                 $"{ValueType},{ValueBeanTypeId},\"{Name}\"}}}}";
        }

        class ProtocolArgument
        {
#if UNITY_2017_1_OR_NEWER
            public string ProtocolName;
#endif
            public long ArgumentBeanTypeId;
            public long ResultBeanTypeId;
            public bool IsRpc;
            public int MetatableRef;
            public int CacheRef = (int)LuaDefine.NoRef; // 存放缓存的table
            public List<int> cacheRefPool;
        }

        class BeanMeta
        {
            public int MetatableRef;
            public readonly SortedDictionary<int, VariableMeta> Variables = new SortedDictionary<int, VariableMeta>();
            public List<int> cacheRefPool;
        }

        class ToLuaVariable
        {
            public readonly Dictionary<long, ByteBuffer> toLuaBuffer = new Dictionary<long, ByteBuffer>();
            public readonly Dictionary<long, IFromLua2> toLuaHandshakeDone = new Dictionary<long, IFromLua2>();
            public readonly Dictionary<long, IFromLua2> toLuaSocketClose = new Dictionary<long, IFromLua2>();

            public void Clear()
            {
                toLuaBuffer.Clear();
                toLuaHandshakeDone.Clear();
                toLuaSocketClose.Clear();
            }
        }

        ILua Lua;

        readonly Dictionary<long, BeanMeta> beanMetas = new Dictionary<long, BeanMeta>(); // Bean.TypeId -> vars
        readonly Dictionary<long, ProtocolArgument> protocolMetas = new Dictionary<long, ProtocolArgument>(); // protocol.TypeId -> Bean.TypeId

        //这个地方和服务器实现有一点点区别，把vector3 等类型做成了配置的类型，而不是像服务的基础类型
        readonly Dictionary<int, BeanMeta> structMetas = new Dictionary<int, BeanMeta>(); // Bean.TypeId -> vars

        // ReSharper disable UnassignedField.Global
        public int OnSocketConnected;
        public int OnSocketClosed;
        public int OnReceiveProtocol;
        // ReSharper restore UnassignedField.Global

        int tableRefId = (int)LuaDefine.NoRef;

        ToLuaVariable toLuaVariableUpdating = new ToLuaVariable();
        ToLuaVariable toLuaVariable = new ToLuaVariable();
        public IntPtr UpdateLuaState;
        public int callThreadId = -1;

        public void InitializeLua(ILua lua)
        {
            Lua = lua;
        }

        // 因为要给lua对象添加引用，所以要记得删除对应引用
        // ReSharper disable once UnusedMember.Local
        void FreeLuaMeta(IntPtr luaState)
        {
            if (tableRefId != (int)LuaDefine.NoRef)
            {
                Lua.LuaL_unref(luaState, Lua.LuaRegistryIndex, tableRefId);
                tableRefId = (int)LuaDefine.NoRef;
            }
            beanMetas.Clear();
            protocolMetas.Clear();
            structMetas.Clear();
        }

        static int VariableType(string variableType, IReadOnlyDictionary<string, long> beanName2BeanId, out long beanId)
        {
            beanId = 0;
            switch (variableType)
            {
                case "bool": return ByteBuffer.LUA_BOOL;
                case "byte":
                case "short":
                case "int":
                case "long": return ByteBuffer.INTEGER;
                case "float": return ByteBuffer.FLOAT;
                case "double": return ByteBuffer.DOUBLE;
                case "binary":
                case "string": return ByteBuffer.BYTES;
                case "list":
                case "set": return ByteBuffer.LIST;
                case "map": return ByteBuffer.MAP;
                case "dynamic": return ByteBuffer.DYNAMIC;
                case "vector2": return ByteBuffer.VECTOR2;
                case "vector2int": return ByteBuffer.VECTOR2INT;
                case "vector3": return ByteBuffer.VECTOR3;
                case "vector3int": return ByteBuffer.VECTOR3INT;
                case "vector4":
                case "quaternion": return ByteBuffer.VECTOR4;
                default:
                    if (beanName2BeanId.TryGetValue(variableType, out beanId))
                        return ByteBuffer.BEAN;
                    throw new Exception($"error bean type not define {variableType}");
            }
        }

        void ParseVariableMeta(IntPtr luaState, IReadOnlyDictionary<string, long> beanName2BeanId,
            ICollection<DynamicMeta> dynamicMetas, IDictionary<int, VariableMeta> variables)
        {
            Lua.GetField(luaState, -1, "variables"); // variables
            for (Lua.PushNil(luaState); Lua.Next(luaState, -2); Lua.Pop(luaState, 1)) // -1 value of varMeta(table) -2 key of varId
            {
                string variableName = Lua.ToString(luaState, -2);
                Lua.GetField(luaState, -1, "id");
                int variableId = (int)Lua.ToInteger(luaState, -1);
                Lua.Pop(luaState, 1);
                Lua.GetField(luaState, -1, "type");
                string variableTypeStr = Lua.ToString(luaState, -1);
                Lua.Pop(luaState, 1);
                int variableType = VariableType(variableTypeStr, beanName2BeanId, out long variableBeanTypeId);
                var variable = new VariableMeta
                {
                    Id = variableId,
                    Name = variableName,
                    Type = variableType,
                    TypeBeanTypeId = variableBeanTypeId
                };
                switch (variableType)
                {
                    // case ByteBuffer.SET:
                    case ByteBuffer.LIST:
                    {
                        Lua.GetField(luaState, -1, "value");
                        string variableValueStr = Lua.ToString(luaState, -1);
                        Lua.Pop(luaState, 1);
                        int variableValueType = VariableType(variableValueStr, beanName2BeanId,
                            out long variableValueTypeId);
                        variable.ValueType = variableValueType;
                        variable.ValueBeanTypeId = variableValueTypeId;
                        break;
                    }
                    case ByteBuffer.MAP:
                    {
                        Lua.GetField(luaState, -1, "value");
                        string variableValueStr = Lua.ToString(luaState, -1);
                        Lua.Pop(luaState, 1);
                        int variableValueType = VariableType(variableValueStr, beanName2BeanId,
                            out long variableValueTypeId);
                        variable.ValueType = variableValueType;
                        variable.ValueBeanTypeId = variableValueTypeId;
                        Lua.GetField(luaState, -1, "key");
                        string variableKeyStr = Lua.ToString(luaState, -1);
                        Lua.Pop(luaState, 1);
                        int variableKeyType = VariableType(variableKeyStr, beanName2BeanId, out long variableKeyTypeId);
                        variable.KeyType = variableKeyType;
                        variable.KeyBeanTypeId = variableKeyTypeId;
                        break;
                    }
                    case ByteBuffer.DYNAMIC:
                    {
                        var dynamicMeta = new DynamicMeta();
                        Lua.GetField(luaState, -1, "dynamcic_meta");
                        for (Lua.PushNil(luaState); Lua.Next(luaState, -2); Lua.Pop(luaState, 1))
                        {
                            long dynamicId = Convert.ToInt64(Lua.ToString(luaState, -2));
                            string dynamicType = Lua.ToString(luaState, -1);
                            if (!beanName2BeanId.TryGetValue(dynamicType, out var dynamicTypeId))
                                throw new Exception($"error dynamic bean type not define {dynamicType}");
                            dynamicMeta.BeanToSpecialTypeId[dynamicTypeId] = dynamicId;
                            variable.DynamicMeta = dynamicMeta;
                            dynamicMetas.Add(dynamicMeta);
                        }
                        Lua.Pop(luaState, 1);
                        break;
                    }
                }
                variables.Add(variable.Id, variable);
            }
        }

        public void LoadMeta(IntPtr luaState)
        {
            FreeLuaMeta(luaState);

            if (!Lua.IsTable(luaState, -1))
                throw new Exception("ZezeMeta not return a table");
            Lua.CreateTable(luaState, 0, 0); // LuaL_ref 使用
            tableRefId = Lua.LuaL_ref(luaState, Lua.LuaRegistryIndex);
            Lua.RawGetI(luaState, Lua.LuaRegistryIndex, tableRefId); // [table, refTable]

            Lua.GetField(luaState, -2, "beans"); // [table, refTable, table.beans]
            var beanName2BeanId = new Dictionary<string, long>();
            var dynamicMetas = new List<DynamicMeta>();
            for (Lua.PushNil(luaState); Lua.Next(luaState, -2); Lua.Pop(luaState, 2)) // [table, refTable, table.beans, beanName, bean]
            {
                string beanName = Lua.ToString(luaState, -2);
                Lua.GetField(luaState, -1, "type_id"); // [table, refTable, table.beans, beanName, bean, beanTypeId]
                long beanTypeId = Convert.ToInt64(Lua.ToString(luaState, -1)); // 获取BeanId
                beanName2BeanId[beanName] = beanTypeId;
            }
            for (Lua.PushNil(luaState); Lua.Next(luaState, -2); Lua.Pop(luaState, 2)) // [table, refTable, table.beans, beanName, bean]
            {
                // string beanName = Lua.ToString(luaState, -2);
                Lua.GetField(luaState, -1, "type_id"); // [table, refTable, table.beans, beanName, bean, beanTypeId]
                long beanTypeId = Convert.ToInt64(Lua.ToString(luaState, -1)); // 获取BeanId
                Lua.Pop(luaState, 1); // [table, refTable, table.beans, beanName, bean]
                Lua.GetField(luaState, -1, "metatable"); // [table, refTable, table.beans, beanName, bean, bean.metatable]
                int metatableRef = Lua.LuaL_ref(luaState, -5); // [table, refTable, table.beans, beanName, bean]
                var beanMeta = new BeanMeta
                {
                    MetatableRef = metatableRef
                };
                ParseVariableMeta(luaState, beanName2BeanId, dynamicMetas, beanMeta.Variables);
                beanMetas.Add(beanTypeId, beanMeta);
            }
            Lua.Pop(luaState, 1); // pop beans

            Lua.GetField(luaState, -2, "structs"); // [table, refTable, table.structs]
            for (Lua.PushNil(luaState); Lua.Next(luaState, -2); Lua.Pop(luaState, 2)) // [table, refTable, table.structs, structName, struct]
            {
                // string beanName = Lua.ToString(luaState, -2);
                Lua.GetField(luaState, -1, "type_id"); // [table, refTable, table.structs, structName, struct, structTypeId]
                int typeId = Convert.ToInt32(Lua.ToString(luaState, -1)); // 获取StructId
                Lua.Pop(luaState, 1); // [table, refTable, table.structs, structName, struct]
                Lua.GetField(luaState, -1, "metatable"); // [table, refTable, table.structs, structName, struct, struct.metatable]
                int metatableRef = Lua.LuaL_ref(luaState, -5); // [table, refTable, table.structs, structName, struct]
                var beanMeta = new BeanMeta
                {
                    MetatableRef = metatableRef
                };
                ParseVariableMeta(luaState, beanName2BeanId, dynamicMetas, beanMeta.Variables);
                structMetas.Add(typeId, beanMeta);
            }
            Lua.Pop(luaState, 1); // pop structs

            Lua.GetField(luaState, -2, "protocols"); // [table, refTable, table.protocols]
            for (Lua.PushNil(luaState); Lua.Next(luaState, -2); Lua.Pop(luaState, 5)) // [table, refTable, table.protocols, protocolName, protocol]
            {
#if UNITY_2017_1_OR_NEWER
                string protocolName = Lua.ToString(luaState, -2);
#endif
                Lua.GetField(luaState, -1, "metatable");
                int metatableRef = Lua.LuaL_ref(luaState, -5); // metatable
                Lua.GetField(luaState, -1, "id");
                // long id = Convert.ToInt64(Lua.ToString(luaState, -1));
                Lua.GetField(luaState, -2, "type_id");
                long typeId = Convert.ToInt64(Lua.ToString(luaState, -1));
                Lua.GetField(luaState, -3, "argument");
                string argumentType = Lua.ToString(luaState, -1);
                Lua.GetField(luaState, -4, "result");
                if (!beanName2BeanId.TryGetValue(argumentType, out var argumentTypeId))
                    throw new Exception($"error bean type not define {argumentTypeId}");

                var pa = new ProtocolArgument
                {
#if UNITY_2017_1_OR_NEWER
                    ProtocolName = protocolName,
#endif
                    ArgumentBeanTypeId = argumentTypeId,
                    MetatableRef = metatableRef
                };
                if (!Lua.IsNil(luaState, -1)) // 存在result就把他视为rpc协议
                {
                    if (!beanName2BeanId.TryGetValue(Lua.ToString(luaState, -1), out var resultTypeId))
                        throw new Exception($"error bean type not define {resultTypeId}");
                    pa.ResultBeanTypeId = resultTypeId;
                    pa.IsRpc = true;
                }
                protocolMetas.Add(typeId, pa);
            }
            Lua.Pop(luaState, 1); // pop protocols

            foreach (var dynamicMeta in dynamicMetas)
            {
                foreach (var keyValuePair in dynamicMeta.BeanToSpecialTypeId)
                    dynamicMeta.SpecialTypeIdToBean[keyValuePair.Value] = beanMetas[keyValuePair.Key];
            }
        }

        internal void CallSocketClose(IntPtr luaState, IFromLua2 service, long socketSessionId)
        {
            if (OnSocketClosed == 0)
            {
                Lua.Pop(luaState, 1);
                return;
            }

            int errFunc = Lua.PCallPrepare(luaState, OnSocketClosed);
            Lua.PushObject(luaState, service);
            Lua.PushLong(luaState, socketSessionId);
            Lua.PCall(luaState, 2, 0, errFunc);
        }

        internal void CallHandshakeDone(IntPtr luaState, IFromLua2 service, long socketSessionId)
        {
            if (OnSocketConnected == 0)
            {
                Lua.Pop(luaState, 1);
                return;
            }

            int errFunc = Lua.PCallPrepare(luaState, OnSocketConnected);
            Lua.PushObject(luaState, service);
            Lua.PushLong(luaState, socketSessionId);
            Lua.PCall(luaState, 2, 0, errFunc);
        }

        public void SendProtocol(IntPtr luaState, AsyncSocket socket)
        {
            if (!Lua.IsTable(luaState, -1))
                throw new Exception("SendProtocol param is not a table.");

            Lua.GetField(luaState, -1, "moduleId");
            int moduleId = (int)Lua.ToInteger(luaState, -1);
            Lua.Pop(luaState, 1);
            Lua.GetField(luaState, -1, "protocolId");
            int protocolId = (int)Lua.ToInteger(luaState, -1);
            Lua.Pop(luaState, 1);
            Lua.GetField(luaState, -1, "resultCode");
            int resultCode = (int)Lua.ToInteger(luaState, -1);
            Lua.Pop(luaState, 1);

            long type = Protocol.MakeTypeId(moduleId, protocolId);
            if (!protocolMetas.TryGetValue(type, out var pa))
                throw new Exception($"protocol not found in meta. ({moduleId},{protocolId}, {type})");

            if (pa.IsRpc)
            {
                Lua.GetField(luaState, -1, "isRequest");
                bool isRequest = Lua.ToBoolean(luaState, -1);
                Lua.Pop(luaState, 1);
                Lua.GetField(luaState, -1, "sessionId");
                long sid = Lua.ToInteger(luaState, -1);
                Lua.Pop(luaState, 1);
                // Lua.GetField(luaState, -1, "Timeout");
                // int timeout = (int)Lua.ToInteger(luaState, -1);
                // Lua.Pop(luaState, 1);

                long argumentBeanTypeId;
                string argumentName;
                if (isRequest)
                {
                    argumentBeanTypeId = pa.ArgumentBeanTypeId;
                    argumentName = "argument";
                }
                else
                {
                    argumentBeanTypeId = pa.ResultBeanTypeId;
                    argumentName = "result";
                }

                // see Rpc.Encode
                int compress = isRequest ? FamilyClass.Request : FamilyClass.Response;
                if (resultCode != 0)
                    compress |= FamilyClass.BitResultCode;

                var bb = ByteBuffer.Allocate();
                bb.WriteInt4(moduleId);
                bb.WriteInt4(protocolId);
                bb.BeginWriteWithSize4(out var outState);

                bb.WriteInt(compress); // FamilyClass
                if (resultCode != 0)
                    bb.WriteLong(resultCode);
                bb.WriteLong(sid);
                Lua.GetField(luaState, -1, argumentName);
                EncodeBean(luaState, bb, argumentBeanTypeId);
                Lua.Pop(luaState, 1);
                bb.EndWriteWithSize4(outState);
                socket.Send(bb);

                // if (timeout > 0)
                //     Scheduler.Instance.Schedule((thisTask) => { SetRpcTimeout(sid); }, timeout);
            }
            else
            {
                // see Protocol.Encode
                int compress = FamilyClass.Protocol;
                if (resultCode != 0)
                    compress |= FamilyClass.BitResultCode;

                var bb = ByteBuffer.Allocate();
                bb.WriteInt4(moduleId);
                bb.WriteInt4(protocolId);
                bb.BeginWriteWithSize4(out var state);

                bb.WriteInt(compress);
                if (resultCode != 0)
                    bb.WriteLong(resultCode);

                Lua.GetField(luaState, -1, "argument");
                EncodeBean(luaState, bb, pa.ArgumentBeanTypeId);
                Lua.Pop(luaState, 1);
                bb.EndWriteWithSize4(state);
                socket.Send(bb);
            }
        }

        void EncodeStruct(IntPtr luaState, ByteBuffer bb, BeanMeta beanMeta, int index = -1) // [table]
        {
            if (!Lua.IsTable(luaState, -1))
                throw new Exception("EncodeStruct need a table");
            if (!Lua.CheckStack(luaState, 1))
                throw new Exception("Lua stack overflow!");

            foreach (var v in beanMeta.Variables.Values)
            {
                // 这里使用string 类型，是为了让lua可以传 k：v格式的简单table，也可以通过new 的方式来携带类型信息，保证dynamic类型
                // get table 方法是会trigger metatable 的，所以只通过string 类型访问就可以了，先这么做，以后再进行测试性能
                Lua.PushString(luaState, v.Name); // [table, name]
                Lua.RawGet(luaState, -1 + index); // [table, value]
                EncodeVariable(luaState, bb, v); // [table, value]
                Lua.Pop(luaState, 1); // [table]
            }
        }

        void EncodeBean(IntPtr luaState, ByteBuffer bb, long beanTypeId, int index = -1)
        {
            if (!Lua.IsTable(luaState, -1))
                throw new Exception("EncodeBean need a table");
            if (!beanMetas.TryGetValue(beanTypeId, out var beanMeta))
                throw new Exception("bean not found in meta for beanTypeId=" + beanTypeId);
            if (!Lua.CheckStack(luaState, 1))
                throw new Exception("Lua stack overflow!");

            int lastId = 0;
            foreach (var v in beanMeta.Variables.Values)
            {
                // 这里使用string 类型，是为了让lua可以传 k：v格式的简单table，也可以通过new 的方式来携带类型信息，保证dynamic类型
                // get table 方法是会trigger metatable 的，所以只通过string 类型访问就可以了，先这么做，以后再进行测试性能
                Lua.PushString(luaState, v.Name);
                Lua.RawGet(luaState, -1 + index);
                if (!Lua.IsNil(luaState, -1)) // allow var not set
                {
                    // bb.WriteInt(v.Type | v.ID << ByteBuffer.TAG_SHIFT);
                    bb.WriteTag(lastId, v.Id, v.Type & ByteBuffer.TAG_MASK);
                    EncodeVariable(luaState, bb, v);
                    lastId = v.Id;
                }
                Lua.Pop(luaState, 1);
            }
            bb.WriteByte(0);
        }

        int EncodeGetTableLength(IntPtr luaState) // [table]
        {
            if (!Lua.IsTable(luaState, -1))
                throw new Exception("EncodeGetTableLength: not a table");

            int len = 0;
            for (Lua.PushNil(luaState); Lua.Next(luaState, -2); Lua.Pop(luaState, 1)) // [table, key, value]
                len++;
            return len; // [table]
        }

        void EncodeVariable(IntPtr luaState, ByteBuffer bb, VariableMeta v, int index = -1)
        {
            switch (v.Type)
            {
                case ByteBuffer.LUA_BOOL:
                    bb.WriteBool(Lua.ToBoolean(luaState, index));
                    break;
                case ByteBuffer.INTEGER:
                    bb.WriteLong(Lua.ToInteger(luaState, index));
                    break;
                case ByteBuffer.FLOAT:
                    bb.WriteFloat((float)Lua.ToNumber(luaState, index));
                    break;
                case ByteBuffer.DOUBLE:
                    bb.WriteDouble(Lua.ToNumber(luaState, index));
                    break;
                case ByteBuffer.BYTES:
                    bb.WriteBytes(Lua.ToBuffer(luaState, index));
                    break;
                case ByteBuffer.LIST:
                {
                    if (!Lua.IsTable(luaState, index))
                        throw new Exception("list must be a table");
                    if (v.Id <= 0)
                        throw new Exception("list cannot define in collection");
                    if (!Lua.CheckStack(luaState, 2))
                        throw new Exception("Lua stack overflow!");
                    int n = EncodeGetTableLength(luaState);
                    bb.WriteListType(n, v.ValueType & ByteBuffer.TAG_MASK);
                    var meta = new VariableMeta { Type = v.ValueType, TypeBeanTypeId = v.ValueBeanTypeId };
                    for (Lua.PushNil(luaState); Lua.Next(luaState, index - 1); Lua.Pop(luaState, 1)) // [table, key, value]
                    {
                        // 这里应该进行修改还没想好该怎么改~~~，先保留
                        EncodeVariable(luaState, bb, meta);
                    }
                    break;
                }
                case ByteBuffer.MAP:
                {
                    if (!Lua.IsTable(luaState, index))
                        throw new Exception("map must be a table");
                    if (v.Id <= 0)
                        throw new Exception("map cannot define in collection");
                    if (!Lua.CheckStack(luaState, 2))
                        throw new Exception("Lua stack overflow!");
                    int n = EncodeGetTableLength(luaState);
                    bb.WriteMapType(n, v.KeyType & ByteBuffer.TAG_MASK, v.ValueType & ByteBuffer.TAG_MASK);
                    var keyMeta = new VariableMeta { Type = v.KeyType, TypeBeanTypeId = v.KeyBeanTypeId };
                    var valueMeta = new VariableMeta { Type = v.ValueType, TypeBeanTypeId = v.ValueBeanTypeId };
                    for (Lua.PushNil(luaState); Lua.Next(luaState, index - 1); Lua.Pop(luaState, 1))
                    {
                        EncodeVariable(luaState, bb, keyMeta, -2);
                        EncodeVariable(luaState, bb, valueMeta);
                    }
                    break;
                }
                case ByteBuffer.BEAN:
                {
                    // if (v.ID > 0)
                    // {
                    //     os.BeginWriteSegment(out var state);
                    //     EncodeBean(luaState,os, v.TypeBeanTypeId, index);
                    //     os.EndWriteSegment(state);
                    // }
                    // else // in collection. direct encode
                    EncodeBean(luaState, bb, v.TypeBeanTypeId);
                    break;
                }
                case ByteBuffer.DYNAMIC:
                {
                    if (v.Id <= 0)
                        throw new Exception("dynamic cannot define in collection");
                    if (!Lua.CheckStack(luaState, 1))
                        throw new Exception("Lua stack overflow!");
                    Lua.GetField(luaState, index, "__type_id__");
                    if (Lua.IsNil(luaState, -1))
                    {
                        Lua.Pop(luaState, 1);
                        Lua.GetField(luaState, index, "__type_name__");
                        string id = Lua.ToString(luaState, -1);
                        throw new Exception($"'__type_id__' not found. dynamic bean needed. {v.Name} {id}");
                    }

                    // 在lua就处理好了相应的类型转换，可以做到协议的生成里，不想把这么特殊的代码保持在c#中
                    var dynamicBeanId = LuaStringToInt64(luaState, -1);
                    Lua.Pop(luaState, 1);
                    bb.WriteLong(dynamicBeanId);
                    if (dynamicBeanId != 0) // 不是empty bean
                    {
                        // os.BeginWriteSegment(out var state);
                        EncodeBean(luaState, bb, dynamicBeanId, index);
                        // os.EndWriteSegment(state);
                    }
                    else
                        bb.WriteByte(0); // empty bean
                    break;
                }
                case ByteBuffer.VECTOR2:
                case ByteBuffer.VECTOR2INT:
                case ByteBuffer.VECTOR3:
                case ByteBuffer.VECTOR3INT:
                case ByteBuffer.VECTOR4:
                {
                    var meta = GetStructMeta(v.Type);
                    if (meta == null)
                        throw new Exception("undefined meta for vector type=" + v.Type);
                    EncodeStruct(luaState, bb, meta, index);
                    break;
                }
                default:
                    throw new Exception("Unknown Tag Type: " + v.Type);
            }
        }

        public bool DecodeAndDispatch(IntPtr luaState, Service service, long typeId, ByteBuffer os)
        {
            int errFunc = Lua.PCallPrepare(luaState, OnReceiveProtocol);

            if (!protocolMetas.TryGetValue(typeId, out var pa))
            {
                throw new Exception($"protocol not found in meta for typeId={typeId} moduleId=" +
                                    $"{Protocol.GetModuleId(typeId)} protocolId={Protocol.GetProtocolId(typeId)}");
            }

            int cacheRef = pa.CacheRef;
            if (cacheRef != (int)LuaDefine.NoRef)
                Lua.RawGetI(luaState, Lua.LuaRegistryIndex, cacheRef);
            else
            {
                int n;
                var pool = pa.cacheRefPool;
                if (pool != null && (n = pool.Count) > 0)
                {
                    cacheRef = pool[--n];
                    pool.RemoveAt(n);
                    Lua.RawGetI(luaState, Lua.LuaRegistryIndex, cacheRef);
                    Lua.LuaL_unref(luaState, Lua.LuaRegistryIndex, cacheRef);
                }
                else
                    Lua.CreateTable(luaState, 0, 8);
            }

            if (service is IFromLua2 fromLua) // 必须是，不报错了。
            {
                Lua.PushString(luaState, "Service");
                Lua.PushObject(luaState, fromLua);
                Lua.SetTable(luaState, -3);
            }

            if (pa.IsRpc)
            {
                int compress = os.ReadInt();
                int familyClass = compress & FamilyClass.FamilyClassMask; // lua需要的话，Push，但懒得看table索引，先不公开了。
                bool isRequest = familyClass == FamilyClass.Request;
                long resultCode = (compress & FamilyClass.BitResultCode) != 0 ? os.ReadLong() : 0;
                long sid = os.ReadLong();
                int resultCode0 = Protocol.GetModuleId(resultCode);
                int resultCode1 = Protocol.GetProtocolId(resultCode);
                string argument;
                long beanTypeId;
                if (isRequest)
                {
                    argument = "argument";
                    beanTypeId = pa.ArgumentBeanTypeId;
                }
                else
                {
                    argument = "result";
                    beanTypeId = pa.ResultBeanTypeId;
                }

                Lua.PushString(luaState, "isRpc");
                Lua.PushBoolean(luaState, true);
                Lua.SetTable(luaState, -3);
                Lua.PushString(luaState, "isRequest");
                Lua.PushBoolean(luaState, isRequest);
                Lua.SetTable(luaState, -3);
                Lua.PushString(luaState, "sessionId");
                Lua.PushLong(luaState, sid);
                Lua.SetTable(luaState, -3);
                Lua.PushString(luaState, "resultCode");
                Lua.PushLong(luaState, resultCode1);
                Lua.SetTable(luaState, -3);
                Lua.PushString(luaState, "resultCodeModule");
                Lua.PushLong(luaState, resultCode0);
                Lua.SetTable(luaState, -3); // [table]
                Lua.PushString(luaState, argument); // [table, arg]
                if (!beanMetas.TryGetValue(beanTypeId, out var beanMeta))
                    throw new Exception("bean not found in meta for typeId=" + beanTypeId);

                try
                {
                    DecodeBean(luaState, os, beanMeta, cacheRef != (int)LuaDefine.NoRef && PushTableField(luaState));
                }
                catch (Exception e)
                {
                    throw new Exception("decode rpc typeId=" + typeId + " exception:", e);
                }
                Lua.SetTable(luaState, -3);
            }
            else
            {
                int compress = os.ReadInt();
                //int familyClass = compress & FamilyClass.FamilyClassMask; // lua需要的话，Push，但懒得看table索引，先不公开了。
                long resultCode = (compress & FamilyClass.BitResultCode) != 0 ? os.ReadLong() : 0;

                Lua.PushString(luaState, "resultCode");
                Lua.PushLong(luaState, resultCode);
                Lua.SetTable(luaState, -3); // [table]
                Lua.PushString(luaState, "argument"); // [table, arg]
                // _ = os.ReadLong();
                if (!beanMetas.TryGetValue(pa.ArgumentBeanTypeId, out var beanMeta))
                    throw new Exception("bean not found in meta for typeId=" + pa.ArgumentBeanTypeId);

                try
                {
                    DecodeBean(luaState, os, beanMeta, cacheRef != (int)LuaDefine.NoRef && PushTableField(luaState));
                }
                catch (Exception e)
                {
                    throw new Exception("decode protocol typeId=" + typeId + " exception:", e);
                }
                Lua.SetTable(luaState, -3);
            }

            Lua.RawGetI(luaState, Lua.LuaRegistryIndex, tableRefId);
            Lua.RawGetI(luaState, -1, pa.MetatableRef);
            Lua.SetMetatable(luaState, -3);
            Lua.Pop(luaState, 1); // pop tableRef

#if UNITY_2017_1_OR_NEWER
            UnityEngine.Profiling.Profiler.BeginSample(pa.ProtocolName);
#endif
            Lua.PCall(luaState, 1, 0, errFunc);
#if UNITY_2017_1_OR_NEWER
            UnityEngine.Profiling.Profiler.EndSample();
#endif

            if (pa.CacheRef != (int)LuaDefine.NoRef)
            {
                Lua.RawGetI(luaState, Lua.LuaRegistryIndex, cacheRef); // [cache]
                CleanLuaTable(luaState);
                Lua.Pop(luaState, 1);
            }
            return true;
        }

        public ByteBuffer Encode(IntPtr luaState)
        {
            if (!Lua.IsTable(luaState, -1))
                throw new Exception("Encode param is not a table.");

            var os = ByteBuffer.Allocate();
            Lua.GetField(luaState, -1, "__type_id__");
            var typeId = LuaStringToInt64(luaState, -1);
            Lua.Pop(luaState, 1);
            os.WriteLong8(typeId);
            EncodeBean(luaState, os, typeId);
            return os;
        }

        public void Decode(IntPtr luaState, ByteBuffer os)
        {
            long type = os.ReadLong8();
            if (beanMetas.TryGetValue(type, out var beanMeta))
                DecodeBean(luaState, os, beanMeta, false);
            else
                throw new Exception($"beanMeta type({type}) is not found");
        }

        void DecodeBean(IntPtr luaState, ByteBuffer bb, BeanMeta beanMeta, bool hasTable)
        {
            if (beanMeta == null)
                throw new Exception("beanMeta type is not found");
            if (!Lua.CheckStack(luaState, 3))
                throw new Exception("Lua stack overflow!");

            if (!hasTable)
            {
                int n;
                var pool = beanMeta.cacheRefPool;
                if (pool != null && (n = pool.Count) > 0)
                {
                    int cacheRef = pool[--n];
                    pool.RemoveAt(n);
                    Lua.RawGetI(luaState, Lua.LuaRegistryIndex, cacheRef); // [table]
                    Lua.LuaL_unref(luaState, Lua.LuaRegistryIndex, cacheRef); // [table]
                }
                else
                    Lua.CreateTable(luaState, 0, beanMeta.Variables.Count); // [table]
            }
            Lua.RawGetI(luaState, Lua.LuaRegistryIndex, tableRefId); // [table, tableRef]
            Lua.RawGetI(luaState, -1, beanMeta.MetatableRef); // [table, tableRef, metatableRef]
            Lua.SetMetatable(luaState, -3); // [table, tableRef]
            Lua.Pop(luaState, 1); // [table]

            int id = 0;
            using (var it = beanMeta.Variables.GetEnumerator())
            {
                for (int t; (t = bb.ReadByte()) != 0;)
                {
                    id += bb.ReadTagSize(t);
                    t &= ByteBuffer.TAG_MASK;

                    bool find = false;
                    // 写的这么恶心是因为给把空协议中的list map创建table，先fix_bug 再看看有没有好方法
                    while (it.MoveNext())
                    {
                        var c = it.Current;
                        var variableMeta = c.Value;
                        while (c.Key > id) // 发现未知id的字段
                        {
                            bb.SkipUnknownField(t);
                            if ((t = bb.ReadByte()) == 0)
                                return;
                            id += bb.ReadTagSize(t);
                            t &= ByteBuffer.TAG_MASK;
                        }
                        if (c.Key == id)
                        {
                            find = true;
                            // 这里本来想设置成int，再通过元表来访问，可是lua 5.1有一些问题，如果升级的话再改
                            // 主要是保持协议版本兼容性，否则升级协议还要进行特殊判断，记得把__next__重写
                            Lua.PushString(luaState, variableMeta.Name); // [table, name]
                            DecodeVariable(luaState, bb, t, variableMeta); // [table, name, value]
                            Lua.SetTable(luaState, -3); // [table]
                            break;
                        }
                        if (variableMeta.Type == ByteBuffer.LIST || variableMeta.Type == ByteBuffer.MAP)
                        {
                            Lua.PushString(luaState, variableMeta.Name); // [table, name]
                            if (hasTable && PushTableField(luaState)) // [table, name, value] or [table, name]
                                Lua.Pop(luaState, 2); // [table]
                            else
                            {
                                Lua.CreateTable(luaState, 0, 0); // [table, name, newTable]
                                Lua.SetTable(luaState, -3); // [table]
                            }
                        }
                    }

                    if (!find)
                    {
                        // throw new Exception($"var not found in meta for typeid={id} in bean {beanMeta.Name}");
                        bb.SkipUnknownField(t);
                    }
                }
            }
        }

        void DecodeStruct(IntPtr luaState, ByteBuffer bb, BeanMeta beanMeta, bool hasTable)
        {
            if (!hasTable)
                Lua.CreateTable(luaState, 0, beanMeta.Variables.Count); // [table]
            Lua.RawGetI(luaState, Lua.LuaRegistryIndex, tableRefId); // [table, tableRef]
            Lua.RawGetI(luaState, -1, beanMeta.MetatableRef); // [table, tableRef, metatable]
            Lua.SetMetatable(luaState, -3); // [table, tableRef]
            Lua.Pop(luaState, 1); // [table]

            foreach (var variablesValue in beanMeta.Variables.Values)
            {
                Lua.PushString(luaState, variablesValue.Name); // [table, name]
                DecodeVariable(luaState, bb, variablesValue.Type, variablesValue); // [table, name, value]
                Lua.SetTable(luaState, -3); // [table]
            }
        }

        BeanMeta GetBeanMeta(int type, long beanId)
        {
            switch (type)
            {
                case ByteBuffer.BEAN:
                    if (beanMetas.TryGetValue(beanId, out var beanMeta))
                        return beanMeta;
                    break;
            }
            return null;
        }

        BeanMeta GetStructMeta(int type)
        {
            return structMetas.TryGetValue(type, out var beanMeta) ? beanMeta : null;
        }

        void DecodeVariable(IntPtr luaState, ByteBuffer bb, int tagType, VariableMeta varMeta, BeanMeta beanMeta = null,
            bool isKV = false) // isKV ? [table] : [table, key]
        {
            switch (tagType)
            {
                case ByteBuffer.LUA_BOOL:
                    Lua.PushBoolean(luaState, bb.ReadBool());
                    break;
                case ByteBuffer.INTEGER:
                    if (varMeta.Type == ByteBuffer.LUA_BOOL)
                        Lua.PushBoolean(luaState, bb.ReadBool());
                    else
                        Lua.PushInteger(luaState, bb.ReadLong());
                    break;
                case ByteBuffer.FLOAT:
                    Lua.PushNumber(luaState, bb.ReadFloat());
                    break;
                case ByteBuffer.DOUBLE:
                    Lua.PushNumber(luaState, bb.ReadDouble());
                    break;
                case ByteBuffer.BYTES:
                    Lua.PushBuffer(luaState, bb.ReadBytes());
                    break;
                case ByteBuffer.LIST:
                {
                    if (!Lua.CheckStack(luaState, 3))
                        throw new Exception("Lua stack overflow!");
                    int t = bb.ReadByte();
                    int n = bb.ReadTagSize(t);
                    if (isKV || !PushTableField(luaState))
                        Lua.CreateTable(luaState, Math.Min(n, 1000), 0);
                    var valueBeanMeta = GetBeanMeta(varMeta.ValueType, varMeta.ValueBeanTypeId);
                    for (int i = 1; i <= n; i++) // 从1开始？
                    {
                        Lua.PushInteger(luaState, i);
                        DecodeVariable(luaState, bb, varMeta.ValueType, varMeta, valueBeanMeta, true);
                        Lua.SetTable(luaState, -3);
                    }
                    break;
                }
                case ByteBuffer.MAP:
                {
                    if (!Lua.CheckStack(luaState, 3))
                        throw new Exception("Lua stack overflow!");
                    int t = bb.ReadByte();
                    int s = t >> ByteBuffer.TAG_SHIFT;
                    t &= ByteBuffer.TAG_MASK;
                    int n = bb.ReadUInt();
                    var keyBeanMeta = GetBeanMeta(s, varMeta.KeyBeanTypeId);
                    var valueBeanMeta = GetBeanMeta(t, varMeta.ValueBeanTypeId);
                    if (isKV || !PushTableField(luaState))
                        Lua.CreateTable(luaState, 0, Math.Min(n, 1000));
                    for (; n > 0; n--)
                    {
                        DecodeVariable(luaState, bb, varMeta.KeyType, varMeta, keyBeanMeta, true);
                        DecodeVariable(luaState, bb, varMeta.ValueType, varMeta, valueBeanMeta, true);
                        Lua.SetTable(luaState, -3);
                    }
                    break;
                }
                case ByteBuffer.BEAN:
                {
                    if (beanMeta == null)
                        beanMeta = GetBeanMeta(varMeta.Type, varMeta.TypeBeanTypeId);
                    DecodeBean(luaState, bb, beanMeta, !isKV && PushTableField(luaState));
                    break;
                }
                case ByteBuffer.DYNAMIC:
                {
                    bool hasTable = !isKV && PushTableField(luaState);
                    long beanTypeId = bb.ReadLong();
                    if (varMeta.DynamicMeta.SpecialTypeIdToBean.TryGetValue(beanTypeId, out var dynamicBeanMeta))
                    {
                        // os.BeginReadSegment(out var state);
                        DecodeBean(luaState, bb, dynamicBeanMeta, hasTable);
                        // os.EndReadSegment(state);
                    }
                    else
                    {
                        bb.SkipUnknownField(ByteBuffer.BEAN);
                        if (!hasTable)
                            Lua.CreateTable(luaState, 0, 0);
                    }
                    // else
                    // {
                    //     // empty bean 先特殊处理一下，反正要改到c版本去，就不管了
                    //     bb.ReadInt();
                    //     if (!hasTable)
                    //         Lua.CreateTable(luaState, 0, 0);
                    // }
                    break;
                }
                case ByteBuffer.VECTOR2:
                case ByteBuffer.VECTOR2INT:
                case ByteBuffer.VECTOR3:
                case ByteBuffer.VECTOR3INT:
                case ByteBuffer.VECTOR4:
                {
                    if (!Lua.CheckStack(luaState, 3))
                        throw new Exception("Lua stack overflow!");
                    if (beanMeta == null)
                        beanMeta = GetStructMeta(tagType);
                    DecodeStruct(luaState, bb, beanMeta, !isKV && PushTableField(luaState));
                    break;
                }
                default:
                    throw new Exception($"Unknown Tag Type {tagType} {varMeta}");
            }
        }

        internal void SetHandshakeDone(long socketSessionId, IFromLua2 service)
        {
            lock (this)
            {
                toLuaVariable.toLuaHandshakeDone[socketSessionId] = service;
            }
        }

        internal void SetSocketClose(long socketSessionId, IFromLua2 service)
        {
            lock (this)
            {
                toLuaVariable.toLuaSocketClose[socketSessionId] = service;
            }
        }

        public void AppendInputBuffer(long socketSessionId, ByteBuffer buffer)
        {
            lock (this)
            {
                if (toLuaVariable.toLuaBuffer.TryGetValue(socketSessionId, out var exist))
                {
                    exist.Append(buffer.Bytes, buffer.ReadIndex, buffer.Size);
                    return;
                }

                var newBuffer = ByteBuffer.Allocate();
                toLuaVariable.toLuaBuffer.Add(socketSessionId, newBuffer);
                newBuffer.Append(buffer.Bytes, buffer.ReadIndex, buffer.Size);
            }
        }

        public void Update(IntPtr luaState, Service service)
        {
            lock (this)
            {
                (toLuaVariable, toLuaVariableUpdating) = (toLuaVariableUpdating, toLuaVariable); // swap
                toLuaVariable.Clear(); // 其实这里已经是清空的,只是为了防御异常情况
            }

            foreach (var e in toLuaVariableUpdating.toLuaHandshakeDone)
            {
#if UNITY_2017_1_OR_NEWER
                UnityEngine.Profiling.Profiler.BeginSample("CallHandshakeDone");
#endif
                CallHandshakeDone(luaState, e.Value, e.Key);
#if UNITY_2017_1_OR_NEWER
                UnityEngine.Profiling.Profiler.EndSample();
#endif
            }

            UpdateLuaState = luaState;
            foreach (var e in toLuaVariableUpdating.toLuaBuffer)
            {
                var sender = service.GetSocket(e.Key);
                if (sender != null)
                {
#if UNITY_2017_1_OR_NEWER
                    UnityEngine.Profiling.Profiler.BeginSample("Protocol.Decode");
#endif
                    Protocol.Decode(service, sender, e.Value, this);
#if UNITY_2017_1_OR_NEWER
                    UnityEngine.Profiling.Profiler.EndSample();
#endif
                }
            }
            UpdateLuaState = default;

            // process remain buffer
#if UNITY_2017_1_OR_NEWER
            UnityEngine.Profiling.Profiler.BeginSample("_toLuaBuffer");
#endif
            lock (this)
            {
                foreach (var e in toLuaVariableUpdating.toLuaBuffer)
                {
                    if (e.Value.Size <= 0)
                        continue; // 数据全部处理完成。

                    e.Value.Campact();
                    if (toLuaVariable.toLuaBuffer.TryGetValue(e.Key, out var exist))
                    {
                        // 处理过程中有新数据到来，加到当前剩余数据后面，然后覆盖掉buffer。
                        e.Value.Append(exist.Bytes, exist.ReadIndex, exist.Size);
                        toLuaVariable.toLuaBuffer[e.Key] = e.Value;
                    }
                    else // 没有新数据到来，有剩余，加回去。下一次update再处理。
                        toLuaVariable.toLuaBuffer.Add(e.Key, e.Value);
                }
            }
#if UNITY_2017_1_OR_NEWER
            UnityEngine.Profiling.Profiler.EndSample();
#endif

            foreach (var e in toLuaVariableUpdating.toLuaSocketClose)
            {
#if UNITY_2017_1_OR_NEWER
                UnityEngine.Profiling.Profiler.BeginSample("CallSocketClose");
#endif
                CallSocketClose(luaState, e.Value, e.Key);
#if UNITY_2017_1_OR_NEWER
                UnityEngine.Profiling.Profiler.EndSample();
#endif
            }

            toLuaVariableUpdating.Clear();
        }

        public bool DecodeAndDispatch(Service service, long sessionId, long typeId, ByteBuffer os)
        {
            return UpdateLuaState != default && DecodeAndDispatch(UpdateLuaState, service, typeId, os);
        }

        // 自动回收指定协议的table,要求处理该类型协议后不能继续持有table,每个类型协议只需调用一次
        // 如果返回false,说明还没调用LoadMeta或者protoTypeId未定义
        public bool AddProtocolCache(IntPtr luaState, int moduleId, int protocolId)
        {
            if (!protocolMetas.TryGetValue(Protocol.MakeTypeId(moduleId, protocolId), out var meta))
                return false;
            Lua.CreateTable(luaState, 0, 8); // [cacheTable]
            meta.CacheRef = Lua.LuaL_ref(luaState, Lua.LuaRegistryIndex);
            return true;
        }

        // 手动回收一个协议的table,要求lua栈顶是待回收的协议table,该table会在以后接收此协议时被复用
        // 如果返回false,说明还没调用LoadMeta或者protoTypeId未定义或者该协议类型已被指定自动回收(AddProtocolCache)或者已回收过多
        public bool RecycleProtocolTable(IntPtr luaState, int moduleId, int protocolId)
        {
            if (!protocolMetas.TryGetValue(Protocol.MakeTypeId(moduleId, protocolId), out var meta)
                || meta.CacheRef != (int)LuaDefine.NoRef)
                return false;
            var pool = meta.cacheRefPool;
            if (pool == null)
                meta.cacheRefPool = pool = new List<int>();
            else if (pool.Count >= 1000)
                return false;
            CleanLuaTable(luaState);
            Lua.PushValue(luaState, -1); // [protoTable, protoTable]
            pool.Add(Lua.LuaL_ref(luaState, Lua.LuaRegistryIndex)); // [protoTable]
            return true;
        }

        bool PushTableField(IntPtr luaState) // [table, key]
        {
            Lua.PushValue(luaState, -1); // [table, key, key]
            Lua.RawGet(luaState, -3); // [table, key, value]
            if (Lua.IsTable(luaState, -1))
                return true;
            Lua.Pop(luaState, 1); // [table, key]
            return false;
        }

        bool CleanLuaTable(IntPtr luaState, int depth = 0)
        {
            var removeTable = false;
            for (Lua.PushNil(luaState); Lua.Next(luaState, -2); Lua.Pop(luaState, 1)) // [table, key, value]
            {
                if (!Lua.IsTable(luaState, -1) || depth > 16 || !Lua.CheckStack(luaState, 4) || CleanLuaTable(luaState, depth + 1)) // 确保不递归太多层,避免无限递归导致栈溢出
                {
                    Lua.PushValue(luaState, -2); // [table, key, value, key]
                    Lua.PushNil(luaState); // [table, key, value, key, nil]
                    Lua.SetTable(luaState, -5); // [table, key, value] value置nil,通常不会真的删除table的node
                }
            }
            if (depth != 0 && Lua.GetMetatable(luaState, -1) != 0) // [table, metatable] or [table]
            {
                Lua.PushNil(luaState); // [table, metatable, nil]
                Lua.SetMetatable(luaState, -3); // [table, metatable]
                Lua.PushString(luaState, "__type_id__"); // [table, metatable, "__type_id__"]
                Lua.RawGet(luaState, -2); // [table, metatable, type_id]
                if (Lua.GetType(luaState, -1) == LuaType.String)
                {
                    var typeId = LuaStringToInt64(luaState, -1);
                    if (beanMetas.TryGetValue(typeId, out var meta))
                    {
                        var pool = meta.cacheRefPool;
                        if (pool == null)
                            meta.cacheRefPool = pool = new List<int>();
                        if (pool.Count < 1000)
                        {
                            Lua.PushValue(luaState, -3); // [table, metatable, type_id, table]
                            pool.Add(Lua.LuaL_ref(luaState, Lua.LuaRegistryIndex)); // [table, metatable, type_id]
                            removeTable = true;
                        }
                    }
                }
                Lua.Pop(luaState, 2); // [table]
            }
            else
            {
                Lua.PushNil(luaState); // [table, nil]
                Lua.SetMetatable(luaState, -2); // [table]
            }
            return removeTable;
        }

        long LuaStringToInt64(IntPtr luaState, int index)
        {
            switch (Lua.GetType(luaState, index))
            {
                case LuaType.String:
                    var ptr = Lua.ToLString(luaState, index, out var len);
                    return ToInt64(ptr, len.ToInt32());
                case LuaType.Number:
                    return Lua.ToInteger(luaState, index);
                default:
                    return Convert.ToInt64(Lua.ToString(luaState, index));
            }
        }

        static unsafe long ToInt64(IntPtr ptr, int len)
        {
            long v = 0;
            byte* p = (byte*)ptr.ToPointer();
            if (p != null)
            {
                bool minus = false;
                for (int i = 0; i < len; i++)
                {
                    byte b = p[i];
                    if (b >= '0' && b <= '9')
                        v = v * 10 + (b - '0');
                    else if (v == 0 && b == '-')
                        minus = true;
                }
                if (minus)
                    v = -v;
            }
            return v;
        }
    }
}
