// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.ProviderDirect
{
    public interface BTransmitAccountReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BTransmitAccount Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public string ActionName { get; }
        public Zeze.Net.Binary Parameter { get; }
        public System.Collections.Generic.IReadOnlySet<string> TargetAccounts { get; }
        public string SenderAccount { get; }
        public string SenderClientId { get; }
    }

    public sealed class BTransmitAccount : Zeze.Transaction.Bean, BTransmitAccountReadOnly
    {
        string _ActionName;
        Zeze.Net.Binary _Parameter; // encoded bean
        readonly Zeze.Transaction.Collections.CollSet1<string> _TargetAccounts; // 查询目标角色。
        string _SenderAccount; // 结果发送给Sender。
        string _SenderClientId; // 结果发送给Sender。

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

        public Zeze.Net.Binary Parameter
        {
            get
            {
                if (!IsManaged)
                    return _Parameter;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Parameter;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Parameter)txn.GetLog(ObjectId + 2);
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
                txn.PutLog(new Log__Parameter() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public Zeze.Transaction.Collections.CollSet1<string> TargetAccounts => _TargetAccounts;
        System.Collections.Generic.IReadOnlySet<string> Zeze.Builtin.ProviderDirect.BTransmitAccountReadOnly.TargetAccounts => _TargetAccounts;

        public string SenderAccount
        {
            get
            {
                if (!IsManaged)
                    return _SenderAccount;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _SenderAccount;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__SenderAccount)txn.GetLog(ObjectId + 4);
                return log != null ? log.Value : _SenderAccount;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _SenderAccount = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__SenderAccount() { Belong = this, VariableId = 4, Value = value });
            }
        }

        public string SenderClientId
        {
            get
            {
                if (!IsManaged)
                    return _SenderClientId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _SenderClientId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__SenderClientId)txn.GetLog(ObjectId + 5);
                return log != null ? log.Value : _SenderClientId;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _SenderClientId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__SenderClientId() { Belong = this, VariableId = 5, Value = value });
            }
        }

        public BTransmitAccount()
        {
            _ActionName = "";
            _Parameter = Zeze.Net.Binary.Empty;
            _TargetAccounts = new Zeze.Transaction.Collections.CollSet1<string>() { VariableId = 3 };
            _SenderAccount = "";
            _SenderClientId = "";
        }

        public BTransmitAccount(string _ActionName_, Zeze.Net.Binary _Parameter_, string _SenderAccount_, string _SenderClientId_)
        {
            _ActionName = _ActionName_;
            _Parameter = _Parameter_;
            _TargetAccounts = new Zeze.Transaction.Collections.CollSet1<string>() { VariableId = 3 };
            _SenderAccount = _SenderAccount_;
            _SenderClientId = _SenderClientId_;
        }

        public void Assign(BTransmitAccount other)
        {
            ActionName = other.ActionName;
            Parameter = other.Parameter;
            TargetAccounts.Clear();
            foreach (var e in other.TargetAccounts)
                TargetAccounts.Add(e);
            SenderAccount = other.SenderAccount;
            SenderClientId = other.SenderClientId;
        }

        public BTransmitAccount CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BTransmitAccount Copy()
        {
            var copy = new BTransmitAccount();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BTransmitAccount a, BTransmitAccount b)
        {
            BTransmitAccount save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 2637210793748287339;
        public override long TypeId => TYPEID;

        sealed class Log__ActionName : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BTransmitAccount)Belong)._ActionName = this.Value; }
        }

        sealed class Log__Parameter : Zeze.Transaction.Log<Zeze.Net.Binary>
        {
            public override void Commit() { ((BTransmitAccount)Belong)._Parameter = this.Value; }
        }


        sealed class Log__SenderAccount : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BTransmitAccount)Belong)._SenderAccount = this.Value; }
        }

        sealed class Log__SenderClientId : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BTransmitAccount)Belong)._SenderClientId = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.ProviderDirect.BTransmitAccount: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ActionName").Append('=').Append(ActionName).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Parameter").Append('=').Append(Parameter).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("TargetAccounts").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in TargetAccounts)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Item).Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("SenderAccount").Append('=').Append(SenderAccount).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("SenderClientId").Append('=').Append(SenderClientId).Append(Environment.NewLine);
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
                var _x_ = Parameter;
                if (_x_ != null && _x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                var _x_ = TargetAccounts;
                int _n_ = _x_?.Count ?? 0;
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
                string _x_ = SenderAccount;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                string _x_ = SenderClientId;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
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
                Parameter = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                var _x_ = TargetAccounts;
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
                SenderAccount = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                SenderClientId = _o_.ReadString(_t_);
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
            _TargetAccounts.InitRootInfo(root, this);
        }

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _TargetAccounts.InitRootInfoWithRedo(root, this);
        }

        public override bool NegativeCheck()
        {
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
                    case 2: _Parameter = vlog.BinaryValue(); break;
                    case 3: _TargetAccounts.FollowerApply(vlog); break;
                    case 4: _SenderAccount = vlog.StringValue(); break;
                    case 5: _SenderClientId = vlog.StringValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            ActionName = "";
            Parameter = Zeze.Net.Binary.Empty;
            TargetAccounts.Clear();
            SenderAccount = "";
            SenderClientId = "";
        }
    }
}
