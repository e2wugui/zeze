// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// 通知用 Zeze.Collections.LinkedMap 下面时自定义数据结构的定义
// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zege.Notify
{
    [System.Serializable]
    public sealed class BNotify : Zeze.Util.ConfBean
    {
        public const int eTypeAddFriend = 0;
        public const int eTypeGroupCert = 1;

        public string Title; // 显示标题
        public int Type; // 通知类型（内部）
        public System.Collections.Generic.Dictionary<string, string> Properties; // 通知属性（由Name决定）
        public Zeze.Net.Binary Data; // 通知数据（由Name决定）
        public long Expire; // 到期时间，默认 0， 表示永久。

        public BNotify()
        {
            Title = "";
            Properties = new System.Collections.Generic.Dictionary<string, string>();
            Data = Zeze.Net.Binary.Empty;
        }

        public const long TYPEID = -1170607700335489618;
        public override long TypeId => -1170607700335489618;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zege.Notify.BNotify: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Title").Append('=').Append(Title).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Type").Append('=').Append(Type).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Properties").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var _kv_ in Properties)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append('(').Append(Environment.NewLine);
                var Key = _kv_.Key;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Key").Append('=').Append(Key).Append(',').Append(Environment.NewLine);
                var Value = _kv_.Value;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Value").Append('=').Append(Value).Append(',').Append(Environment.NewLine);
                sb.Append(Zeze.Util.Str.Indent(level)).Append(')').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Data").Append('=').Append(Data).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Expire").Append('=').Append(Expire).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = Title;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                int _x_ = Type;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                var _x_ = Properties;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.MAP);
                    _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BYTES);
                    foreach (var _e_ in _x_)
                    {
                        _o_.WriteString(_e_.Key);
                        _o_.WriteString(_e_.Value);
                        _n_--;
                    }
                    if (_n_ != 0)
                        throw new System.Exception(_n_.ToString());
                }
            }
            {
                var _x_ = Data;
                if (_x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                long _x_ = Expire;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            _o_.WriteByte(0);
        }

        public override void Decode(ByteBuffer _o_)
        {
            int _t_ = _o_.ReadByte();
            int _i_ = _o_.ReadTagSize(_t_);
            if (_i_ == 1)
            {
                Title = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                Type = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                var _x_ = Properties;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP)
                {
                    int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                    for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--)
                    {
                        var _k_ = _o_.ReadString(_s_);
                        var _v_ = _o_.ReadString(_t_);
                        _x_.Add(_k_, _v_);
                    }
                }
                else
                    _o_.SkipUnknownFieldOrThrow(_t_, "Map");
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                Data = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                Expire = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }


        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: Title = ((Zeze.Transaction.Log<string>)vlog).Value; break;
                    case 2: Type = ((Zeze.Transaction.Log<int>)vlog).Value; break;
                    case 3: Zeze.Transaction.Collections.CollApply.ApplyMap1(Properties, vlog); break;
                    case 4: Data = ((Zeze.Transaction.Log<Zeze.Net.Binary>)vlog).Value; break;
                    case 5: Expire = ((Zeze.Transaction.Log<long>)vlog).Value; break;
                }
            }
        }

        public override void ClearParameters()
        {
            Title = "";
            Type = 0;
            Properties.Clear();
            Data = Zeze.Net.Binary.Empty;
            Expire = 0;
        }
    }
}
