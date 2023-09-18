// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zege.User
{
    [System.Serializable]
    public sealed class BUser : Zeze.Util.ConfBean
    {
        public const int StatePrepare = 0;
        public const int StateCreated = 1;
        public const int RealFlagIdentityCard = 1;
        public const int RealFlagPhone = 2;
        public const int RealFlagBankCard = 4;
        public const int RealFlagFaceToFace = 8;

        public string Account;
        public int Type;
        public long CreateTime;
        public string Nick; // 不唯一
        public int LastCertIndex; // 给客户端分配Index用，递增。客户端对自己需要保存所有证书和Key。
        public Zeze.Net.Binary Cert;
        public int State;
        public long PrepareTime;
        public Zeze.Net.Binary PrepareRandomData;
        public long RealFlags;
        public string RealName;
        public string IdentityCard; // 和账号一一对应
        public string Phone; // 一个phone可以对应多个账号，有上限
        public System.Collections.Generic.List<string> BankCard;
        public bool FaceToFace; // 当面认证(银行合作，付费？)，保存视频照片等认证记录，需要 IdentityCard 和 MobilePhone，可以不用绑定银行卡。

        public BUser()
        {
            Account = "";
            Nick = "";
            LastCertIndex = -1;
            Cert = Zeze.Net.Binary.Empty;
            PrepareRandomData = Zeze.Net.Binary.Empty;
            RealName = "";
            IdentityCard = "";
            Phone = "";
            BankCard = new System.Collections.Generic.List<string>();
        }

        public const long TYPEID = -7447132324810043446;
        public override long TypeId => -7447132324810043446;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zege.User.BUser: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Account").Append('=').Append(Account).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Type").Append('=').Append(Type).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("CreateTime").Append('=').Append(CreateTime).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Nick").Append('=').Append(Nick).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LastCertIndex").Append('=').Append(LastCertIndex).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Cert").Append('=').Append(Cert).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("State").Append('=').Append(State).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("PrepareTime").Append('=').Append(PrepareTime).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("PrepareRandomData").Append('=').Append(PrepareRandomData).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("RealFlags").Append('=').Append(RealFlags).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("RealName").Append('=').Append(RealName).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("IdentityCard").Append('=').Append(IdentityCard).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Phone").Append('=').Append(Phone).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("BankCard").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in BankCard)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Item).Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("FaceToFace").Append('=').Append(FaceToFace).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = Account;
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
                long _x_ = CreateTime;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                string _x_ = Nick;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                int _x_ = LastCertIndex;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 20, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                var _x_ = Cert;
                if (_x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 21, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                int _x_ = State;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 22, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                long _x_ = PrepareTime;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 23, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                var _x_ = PrepareRandomData;
                if (_x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 24, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                long _x_ = RealFlags;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 30, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                string _x_ = RealName;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 31, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                string _x_ = IdentityCard;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 32, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                string _x_ = Phone;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 33, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = BankCard;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 34, ByteBuffer.LIST);
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
                bool _x_ = FaceToFace;
                if (_x_)
                {
                    _i_ = _o_.WriteTag(_i_, 35, ByteBuffer.INTEGER);
                    _o_.WriteByte(1);
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
                Account = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                Type = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                CreateTime = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                Nick = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while ((_t_ & 0xff) > 1 && _i_ < 20)
            {
                _o_.SkipUnknownField(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 20)
            {
                LastCertIndex = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            else
                LastCertIndex = 0;
            if (_i_ == 21)
            {
                Cert = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 22)
            {
                State = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 23)
            {
                PrepareTime = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 24)
            {
                PrepareRandomData = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while ((_t_ & 0xff) > 1 && _i_ < 30)
            {
                _o_.SkipUnknownField(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 30)
            {
                RealFlags = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 31)
            {
                RealName = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 32)
            {
                IdentityCard = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 33)
            {
                Phone = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 34)
            {
                var _x_ = BankCard;
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
            if (_i_ == 35)
            {
                FaceToFace = _o_.ReadBool(_t_);
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
                    case 1: Account = ((Zeze.Transaction.Log<string>)vlog).Value; break;
                    case 2: Type = ((Zeze.Transaction.Log<int>)vlog).Value; break;
                    case 3: CreateTime = ((Zeze.Transaction.Log<long>)vlog).Value; break;
                    case 4: Nick = ((Zeze.Transaction.Log<string>)vlog).Value; break;
                    case 20: LastCertIndex = ((Zeze.Transaction.Log<int>)vlog).Value; break;
                    case 21: Cert = ((Zeze.Transaction.Log<Zeze.Net.Binary>)vlog).Value; break;
                    case 22: State = ((Zeze.Transaction.Log<int>)vlog).Value; break;
                    case 23: PrepareTime = ((Zeze.Transaction.Log<long>)vlog).Value; break;
                    case 24: PrepareRandomData = ((Zeze.Transaction.Log<Zeze.Net.Binary>)vlog).Value; break;
                    case 30: RealFlags = ((Zeze.Transaction.Log<long>)vlog).Value; break;
                    case 31: RealName = ((Zeze.Transaction.Log<string>)vlog).Value; break;
                    case 32: IdentityCard = ((Zeze.Transaction.Log<string>)vlog).Value; break;
                    case 33: Phone = ((Zeze.Transaction.Log<string>)vlog).Value; break;
                    case 34: Zeze.Transaction.Collections.CollApply.ApplyList1(BankCard, vlog); break;
                    case 35: FaceToFace = ((Zeze.Transaction.Log<bool>)vlog).Value; break;
                }
            }
        }

        public override void ClearParameters()
        {
            Account = "";
            Type = 0;
            CreateTime = 0;
            Nick = "";
            LastCertIndex = -1;
            Cert = Zeze.Net.Binary.Empty;
            State = 0;
            PrepareTime = 0;
            PrepareRandomData = Zeze.Net.Binary.Empty;
            RealFlags = 0;
            RealName = "";
            IdentityCard = "";
            Phone = "";
            BankCard.Clear();
            FaceToFace = false;
        }
    }
}
