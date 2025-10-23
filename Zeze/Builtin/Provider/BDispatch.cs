// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// link to gs
// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Provider
{
    public interface BDispatchReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BDispatch Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public long LinkSid { get; }
        public string Account { get; }
        public long ProtocolType { get; }
        public Zeze.Net.Binary ProtocolData { get; }
        public string Context { get; }
        public Zeze.Net.Binary Contextx { get; }
    }

    public sealed class BDispatch : Zeze.Transaction.Bean, BDispatchReadOnly
    {
        long _linkSid;
        string _account;
        long _protocolType;
        Zeze.Net.Binary _protocolData; // 协议打包，不包括 type, size
        string _context; // SetUserState
        Zeze.Net.Binary _contextx; // SetUserState

        public long LinkSid
        {
            get
            {
                if (!IsManaged)
                    return _linkSid;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _linkSid;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__linkSid)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _linkSid;
            }
            set
            {
                if (!IsManaged)
                {
                    _linkSid = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__linkSid() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public string Account
        {
            get
            {
                if (!IsManaged)
                    return _account;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _account;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__account)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _account;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _account = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__account() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public long ProtocolType
        {
            get
            {
                if (!IsManaged)
                    return _protocolType;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _protocolType;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__protocolType)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _protocolType;
            }
            set
            {
                if (!IsManaged)
                {
                    _protocolType = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__protocolType() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public Zeze.Net.Binary ProtocolData
        {
            get
            {
                if (!IsManaged)
                    return _protocolData;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _protocolData;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__protocolData)txn.GetLog(ObjectId + 4);
                return log != null ? log.Value : _protocolData;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _protocolData = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__protocolData() { Belong = this, VariableId = 4, Value = value });
            }
        }

        public string Context
        {
            get
            {
                if (!IsManaged)
                    return _context;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _context;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__context)txn.GetLog(ObjectId + 5);
                return log != null ? log.Value : _context;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _context = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__context() { Belong = this, VariableId = 5, Value = value });
            }
        }

        public Zeze.Net.Binary Contextx
        {
            get
            {
                if (!IsManaged)
                    return _contextx;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _contextx;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__contextx)txn.GetLog(ObjectId + 6);
                return log != null ? log.Value : _contextx;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _contextx = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__contextx() { Belong = this, VariableId = 6, Value = value });
            }
        }

        public BDispatch()
        {
            _account = "";
            _protocolData = Zeze.Net.Binary.Empty;
            _context = "";
            _contextx = Zeze.Net.Binary.Empty;
        }

        public BDispatch(long _linkSid_, string _account_, long _protocolType_, Zeze.Net.Binary _protocolData_, string _context_, Zeze.Net.Binary _contextx_)
        {
            _linkSid = _linkSid_;
            _account = _account_;
            _protocolType = _protocolType_;
            _protocolData = _protocolData_;
            _context = _context_;
            _contextx = _contextx_;
        }

        public void Assign(BDispatch other)
        {
            LinkSid = other.LinkSid;
            Account = other.Account;
            ProtocolType = other.ProtocolType;
            ProtocolData = other.ProtocolData;
            Context = other.Context;
            Contextx = other.Contextx;
        }

        public BDispatch CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BDispatch Copy()
        {
            var copy = new BDispatch();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BDispatch a, BDispatch b)
        {
            BDispatch save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -496680173908943081;
        public override long TypeId => TYPEID;

        sealed class Log__linkSid : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BDispatch)Belong)._linkSid = this.Value; }
        }

        sealed class Log__account : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BDispatch)Belong)._account = this.Value; }
        }

        sealed class Log__protocolType : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BDispatch)Belong)._protocolType = this.Value; }
        }

        sealed class Log__protocolData : Zeze.Transaction.Log<Zeze.Net.Binary>
        {
            public override void Commit() { ((BDispatch)Belong)._protocolData = this.Value; }
        }

        sealed class Log__context : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BDispatch)Belong)._context = this.Value; }
        }

        sealed class Log__contextx : Zeze.Transaction.Log<Zeze.Net.Binary>
        {
            public override void Commit() { ((BDispatch)Belong)._contextx = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Provider.BDispatch: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkSid").Append('=').Append(LinkSid).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Account").Append('=').Append(Account).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ProtocolType").Append('=').Append(ProtocolType).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ProtocolData").Append('=').Append(ProtocolData).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Context").Append('=').Append(Context).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Contextx").Append('=').Append(Contextx).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = LinkSid;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                string _x_ = Account;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                long _x_ = ProtocolType;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                var _x_ = ProtocolData;
                if (_x_ != null && _x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                string _x_ = Context;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = Contextx;
                if (_x_ != null && _x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
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
                LinkSid = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                Account = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                ProtocolType = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                ProtocolData = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                Context = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 6)
            {
                Contextx = _o_.ReadBinary(_t_);
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
            if (LinkSid < 0) return true;
            if (ProtocolType < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _linkSid = vlog.LongValue(); break;
                    case 2: _account = vlog.StringValue(); break;
                    case 3: _protocolType = vlog.LongValue(); break;
                    case 4: _protocolData = vlog.BinaryValue(); break;
                    case 5: _context = vlog.StringValue(); break;
                    case 6: _contextx = vlog.BinaryValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            LinkSid = 0;
            Account = "";
            ProtocolType = 0;
            ProtocolData = Zeze.Net.Binary.Empty;
            Context = "";
            Contextx = Zeze.Net.Binary.Empty;
        }
    }
}
