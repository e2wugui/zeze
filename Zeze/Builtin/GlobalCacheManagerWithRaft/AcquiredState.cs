// auto-generated rocks
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.GlobalCacheManagerWithRaft
{
    public sealed class AcquiredState : Zeze.Raft.RocksRaft.Bean
    {
        int _State;

        public int State
        {
            get
            {
                if (!IsManaged)
                    return _State;
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                if (txn == null) return _State;
                var log = txn.GetLog(ObjectId + 1);
                return log != null ? ((Zeze.Raft.RocksRaft.Log<int>)log).Value : _State;
            }
            set
            {
                if (!IsManaged)
                {
                    _State = value;
                    return;
                }
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                txn.PutLog(new Zeze.Raft.RocksRaft.Log<int>() { Belong = this, VariableId = 1, Value = value, });
            }
        }

        public AcquiredState() : this(0)
        {
        }

        public AcquiredState(int _varId_) : base(_varId_)
        {
        }

        public void Assign(AcquiredState other)
        {
            State = other.State;
        }

        public AcquiredState CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public AcquiredState Copy()
        {
            var copy = new AcquiredState();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(AcquiredState a, AcquiredState b)
        {
            AcquiredState save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Raft.RocksRaft.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = -449879240583806688;
        public override long TypeId => TYPEID;

        public override string ToString()
        {
            System.Text.StringBuilder sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.GlobalCacheManagerWithRaft.AcquiredState: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("State").Append('=').Append(State).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = State;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
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
                State = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }

        protected override void InitChildrenRootInfo(Zeze.Raft.RocksRaft.Record.RootInfo root)
        {
        }

        public override void LeaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog)
        {
            switch (vlog.VariableId)
            {
                case 1: _State = ((Zeze.Raft.RocksRaft.Log<int>)vlog).Value; break;
            }
        }

        public override void FollowerApply(Zeze.Raft.RocksRaft.Log log)
        {
            var blog = (Zeze.Raft.RocksRaft.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _State = ((Zeze.Raft.RocksRaft.Log<int>)vlog).Value; break;
                }
            }
        }

    }
}
