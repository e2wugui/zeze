using System;
using System.Collections.Generic;
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
        public string Name { get; } // Service Name
        public Service Service { get; }

        public ToLua ToLua { get; }
    }

    public class ToLuaServiceClient2 : HandshakeClient, IFromLua2
    {
        public ToLua ToLua { get; } = new ToLua();
        public Service Service => this;

        public ToLuaServiceClient2(string name, Config config) : base(name, config)
        {
        }

        public void InitializeLua(ILua iLua)
        {
            ToLua.InitializeLua(iLua);
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
    public class ToLuaServiceServer2 : HandshakeServer, IFromLua2
    {
        public ToLua ToLua { get; private set; } = new ToLua();
        public Service Service => this;

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
            {
                base.OnSocketProcessInputBuffer(so, input);
            }
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

    public interface ILua
    {
        public bool DoString(IntPtr luaState, string str);

        public void GetField(IntPtr luaState, int index, string name);

        public void PushNil(IntPtr luaState);

        public bool IsTable(IntPtr luaState, int index);

        public bool Next(IntPtr luaState, int index);

        public long ToInteger(IntPtr luaState, int index);
        public long ToLong(IntPtr luaState, int index);

        public void Pop(IntPtr luaState, int n);

        public LuaType GetGlobal(IntPtr luaState, string name);

        public void PushObject(IntPtr luaState, object obj);

        public void PushInteger(IntPtr luaState, long l);
        public void PushLong(IntPtr luaState, long l);

        public void PCall(IntPtr luaState, int arguments, int results, int errFunc);

        public int PCallPrepare(IntPtr luaState, int funcRef);

        public void PushString(IntPtr luaState, string str);

        public void SetTable(IntPtr luaState, int index);

        public void GetTable(IntPtr luaState, int index);

        public bool IsNil(IntPtr luaState, int index);

        public double ToNumber(IntPtr luaState, int index);

        public string ToString(IntPtr luaState, int index);

        public byte[] ToBuffer(IntPtr luaState, int index);

        public bool ToBoolean(IntPtr luaState, int index);

        public void CreateTable(IntPtr luaState, int elements, int records);

        public void PushBoolean(IntPtr luaState, bool v);

        public void PushNumber(IntPtr luaState, double number);

        public void PushBuffer(IntPtr luaState, byte[] buffer);

        public T ToObject<T>(IntPtr luaState, int index);

        public int LuaL_ref(IntPtr luaState, int t);

        public void LuaL_unref(IntPtr luaState, int t, int reference);

        public int LuaRegistryIndex { get; }

        public void RawGet(IntPtr luaState, int index);

        public void RawGetI(IntPtr luaState, int index, int n);

        public void SetMetatable(IntPtr luaState, int index);

        public int GetTop(IntPtr luaState);
    }

    public class ToLua : Protocol.IDecodeAndDispatch
    {
        private ILua Lua { get; set; }

        public void InitializeLua(ILua lua)
        {
            Lua = lua;
        }

        private class DynamicMeta
        {
            public readonly Dictionary<long, BeanMeta> SpecialTypeIdToBean = new Dictionary<long, BeanMeta>();
            public readonly Dictionary<long, long> BeanToSpecialTypeId = new Dictionary<long, long>();
        }

        private class VariableMeta
        {
            public int Id { get; set; }

            public int Type { get; set; }
            public long TypeBeanTypeId { get; set; }
            public int KeyType { get; set; }
            public long KeyBeanTypeId { get; set; }

            public int ValueType { get; set; }

            public long ValueBeanTypeId { get; set; }
            public string Name { get; set; }

            public DynamicMeta DynamicMeta { get; set; }

            public override string ToString()
            {
                return
                    $"{{[{Id}]={{{Type},{TypeBeanTypeId},{KeyType},{KeyBeanTypeId},{ValueType},{ValueBeanTypeId},\"{Name}\"}}}}";
            }
        }

        private class ProtocolArgument
        {
            public string ProtocolName { get; set; }
            public long ArgumentBeanTypeId { get; set; }
            public long ResultBeanTypeId { get; set; }
            public bool IsRpc { get; set; } = false;
            public long TypeId { get; set; }
            public int MetatableRef { get; set; }
        };

        private class BeanMeta
        {
            public string Name { get; set; }
            public long BeanTypeId { get; set; }
            public int MetatableRef { get; set; }
            public SortedDictionary<int, VariableMeta> Variables { get; } = new SortedDictionary<int, VariableMeta>();
        }

        private readonly Dictionary<long, BeanMeta> _beanMetas
            = new Dictionary<long, BeanMeta>(); // Bean.TypeId -> vars

        private readonly Dictionary<long, ProtocolArgument> _protocolMetas
            = new Dictionary<long, ProtocolArgument>(); // protocol.TypeId -> Bean.TypeId

        //这个地方和服务器实现有一点点区别，把vector3 等类型做成了配置的类型，而不是像服务的基础类型
        private readonly Dictionary<int, BeanMeta> _structMetas
            = new Dictionary<int, BeanMeta>(); // Bean.TypeId -> vars

        public int OnSocketConnected { get; set; }
        public int OnSocketClosed { get; set; }
        public int OnReceiveProtocol { get; set; }

        // 因为要给lua 对象添加引用，所以要记得删除对应引用
        private void FreeLuaMeta(IntPtr luaState)
        {
            if (TableRefId != 0)
            {
                Lua.LuaL_unref(luaState, Lua.LuaRegistryIndex, TableRefId);
            }
            TableRefId = 0;
            _beanMetas.Clear();
            _protocolMetas.Clear();
            _structMetas.Clear();
        }

        private int VariableType(String variableType, Dictionary<string, long> beanName2BeanId, out long beanId)
        {
            beanId = 0;
            switch (variableType)
            {
                case "binary":
                    return ByteBuffer.BYTES;
                case "bool":
                    return ByteBuffer.LUA_BOOL;
                case "byte":
                    return ByteBuffer.INTEGER;
                case "double":
                    return ByteBuffer.DOUBLE;
                case "dynamic":
                    return ByteBuffer.DYNAMIC;
                case "float":
                    return ByteBuffer.FLOAT;
                case "int":
                    return ByteBuffer.INTEGER;
                case "list":
                    return ByteBuffer.LIST;
                case "long":
                    return ByteBuffer.INTEGER;
                case "map":
                    return ByteBuffer.MAP;
                case "set":
                    return ByteBuffer.LIST;
                case "short":
                    return ByteBuffer.INTEGER;
                case "string":
                    return ByteBuffer.BYTES;
                case "vector2":
                    return ByteBuffer.VECTOR2;
                case "vector2int":
                    return ByteBuffer.VECTOR2INT;
                case "vector3":
                    return ByteBuffer.VECTOR3;
                case "vector3int":
                    return ByteBuffer.VECTOR3INT;
                case "vector4":
                case "quaternion":
                    return ByteBuffer.VECTOR4;
                default:
                    if (beanName2BeanId.TryGetValue(variableType, out beanId))
                    {
                        return ByteBuffer.BEAN;
                    }
                    else
                    {
                        throw new Exception($"error bean type not define {variableType}");
                    }
            }
        }

        private void ParseVariableMeta(IntPtr luaState, Dictionary<string, long> beanName2BeanId,
            List<DynamicMeta> dynamicMetas, SortedDictionary<int, VariableMeta> variables)
        {
            Lua.GetField(luaState, -1, "variables"); // variables
            Lua.PushNil(luaState);
            while (Lua.Next(luaState, -2)) // -1 value of varmeta(table) -2 key of varid
            {
                string variableName = Lua.ToString(luaState, -2);
                Lua.GetField(luaState, -1, "id");
                int variableId = (int)Lua.ToInteger(luaState, -1);
                Lua.Pop(luaState, 1);
                Lua.GetField(luaState, -1, "type");
                string variableTypeStr = Lua.ToString(luaState, -1);
                Lua.Pop(luaState, 1);
                int variableType = VariableType(variableTypeStr, beanName2BeanId, out long variableBeanTypeId);
                VariableMeta variable = new VariableMeta()
                {
                    Id = variableId, Name = variableName, Type = variableType, TypeBeanTypeId = variableBeanTypeId
                };
                switch (variableType)
                {
                    case ByteBuffer.LIST:
                        // case ByteBuffer.SET:
                        {
                            Lua.GetField(luaState, -1, "value");
                            string variableValueStr = Lua.ToString(luaState, -1);
                            Lua.Pop(luaState, 1);
                            int variableValueType = VariableType(variableValueStr, beanName2BeanId,
                                out long variableValueTypeId);
                            variable.ValueType = variableValueType;
                            variable.ValueBeanTypeId = variableValueTypeId;
                        }
                        break;
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
                            int variableKeyType = VariableType(variableKeyStr, beanName2BeanId,
                                out long variableKeyTypeId);
                            variable.KeyType = variableKeyType;
                            variable.KeyBeanTypeId = variableKeyTypeId;
                        }
                        break;
                    case ByteBuffer.DYNAMIC:
                        break;
                }

                if (variableType == ByteBuffer.DYNAMIC)
                {
                    DynamicMeta dynamicMeta = new DynamicMeta();
                    Lua.GetField(luaState, -1, "dynamcic_meta");
                    Lua.PushNil(luaState);
                    while (Lua.Next(luaState, -2))
                    {
                        long dynamicId = Convert.ToInt64(Lua.ToString(luaState, -2));
                        string dynamicType = Lua.ToString(luaState, -1);
                        if (!beanName2BeanId.TryGetValue(dynamicType, out var dynamicTypeId))
                        {
                            throw new Exception($"error dynamic bean type not define {dynamicType}");
                        }

                        dynamicMeta.BeanToSpecialTypeId[dynamicTypeId] = dynamicId;
                        variable.DynamicMeta = dynamicMeta;
                        dynamicMetas.Add(dynamicMeta);
                        Lua.Pop(luaState, 1);
                    }

                    Lua.Pop(luaState, 1);
                }

                Lua.Pop(luaState, 1); // pop value
                variables.Add(variable.Id, variable);
            }
        }

        public void LoadMeta(IntPtr luaState)
        {
            _beanMetas.Clear();
            _protocolMetas.Clear();
            List<DynamicMeta> dynamicMetas = new List<DynamicMeta>();

            if (false == Lua.IsTable(luaState, -1))
                throw new Exception("ZezeMeta not return a table");
            // lual_ref 使用
            Lua.CreateTable(luaState, 0, 0);
            TableRefId = Lua.LuaL_ref(luaState, Lua.LuaRegistryIndex);
            Lua.RawGetI(luaState, Lua.LuaRegistryIndex, TableRefId);
            Lua.GetField(luaState, -2, "beans");
            Lua.PushNil(luaState);
            var beanName2BeanId = new Dictionary<string, long>();
            while (Lua.Next(luaState, -2)) // -1 value of vars(table) -2 key of bean.TypeId
            {
                string beanName = Lua.ToString(luaState, -2);
                Lua.GetField(luaState, -1, "type_id");
                long beanTypeId = Convert.ToInt64(Lua.ToString(luaState, -1)); // 获取BeanId
                beanName2BeanId[beanName] = beanTypeId;
                Lua.Pop(luaState, 2);
            }

            Lua.PushNil(luaState);
            while (Lua.Next(luaState, -2)) // -1 value of vars(table) -2 key of bean.TypeId
            {
                string beanName = Lua.ToString(luaState, -2);
                Lua.GetField(luaState, -1, "type_id");
                long beanTypeId = Convert.ToInt64(Lua.ToString(luaState, -1)); // 获取BeanId
                Lua.Pop(luaState, 1);
                Lua.GetField(luaState, -1, "metatable");
                int metatableRef = Lua.LuaL_ref(luaState, -5); // metatable
                var beanMeta = new BeanMeta()
                {
                    Name = beanName, BeanTypeId = beanTypeId, MetatableRef = metatableRef,
                };
                ParseVariableMeta(luaState, beanName2BeanId, dynamicMetas, beanMeta.Variables);
                _beanMetas.Add(beanTypeId, beanMeta);
                Lua.Pop(luaState, 2); // pop value
            }
            Lua.Pop(luaState, 1); // pop beans


            Lua.GetField(luaState, -2, "structs");

            Lua.PushNil(luaState);
            while (Lua.Next(luaState, -2)) // -1 value of vars(table) -2 key of bean.TypeId
            {
                string beanName = Lua.ToString(luaState, -2);
                Lua.GetField(luaState, -1, "type_id");
                int typeId = Convert.ToInt32(Lua.ToString(luaState, -1)); // 获取BeanId
                Lua.Pop(luaState, 1);
                Lua.GetField(luaState, -1, "metatable");
                int metatableRef = Lua.LuaL_ref(luaState, -5); // metatable
                var beanMeta = new BeanMeta()
                {
                    Name = beanName, BeanTypeId = typeId, MetatableRef = metatableRef,
                };
                ParseVariableMeta(luaState, beanName2BeanId, dynamicMetas, beanMeta.Variables);

                _structMetas.Add(typeId, beanMeta);
                Lua.Pop(luaState, 2); // pop value
            }
            Lua.Pop(luaState, 1); // pop beans

            Lua.GetField(luaState, -2, "protocols");
            Lua.PushNil(luaState);
            while (Lua.Next(luaState, -2)) // -1 value of Protocol.Argument.BeanTypeId -2 Protocol.TypeId
            {
                string protocolName = Lua.ToString(luaState, -2);
                Lua.GetField(luaState, -1, "metatable");
                int metatableRef = Lua.LuaL_ref(luaState, -5); // metatable
                Lua.GetField(luaState, -1, "id");
                long id = Convert.ToInt64(Lua.ToString(luaState, -1));
                Lua.GetField(luaState, -2, "type_id");
                long typeId = Convert.ToInt64(Lua.ToString(luaState, -1));
                Lua.GetField(luaState, -3, "argument");
                string argumentType = Lua.ToString(luaState, -1);
                Lua.GetField(luaState, -4, "result");

                if (!beanName2BeanId.TryGetValue(argumentType, out var argumentTypeId))
                {
                    throw new Exception($"error bean type not define {argumentTypeId}");
                }

                ProtocolArgument pa = new ProtocolArgument()
                {
                    TypeId = typeId,
                    ProtocolName = protocolName,
                    ArgumentBeanTypeId = argumentTypeId,
                    MetatableRef = metatableRef,
                };
                if (!Lua.IsNil(luaState, -1)) // 存在result就把他视为rpc协议
                {
                    string resultType = Lua.ToString(luaState, -1);
                    if (!beanName2BeanId.TryGetValue(resultType, out var resultTypeId))
                    {
                        throw new Exception($"error bean type not define {resultTypeId}");
                    }

                    pa.ResultBeanTypeId = resultTypeId;
                    pa.IsRpc = true;
                }

                _protocolMetas.Add(pa.TypeId, pa);
                Lua.Pop(luaState, 5); // pop value
            }

            Lua.Pop(luaState, 1);

            foreach (DynamicMeta dynamicMeta in dynamicMetas)
            {
                foreach (var keyValuePair in dynamicMeta.BeanToSpecialTypeId)
                {
                    dynamicMeta.SpecialTypeIdToBean[keyValuePair.Value] = _beanMetas[keyValuePair.Key];
                }
            }
        }

        private int TableRefId { get; set; }

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
            if (false == Lua.IsTable(luaState, -1))
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
            if (false == _protocolMetas.TryGetValue(type, out var pa))
                throw new Exception($"protocol not found in meta. ({moduleId},{protocolId}, {type})");

            if (pa.IsRpc)
            {
                Lua.GetField(luaState, -1, "isRequest");
                bool isRequest = Lua.ToBoolean(luaState, -1);
                Lua.Pop(luaState, 1);
                Lua.GetField(luaState, -1, "sessionId");
                long sid = Lua.ToInteger(luaState, -1);
                Lua.Pop(luaState, 1);
                Lua.GetField(luaState, -1, "Timeout");
                int timeout = (int)Lua.ToInteger(luaState, -1);
                Lua.Pop(luaState, 1);

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

                ByteBuffer bb = ByteBuffer.Allocate();
                bb.WriteInt4(moduleId);
                bb.WriteInt4(protocolId);
                bb.BeginWriteWithSize4(out var outstate);

                bb.WriteInt(compress); // FamilyClass
                if (resultCode != 0)
                    bb.WriteLong(resultCode);
                bb.WriteLong(sid);
                Lua.GetField(luaState, -1, argumentName);
                EncodeBean(luaState, bb, argumentBeanTypeId);
                Lua.Pop(luaState, 1);
                bb.EndWriteWithSize4(outstate);
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

                ByteBuffer bb = ByteBuffer.Allocate();
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

        private void EncodeStruct(IntPtr luaState, ByteBuffer bb, BeanMeta beanMeta, int index = -1)
        {
            if (false == Lua.IsTable(luaState, -1))
                throw new Exception("encodebean need a table");

            foreach (var v in beanMeta.Variables.Values)
            {
                // 这里使用string 类型，是为了让lua可以传 k：v格式的简单table，也可以通过new 的方式来携带类型信息，保证dynamic类型
                // get table 方法是会trigger metatable 的，所以只通过string 类型访问就可以了，先这么做，以后再进行测试性能
                Lua.PushString(luaState, v.Name);
                Lua.RawGet(luaState, -1 + index);
                EncodeVariable(luaState, bb, v);
                Lua.Pop(luaState, 1);
            }
        }

        private void EncodeBean(IntPtr luaState, ByteBuffer bb, long beanTypeId, int index = -1)
        {
            if (false == Lua.IsTable(luaState, -1))
                throw new Exception("encode struct need a table");

            if (false == _beanMetas.TryGetValue(beanTypeId, out var beanMeta))
                throw new Exception("bean not found in meta for beanTypeId=" + beanTypeId);

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

        private int EncodeGetTableLength(IntPtr luaState)
        {
            if (false == Lua.IsTable(luaState, -1))
                throw new Exception("EncodeGetTableLength: not a table");
            int len = 0;
            Lua.PushNil(luaState);
            while (Lua.Next(luaState, -2))
            {
                ++len;
                Lua.Pop(luaState, 1);
            }

            return len;
        }

        private void EncodeVariable(IntPtr luaState, ByteBuffer bb, VariableMeta v, int index = -1)
        {
            switch (v.Type)
            {
                case ByteBuffer.LUA_BOOL:
                    bool vb = Lua.ToBoolean(luaState, index);
                    bb.WriteBool(vb);
                    break;
                case ByteBuffer.INTEGER:
                    long vi = Lua.ToInteger(luaState, index);
                    bb.WriteLong(vi);
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
                        if (false == Lua.IsTable(luaState, -1))
                            throw new Exception("list must be a table");
                        if (v.Id <= 0)
                            throw new Exception("list cannot define in collection");
                        int n = EncodeGetTableLength(luaState);
                        bb.WriteListType(n, v.ValueType & ByteBuffer.TAG_MASK);
                        Lua.PushNil(luaState);
                        while (Lua.Next(luaState, -2))
                        {
                            // 这里应该进行修改还没想好该怎么改~~~，先保留
                            EncodeVariable(luaState, bb,
                                new VariableMeta() { Id = 0, Type = v.ValueType, TypeBeanTypeId = v.ValueBeanTypeId });
                            Lua.Pop(luaState, 1);
                        }

                        break;
                    }
                case ByteBuffer.MAP:
                    {
                        if (false == Lua.IsTable(luaState, -1))
                            throw new Exception("map must be a table");
                        if (v.Id <= 0)
                            throw new Exception("map cannot define in collection");
                        int n = EncodeGetTableLength(luaState);
                        bb.WriteMapType(n, v.KeyType & ByteBuffer.TAG_MASK, v.ValueType & ByteBuffer.TAG_MASK);
                        Lua.PushNil(luaState);
                        while (Lua.Next(luaState, -2))
                        {
                            EncodeVariable(luaState, bb,
                                new VariableMeta() { Id = 0, Type = v.KeyType, TypeBeanTypeId = v.KeyBeanTypeId }, -2);
                            EncodeVariable(luaState, bb,
                                new VariableMeta() { Id = 0, Type = v.ValueType, TypeBeanTypeId = v.ValueBeanTypeId });
                            Lua.Pop(luaState, 1);
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
                        // else
                        // {
                        //     // in collection. direct encode
                        EncodeBean(luaState, bb, v.TypeBeanTypeId);
                        // }
                        break;
                    }

                case ByteBuffer.DYNAMIC:
                    {
                        if (v.Id <= 0)
                            throw new Exception("dynamic cannot define in collection");
                        Lua.GetField(luaState, -1, "__type_id__");
                        if (Lua.IsNil(luaState, -1))
                        {
                            Lua.Pop(luaState, 1);
                            Lua.GetField(luaState, -1, "__type_name__");
                            string id = Lua.ToString(luaState, -1);
                            throw new Exception($"'__type_id__' not found. dynamic bean needed.{v.Name} {id}");
                        }


                        // 在lua就处理好了相应的类型转换，可以做到协议的生成里，不想把这么特殊的代码保持在c#中
                        long dynamicBeanId = Convert.ToInt64(Lua.ToString(luaState, -1));
                        bb.WriteLong(dynamicBeanId);
                        if (dynamicBeanId != 0) // 不是empty bean
                        {
                            Lua.Pop(luaState, 1);
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
                    var meta = GetStructMeta(v.Type);
                    EncodeStruct(luaState, bb, meta, index);

                    break;
                default:
                    throw new Exception("Unknown Tag Type");
            }
        }

        public bool DecodeAndDispatch(IntPtr luaState, Service service, long sessionId, long typeId, ByteBuffer os)
        {
            int errFunc = Lua.PCallPrepare(luaState, OnReceiveProtocol);

            if (false == _protocolMetas.TryGetValue(typeId, out var pa))
            {
                throw new Exception(
                    $"protocol not found in meta for typeid={typeId} moduleId={Protocol.GetModuleId(typeId)} protocolId={Protocol.GetProtocolId(typeId)}");
            }

            // 现在不支持 Rpc.但是代码没有检查。
            // 生成的时候报错。
            Lua.CreateTable(luaState, 0, 16);

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
                long resultCode = ((compress & FamilyClass.BitResultCode) != 0) ? os.ReadLong() : 0;
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
                Lua.SetTable(luaState, -3);
                Lua.PushString(luaState, argument);
                if (!_beanMetas.TryGetValue(beanTypeId, out var beanMeta))
                {
                    throw new Exception("bean not found in meta for typeid=" + beanTypeId);
                }

                DecodeBean(luaState, os, beanMeta);
                Lua.SetTable(luaState, -3);
            }
            else
            {
                int compress = os.ReadInt();
                //int familyClass = compress & FamilyClass.FamilyClassMask; // lua需要的话，Push，但懒得看table索引，先不公开了。
                long resultCode = ((compress & FamilyClass.BitResultCode) != 0) ? os.ReadLong() : 0;
                
                Lua.PushString(luaState, "resultCode");
                Lua.PushLong(luaState, resultCode);
                Lua.SetTable(luaState, -3);
                Lua.PushString(luaState, "argument");
                // _ = os.ReadLong();
                if (!_beanMetas.TryGetValue(pa.ArgumentBeanTypeId, out var beanMeta))
                {
                    throw new Exception("bean not found in meta for typeid=" + pa.ArgumentBeanTypeId);
                }

                DecodeBean(luaState, os, beanMeta);
                Lua.SetTable(luaState, -3);
            }


            Lua.RawGetI(luaState, Lua.LuaRegistryIndex, TableRefId);
            Lua.RawGetI(luaState, -1, pa.MetatableRef);
            Lua.SetMetatable(luaState, -3);
            Lua.Pop(luaState, 1); // pop tableRefId
            Lua.PCall(luaState, 1, 0, errFunc);

            return true;
        }

        public ByteBuffer Encode(IntPtr luaState)
        {
            if (false == Lua.IsTable(luaState, -1))
                throw new Exception("Encode param is not a table.");

            ByteBuffer os = ByteBuffer.Allocate();
            Lua.GetField(luaState, -1, "__type_id__");
            long typeId =  Convert.ToInt64(Lua.ToString(luaState, -1));
            Lua.Pop(luaState, 1);
            os.WriteLong8(typeId);
            EncodeBean(luaState, os, typeId);
            return os;
        }

        public void Decode(IntPtr luaState, ByteBuffer os)
        {
            long type = os.ReadLong8();
            if (_beanMetas.TryGetValue(type, out var beanMeta))
            {
                DecodeBean(luaState, os, beanMeta);
            }
            else
            {
                throw new Exception($"beanMeta type is not found");
            }

        }

        private void DecodeBean(IntPtr luaState, ByteBuffer bb, BeanMeta beanMeta)
        {
            if (beanMeta == null)
            {
                throw new Exception($"beanMeta type is not found");
            }

            Lua.CreateTable(luaState, 0, 32);
            Lua.RawGetI(luaState, Lua.LuaRegistryIndex, TableRefId);
            Lua.RawGetI(luaState, -1, beanMeta.MetatableRef);
            Lua.SetMetatable(luaState, -3);
            Lua.Pop(luaState, 1);

            int t;
            int id = 0;
            SortedDictionary<int, VariableMeta>.Enumerator it = beanMeta.Variables.GetEnumerator();
            while ((t = bb.ReadByte()) != 0)
            {
                id += bb.ReadTagSize(t);
                t &= ByteBuffer.TAG_MASK;

                bool find = false;
                // 写的这么恶心是因为给把空协议中的list map创建table，先fix_bug 再看看有没有好方法
                while (it.MoveNext())
                {
                    var c = it.Current;
                    var variableMeta = c.Value;
                    if (c.Key == id)
                    {
                        find = true;
                        // 这里本来想设置成int，再通过元表来访问，可是lua 5.1 有一些问题，如果升级的话再改
                        // 主要是保持协议版本兼容性，否则升级协议还要进行特殊判断，记得把__next__重写
                        Lua.PushString(luaState, variableMeta.Name);
                        DecodeVariable(luaState, bb, t, variableMeta);
                        Lua.SetTable(luaState, -3);
                        break;
                    }

                    if (variableMeta.Type == ByteBuffer.LIST || variableMeta.Type == ByteBuffer.MAP)
                    {
                        Lua.PushString(luaState, variableMeta.Name);
                        Lua.CreateTable(luaState, 0, 0);
                        Lua.SetTable(luaState, -3);
                    }
                }

                if (!find)
                {
                    throw new Exception($"var not found in meta for typeid={id} in bean {beanMeta.Name}");
                }
            }
        }

        private void DecodeStruct(IntPtr luaState, ByteBuffer bb, BeanMeta beanMeta)
        {
            Lua.CreateTable(luaState, 0, beanMeta.Variables.Count);
            Lua.RawGetI(luaState, Lua.LuaRegistryIndex, TableRefId);
            Lua.RawGetI(luaState, -1, beanMeta.MetatableRef);
            Lua.SetMetatable(luaState, -3);
            Lua.Pop(luaState, 1);

            foreach (VariableMeta variablesValue in beanMeta.Variables.Values)
            {
                Lua.PushString(luaState, variablesValue.Name);
                DecodeVariable(luaState, bb, variablesValue.Type, variablesValue);
                Lua.SetTable(luaState, -3);
            }
        }

        private BeanMeta GetBeanMeta(int type, long beanId)
        {
            switch (type)
            {
                case ByteBuffer.BEAN:
                    if (_beanMetas.TryGetValue(beanId, out var beanMeta))
                        return beanMeta;
                    break;
            }

            return null;
        }

        private BeanMeta GetStructMeta(int type)
        {
            return _structMetas.TryGetValue(type, out var beanMeta) ? beanMeta : null;
        }

        private void DecodeVariable(IntPtr luaState, ByteBuffer bb, int tagType, VariableMeta varMeta,
            BeanMeta beanMeta = null)
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
                        int t = bb.ReadByte();
                        int n = bb.ReadTagSize(t);
                        Lua.CreateTable(luaState, Math.Min(n, 1000), 0);
                        var valueBeanMeta = GetBeanMeta(varMeta.ValueType, varMeta.ValueBeanTypeId);
                        for (int i = 1; i <= n; i++) // 从1开始？
                        {
                            Lua.PushInteger(luaState, i);
                            DecodeVariable(luaState, bb, varMeta.ValueType, varMeta, valueBeanMeta);
                            Lua.SetTable(luaState, -3);
                        }

                        break;
                    }

                case ByteBuffer.MAP:
                    {
                        int t = bb.ReadByte();
                        int s = t >> ByteBuffer.TAG_SHIFT;
                        t &= ByteBuffer.TAG_MASK;
                        int n = bb.ReadUInt();
                        var keyBeanMeta = GetBeanMeta(s, varMeta.KeyBeanTypeId);
                        var valueBeanMeta = GetBeanMeta(t, varMeta.ValueBeanTypeId);
                        Lua.CreateTable(luaState, 0, Math.Min(n, 1000));
                        for (; n > 0; n--)
                        {
                            DecodeVariable(luaState, bb, varMeta.KeyType, varMeta, keyBeanMeta);
                            DecodeVariable(luaState, bb, varMeta.ValueType, varMeta, valueBeanMeta);
                            Lua.SetTable(luaState, -3);
                        }

                        break;
                    }
                case ByteBuffer.BEAN:
                    beanMeta ??= GetBeanMeta(varMeta.Type, varMeta.TypeBeanTypeId);

                    DecodeBean(luaState, bb, beanMeta);
                    break;
                case ByteBuffer.DYNAMIC:
                    {
                        long beanTypeId = bb.ReadLong();

                        if (varMeta.DynamicMeta.SpecialTypeIdToBean.TryGetValue(beanTypeId,
                                out var dynamicBeanMeta))
                        {
                            // os.BeginReadSegment(out var state);
                            DecodeBean(luaState, bb, dynamicBeanMeta);
                            // os.EndReadSegment(state);
                        }
                        // else
                        // {
                        //     // empty bean 先特殊处理一下，反正要改到c版本去，就不管了
                        //     bb.ReadInt();
                        //     Lua.CreateTable(luaState, 0, 0);
                        // }
                    }
                    break;
                case ByteBuffer.VECTOR2:
                case ByteBuffer.VECTOR2INT:
                case ByteBuffer.VECTOR3:
                case ByteBuffer.VECTOR3INT:
                case ByteBuffer.VECTOR4:
                    beanMeta ??= GetStructMeta(tagType);
                    DecodeStruct(luaState, bb, beanMeta);
                    break;
                default:
                    throw new Exception($"Unknown Tag Type {tagType} {varMeta}");
            }
        }

        private Dictionary<long, ByteBuffer> _toLuaBuffer = new Dictionary<long, ByteBuffer>();
        private Dictionary<long, IFromLua2> _toLuaHandshakeDone = new Dictionary<long, IFromLua2>();
        private Dictionary<long, IFromLua2> _toLuaSocketClose = new Dictionary<long, IFromLua2>();

        internal void SetHandshakeDone(long socketSessionId, IFromLua2 service)
        {
            lock (this)
            {
                _toLuaHandshakeDone[socketSessionId] = service;
            }
        }

        internal void SetSocketClose(long socketSessionId, IFromLua2 service)
        {
            lock (this)
            {
                _toLuaSocketClose[socketSessionId] = service;
            }
        }

        public void AppendInputBuffer(long socketSessionId, ByteBuffer buffer)
        {
            lock (this)
            {
                if (_toLuaBuffer.TryGetValue(socketSessionId, out var exist))
                {
                    exist.Append(buffer.Bytes, buffer.ReadIndex, buffer.Size);
                    return;
                }

                ByteBuffer newBuffer = ByteBuffer.Allocate();
                _toLuaBuffer.Add(socketSessionId, newBuffer);
                newBuffer.Append(buffer.Bytes, buffer.ReadIndex, buffer.Size);
            }
        }

        public void Update(IntPtr luaState, Service service)
        {
            Dictionary<long, IFromLua2> handshakeTmp;
            Dictionary<long, IFromLua2> socketCloseTmp;
            Dictionary<long, ByteBuffer> inputTmp;
            lock (this)
            {
                handshakeTmp = _toLuaHandshakeDone;
                socketCloseTmp = _toLuaSocketClose;
                inputTmp = _toLuaBuffer;
                _toLuaBuffer = new Dictionary<long, ByteBuffer>();
                _toLuaHandshakeDone = new Dictionary<long, IFromLua2>();
                _toLuaSocketClose = new Dictionary<long, IFromLua2>();
            }

            foreach (var e in socketCloseTmp)
            {
                CallSocketClose(luaState, e.Value, e.Key);
            }

            foreach (var e in handshakeTmp)
            {
                CallHandshakeDone(luaState, e.Value, e.Key);
            }

            _updateLuaState = luaState;
            foreach (var e in inputTmp)
            {
                AsyncSocket sender = service.GetSocket(e.Key);
                if (null == sender)
                    continue;

                Protocol.Decode(service, sender, e.Value, this);
            }
            _updateLuaState = default;

            lock (this)
            {
                foreach (var e in inputTmp)
                {
                    if (e.Value.Size <= 0)
                        continue; // 数据全部处理完成。

                    e.Value.Campact();
                    if (_toLuaBuffer.TryGetValue(e.Key, out var exist))
                    {
                        // 处理过程中有新数据到来，加到当前剩余数据后面，然后覆盖掉buffer。
                        e.Value.Append(exist.Bytes, exist.ReadIndex, exist.Size);
                        _toLuaBuffer[e.Key] = e.Value;
                    }
                    else
                    {
                        // 没有新数据到来，有剩余，加回去。下一次update再处理。
                        _toLuaBuffer.Add(e.Key, e.Value);
                    }
                }
            }
        }

        private IntPtr _updateLuaState;

        public bool DecodeAndDispatch(Service service, long sessionId, long typeId, ByteBuffer os)
        {
            if (_updateLuaState == default)
            {
                return false;
            }
            return DecodeAndDispatch(_updateLuaState, service, sessionId, typeId, os);
        }
    }
}
