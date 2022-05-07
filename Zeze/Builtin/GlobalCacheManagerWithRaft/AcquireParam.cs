// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.GlobalCacheManagerWithRaft
{
    public interface AcquireParamReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey GlobalTableKey { get; }
        public int State { get; }
    }

    public sealed class AcquireParam : Zeze.Transaction.Bean, AcquireParamReadOnly
    {
        Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey _GlobalTableKey;
        int _State;

        public Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey GlobalTableKey
        {
            get
            {
                if (!IsManaged)
                    return _GlobalTableKey;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _GlobalTableKey;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__GlobalTableKey)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _GlobalTableKey;
            }
            set
            {
                if (value == null)
                    throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _GlobalTableKey = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__GlobalTableKey() { Belong = this, VariableId = 1, Value = value });
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

        public AcquireParam() : this(0)
        {
        }

        public AcquireParam(int _varId_) : base(_varId_)
        {
            _GlobalTableKey = new Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey();
        }

        public void Assign(AcquireParam other)
        {
            GlobalTableKey = other.GlobalTableKey;
            State = other.State;
        }

        public AcquireParam CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public AcquireParam Copy()
        {
            var copy = new AcquireParam();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(AcquireParam a, AcquireParam b)
        {
            AcquireParam save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = 8991661748018394550;
        public override long TypeId => TYPEID;

        sealed class Log__GlobalTableKey : Zeze.Transaction.Log<Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey>
        {
            public override void Commit() { ((AcquireParam)Belong)._GlobalTableKey = this.Value; }
        }

        sealed class Log__State : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((AcquireParam)Belong)._State = this.Value; }
        }

        public override string ToString()
        {
            System.Text.StringBuilder sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.GlobalCacheManagerWithRaft.AcquireParam: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("GlobalTableKey").Append('=').Append(Environment.NewLine);
            GlobalTableKey.BuildString(sb, level + 4);
            sb.Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("State").Append('=').Append(State).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _a_ = _o_.WriteIndex;
                int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
                int _b_ = _o_.WriteIndex;
                GlobalTableKey.Encode(_o_);
                if (_b_ + 1 == _o_.WriteIndex)
                    _o_.WriteIndex = _a_;
                else
                    _i_ = _j_;
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
                _o_.ReadBean(GlobalTableKey, _t_);
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
                    case 1: _GlobalTableKey = ((Zeze.Transaction.Log<Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey>)vlog).Value; break;
                    case 2: _State = ((Zeze.Transaction.Log<int>)vlog).Value; break;
                }
            }
        }

    }
}
