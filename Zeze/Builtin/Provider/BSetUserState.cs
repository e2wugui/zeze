// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Provider
{
    public interface BSetUserStateReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BSetUserState Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public long LinkSid { get; }
        public string Context { get; }
        public Zeze.Net.Binary Contextx { get; }
    }

    public sealed class BSetUserState : Zeze.Transaction.Bean, BSetUserStateReadOnly
    {
        long _linkSid;
        string _context;
        Zeze.Net.Binary _contextx;

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

        public string Context
        {
            get
            {
                if (!IsManaged)
                    return _context;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _context;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__context)txn.GetLog(ObjectId + 2);
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
                txn.PutLog(new Log__context() { Belong = this, VariableId = 2, Value = value });
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
                var log = (Log__contextx)txn.GetLog(ObjectId + 3);
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
                txn.PutLog(new Log__contextx() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public BSetUserState()
        {
            _context = "";
            _contextx = Zeze.Net.Binary.Empty;
        }

        public BSetUserState(long _linkSid_, string _context_, Zeze.Net.Binary _contextx_)
        {
            _linkSid = _linkSid_;
            _context = _context_;
            _contextx = _contextx_;
        }

        public void Assign(BSetUserState other)
        {
            LinkSid = other.LinkSid;
            Context = other.Context;
            Contextx = other.Contextx;
        }

        public BSetUserState CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BSetUserState Copy()
        {
            var copy = new BSetUserState();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BSetUserState a, BSetUserState b)
        {
            BSetUserState save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -4860388989628287875;
        public override long TypeId => TYPEID;

        sealed class Log__linkSid : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BSetUserState)Belong)._linkSid = this.Value; }
        }

        sealed class Log__context : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BSetUserState)Belong)._context = this.Value; }
        }

        sealed class Log__contextx : Zeze.Transaction.Log<Zeze.Net.Binary>
        {
            public override void Commit() { ((BSetUserState)Belong)._contextx = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Provider.BSetUserState: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkSid").Append('=').Append(LinkSid).Append(',').Append(Environment.NewLine);
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
                string _x_ = Context;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = Contextx;
                if (_x_ != null && _x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
                Context = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
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
                    case 2: _context = vlog.StringValue(); break;
                    case 3: _contextx = vlog.BinaryValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            LinkSid = 0;
            Context = "";
            Contextx = Zeze.Net.Binary.Empty;
        }
    }
}
