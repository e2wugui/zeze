// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.GlobalCacheManagerWithRaft
{
    public interface ReduceParamReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public Zeze.Net.Binary GlobalKey { get; }
        public int State { get; }
    }

    public sealed class ReduceParam : Zeze.Transaction.Bean, ReduceParamReadOnly
    {
        Zeze.Net.Binary _GlobalKey;
        int _State;

        public Zeze.Net.Binary GlobalKey
        {
            get
            {
                if (!IsManaged)
                    return _GlobalKey;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _GlobalKey;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__GlobalKey)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _GlobalKey;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _GlobalKey = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__GlobalKey() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public int State
        {
            get
            {
                if (!IsManaged)
                    return _State;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _State;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__State)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _State;
            }
            set
            {
                if (!IsManaged)
                {
                    _State = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__State() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public ReduceParam() : this(0)
        {
        }

        public ReduceParam(int _varId_) : base(_varId_)
        {
            _GlobalKey = Zeze.Net.Binary.Empty;
        }

        public void Assign(ReduceParam other)
        {
            GlobalKey = other.GlobalKey;
            State = other.State;
        }

        public ReduceParam CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public ReduceParam Copy()
        {
            var copy = new ReduceParam();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(ReduceParam a, ReduceParam b)
        {
            ReduceParam save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = -4489915946741208436;
        public override long TypeId => TYPEID;

        sealed class Log__GlobalKey : Zeze.Transaction.Log<Zeze.Net.Binary>
        {
            public override void Commit() { ((ReduceParam)Belong)._GlobalKey = this.Value; }
        }

        sealed class Log__State : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((ReduceParam)Belong)._State = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.GlobalCacheManagerWithRaft.ReduceParam: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("GlobalKey").Append('=').Append(GlobalKey).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("State").Append('=').Append(State).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                var _x_ = GlobalKey;
                if (_x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                int _x_ = State;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
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
                GlobalKey = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                State = _o_.ReadInt(_t_);
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
        }

        public override bool NegativeCheck()
        {
            if (State < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _GlobalKey = ((Zeze.Transaction.Log<Zeze.Net.Binary>)vlog).Value; break;
                    case 2: _State = ((Zeze.Transaction.Log<int>)vlog).Value; break;
                }
            }
        }

    }
}
