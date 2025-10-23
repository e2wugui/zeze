// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Provider
{
    public interface BKickReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BKick Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public long Linksid { get; }
        public int Code { get; }
        public string Desc { get; }
    }

    public sealed class BKick : Zeze.Transaction.Bean, BKickReadOnly
    {
        public const int ErrorProtocolUnknown = 1;
        public const int ErrorDecode = 2;
        public const int ErrorProtocolException = 3;
        public const int ErrorDuplicateLogin = 4;
        public const int ErrorSeeDescription = 5;

        long _linksid;
        int _code;
        string _desc; // // for debug

        public long Linksid
        {
            get
            {
                if (!IsManaged)
                    return _linksid;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _linksid;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__linksid)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _linksid;
            }
            set
            {
                if (!IsManaged)
                {
                    _linksid = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__linksid() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public int Code
        {
            get
            {
                if (!IsManaged)
                    return _code;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _code;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__code)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _code;
            }
            set
            {
                if (!IsManaged)
                {
                    _code = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__code() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public string Desc
        {
            get
            {
                if (!IsManaged)
                    return _desc;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _desc;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__desc)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _desc;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _desc = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__desc() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public BKick()
        {
            _desc = "";
        }

        public BKick(long _linksid_, int _code_, string _desc_)
        {
            _linksid = _linksid_;
            _code = _code_;
            _desc = _desc_;
        }

        public void Assign(BKick other)
        {
            Linksid = other.Linksid;
            Code = other.Code;
            Desc = other.Desc;
        }

        public BKick CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BKick Copy()
        {
            var copy = new BKick();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BKick a, BKick b)
        {
            BKick save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -6855697390328479333;
        public override long TypeId => TYPEID;

        sealed class Log__linksid : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BKick)Belong)._linksid = this.Value; }
        }

        sealed class Log__code : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BKick)Belong)._code = this.Value; }
        }

        sealed class Log__desc : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BKick)Belong)._desc = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Provider.BKick: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Linksid").Append('=').Append(Linksid).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Code").Append('=').Append(Code).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Desc").Append('=').Append(Desc).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = Linksid;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                int _x_ = Code;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                string _x_ = Desc;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
                Linksid = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                Code = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                Desc = _o_.ReadString(_t_);
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
            if (Linksid < 0) return true;
            if (Code < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _linksid = vlog.LongValue(); break;
                    case 2: _code = vlog.IntValue(); break;
                    case 3: _desc = vlog.StringValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            Linksid = 0;
            Code = 0;
            Desc = "";
        }
    }
}
