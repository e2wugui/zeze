
#define USE_KERA_LUA

using System;
using System.Collections.Generic;
using System.Text;
using KeraLua;
using Zeze.Serialize;

namespace Zeze.Net
{
    public class ProtocolToLua
    {
#if USE_KERA_LUA
        public KeraLua.Lua Lua { get; }

        public ProtocolToLua(KeraLua.Lua lua)
        {
            this.Lua = lua;
            if (false == this.Lua.DoString("require  'ProtocolDispatcher'"))
                throw new Exception("require  'ProtocolDispatcher' Error.");
        }

        public void DecodeAndDispatch(int typeId, ByteBuffer _os_)
        {
            this.Lua.GetGlobal("DispatchProtocol"); // push func onto stack

            // 现在不支持 Rpc.但是代码没有检查。
            // 生成的时候报错。
            Lua.CreateTable(0, 8);

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

            Lua.Call(1, 0);
        }

        public void DecodeBean(ByteBuffer _os_)
        {
            Lua.CreateTable(0, 32);
            for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_)
            {
                int _tagid_ = _os_.ReadInt();
                int _varid_ = (_tagid_ >> Helper.TAG_SHIFT) & Helper.ID_MASK;
                int _tagType_ = _tagid_ & Helper.TAG_MASK;
                Lua.PushInteger(_varid_);
                DecodeVariable(_os_, _tagType_);
                Lua.SetTable(-3);
            }
        }

        public void DecodeVariable(ByteBuffer _os_, int _tagType_)
        {
            switch (_tagType_)
            {
                case Helper.BOOL:
                    Lua.PushBoolean(_os_.ReadBool());
                    break;
                case Helper.BYTE:
                    Lua.PushInteger(_os_.ReadByte());
                    break;
                case Helper.SHORT:
                    Lua.PushInteger(_os_.ReadShort());
                    break;
                case Helper.INT:
                    Lua.PushInteger(_os_.ReadInt());
                    break;
                case Helper.LONG:
                    Lua.PushInteger(_os_.ReadLong());
                    break;
                case Helper.FLOAT:
                    Lua.PushNumber(_os_.ReadFloat());
                    break;
                case Helper.DOUBLE:
                    Lua.PushNumber(_os_.ReadDouble());
                    break;
                case Helper.STRING:
                    Lua.PushString(_os_.ReadString());
                    break;
                case Helper.BYTES:
                    Lua.PushBuffer(_os_.ReadBytes());
                    break;
                case Helper.LIST:
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
                case Helper.SET:
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
                case Helper.MAP:
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
                case Helper.BEAN:
                    {
                        _os_.BeginReadSegment(out var _state_);
                        DecodeBean(_os_);
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case Helper.DYNAMIC:
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
#endif // end USE_KERA_LUA
    }
}
