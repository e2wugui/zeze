// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Online
{
    public interface BReliableNotifyConfirmReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BReliableNotifyConfirm Copy();

        public string ClientId { get; }
        public long ReliableNotifyConfirmIndex { get; }
        public bool Sync { get; }
    }

    public sealed class BReliableNotifyConfirm : Zeze.Transaction.Bean, BReliableNotifyConfirmReadOnly
    {
        string _ClientId;
        long _ReliableNotifyConfirmIndex;
        bool _Sync;

        public string ClientId
        {
            get
            {
                if (!IsManaged)
                    return _ClientId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ClientId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ClientId)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _ClientId;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _ClientId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ClientId() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public long ReliableNotifyConfirmIndex
        {
            get
            {
                if (!IsManaged)
                    return _ReliableNotifyConfirmIndex;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ReliableNotifyConfirmIndex;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ReliableNotifyConfirmIndex)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _ReliableNotifyConfirmIndex;
            }
            set
            {
                if (!IsManaged)
                {
                    _ReliableNotifyConfirmIndex = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ReliableNotifyConfirmIndex() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public bool Sync
        {
            get
            {
                if (!IsManaged)
                    return _Sync;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Sync;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Sync)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _Sync;
            }
            set
            {
                if (!IsManaged)
                {
                    _Sync = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Sync() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public BReliableNotifyConfirm()
        {
            _ClientId = "";
        }

        public BReliableNotifyConfirm(string _ClientId_, long _ReliableNotifyConfirmIndex_, bool _Sync_)
        {
            _ClientId = _ClientId_;
            _ReliableNotifyConfirmIndex = _ReliableNotifyConfirmIndex_;
            _Sync = _Sync_;
        }

        public void Assign(BReliableNotifyConfirm other)
        {
            ClientId = other.ClientId;
            ReliableNotifyConfirmIndex = other.ReliableNotifyConfirmIndex;
            Sync = other.Sync;
        }

        public BReliableNotifyConfirm CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BReliableNotifyConfirm Copy()
        {
            var copy = new BReliableNotifyConfirm();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BReliableNotifyConfirm a, BReliableNotifyConfirm b)
        {
            BReliableNotifyConfirm save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 7657736965823286884;
        public override long TypeId => TYPEID;

        sealed class Log__ClientId : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BReliableNotifyConfirm)Belong)._ClientId = this.Value; }
        }

        sealed class Log__ReliableNotifyConfirmIndex : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BReliableNotifyConfirm)Belong)._ReliableNotifyConfirmIndex = this.Value; }
        }

        sealed class Log__Sync : Zeze.Transaction.Log<bool>
        {
            public override void Commit() { ((BReliableNotifyConfirm)Belong)._Sync = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Online.BReliableNotifyConfirm: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ClientId").Append('=').Append(ClientId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ReliableNotifyConfirmIndex").Append('=').Append(ReliableNotifyConfirmIndex).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Sync").Append('=').Append(Sync).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = ClientId;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                long _x_ = ReliableNotifyConfirmIndex;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                bool _x_ = Sync;
                if (_x_)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
                ClientId = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                ReliableNotifyConfirmIndex = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                Sync = _o_.ReadBool(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }

        public override bool NegativeCheck()
        {
            if (ReliableNotifyConfirmIndex < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _ClientId = vlog.StringValue(); break;
                    case 2: _ReliableNotifyConfirmIndex = vlog.LongValue(); break;
                    case 3: _Sync = vlog.BoolValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            ClientId = "";
            ReliableNotifyConfirmIndex = 0;
            Sync = false;
        }
    }
}
