// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Online
{
    [System.Serializable]
    public sealed class BOnline : Zeze.Util.ConfBean
    {
        public Zeze.Builtin.Online.BLink Link;
        public long LoginVersion;
        public System.Collections.Generic.HashSet<string> ReliableNotifyMark;
        public long ReliableNotifyIndex;
        public long ReliableNotifyConfirmIndex;
        public int ServerId;
        public long LogoutVersion;

        public BOnline()
        {
            Link = new Zeze.Builtin.Online.BLink();
            ReliableNotifyMark = new System.Collections.Generic.HashSet<string>();
        }

        public const long TYPEID = -7786403987996508020;
        public override long TypeId => -7786403987996508020;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Online.BOnline: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Link").Append('=').Append(Environment.NewLine);
            Link.BuildString(sb, level + 4);
            sb.Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LoginVersion").Append('=').Append(LoginVersion).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ReliableNotifyMark").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in ReliableNotifyMark)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Item).Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ReliableNotifyIndex").Append('=').Append(ReliableNotifyIndex).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ReliableNotifyConfirmIndex").Append('=').Append(ReliableNotifyConfirmIndex).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ServerId").Append('=').Append(ServerId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LogoutVersion").Append('=').Append(LogoutVersion).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _a_ = _o_.WriteIndex;
                int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
                int _b_ = _o_.WriteIndex;
                Link.Encode(_o_);
                if (_b_ + 1 == _o_.WriteIndex)
                    _o_.WriteIndex = _a_;
                else
                    _i_ = _j_;
            }
            {
                long _x_ = LoginVersion;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                var _x_ = ReliableNotifyMark;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.BYTES);
                    foreach (var _v_ in _x_)
                    {
                        _o_.WriteString(_v_);
                        _n_--;
                    }
                    if (_n_ != 0)
                        throw new System.Exception(_n_.ToString());
                }
            }
            {
                long _x_ = ReliableNotifyIndex;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                long _x_ = ReliableNotifyConfirmIndex;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                int _x_ = ServerId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                long _x_ = LogoutVersion;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
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
                _o_.ReadBean(Link, _t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                LoginVersion = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                var _x_ = ReliableNotifyMark;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    {
                        _x_.Add(_o_.ReadString(_t_));
                    }
                }
                else
                    _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                ReliableNotifyIndex = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                ReliableNotifyConfirmIndex = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 6)
            {
                ServerId = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 7)
            {
                LogoutVersion = _o_.ReadLong(_t_);
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
                    case 1: Link = ((Zeze.Transaction.Log<Zeze.Builtin.Online.BLink>)vlog).Value; break;
                    case 2: LoginVersion = ((Zeze.Transaction.Log<long>)vlog).Value; break;
                    case 3: Zeze.Transaction.Collections.CollApply.ApplySet1(ReliableNotifyMark, vlog); break;
                    case 4: ReliableNotifyIndex = ((Zeze.Transaction.Log<long>)vlog).Value; break;
                    case 5: ReliableNotifyConfirmIndex = ((Zeze.Transaction.Log<long>)vlog).Value; break;
                    case 6: ServerId = ((Zeze.Transaction.Log<int>)vlog).Value; break;
                    case 7: LogoutVersion = ((Zeze.Transaction.Log<long>)vlog).Value; break;
                }
            }
        }

        public override void ClearParameters()
        {
            Link = new Zeze.Builtin.Online.BLink();
            LoginVersion = 0;
            ReliableNotifyMark.Clear();
            ReliableNotifyIndex = 0;
            ReliableNotifyConfirmIndex = 0;
            ServerId = 0;
            LogoutVersion = 0;
        }
    }
}
