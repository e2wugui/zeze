// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// 协议：群及部门管理
// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zege.Friend
{
    [System.Serializable]
    public sealed class BCreateGroup : Zeze.Util.ConfBean
    {
        public System.Collections.Generic.HashSet<string> Members;
        public Zeze.Net.Binary RsaPublicKey;
        public Zeze.Net.Binary RandomData;
        public Zeze.Net.Binary Signed;

        public BCreateGroup()
        {
            Members = new System.Collections.Generic.HashSet<string>();
            RsaPublicKey = Zeze.Net.Binary.Empty;
            RandomData = Zeze.Net.Binary.Empty;
            Signed = Zeze.Net.Binary.Empty;
        }

        public const long TYPEID = 5862994491410666795;
        public override long TypeId => 5862994491410666795;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zege.Friend.BCreateGroup: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Members").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in Members)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Item).Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("RsaPublicKey").Append('=').Append(RsaPublicKey).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("RandomData").Append('=').Append(RandomData).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Signed").Append('=').Append(Signed).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                var _x_ = Members;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
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
                var _x_ = RsaPublicKey;
                if (_x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                var _x_ = RandomData;
                if (_x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                var _x_ = Signed;
                if (_x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
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
                var _x_ = Members;
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
            if (_i_ == 2)
            {
                RsaPublicKey = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                RandomData = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                Signed = _o_.ReadBinary(_t_);
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
                    case 1: Zeze.Transaction.Collections.CollApply.ApplySet1(Members, vlog); break;
                    case 2: RsaPublicKey = ((Zeze.Transaction.Log<Zeze.Net.Binary>)vlog).Value; break;
                    case 3: RandomData = ((Zeze.Transaction.Log<Zeze.Net.Binary>)vlog).Value; break;
                    case 4: Signed = ((Zeze.Transaction.Log<Zeze.Net.Binary>)vlog).Value; break;
                }
            }
        }

        public override void ClearParameters()
        {
            Members.Clear();
            RsaPublicKey = Zeze.Net.Binary.Empty;
            RandomData = Zeze.Net.Binary.Empty;
            Signed = Zeze.Net.Binary.Empty;
        }
    }
}
