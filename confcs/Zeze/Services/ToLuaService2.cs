using System;
using System.Collections.Generic;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Services.ToLuaService2;
#if UNITY_EDITOR_WIN || UNITY_STANDALONE_WIN
using System.Runtime.InteropServices;
#endif
using lua_State=System.IntPtr;
using lua_Number=System.Double;
using lua_Integer=System.Int64;

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
        public ToLua ToLua => ToLua.Instance;
#if USE_CONFCS
        public LoginQueueClient LoginQueueClient { get; } = new LoginQueueClient();
#endif
        public ToLuaServiceClient2(string name, Application zeze)
            : base(name, zeze.Config)
        {
        }

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

        protected override void OnKeepAliveTimeout(AsyncSocket socket)
        {
            //base.OnKeepAliveTimeout(socket);
            ToLua.OnKeepAliveTimeout(socket.SessionId, this);
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
            if (ToLua.UpdateLuaState != default) // 在主线程中主动关闭网络，直接同步返回回调
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
        
#if UNITY_EDITOR_WIN || UNITY_STANDALONE_WIN
        [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
        public delegate int lua_CFunction(IntPtr luaState);
#else
	    public delegate int lua_CFunction(lua_State luaState);
#endif
        int LUA_REGISTRYINDEX { get; }
        void lua_pushnil(lua_State luaState);
        void lua_pushboolean(lua_State luaState, bool b);
        void lua_pushinteger(lua_State luaState, lua_Integer n);
        void lua_pushnumber(lua_State luaState, lua_Number n);
        void lua_pushvalue(lua_State luaState, int index);
        
        void lua_pushcfunction (lua_State L, lua_CFunction f);
        
        void lua_pushstring (lua_State L, string s);
        
        void lua_pushlstring (lua_State L, byte[] s, int len);
        void lua_pop(lua_State luaState, int n);
        int lua_gettop(lua_State luaState);
        bool lua_checkstack (lua_State luaState, int n);
        
        // int lua_isboolean (lua_State luaState, int index);
        // int lua_isinteger (lua_State luaState, int index);
        bool lua_isnil (lua_State luaState, int index);
        bool lua_istable(lua_State luaState, int index);
        void lua_setglobal(lua_State L, string name);
        lua_Number lua_tonumber (lua_State L, int index);
        lua_Integer lua_tointeger (lua_State L, int index);
        bool lua_toboolean (lua_State L, int index);
        
        void lua_createtable (lua_State L, int narr, int nrec);
        // int lua_gettable (lua_State L, int index);
        void lua_settable (lua_State L, int index);
        int lua_getmetatable (lua_State L, int index);
        void lua_setmetatable (lua_State L, int index);
        int lua_getfield (lua_State L, int index, string k);
        int lua_rawget (lua_State L, int index);
        int lua_rawgeti (lua_State L, int index, lua_Integer n);
        bool lua_next (lua_State L, int index);
        int lua_getglobal (lua_State L, string name);
        LuaType lua_type (lua_State L, int index);
            
        int luaL_ref (lua_State L, int t);
        void luaL_unref (lua_State L, int t, int @ref);
        int lua_pcall (lua_State L, int nargs, int nresults, int msgh);

        string lua_tostring(lua_State L, int index);
        IntPtr lua_tolstring (lua_State L, int index, out int len);
        
        // int PCallPrepare(IntPtr luaState, int funcRef);
        // void PCall(IntPtr luaState, int arguments, int results, int errFunc);
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
            // public long TypeId;
            public int MetatableRef;
            public readonly List<VariableMeta> Variables = new List<VariableMeta>();
            public List<int> cacheRefPool;
        }

        class ToLuaVariable
        {
            public readonly Dictionary<long, ByteBuffer> toLuaBuffer = new Dictionary<long, ByteBuffer>();
            public readonly Dictionary<long, IFromLua2> toLuaHandshakeDone = new Dictionary<long, IFromLua2>();
            public readonly Dictionary<long, IFromLua2> toLuaSocketClose = new Dictionary<long, IFromLua2>();
            public readonly Dictionary<long, IFromLua2> toLuaOnKeepAliveTimeout = new Dictionary<long, IFromLua2>();
            public readonly List<Action> toLuaAction = new List<Action>(); // Action里面call各种lua函数，不需要注册。

            public void Clear()
            {
                toLuaBuffer.Clear();
                toLuaHandshakeDone.Clear();
                toLuaSocketClose.Clear();
                toLuaOnKeepAliveTimeout.Clear();
                toLuaAction.Clear();
            }
        }

        static readonly IComparer<VariableMeta> varComparer = Comparer<VariableMeta>.Create((a, b) => a.Id - b.Id);

        ILua Lua;

        readonly Dictionary<long, BeanMeta> beanMetas = new Dictionary<long, BeanMeta>(); // Bean.TypeId -> vars
        readonly Dictionary<long, ProtocolArgument> protocolMetas = new Dictionary<long, ProtocolArgument>(); // protocol.TypeId -> Bean.TypeId

        //这个地方和服务器实现有一点点区别，把vector3 等类型做成了配置的类型，而不是像服务的基础类型
        readonly Dictionary<int, BeanMeta> structMetas = new Dictionary<int, BeanMeta>(); // Bean.TypeId -> vars

        // ReSharper disable UnassignedField.Global
        public int OnSocketConnected;
        public int OnSocketClosed;
        public int OnReceiveProtocol;
        public int OnKeepAliveTimeoutLua;
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
                Lua.luaL_unref(luaState, Lua.LUA_REGISTRYINDEX, tableRefId);
                tableRefId = (int)LuaDefine.NoRef;
            }
            beanMetas.Clear();
            protocolMetas.Clear();
            structMetas.Clear();
        }

        private static int ToVariableTypeId(string variableType, IReadOnlyDictionary<string, long> beanName2BeanId, out long beanId)
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
                case "array":
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
            ICollection<DynamicMeta> dynamicMetas, List<VariableMeta> variables)
        {
            Lua.lua_getfield(luaState, -1, "variables"); // variables
            // LuaTable variablesLuaTable;
            // foreach (var (variableName, variableLuaTable) in variablesLuaTable)
            // {
            //     var variableId = variableLuaTable.GetInt("Id");
            //     var variableTypeName = variableLuaTable.GetString("type");
            //     int variableType = ToVariableTypeId(variableTypeName, beanName2BeanId, out long variableBeanTypeId);
            //     var variableMeta = new VariableMeta
            //     {
            //         Id = (int)variableId,
            //         Name = variableName,
            //         Type = variableType,
            //         TypeBeanTypeId = variableBeanTypeId
            //     };
            //     switch (variableType)
            //     {
            //         case ByteBuffer.LIST:
            //         {
            //             var variableValueStr = variableLuaTable.GetString("type");
            //             int variableValueType = ToVariableTypeId(variableValueStr, beanName2BeanId,
            //                 out long variableValueTypeId);
            //             variableMeta.ValueType = variableValueType;
            //             variableMeta.ValueBeanTypeId = variableValueTypeId;
            //             break;
            //         }
            //         case ByteBuffer.MAP:
            //         {
            //             var variableKeyStr = variableLuaTable.GetString("key");
            //             var variableValueStr = variableLuaTable.GetString("value");
            //             
            //             int variableValueType = ToVariableTypeId(variableValueStr, beanName2BeanId,
            //                 out long variableValueTypeId);
            //             variableMeta.ValueType = variableValueType;
            //             variableMeta.ValueBeanTypeId = variableValueTypeId;
            //             int variableKeyType = ToVariableTypeId(variableKeyStr, beanName2BeanId, out long variableKeyTypeId);
            //             variableMeta.KeyType = variableKeyType;
            //             variableMeta.KeyBeanTypeId = variableKeyTypeId;
            //             break;
            //         }
            //         case ByteBuffer.DYNAMIC:
            //         {
            //             var dynamicMeta = new DynamicMeta();
            //             Lua.lua_getfield(luaState, -1, "dynamcic_meta");
            //             for (Lua.lua_pushnil(luaState); Lua.lua_next(luaState, -2); Lua.lua_pop(luaState, 1))
            //             {
            //                 long dynamicId = Convert.ToInt64(Lua.lua_tostring(luaState, -2));
            //                 string dynamicType = Lua.lua_tostring(luaState, -1);
            //                 if (!beanName2BeanId.TryGetValue(dynamicType, out var dynamicTypeId))
            //                     throw new Exception($"error dynamic bean type not define {dynamicType}");
            //                 dynamicMeta.BeanToSpecialTypeId[dynamicTypeId] = dynamicId;
            //                 variableMeta.DynamicMeta = dynamicMeta;
            //                 dynamicMetas.Add(dynamicMeta);
            //             }
            //             Lua.lua_pop(luaState, 1);
            //             break;
            //         }
            //     }
            //     int index = variables.BinarySearch(variableMeta, varComparer);
            //     if (index < 0)
            //         variables.Insert(~index, variableMeta);
            //     
            // }
            for (Lua.lua_pushnil(luaState); Lua.lua_next(luaState, -2); Lua.lua_pop(luaState, 1)) // -1 value of varMeta(table) -2 key of varId
            {
                string variableName = Lua.lua_tostring(luaState, -2);
                Lua.lua_getfield(luaState, -1, "id");
                int variableId = (int)Lua.lua_tointeger(luaState, -1);
                Lua.lua_pop(luaState, 1);
                Lua.lua_getfield(luaState, -1, "type");
                string variableTypeStr = Lua.lua_tostring(luaState, -1);
                Lua.lua_pop(luaState, 1);
                int variableType = ToVariableTypeId(variableTypeStr, beanName2BeanId, out long variableBeanTypeId);
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
                        Lua.lua_getfield(luaState, -1, "value");
                        string variableValueStr = Lua.lua_tostring(luaState, -1);
                        Lua.lua_pop(luaState, 1);
                        int variableValueType = ToVariableTypeId(variableValueStr, beanName2BeanId,
                            out long variableValueTypeId);
                        variable.ValueType = variableValueType;
                        variable.ValueBeanTypeId = variableValueTypeId;
                        break;
                    }
                    case ByteBuffer.MAP:
                    {
                        Lua.lua_getfield(luaState, -1, "value");
                        string variableValueStr = Lua.lua_tostring(luaState, -1);
                        Lua.lua_pop(luaState, 1);
                        int variableValueType = ToVariableTypeId(variableValueStr, beanName2BeanId,
                            out long variableValueTypeId);
                        variable.ValueType = variableValueType;
                        variable.ValueBeanTypeId = variableValueTypeId;
                        Lua.lua_getfield(luaState, -1, "key");
                        string variableKeyStr = Lua.lua_tostring(luaState, -1);
                        Lua.lua_pop(luaState, 1);
                        int variableKeyType = ToVariableTypeId(variableKeyStr, beanName2BeanId, out long variableKeyTypeId);
                        variable.KeyType = variableKeyType;
                        variable.KeyBeanTypeId = variableKeyTypeId;
                        break;
                    }
                    case ByteBuffer.DYNAMIC:
                    {
                        var dynamicMeta = new DynamicMeta();
                        Lua.lua_getfield(luaState, -1, "dynamcic_meta");
                        for (Lua.lua_pushnil(luaState); Lua.lua_next(luaState, -2); Lua.lua_pop(luaState, 1))
                        {
                            long dynamicId = Convert.ToInt64(Lua.lua_tostring(luaState, -2));
                            string dynamicType = Lua.lua_tostring(luaState, -1);
                            if (!beanName2BeanId.TryGetValue(dynamicType, out var dynamicTypeId))
                                throw new Exception($"error dynamic bean type not define {dynamicType}");
                            dynamicMeta.BeanToSpecialTypeId[dynamicTypeId] = dynamicId;
                            variable.DynamicMeta = dynamicMeta;
                            dynamicMetas.Add(dynamicMeta);
                        }
                        Lua.lua_pop(luaState, 1);
                        break;
                    }
                }
                int index = variables.BinarySearch(variable, varComparer);
                if (index < 0)
                    variables.Insert(~index, variable);
            }
        }

        public void LoadMeta(IntPtr luaState)
        {
            FreeLuaMeta(luaState);

            if (!Lua.lua_istable(luaState, -1))
                throw new Exception("ZezeMeta not return a table: " + Lua.lua_type(luaState, -1));
            Lua.lua_createtable(luaState, 0, 0); // LuaL_ref 使用
            tableRefId = Lua.luaL_ref(luaState, Lua.LUA_REGISTRYINDEX);
            Lua.lua_rawgeti(luaState, Lua.LUA_REGISTRYINDEX, tableRefId); // [table, refTable]

            Lua.lua_getfield(luaState, -2, "beans"); // [table, refTable, table.beans]
            var beanName2BeanId = new Dictionary<string, long>();
            var dynamicMetas = new List<DynamicMeta>();
            // LuaTable beans = new LuaTable();
            // foreach (var (beanName, bean) in beans)
            // {
            //     var beanTypeId = bean.GetStringInt("type_id");
            //     beanName2BeanId[beanName] = beanTypeId;
            // }
            for (Lua.lua_pushnil(luaState); Lua.lua_next(luaState, -2); Lua.lua_pop(luaState, 2)) // [table, refTable, table.beans, beanName, bean]
            {
                string beanName = Lua.lua_tostring(luaState, -2);
                Lua.lua_getfield(luaState, -1, "type_id"); // [table, refTable, table.beans, beanName, bean, beanTypeId]
                long beanTypeId = Convert.ToInt64(Lua.lua_tostring(luaState, -1)); // 获取BeanId
                beanName2BeanId[beanName] = beanTypeId;
            }
            // foreach (var (beanName, bean) in beans)
            // {
            //     var beanTypeId = bean.GetStringInt("type_id");
            //     beanName2BeanId[beanName] = beanTypeId;
            // }
            
            for (Lua.lua_pushnil(luaState); Lua.lua_next(luaState, -2); Lua.lua_pop(luaState, 2)) // [table, refTable, table.beans, beanName, bean]
            {
                // string beanName = Lua.lua_tostring(luaState, -2);
                Lua.lua_getfield(luaState, -1, "type_id"); // [table, refTable, table.beans, beanName, bean, beanTypeId]
                long beanTypeId = Convert.ToInt64(Lua.lua_tostring(luaState, -1)); // 获取BeanId
                Lua.lua_pop(luaState, 1); // [table, refTable, table.beans, beanName, bean]
                Lua.lua_getfield(luaState, -1, "metatable"); // [table, refTable, table.beans, beanName, bean, bean.metatable]
                int metatableRef = Lua.luaL_ref(luaState, -5); // [table, refTable, table.beans, beanName, bean]
                var beanMeta = new BeanMeta
                {
                    // TypeId = beanTypeId,
                    MetatableRef = metatableRef
                };
                ParseVariableMeta(luaState, beanName2BeanId, dynamicMetas, beanMeta.Variables);
                beanMetas.Add(beanTypeId, beanMeta);
            }
            Lua.lua_pop(luaState, 1); // pop beans

            Lua.lua_getfield(luaState, -2, "structs"); // [table, refTable, table.structs]
            for (Lua.lua_pushnil(luaState); Lua.lua_next(luaState, -2); Lua.lua_pop(luaState, 2)) // [table, refTable, table.structs, structName, struct]
            {
                // string beanName = Lua.lua_tostring(luaState, -2);
                Lua.lua_getfield(luaState, -1, "type_id"); // [table, refTable, table.structs, structName, struct, structTypeId]
                int typeId = Convert.ToInt32(Lua.lua_tostring(luaState, -1)); // 获取StructId
                Lua.lua_pop(luaState, 1); // [table, refTable, table.structs, structName, struct]
                Lua.lua_getfield(luaState, -1, "metatable"); // [table, refTable, table.structs, structName, struct, struct.metatable]
                int metatableRef = Lua.luaL_ref(luaState, -5); // [table, refTable, table.structs, structName, struct]
                var beanMeta = new BeanMeta
                {
                    // TypeId = typeId,
                    MetatableRef = metatableRef
                };
                ParseVariableMeta(luaState, beanName2BeanId, dynamicMetas, beanMeta.Variables);
                structMetas.Add(typeId, beanMeta);
            }
            Lua.lua_pop(luaState, 1); // pop structs

            Lua.lua_getfield(luaState, -2, "protocols"); // [table, refTable, table.protocols]
            for (Lua.lua_pushnil(luaState); Lua.lua_next(luaState, -2); Lua.lua_pop(luaState, 5)) // [table, refTable, table.protocols, protocolName, protocol]
            {
#if UNITY_2017_1_OR_NEWER
                string protocolName = Lua.lua_tostring(luaState, -2);
#endif
                Lua.lua_getfield(luaState, -1, "metatable");
                int metatableRef = Lua.luaL_ref(luaState, -5); // metatable
                Lua.lua_getfield(luaState, -1, "id");
                // long id = Convert.ToInt64(Lua.lua_tostring(luaState, -1));
                Lua.lua_getfield(luaState, -2, "type_id");
                long typeId = Convert.ToInt64(Lua.lua_tostring(luaState, -1));
                Lua.lua_getfield(luaState, -3, "argument");
                string argumentType = Lua.lua_tostring(luaState, -1);
                Lua.lua_getfield(luaState, -4, "result");
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
                if (!Lua.lua_isnil(luaState, -1)) // 存在result就把他视为rpc协议
                {
                    if (!beanName2BeanId.TryGetValue(Lua.lua_tostring(luaState, -1), out var resultTypeId))
                        throw new Exception($"error bean type not define {resultTypeId}");
                    pa.ResultBeanTypeId = resultTypeId;
                    pa.IsRpc = true;
                }
                protocolMetas.Add(typeId, pa);
            }
            Lua.lua_pop(luaState, 1); // pop protocols

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
                Lua.lua_pop(luaState, 1);
                return;
            }

            // int errFunc = Lua.PCallPrepare(luaState, OnSocketClosed);
            int errFunc = 0;
            Lua.lua_rawgeti(luaState, Lua.LUA_REGISTRYINDEX, OnSocketClosed);
            // Lua.PushObject(luaState, service);  
            Lua.lua_pushinteger(luaState, socketSessionId);
            Lua.lua_pcall(luaState, 1, 0, errFunc);
        }

        internal void CallHandshakeDone(IntPtr luaState, IFromLua2 service, long socketSessionId)
        {
            if (OnSocketConnected == 0)
            {
                Lua.lua_pop(luaState, 1);
                return;
            }

            // int errFunc = Lua.PCallPrepare(luaState, OnSocketConnected);
            int errFunc = 0;
            Lua.lua_rawgeti(luaState, Lua.LUA_REGISTRYINDEX, OnSocketConnected);
            // Lua.PushObject(luaState, service);
            Lua.lua_pushinteger(luaState, socketSessionId);
            Lua.lua_pcall(luaState, 1, 0, errFunc);
        }

        internal void CallOnKeepAliveTimeout(IntPtr luaState, IFromLua2 service, long socketSessionId)
        {
            if (OnKeepAliveTimeoutLua == 0)
            {
                Lua.lua_pop(luaState, 1);
                return;
            }

            // int errFunc = Lua.PCallPrepare(luaState, OnKeepAliveTimeoutLua);
            int errFunc = 0;
            Lua.lua_rawgeti(luaState, Lua.LUA_REGISTRYINDEX, OnKeepAliveTimeoutLua);
            // Lua.PushObject(luaState, service);
            Lua.lua_pushinteger(luaState, socketSessionId);
            Lua.lua_pcall(luaState, 1, 0, errFunc);
        }

        public void SendProtocol(IntPtr luaState, AsyncSocket socket)
        {
            if (!Lua.lua_istable(luaState, -1))
                throw new Exception("SendProtocol param is not a table: " + Lua.lua_type(luaState, -1));

            Lua.lua_getfield(luaState, -1, "moduleId");
            int moduleId = (int)Lua.lua_tointeger(luaState, -1);
            Lua.lua_pop(luaState, 1);
            Lua.lua_getfield(luaState, -1, "protocolId");
            int protocolId = (int)Lua.lua_tointeger(luaState, -1);
            Lua.lua_pop(luaState, 1);
            Lua.lua_getfield(luaState, -1, "resultCode");
            long resultCode = Lua.lua_tointeger(luaState, -1);
            Lua.lua_pop(luaState, 1);

            long type = Protocol.MakeTypeId(moduleId, protocolId);
            if (!protocolMetas.TryGetValue(type, out var pa))
                throw new Exception($"protocol not found in meta. ({moduleId},{protocolId}, {type})");

            if (pa.IsRpc)
            {
                Lua.lua_getfield(luaState, -1, "isRequest");
                bool isRequest = Lua.lua_toboolean(luaState, -1);
                Lua.lua_pop(luaState, 1);
                Lua.lua_getfield(luaState, -1, "sessionId");
                long sid = Lua.lua_tointeger(luaState, -1);
                Lua.lua_pop(luaState, 1);
                // Lua.lua_getfield(luaState, -1, "Timeout");
                // int timeout = (int)Lua.lua_tointeger(luaState, -1);
                // Lua.lua_pop(luaState, 1);

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

                bb.WriteUInt(compress); // FamilyClass
                if (resultCode != 0)
                    bb.WriteLong(resultCode);
                bb.WriteLong(sid);
                Lua.lua_getfield(luaState, -1, argumentName);
                EncodeBean(luaState, bb, argumentBeanTypeId);
                Lua.lua_pop(luaState, 1);
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

                bb.WriteUInt(compress);
                if (resultCode != 0)
                    bb.WriteLong(resultCode);

                Lua.lua_getfield(luaState, -1, "argument");
                EncodeBean(luaState, bb, pa.ArgumentBeanTypeId);
                Lua.lua_pop(luaState, 1);
                bb.EndWriteWithSize4(state);
                socket.Send(bb);
            }
        }

        void EncodeStruct(IntPtr luaState, ByteBuffer bb, BeanMeta beanMeta, int index = -1) // [table]
        {
            if (!Lua.lua_istable(luaState, -1))
                throw new Exception("EncodeStruct need a table: " + Lua.lua_type(luaState, -1));
            if (!Lua.lua_checkstack(luaState, 1))
                throw new Exception("Lua stack overflow!");

            foreach (var v in beanMeta.Variables)
            {
                // 这里使用string 类型，是为了让lua可以传 k：v格式的简单table，也可以通过new 的方式来携带类型信息，保证dynamic类型
                // get table 方法是会trigger metatable 的，所以只通过string 类型访问就可以了，先这么做，以后再进行测试性能
                Lua.lua_pushstring(luaState, v.Name); // [table, name]
                Lua.lua_rawget(luaState, -1 + index); // [table, value]
                EncodeVariable(luaState, bb, v); // [table, value]
                Lua.lua_pop(luaState, 1); // [table]
            }
        }

        void EncodeBean(IntPtr luaState, ByteBuffer bb, long beanTypeId, int index = -1)
        {
            if (!Lua.lua_istable(luaState, -1))
                throw new Exception("EncodeBean need a table: " + Lua.lua_type(luaState, -1));
            if (!beanMetas.TryGetValue(beanTypeId, out var beanMeta))
                throw new Exception("bean not found in meta for beanTypeId=" + beanTypeId);
            if (!Lua.lua_checkstack(luaState, 1))
                throw new Exception("Lua stack overflow!");

            int lastId = 0;
            foreach (var v in beanMeta.Variables)
            {
                // 这里使用string 类型，是为了让lua可以传 k：v格式的简单table，也可以通过new 的方式来携带类型信息，保证dynamic类型
                // get table 方法是会trigger metatable 的，所以只通过string 类型访问就可以了，先这么做，以后再进行测试性能
                Lua.lua_pushstring(luaState, v.Name);
                Lua.lua_rawget(luaState, -1 + index);
                if (!Lua.lua_isnil(luaState, -1)) // allow var not set
                {
                    // bb.WriteInt(v.Type | v.ID << ByteBuffer.TAG_SHIFT);
                    bb.WriteTag(lastId, v.Id, v.Type & ByteBuffer.TAG_MASK);
                    EncodeVariable(luaState, bb, v);
                    lastId = v.Id;
                }
                Lua.lua_pop(luaState, 1);
            }
            bb.WriteByte(0);
        }

        private int CheckListOffset(IntPtr luaState) // [table]
        {
            Lua.lua_rawgeti(luaState, -1, 0xf);
            if (Lua.lua_isnil(luaState, -1))
            {
                Lua.lua_pop(luaState, 1);
                return 1;
            }
            Lua.lua_pop(luaState, 1);
            
            Lua.lua_rawgeti(luaState, -1, 0xf + 0x80);
            if (Lua.lua_isnil(luaState, -1))
            {
                Lua.lua_pop(luaState, 1);
                return 2;
            }
            Lua.lua_pop(luaState, 1);
            
            Lua.lua_rawgeti(luaState, -1, 0xf + 0x4000);
            if (Lua.lua_isnil(luaState, -1))
            {
                Lua.lua_pop(luaState, 1);
                return 3;
            }
            Lua.lua_pop(luaState, 1);
            
            Lua.lua_rawgeti(luaState, -1, 0xf + 0x20_0000 );
            if (Lua.lua_isnil(luaState, -1))
            {
                Lua.lua_pop(luaState, 1);
                return 4;
            }
            Lua.lua_pop(luaState, 1);
            
            Lua.lua_rawgeti(luaState, -1, 0xf + 0x1000_0000);
            if (Lua.lua_isnil(luaState, -1))
            {
                Lua.lua_pop(luaState, 1);
                return 5;
            }
            Lua.lua_pop(luaState, 1);
            return 6;
        }
        
        // ReSharper disable once UnusedMember.Local
        int EncodeGetTableLength(IntPtr luaState) // [table]
        {
            if (!Lua.lua_istable(luaState, -1))
                throw new Exception("EncodeGetTableLength: not a table: " + Lua.lua_type(luaState, -1));

            int len = 0;
            for (Lua.lua_pushnil(luaState); Lua.lua_next(luaState, -2); Lua.lua_pop(luaState, 1)) // [table, key, value]
                len++;
            return len; // [table]
        }

        void EncodeVariable(IntPtr luaState, ByteBuffer bb, VariableMeta v, int index = -1)
        {
            try
            {
                switch (v.Type)
                {
                    case ByteBuffer.LUA_BOOL:
                        bb.WriteBool(Lua.lua_toboolean(luaState, index));
                        break;
                    case ByteBuffer.INTEGER:
                        bb.WriteLong(Lua.lua_tointeger(luaState, index));
                        break;
                    case ByteBuffer.FLOAT:
                        bb.WriteFloat((float)Lua.lua_tonumber(luaState, index));
                        break;
                    case ByteBuffer.DOUBLE:
                        bb.WriteDouble(Lua.lua_tonumber(luaState, index));
                        break;
                    case ByteBuffer.BYTES:
                        IntPtr str = Lua.lua_tolstring(luaState, index, out var len); // fix il2cpp 64 bit
                        if (str != IntPtr.Zero)
                        {
                            unsafe
                            {
                                bb.WriteBytes((byte*)str.ToPointer(), len);
                            }
                        }
                        else
                        {
                            bb.WriteUInt(0);
                        }
                        break;
                    case ByteBuffer.LIST:
                    {
                        if (!Lua.lua_istable(luaState, index))
                            throw new Exception("list must be a table: " + Lua.lua_type(luaState, index));
                        if (v.Id <= 0)
                            throw new Exception("list cannot define in collection");
                        if (!Lua.lua_checkstack(luaState, 2))
                            throw new Exception("Lua stack overflow!");
                        int length = 0;
                        var writeIndex = bb.WriteIndex;
                        int offset = CheckListOffset(luaState);
                        bb.EnsureWrite(offset);
                        bb.WriteIndex = writeIndex + offset;
                        var meta = new VariableMeta { Type = v.ValueType, TypeBeanTypeId = v.ValueBeanTypeId };
                        int topIdx = Lua.lua_gettop(luaState);
                        for (Lua.lua_pushnil(luaState); Lua.lua_next(luaState, index - 1); Lua.lua_pop(luaState, 1)) // [table, key, value]
                        {
                            // 这里应该进行修改还没想好该怎么改~~~，先保留
                            try
                            {
                                length++;
                                EncodeVariable(luaState, bb, meta);
                            }
                            catch (Exception e)
                            {
                                throw new Exception($"encode list value failed: key={Lua.lua_tostring(luaState, topIdx + 1)}", e);
                            }
                        }
                        var nextWriteIndex = bb.WriteIndex;
                        bb.WriteIndex = writeIndex;
                        bb.WriteListType(length, v.ValueType & ByteBuffer.TAG_MASK);
                        bb.WriteIndex = nextWriteIndex;
                        break;
                    }
                    case ByteBuffer.MAP:
                    {
                        if (!Lua.lua_istable(luaState, index))
                            throw new Exception("map must be a table: " + Lua.lua_type(luaState, index));
                        if (v.Id <= 0)
                            throw new Exception("map cannot define in collection");
                        if (!Lua.lua_checkstack(luaState, 2))
                            throw new Exception("Lua stack overflow!");
                        int length = 0;
                        var writeIndex = bb.WriteIndex;
                        bb.EnsureWrite(2);
                        bb.WriteIndex = writeIndex + 2;
                        
                        var keyMeta = new VariableMeta { Type = v.KeyType, TypeBeanTypeId = v.KeyBeanTypeId };
                        var valueMeta = new VariableMeta { Type = v.ValueType, TypeBeanTypeId = v.ValueBeanTypeId };
                        int topIdx = Lua.lua_gettop(luaState);
                        for (Lua.lua_pushnil(luaState); Lua.lua_next(luaState, index - 1); Lua.lua_pop(luaState, 1)) // [table, key, value]
                        {
                            length++;
                            EncodeVariable(luaState, bb, keyMeta, -2);
                            try
                            {
                                EncodeVariable(luaState, bb, valueMeta);
                            }
                            catch (Exception e)
                            {
                                throw new Exception($"encode map value failed: key={Lua.lua_tostring(luaState, topIdx + 1)}", e);
                            }
                        }
                        var offset = ByteBuffer.WriteUIntSize(length) + 1 - 2; 
                        if (offset > 0)
                        {
                            bb.EnsureWrite(offset);
                            Buffer.BlockCopy(bb.Bytes, writeIndex + 2, bb.Bytes, writeIndex + offset + 2, bb.WriteIndex - writeIndex - 2);
                        }
                        
                        var nextWriteIndex = bb.WriteIndex + offset;
                        bb.WriteIndex = writeIndex;
                        bb.WriteMapType(length, v.KeyType & ByteBuffer.TAG_MASK, v.ValueType & ByteBuffer.TAG_MASK);
                        bb.WriteIndex = nextWriteIndex;
                        
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
                        if (!Lua.lua_checkstack(luaState, 1))
                            throw new Exception("Lua stack overflow!");
                        Lua.lua_getfield(luaState, index, "__type_id__");
                        if (Lua.lua_isnil(luaState, -1))
                        {
                            Lua.lua_pop(luaState, 1);
                            Lua.lua_getfield(luaState, index, "__type_name__");
                            string id = Lua.lua_tostring(luaState, -1);
                            throw new Exception($"'__type_id__' not found. dynamic bean needed. {v.Name} {id}");
                        }

                        // 在lua就处理好了相应的类型转换，可以做到协议的生成里，不想把这么特殊的代码保持在c#中
                        var dynamicBeanId = LuaStringToInt64(luaState, -1);
                        Lua.lua_pop(luaState, 1);
                        bb.WriteLong(dynamicBeanId);
                        if (dynamicBeanId != 0) // 不是empty bean
                        {
                            // os.BeginWriteSegment(out var state);
                            try
                            {
                                EncodeBean(luaState, bb, dynamicBeanId, index);
                            }
                            catch (Exception e)
                            {
                                throw new Exception($"encode dynamic failed: typeId={dynamicBeanId}", e);
                            }
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
            catch (Exception e)
            {
                if (v.Id == 0 && v.Name == null)
                    throw;
                throw new Exception($"encode variable failed: name={v.Name} id={v.Id} type={v.Type},{v.TypeBeanTypeId}", e);
            }
        }

        public bool DecodeAndDispatch(IntPtr luaState, Service service, long typeId, ByteBuffer os)
        {
            // int errFunc = Lua.PCallPrepare(luaState, OnReceiveProtocol);
            int errFunc = 0;
            Lua.lua_rawgeti(luaState, Lua.LUA_REGISTRYINDEX, OnReceiveProtocol);
            if (!protocolMetas.TryGetValue(typeId, out var pa))
            {
                throw new Exception($"protocol not found in meta for typeId={typeId} moduleId=" +
                                    $"{Protocol.GetModuleId(typeId)} protocolId={Protocol.GetProtocolId(typeId)}");
            }

            int cacheRef = pa.CacheRef;
            if (cacheRef != (int)LuaDefine.NoRef)
                Lua.lua_rawgeti(luaState, Lua.LUA_REGISTRYINDEX, cacheRef);
            else
            {
                int n;
                var pool = pa.cacheRefPool;
                if (pool != null && (n = pool.Count) > 0)
                {
                    cacheRef = pool[--n];
                    pool.RemoveAt(n);
                    Lua.lua_rawgeti(luaState, Lua.LUA_REGISTRYINDEX, cacheRef);
                    Lua.luaL_unref(luaState, Lua.LUA_REGISTRYINDEX, cacheRef);
                }
                else
                    Lua.lua_createtable(luaState, 0, 8);
            }

            // if (service is IFromLua2 fromLua) // 必须是，不报错了。
            {
                // 先把这里的逻辑屏蔽了，如果lua 需要的的话，通过 id 转递的方式，基于基础的lua 接口，就不做对象的传递了
                // Lua.lua_pushstring(luaState, "Service");
                // Lua.PushObject(luaState, fromLua);
                // Lua.lua_settable(luaState, -3);
            }

            if (pa.IsRpc)
            {
                int compress = os.ReadUInt();
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

                Lua.lua_pushstring(luaState, "isRpc");
                Lua.lua_pushboolean(luaState, true);
                Lua.lua_settable(luaState, -3);
                Lua.lua_pushstring(luaState, "isRequest");
                Lua.lua_pushboolean(luaState, isRequest);
                Lua.lua_settable(luaState, -3);
                Lua.lua_pushstring(luaState, "sessionId");
                Lua.lua_pushinteger(luaState, sid);
                Lua.lua_settable(luaState, -3);
                Lua.lua_pushstring(luaState, "resultCode");
                Lua.lua_pushinteger(luaState, resultCode1);
                Lua.lua_settable(luaState, -3);
                Lua.lua_pushstring(luaState, "resultCodeModule");
                Lua.lua_pushinteger(luaState, resultCode0);
                Lua.lua_settable(luaState, -3); // [table]
                Lua.lua_pushstring(luaState, argument); // [table, arg]
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
                Lua.lua_settable(luaState, -3);
            }
            else
            {
                int compress = os.ReadUInt();
                //int familyClass = compress & FamilyClass.FamilyClassMask; // lua需要的话，Push，但懒得看table索引，先不公开了。
                long resultCode = (compress & FamilyClass.BitResultCode) != 0 ? os.ReadLong() : 0;

                Lua.lua_pushstring(luaState, "resultCode");
                Lua.lua_pushinteger(luaState, resultCode);
                Lua.lua_settable(luaState, -3); // [table]
                Lua.lua_pushstring(luaState, "argument"); // [table, arg]
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
                Lua.lua_settable(luaState, -3);
            }

            Lua.lua_rawgeti(luaState, Lua.LUA_REGISTRYINDEX, tableRefId);
            Lua.lua_rawgeti(luaState, -1, pa.MetatableRef);
            Lua.lua_setmetatable(luaState, -3);
            Lua.lua_pop(luaState, 1); // pop tableRef

#if UNITY_2017_1_OR_NEWER
            UnityEngine.Profiling.Profiler.BeginSample(pa.ProtocolName);
#endif
            Lua.lua_pcall(luaState, 1, 0, errFunc);
#if UNITY_2017_1_OR_NEWER
            UnityEngine.Profiling.Profiler.EndSample();
#endif
            
            if (pa.CacheRef != (int)LuaDefine.NoRef)
            {
                Lua.lua_rawgeti(luaState, Lua.LUA_REGISTRYINDEX, cacheRef); // [cache]
                CleanLuaTable(luaState);
                Lua.lua_pop(luaState, 1);
            }
            return true;
        }

        public ByteBuffer Encode(IntPtr luaState)
        {
            if (!Lua.lua_istable(luaState, -1))
                throw new Exception("Encode param is not a table: " + Lua.lua_type(luaState, -1));

            var os = ByteBuffer.Allocate();
            Lua.lua_getfield(luaState, -1, "__type_id__");
            var typeId = LuaStringToInt64(luaState, -1);
            Lua.lua_pop(luaState, 1);
            os.WriteLong8(typeId);
            try
            {
                EncodeBean(luaState, os, typeId);
            }
            catch (Exception e)
            {
                throw new Exception($"encode bean failed: typeId={typeId}", e);
            }
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
            if (!Lua.lua_checkstack(luaState, 3))
                throw new Exception("Lua stack overflow!");

            if (!hasTable)
            {
                int n;
                var pool = beanMeta.cacheRefPool;
                if (pool != null && (n = pool.Count) > 0)
                {
                    int cacheRef = pool[--n];
                    pool.RemoveAt(n);
                    Lua.lua_rawgeti(luaState, Lua.LUA_REGISTRYINDEX, cacheRef); // [table]
                    Lua.luaL_unref(luaState, Lua.LUA_REGISTRYINDEX, cacheRef); // [table]
                }
                else
                    Lua.lua_createtable(luaState, 0, beanMeta.Variables.Count); // [table]
            }
            Lua.lua_rawgeti(luaState, Lua.LUA_REGISTRYINDEX, tableRefId); // [table, tableRef]
            Lua.lua_rawgeti(luaState, -1, beanMeta.MetatableRef); // [table, tableRef, metatableRef]
            Lua.lua_setmetatable(luaState, -3); // [table, tableRef]
            Lua.lua_pop(luaState, 1); // [table]

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
                        var variableMeta = it.Current;
                        // ReSharper disable once PossibleNullReferenceException
                        while (variableMeta.Id > id) // 发现未知id的字段
                        {
                            bb.SkipUnknownField(t);
                            if ((t = bb.ReadByte()) == 0)
                                return;
                            id += bb.ReadTagSize(t);
                            t &= ByteBuffer.TAG_MASK;
                        }
                        if (variableMeta.Id == id)
                        {
                            find = true;
                            // 这里本来想设置成int，再通过元表来访问，可是lua 5.1有一些问题，如果升级的话再改
                            // 主要是保持协议版本兼容性，否则升级协议还要进行特殊判断，记得把__next__重写
                            Lua.lua_pushstring(luaState, variableMeta.Name); // [table, name]
                            DecodeVariable(luaState, bb, t, variableMeta.Type, variableMeta); // [table, name, value]
                            Lua.lua_settable(luaState, -3); // [table]
                            break;
                        }
                        if (variableMeta.Type == ByteBuffer.LIST || variableMeta.Type == ByteBuffer.MAP)
                        {
                            Lua.lua_pushstring(luaState, variableMeta.Name); // [table, name]
                            if (hasTable && PushTableField(luaState)) // [table, name, value] or [table, name]
                                Lua.lua_pop(luaState, 2); // [table]
                            else
                            {
                                Lua.lua_createtable(luaState, 0, 0); // [table, name, newTable]
                                Lua.lua_settable(luaState, -3); // [table]
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
                Lua.lua_createtable(luaState, 0, beanMeta.Variables.Count); // [table]
            Lua.lua_rawgeti(luaState, Lua.LUA_REGISTRYINDEX, tableRefId); // [table, tableRef]
            Lua.lua_rawgeti(luaState, -1, beanMeta.MetatableRef); // [table, tableRef, metatable]
            Lua.lua_setmetatable(luaState, -3); // [table, tableRef]
            Lua.lua_pop(luaState, 1); // [table]

            foreach (var variablesValue in beanMeta.Variables)
            {
                Lua.lua_pushstring(luaState, variablesValue.Name); // [table, name]
                DecodeVariable(luaState, bb, variablesValue.Type, variablesValue.Type, variablesValue); // [table, name, value]
                Lua.lua_settable(luaState, -3); // [table]
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

        void DecodeVariable(IntPtr luaState, ByteBuffer bb, int tagType, int defType, VariableMeta varMeta,
            BeanMeta beanMeta = null, bool isKV = false) // isKV ? [table] : [table, key]
        {
            switch (tagType)
            {
                case ByteBuffer.LUA_BOOL:
                    Lua.lua_pushboolean(luaState, bb.ReadBool());
                    break;
                case ByteBuffer.INTEGER:
                    if (defType == ByteBuffer.LUA_BOOL)
                        Lua.lua_pushboolean(luaState, bb.ReadBool());
                    else
                        Lua.lua_pushinteger(luaState, bb.ReadLong());
                    break;
                case ByteBuffer.FLOAT:
                    Lua.lua_pushnumber(luaState, bb.ReadFloat());
                    break;
                case ByteBuffer.DOUBLE:
                    Lua.lua_pushnumber(luaState, bb.ReadDouble());
                    break;
                case ByteBuffer.BYTES:
                    var bytes = bb.ReadBytes();
                    Lua.lua_pushlstring(luaState, bytes, bytes.Length);
                    break;
                case ByteBuffer.LIST:
                {
                    if (!Lua.lua_checkstack(luaState, 3))
                        throw new Exception("Lua stack overflow!");
                    int t = bb.ReadByte();
                    int n = bb.ReadTagSize(t);
                    if (isKV || !PushTableField(luaState))
                        Lua.lua_createtable(luaState, Math.Min(n, 1000), 0);
                    t &= ByteBuffer.TAG_MASK;
                    var valueBeanMeta = GetBeanMeta(t, varMeta.ValueBeanTypeId);
                    for (int i = 1; i <= n; i++) // 从1开始？
                    {
                        Lua.lua_pushinteger(luaState, i);
                        DecodeVariable(luaState, bb, t, varMeta.ValueType, varMeta, valueBeanMeta, true);
                        Lua.lua_settable(luaState, -3);
                    }
                    break;
                }
                case ByteBuffer.MAP:
                {
                    if (!Lua.lua_checkstack(luaState, 3))
                        throw new Exception("Lua stack overflow!");
                    int t = bb.ReadByte();
                    int s = t >> ByteBuffer.TAG_SHIFT;
                    t &= ByteBuffer.TAG_MASK;
                    int n = bb.ReadUInt();
                    var keyBeanMeta = GetBeanMeta(s, varMeta.KeyBeanTypeId);
                    var valueBeanMeta = GetBeanMeta(t, varMeta.ValueBeanTypeId);
                    if (isKV || !PushTableField(luaState))
                        Lua.lua_createtable(luaState, 0, Math.Min(n, 1000));
                    for (; n > 0; n--)
                    {
                        DecodeVariable(luaState, bb, s, varMeta.KeyType, varMeta, keyBeanMeta, true);
                        DecodeVariable(luaState, bb, t, varMeta.ValueType, varMeta, valueBeanMeta, true);
                        Lua.lua_settable(luaState, -3);
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
                    if (varMeta.DynamicMeta != null && varMeta.DynamicMeta.SpecialTypeIdToBean.TryGetValue(beanTypeId, out var dynamicBeanMeta)
                        || beanMetas.TryGetValue(beanTypeId, out dynamicBeanMeta))
                    {
                        DecodeBean(luaState, bb, dynamicBeanMeta, hasTable);
                    }
                    else
                    {
                        bb.SkipUnknownField(ByteBuffer.BEAN);
                        if (!hasTable)
                            Lua.lua_createtable(luaState, 0, 0);
                        Lua.lua_pushstring(luaState, "__type_id__");
                        Lua.lua_pushstring(luaState, beanTypeId.ToString());
                        Lua.lua_settable(luaState, -3);
                    }
                    break;
                }
                case ByteBuffer.VECTOR2:
                case ByteBuffer.VECTOR2INT:
                case ByteBuffer.VECTOR3:
                case ByteBuffer.VECTOR3INT:
                case ByteBuffer.VECTOR4:
                {
                    if (!Lua.lua_checkstack(luaState, 3))
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

        internal void OnKeepAliveTimeout(long sessionId, IFromLua2 service)
        {
            lock (this)
            {
                toLuaVariable.toLuaOnKeepAliveTimeout[sessionId] = service;
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
                CallHandshakeDone(luaState, e.Value, e.Key);
            }

            foreach (var e in toLuaVariableUpdating.toLuaOnKeepAliveTimeout)
            {
                CallOnKeepAliveTimeout(luaState, e.Value, e.Key);
            }

            foreach (var  e in toLuaVariableUpdating.toLuaAction)
            {
                e.Invoke();
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
            Lua.lua_createtable(luaState, 0, 8); // [cacheTable]
            meta.CacheRef = Lua.luaL_ref(luaState, Lua.LUA_REGISTRYINDEX);
            return true;
        }

        // 手动回收一个协议的table,要求lua栈顶是待回收的协议table,该table会在以后接收此协议时被复用
        // 如果返回false,说明还没调用LoadMeta或者protoTypeId未定义或者该协议类型已被指定自动回收(AddProtocolCache)或者已回收过多
        public bool RecycleProtocolTable(IntPtr luaState, int moduleId, int protocolId, int maxCount = 1000)
        {
            if (!protocolMetas.TryGetValue(Protocol.MakeTypeId(moduleId, protocolId), out var meta)
                || meta.CacheRef != (int)LuaDefine.NoRef)
                return false;
            var pool = meta.cacheRefPool;
            if (pool == null)
                meta.cacheRefPool = pool = new List<int>();
            else if (pool.Count >= maxCount)
                return false;
            CleanLuaTable(luaState);
            Lua.lua_pushvalue(luaState, -1); // [protoTable, protoTable]
            pool.Add(Lua.luaL_ref(luaState, Lua.LUA_REGISTRYINDEX)); // [protoTable]
            return true;
        }

        bool PushTableField(IntPtr luaState) // [table, key]
        {
            Lua.lua_pushvalue(luaState, -1); // [table, key, key]
            Lua.lua_rawget(luaState, -3); // [table, key, value]
            if (Lua.lua_istable(luaState, -1))
                return true;
            Lua.lua_pop(luaState, 1); // [table, key]
            return false;
        }

        bool CleanLuaTable(IntPtr luaState, int depth = 0)
        {
            var removeTable = false;
            for (Lua.lua_pushnil(luaState); Lua.lua_next(luaState, -2); Lua.lua_pop(luaState, 1)) // [table, key, value]
            {
                if (!Lua.lua_istable(luaState, -1) || depth > 16 || !Lua.lua_checkstack(luaState, 4) || CleanLuaTable(luaState, depth + 1)) // 确保不递归太多层,避免无限递归导致栈溢出
                {
                    Lua.lua_pushvalue(luaState, -2); // [table, key, value, key]
                    Lua.lua_pushnil(luaState); // [table, key, value, key, nil]
                    Lua.lua_settable(luaState, -5); // [table, key, value] value置nil,通常不会真的删除table的node
                }
            }
            if (depth != 0 && Lua.lua_getmetatable(luaState, -1) != 0) // [table, metatable] or [table]
            {
                Lua.lua_pushnil(luaState); // [table, metatable, nil]
                Lua.lua_setmetatable(luaState, -3); // [table, metatable]
                Lua.lua_pushstring(luaState, "__type_id__"); // [table, metatable, "__type_id__"]
                Lua.lua_rawget(luaState, -2); // [table, metatable, type_id]
                if (Lua.lua_type(luaState, -1) == LuaType.String)
                {
                    var typeId = LuaStringToInt64(luaState, -1);
                    if (beanMetas.TryGetValue(typeId, out var meta))
                    {
                        var pool = meta.cacheRefPool;
                        if (pool == null)
                            meta.cacheRefPool = pool = new List<int>();
                        if (pool.Count < 1000)
                        {
                            Lua.lua_pushvalue(luaState, -3); // [table, metatable, type_id, table]
                            pool.Add(Lua.luaL_ref(luaState, Lua.LUA_REGISTRYINDEX)); // [table, metatable, type_id]
                            removeTable = true;
                        }
                    }
                }
                Lua.lua_pop(luaState, 2); // [table]
            }
            else
            {
                Lua.lua_pushnil(luaState); // [table, nil]
                Lua.lua_setmetatable(luaState, -2); // [table]
            }
            return removeTable;
        }

        long LuaStringToInt64(IntPtr luaState, int index)
        {
            switch (Lua.lua_type(luaState, index))
            {
                case LuaType.String:
                    var ptr = Lua.lua_tolstring(luaState, index, out var len);
                    return ToInt64(ptr, len);
                case LuaType.Number:
                    return Lua.lua_tointeger(luaState, index);
                default:
                    return Convert.ToInt64(Lua.lua_tostring(luaState, index));
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

        private void ExportFunction(lua_State l, string name, ILua.lua_CFunction func)
        {
            // 这里保持 delegate 引用，防止gc, 如果是static 方法就不需要了
            // _luaCFunctions.Add(func);
            
            Lua.lua_pushstring(l, name);
            Lua.lua_pushcfunction(l, func);
            Lua.lua_settable(l, -3);
        }
        
        public void ExportToLua(lua_State l, ToLuaServiceClient2 service)
        {
            _serviceClient = service;
            Lua.lua_createtable(l, 0, 0);
            Lua.lua_pushvalue(l, -1);
            Lua.lua_setglobal(l, "zeze");
            Lua.lua_pushstring(l, "LuaClient");
            Lua.lua_createtable(l, 0, 0);
            ExportFunction(l, "LoadMeta", LoadMeta0);
            ExportFunction(l, "Update", Update);
            ExportFunction(l, "Close", Close);
            ExportFunction(l, "Connect", Connect);
            ExportFunction(l, "ConnectWebsocket", ConnectWebsocket);
#if USE_CONFCS
            ExportFunction(l, "ConnectLoginQueue", ConnectLoginQueue);
#endif
            ExportFunction(l, "SendProtocol", SendProtocol);
            ExportFunction(l, "SetOnSocketConnected", SetOnSocketConnected);
            ExportFunction(l, "SetOnSocketClosed", SetOnSocketClosed);
            ExportFunction(l, "SetOnReceiveProtocol", SetOnReceiveProtocol);
            ExportFunction(l, "SetKeepAliveTimeout", SetKeepAliveTimeout);

            Lua.lua_settable(l, -3);
            Lua.lua_pop(l, 1);
        }
        
        public static readonly ToLua Instance = new ToLua();
        public static ToLuaServiceClient2 _serviceClient;
        
        public static int SendProtocol(IntPtr luaState)
        {
            Instance.SendProtocol(luaState, _serviceClient.Service.GetSocket());
            return 0;
        }

        public static int Update(IntPtr luaState)
        {
            Instance.Update(luaState, _serviceClient);
            return 0;
        }

        public static int Connect(IntPtr luaState)
        {
            string host = Instance.Lua.lua_tostring(luaState, -3);
            int port = (int)Instance.Lua.lua_tointeger(luaState, -2);
            bool autoReconnect = Instance.Lua.lua_toboolean(luaState, -1);
            _serviceClient.Connect(host, port, autoReconnect);
            return 0;
        }

#if USE_CONFCS
        public static int ConnectLoginQueue(IntPtr luaState)
        {
            string url = Instance.Lua.lua_tostring(luaState, -2);
            int port = (int)Instance.Lua.lua_tointeger(luaState, -1);
            _serviceClient.LoginQueueClient.QueueFull = () =>
            {
                _serviceClient.ToLua.toLuaVariable.toLuaAction.Add(() =>
                {
                    // todo call lua OnQueueFull()
                });
            };
            _serviceClient.LoginQueueClient.QueuePosition = (queuePosition) =>
            {
                _serviceClient.ToLua.toLuaVariable.toLuaAction.Add(() =>
                {
                    // todo call lua OnQueuePosition(queuePosition.QueuePosition)
                });
            };
            _serviceClient.LoginQueueClient.LoginToken = (loginToken) =>
            {
                _serviceClient.ToLua.toLuaVariable.toLuaAction.Add(() =>
                {
                    // todo call lua OnLoginToken(loginToken.LinkIp, loginToken.LinkPort, loginToken.Token)
                });
            };
            _serviceClient.LoginQueueClient.Connect(url, port);
            return 0;
        }
#endif
        public static int ConnectWebsocket(IntPtr luaState)
        {
            string url = Instance.Lua.lua_tostring(luaState, -2);
            bool autoReconnect = Instance.Lua.lua_toboolean(luaState, -1);
            _serviceClient.ConnectWebsocket(url, autoReconnect);
            return 0;
        }

        public static int Close(IntPtr luaState)
        {
            _serviceClient.Stop();
            return 0;
        }

        public static int LoadMeta0(IntPtr luaState)
        {
            Instance.LoadMeta(luaState);
            return 0;
        }

        public static int SetOnSocketConnected(IntPtr luaState)
        {
            int reference = Instance.Lua.luaL_ref(luaState, Instance.Lua.LUA_REGISTRYINDEX);
            Instance.OnSocketConnected = reference;
            return 0;
        }

        public static int SetKeepAliveTimeout(IntPtr luaState)
        {
            int reference = Instance.Lua.luaL_ref(luaState, Instance.Lua.LUA_REGISTRYINDEX);
            Instance.OnKeepAliveTimeoutLua = reference;
            return 0;
        }

        public static int SetOnSocketClosed(IntPtr luaState)
        {
            int reference = Instance.Lua.luaL_ref(luaState, Instance.Lua.LUA_REGISTRYINDEX);
            Instance.OnSocketClosed = reference;
            return 0;
        }

        public static int SetOnReceiveProtocol(IntPtr luaState)
        {
            int reference = Instance.Lua.luaL_ref(luaState, Instance.Lua.LUA_REGISTRYINDEX);
            Instance.OnReceiveProtocol = reference;
            return 0;
        }

        /*
        private struct LuaTable : IEnumerable<KeyValuePair<string, LuaTable>> 
        {
            private lua_State _l;
            private ILua _lua;
            private int _tableIndex;

            public LuaTable(lua_State l)
            {
                _l = l;
                _lua = null;
                _tableIndex = _lua.lua_gettop(l);
            }
            
            // public LuaTable()
            // {
            //     _lua = null;
            //     _tableIndex = 0;
            // }

            public lua_Integer GetInt(string fieldName)
            {
                _lua.lua_getfield(_l, -1, fieldName);
                var v = _lua.lua_tointeger(_l, -1);
                _lua.lua_pop(_l, 1);
                return v;
            }

            public string GetString(string fieldName)
            {
                _lua.lua_getfield(_l, -1, fieldName);
                var v = _lua.lua_tointeger(_l, -1);
                _lua.lua_pop(_l, 1);
                return "";
            }

            
            public lua_Integer GetStringInt(string fieldName)
            {
                _lua.lua_getfield(_l, -1, fieldName);
                var v = _lua.lua_tointeger(_l, -1);
                _lua.lua_pop(_l, 1);
                return v;
            }


            public IEnumerator<KeyValuePair<string, LuaTable>> GetEnumerator()
            {
                throw new NotImplementedException();
            }

            IEnumerator IEnumerable.GetEnumerator()
            {
                return GetEnumerator();
            }
        }
        */
    }

#if USE_ToLua_LUA
    public class ToLuaApi : ILua
    {
        
        public int LUA_REGISTRYINDEX => LuaInterface.LuaIndexes.LUA_REGISTRYINDEX;
        public void lua_pushnil(lua_State luaState)
        {
            LuaInterface.LuaDLL.lua_pushnil(luaState);
        }

        public void lua_pushboolean(lua_State luaState, bool b)
        {
            LuaInterface.LuaDLL.lua_pushboolean(luaState, b);
        }

        public void lua_pushinteger(lua_State luaState, lua_Integer n)
        {
            LuaInterface.LuaDLL.lua_pushnumber(luaState, n);
        }

        public void lua_pushnumber(lua_State luaState, lua_Number n)
        {
            LuaInterface.LuaDLL.lua_pushnumber(luaState, n);
        }

        public void lua_pushvalue(lua_State luaState, int index)
        {
            LuaInterface.LuaDLL.lua_pushvalue(luaState, index);
        }

        public void lua_pushcfunction(lua_State L, ILua.lua_CFunction f)
        {
            LuaInterface.LuaCSFunction converted = (LuaInterface.LuaCSFunction)Delegate.CreateDelegate(
                typeof(LuaInterface.LuaCSFunction), 
                f.Target,
                f.Method
            );
            LuaInterface.LuaDLL.lua_pushcfunction(L, converted);
        }

        public void lua_pushstring(lua_State L, string s)
        {
            LuaInterface.LuaDLL.lua_pushstring(L, s);
        }

        public void lua_pushlstring(lua_State L, byte[] s, int len)
        {
            LuaInterface.LuaDLL.lua_pushlstring(L, s, len);
        }

        public void lua_pop(lua_State luaState, int n)
        {
            LuaInterface.LuaDLL.lua_pop(luaState, n);
        }

        public int lua_gettop(lua_State luaState)
        {
            return LuaInterface.LuaDLL.lua_gettop(luaState);
        }

        public bool lua_checkstack(lua_State luaState, int n)
        {
            return LuaInterface.LuaDLL.lua_checkstack(luaState, n) > 0;
        }

        public bool lua_isnil(lua_State luaState, int index)
        {
            return LuaInterface.LuaDLL.lua_isnil(luaState, index);
        }

        public bool lua_istable(lua_State luaState, int index)
        {
            return LuaInterface.LuaDLL.lua_istable(luaState, index);
        }

        public void lua_setglobal(lua_State L, string name)
        {
            LuaInterface.LuaDLL.lua_setglobal(L, name);
        }

        public lua_Number lua_tonumber(lua_State L, int index)
        {
            return LuaInterface.LuaDLL.lua_tonumber(L, index);
        }

        public lua_Integer lua_tointeger(lua_State L, int index)
        {
            return LuaInterface.LuaDLL.lua_tointeger(L, index);
        }

        public bool lua_toboolean(lua_State L, int index)
        {
            return LuaInterface.LuaDLL.lua_toboolean(L, index);
        }

        public void lua_createtable(lua_State L, int narr, int nrec)
        {
            LuaInterface.LuaDLL.lua_createtable(L, narr, nrec);
        }

        public void lua_settable(lua_State L, int index)
        {
            LuaInterface.LuaDLL.lua_settable(L, index);
        }

        public int lua_getmetatable(lua_State L, int index)
        {
            return LuaInterface.LuaDLL.lua_getmetatable(L, index);
        }

        public void lua_setmetatable(lua_State L, int index)
        {
            LuaInterface.LuaDLL.lua_setmetatable(L, index);
        }

        public int lua_getfield(lua_State L, int index, string k)
        {
            LuaInterface.LuaDLL.lua_getfield(L, index, k);
            return (int)LuaInterface.LuaDLL.lua_type(L, -1);
        }

        public int lua_rawget(lua_State L, int index)
        {
            LuaInterface.LuaDLL.lua_rawget(L, index);
            return (int)LuaInterface.LuaDLL.lua_type(L, -1);
        }

        public int lua_rawgeti(lua_State L, int index, lua_Integer n)
        {
            LuaInterface.LuaDLL.lua_rawgeti(L, index, (int)n);
            return (int)LuaInterface.LuaDLL.lua_type(L, -1);
        }

        public bool lua_next(lua_State L, int index)
        {
            return LuaInterface.LuaDLL.lua_next(L, index) > 0;
        }

        public int lua_getglobal(lua_State L, string name)
        {
            LuaInterface.LuaDLL.lua_getglobal(L, name);
            return (int)LuaInterface.LuaDLL.lua_type(L, -1);
        }

        public LuaType lua_type(lua_State L, int index)
        {
            return (LuaType)LuaInterface.LuaDLL.lua_type(L, index);
        }

        public int luaL_ref(lua_State L, int t)
        {
            return LuaInterface.LuaDLL.luaL_ref(L, t);
        }

        public void luaL_unref(lua_State L, int t, int @ref)
        {
            LuaInterface.LuaDLL.luaL_unref(L, t, @ref);
        }

        public int lua_pcall(lua_State L, int nargs, int nresults, int msgh)
        {
            return LuaInterface.LuaDLL.lua_pcall(L, nargs, nresults, msgh);
        }

        public string lua_tostring(lua_State L, int index)
        {
            return LuaInterface.LuaDLL.lua_tostring(L, index);
        }

        public lua_State lua_tolstring(lua_State L, int index, out int len)
        {
            return LuaInterface.LuaDLL.lua_tolstring(L, index, out len);
        }
    }
#endif
}
