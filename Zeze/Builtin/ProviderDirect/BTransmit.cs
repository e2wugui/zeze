// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.ProviderDirect
{
    public interface BTransmitReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BTransmit Copy();

        public string ActionName { get; }
        public System.Collections.Generic.IReadOnlySet<long> Roles { get; }
        public long Sender { get; }
        public Zeze.Net.Binary Parameter { get; }
    }

    public sealed class BTransmit : Zeze.Transaction.Bean, BTransmitReadOnly
    {
        string _ActionName;
        readonly Zeze.Transaction.Collections.CollSet1<long> _Roles; // 查询目标角色。
        long _Sender; // 结果发送给Sender。
        Zeze.Net.Binary _Parameter; // encoded bean

        public string ActionName
        {
            get
            {
                if (!IsManaged)
                    return _ActionName;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ActionName;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ActionName)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _ActionName;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _ActionName = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ActionName() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public Zeze.Transaction.Collections.CollSet1<long> Roles => _Roles;
        System.Collections.Generic.IReadOnlySet<long> Zeze.Builtin.ProviderDirect.BTransmitReadOnly.Roles => _Roles;

        public long Sender
        {
            get
            {
                if (!IsManaged)
                    return _Sender;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Sender;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Sender)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _Sender;
            }
            set
            {
                if (!IsManaged)
                {
                    _Sender = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Sender() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public Zeze.Net.Binary Parameter
        {
            get
            {
                if (!IsManaged)
                    return _Parameter;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Parameter;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Parameter)txn.GetLog(ObjectId + 4);
                return log != null ? log.Value : _Parameter;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _Parameter = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Parameter() { Belong = this, VariableId = 4, Value = value });
            }
        }

        public BTransmit()
        {
            _ActionName = "";
            _Roles = new Zeze.Transaction.Collections.CollSet1<long>() { VariableId = 2 };
            _Parameter = Zeze.Net.Binary.Empty;
        }

        public BTransmit(string _ActionName_, long _Sender_, Zeze.Net.Binary _Parameter_)
        {
            _ActionName = _ActionName_;
            _Roles = new Zeze.Transaction.Collections.CollSet1<long>() { VariableId = 2 };
            _Sender = _Sender_;
            _Parameter = _Parameter_;
        }

        public void Assign(BTransmit other)
        {
            ActionName = other.ActionName;
            Roles.Clear();
            foreach (var e in other.Roles)
                Roles.Add(e);
            Sender = other.Sender;
            Parameter = other.Parameter;
        }

        public BTransmit CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BTransmit Copy()
        {
            var copy = new BTransmit();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BTransmit a, BTransmit b)
        {
            BTransmit save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 7395081565293443928;
        public override long TypeId => TYPEID;

        sealed class Log__ActionName : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BTransmit)Belong)._ActionName = this.Value; }
        }


        sealed class Log__Sender : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BTransmit)Belong)._Sender = this.Value; }
        }

        sealed class Log__Parameter : Zeze.Transaction.Log<Zeze.Net.Binary>
        {
            public override void Commit() { ((BTransmit)Belong)._Parameter = this.Value; }
        }

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.ProviderDirect.BTransmit: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ActionName").Append('=').Append(ActionName).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Roles").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in Roles)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Item).Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Sender").Append('=').Append(Sender).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Parameter").Append('=').Append(Parameter).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = ActionName;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = Roles;
                int _n_ = _x_?.Count ?? 0;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                    foreach (var _v_ in _x_)
                    {
                        _o_.WriteLong(_v_);
                        _n_--;
                    }
                    if (_n_ != 0)
                        throw new System.Exception(_n_.ToString());
                }
            }
            {
                long _x_ = Sender;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                var _x_ = Parameter;
                if (_x_ != null && _x_.Count != 0)
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
                ActionName = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                var _x_ = Roles;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    {
                        _x_.Add(_o_.ReadLong(_t_));
                    }
                }
                else
                    _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                Sender = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                Parameter = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }

        protected override void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root)
        {
            _Roles.InitRootInfo(root, this);
        }

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _Roles.InitRootInfoWithRedo(root, this);
        }

        public override bool NegativeCheck()
        {
            foreach (var _v_ in Roles)
            {
                if (_v_ < 0) return true;
            }
            if (Sender < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _ActionName = vlog.StringValue(); break;
                    case 2: _Roles.FollowerApply(vlog); break;
                    case 3: _Sender = vlog.LongValue(); break;
                    case 4: _Parameter = vlog.BinaryValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            ActionName = "";
            Roles.Clear();
            Sender = 0;
            Parameter = Zeze.Net.Binary.Empty;
        }
    }
}
