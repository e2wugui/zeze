// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// tables
// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Online
{
    public interface BOnlineReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BOnline Copy();

        public string LinkName { get; }
        public long LinkSid { get; }
    }

    public sealed class BOnline : Zeze.Transaction.Bean, BOnlineReadOnly
    {
        string _LinkName;
        long _LinkSid;

        public string _zeze_map_key_string_ { get; set; }

        public string LinkName
        {
            get
            {
                if (!IsManaged)
                    return _LinkName;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _LinkName;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__LinkName)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _LinkName;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _LinkName = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__LinkName() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public long LinkSid
        {
            get
            {
                if (!IsManaged)
                    return _LinkSid;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _LinkSid;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__LinkSid)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _LinkSid;
            }
            set
            {
                if (!IsManaged)
                {
                    _LinkSid = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__LinkSid() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public BOnline()
        {
            _LinkName = "";
        }

        public BOnline(string _LinkName_, long _LinkSid_)
        {
            _LinkName = _LinkName_;
            _LinkSid = _LinkSid_;
        }

        public void Assign(BOnline other)
        {
            LinkName = other.LinkName;
            LinkSid = other.LinkSid;
        }

        public BOnline CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BOnline Copy()
        {
            var copy = new BOnline();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BOnline a, BOnline b)
        {
            BOnline save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -7786403987996508020;
        public override long TypeId => TYPEID;

        sealed class Log__LinkName : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BOnline)Belong)._LinkName = this.Value; }
        }

        sealed class Log__LinkSid : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BOnline)Belong)._LinkSid = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Online.BOnline: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkName").Append('=').Append(LinkName).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkSid").Append('=').Append(LinkSid).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = LinkName;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                long _x_ = LinkSid;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
                LinkName = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                LinkSid = _o_.ReadLong(_t_);
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
                    case 1: _LinkName = vlog.StringValue(); break;
                    case 2: _LinkSid = vlog.LongValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            LinkName = "";
            LinkSid = 0;
        }
    }
}
