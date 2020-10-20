
#define USE_KERA_LUA

using System;
using System.Collections.Generic;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Text;
using Zeze.Net;
using Zeze.Serialize;

namespace Zeze.Services
{    
    public class ToLuaServiceClient : HandshakeClient
    {
        public ToLuaService.ToLua ToLua { get; private set; }
        private readonly ToLuaService.Helper Helper = new ToLuaService.Helper();

        public ToLuaServiceClient(string name, Zeze.Application zeze) : base(name, zeze)
        {
        }

        /// <summary>
        /// 在lua线程中调用，一般实现：
        /// 1 创建lua线程，
        /// 2 调用BindInLuaThread初始化
        /// 3 调用lua.main进入lua代码
        /// 4 在lua.main中回调Bind中注册的方法回调（创建连接，检测连接状态，收发协议）
        /// </summary>
        /// <param name="toLua"></param>
        public void InitializeLua(ToLuaService.ToLua toLua)
        {
            ToLua = toLua;

            // void NetServiceUpdate()
            ToLua.RegisterFunction("NetServiceUpdate", NetServiceUpdate);

            // 由 Protocol 的 lua 生成代码调用，其中 sesionId 从全局变量 NetServiceCurrentSessionId 中读取，
            // 对于客户端，连接 HandshakeDone 以后保存 sessionId 到 NetServiceCurrentSessionId 中，以后不用重新设置。
            // 对于服务器，连接 HandshakeDone 以后保存 sessionId 自己的结构中，发送前需要把当前连接设置到 NetServiceCurrentSessionId 中。 
            // void NetServiceSendProtocol(sessionId, protocol) 
            ToLua.RegisterFunction("NetServiceSendProtocol", NetServiceSendProtocol);

            // TODO 在 lua 中创建连接？
        }

        private int NetServiceSendProtocol(IntPtr luaState)
        {
            ToLua.SendProtocol(this, luaState);
            return 0;
        }

        private int NetServiceUpdate(IntPtr luaState)
        {
            Helper.Update(this, ToLua);
            return 0;
        }

        public override void OnHandshakeDone(AsyncSocket sender)
        {
            Helper.SetHandshakeDone(sender.SessionId);
        }

        public override void OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input)
        {
            Helper.AppendInputBuffer(so.SessionId, input);
            input.ReadIndex = input.WriteIndex;
        }
    }

    // 完全 ToLuaServiceClient，由于 c# 无法写 class S<T> : T where T : Net.Service，复制一份.
    public class ToLuaServiceServer : HandshakeServer
    {
        public ToLuaService.ToLua ToLua { get; private set; }
        private readonly ToLuaService.Helper Helper = new ToLuaService.Helper();

        public ToLuaServiceServer(string name, Zeze.Application zeze) : base(name, zeze)
        {
        }

        public void InitializeLua(ToLuaService.ToLua toLua)
        {
            ToLua = toLua;
            ToLua.RegisterFunction("NetServiceUpdate", NetServiceUpdate);
            ToLua.RegisterFunction("NetServiceSendProtocol", NetServiceSendProtocol);
        }

        private int NetServiceSendProtocol(IntPtr luaState)
        {
            ToLua.SendProtocol(this, luaState);
            return 0;
        }

        private int NetServiceUpdate(IntPtr luaState)
        {
            Helper.Update(this, ToLua);
            return 0;
        }

        public override void OnHandshakeDone(AsyncSocket sender)
        {
            Helper.SetHandshakeDone(sender.SessionId);
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
            if (false == this.Lua.DoString("require  'ProtocolDispatcher'"))
                throw new Exception("require  'ProtocolDispatcher' Error.");
        }

        public void RegisterFunction(string name, KeraLua.LuaFunction func)
        {
            Lua.Register(name, func);
        }

        public void CallHandshakeDone(long socketSessionId)
        {
            // void OnHandshakeDone(long sessionId)
            this.Lua.GetGlobal("OnHandshakeDone"); // push func onto stack
            Lua.PushInteger(socketSessionId);
            Lua.Call(1, 0);
        }

        public void SendProtocol(Net.Service service, IntPtr luaState)
        {
            KeraLua.Lua lua = KeraLua.Lua.FromIntPtr(luaState);

            long sessionId = lua.ToInteger(-2);
            AsyncSocket socket = service.GetSocket(sessionId);
            if (null == socket)
                return;

            if (false == lua.IsTable(-1))
                return;

            lua.GetField(-1, "");

            // socket.Send();            
        }

        public bool DecodeAndDispatch(long sessionId, int typeId, ByteBuffer _os_)
        {
            this.Lua.GetGlobal("OnDispatchProtocol"); // push func onto stack
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

        public void DecodeBean(ByteBuffer _os_)
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

        public void DecodeVariable(ByteBuffer _os_, int _tagType_)
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

    class Helper
    {

        private Dictionary<long, ByteBuffer> ToLuaBuffer = new Dictionary<long, ByteBuffer>();
        private Dictionary<long, bool> ToLuaHandshakeDone = new Dictionary<long, bool>();

        public void SetHandshakeDone(long socketSessionId)
        {
            lock (this)
            {
                ToLuaHandshakeDone[socketSessionId] = true;
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
            Dictionary<long, bool> handshakeTmp;
            Dictionary<long, Serialize.ByteBuffer> inputTmp;
            lock (this)
            {
                handshakeTmp = ToLuaHandshakeDone;
                inputTmp = ToLuaBuffer;
                ToLuaBuffer = new Dictionary<long, ByteBuffer>();
                ToLuaHandshakeDone = new Dictionary<long, bool>();
            }

            foreach (var e in handshakeTmp)
            {
                toLua.CallHandshakeDone(e.Key);
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
