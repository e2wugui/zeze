// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Online
{
    public interface BVersionReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BVersion Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public long LoginVersion { get; }
        public System.Collections.Generic.IReadOnlySet<string> ReliableNotifyMark { get; }
        public long ReliableNotifyIndex { get; }
        public long ReliableNotifyConfirmIndex { get; }
        public int ServerId { get; }
        public long LogoutVersion { get; }
    }

    public sealed class BVersion : Zeze.Transaction.Bean, BVersionReadOnly
    {
        long _LoginVersion;
        readonly Zeze.Transaction.Collections.CollSet1<string> _ReliableNotifyMark;
        long _ReliableNotifyIndex;
        long _ReliableNotifyConfirmIndex;
        int _ServerId;
        long _LogoutVersion;

        public string _zeze_map_key_string_ { get; set; }

        public long LoginVersion
        {
            get
            {
                if (!IsManaged)
                    return _LoginVersion;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _LoginVersion;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__LoginVersion)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _LoginVersion;
            }
            set
            {
                if (!IsManaged)
                {
                    _LoginVersion = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__LoginVersion() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public Zeze.Transaction.Collections.CollSet1<string> ReliableNotifyMark => _ReliableNotifyMark;
        System.Collections.Generic.IReadOnlySet<string> Zeze.Builtin.Online.BVersionReadOnly.ReliableNotifyMark => _ReliableNotifyMark;

        public long ReliableNotifyIndex
        {
            get
            {
                if (!IsManaged)
                    return _ReliableNotifyIndex;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ReliableNotifyIndex;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ReliableNotifyIndex)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _ReliableNotifyIndex;
            }
            set
            {
                if (!IsManaged)
                {
                    _ReliableNotifyIndex = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ReliableNotifyIndex() { Belong = this, VariableId = 3, Value = value });
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
                var log = (Log__ReliableNotifyConfirmIndex)txn.GetLog(ObjectId + 4);
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
                txn.PutLog(new Log__ReliableNotifyConfirmIndex() { Belong = this, VariableId = 4, Value = value });
            }
        }

        public int ServerId
        {
            get
            {
                if (!IsManaged)
                    return _ServerId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ServerId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ServerId)txn.GetLog(ObjectId + 5);
                return log != null ? log.Value : _ServerId;
            }
            set
            {
                if (!IsManaged)
                {
                    _ServerId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ServerId() { Belong = this, VariableId = 5, Value = value });
            }
        }

        public long LogoutVersion
        {
            get
            {
                if (!IsManaged)
                    return _LogoutVersion;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _LogoutVersion;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__LogoutVersion)txn.GetLog(ObjectId + 6);
                return log != null ? log.Value : _LogoutVersion;
            }
            set
            {
                if (!IsManaged)
                {
                    _LogoutVersion = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__LogoutVersion() { Belong = this, VariableId = 6, Value = value });
            }
        }

        public BVersion()
        {
            _ReliableNotifyMark = new Zeze.Transaction.Collections.CollSet1<string>() { VariableId = 2 };
        }

        public BVersion(long _LoginVersion_, long _ReliableNotifyIndex_, long _ReliableNotifyConfirmIndex_, int _ServerId_, long _LogoutVersion_)
        {
            _LoginVersion = _LoginVersion_;
            _ReliableNotifyMark = new Zeze.Transaction.Collections.CollSet1<string>() { VariableId = 2 };
            _ReliableNotifyIndex = _ReliableNotifyIndex_;
            _ReliableNotifyConfirmIndex = _ReliableNotifyConfirmIndex_;
            _ServerId = _ServerId_;
            _LogoutVersion = _LogoutVersion_;
        }

        public void Assign(BVersion other)
        {
            LoginVersion = other.LoginVersion;
            ReliableNotifyMark.Clear();
            foreach (var e in other.ReliableNotifyMark)
                ReliableNotifyMark.Add(e);
            ReliableNotifyIndex = other.ReliableNotifyIndex;
            ReliableNotifyConfirmIndex = other.ReliableNotifyConfirmIndex;
            ServerId = other.ServerId;
            LogoutVersion = other.LogoutVersion;
        }

        public BVersion CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BVersion Copy()
        {
            var copy = new BVersion();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BVersion a, BVersion b)
        {
            BVersion save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 4746858989717188809;
        public override long TypeId => TYPEID;

        sealed class Log__LoginVersion : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BVersion)Belong)._LoginVersion = this.Value; }
        }


        sealed class Log__ReliableNotifyIndex : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BVersion)Belong)._ReliableNotifyIndex = this.Value; }
        }

        sealed class Log__ReliableNotifyConfirmIndex : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BVersion)Belong)._ReliableNotifyConfirmIndex = this.Value; }
        }

        sealed class Log__ServerId : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BVersion)Belong)._ServerId = this.Value; }
        }

        sealed class Log__LogoutVersion : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BVersion)Belong)._LogoutVersion = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Online.BVersion: {").Append(Environment.NewLine);
            level += 4;
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
                long _x_ = LoginVersion;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                var _x_ = ReliableNotifyMark;
                int _n_ = _x_?.Count ?? 0;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
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
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                long _x_ = ReliableNotifyConfirmIndex;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                int _x_ = ServerId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                long _x_ = LogoutVersion;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
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
                LoginVersion = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
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
            if (_i_ == 3)
            {
                ReliableNotifyIndex = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                ReliableNotifyConfirmIndex = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                ServerId = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 6)
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

        protected override void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root)
        {
            _ReliableNotifyMark.InitRootInfo(root, this);
        }

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _ReliableNotifyMark.InitRootInfoWithRedo(root, this);
        }

        public override bool NegativeCheck()
        {
            if (LoginVersion < 0) return true;
            if (ReliableNotifyIndex < 0) return true;
            if (ReliableNotifyConfirmIndex < 0) return true;
            if (ServerId < 0) return true;
            if (LogoutVersion < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _LoginVersion = vlog.LongValue(); break;
                    case 2: _ReliableNotifyMark.FollowerApply(vlog); break;
                    case 3: _ReliableNotifyIndex = vlog.LongValue(); break;
                    case 4: _ReliableNotifyConfirmIndex = vlog.LongValue(); break;
                    case 5: _ServerId = vlog.IntValue(); break;
                    case 6: _LogoutVersion = vlog.LongValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            LoginVersion = 0;
            ReliableNotifyMark.Clear();
            ReliableNotifyIndex = 0;
            ReliableNotifyConfirmIndex = 0;
            ServerId = 0;
            LogoutVersion = 0;
        }
    }
}
