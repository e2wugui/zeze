// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.RedoQueue
{
    public interface BTaskIdReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BTaskId Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public long TaskId { get; }
    }

    public sealed class BTaskId : Zeze.Transaction.Bean, BTaskIdReadOnly
    {
        long _TaskId;

        public long TaskId
        {
            get
            {
                if (!IsManaged)
                    return _TaskId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _TaskId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__TaskId)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _TaskId;
            }
            set
            {
                if (!IsManaged)
                {
                    _TaskId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__TaskId() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public BTaskId()
        {
        }

        public BTaskId(long _TaskId_)
        {
            _TaskId = _TaskId_;
        }

        public void Assign(BTaskId other)
        {
            TaskId = other.TaskId;
        }

        public BTaskId CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BTaskId Copy()
        {
            var copy = new BTaskId();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BTaskId a, BTaskId b)
        {
            BTaskId save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -3646825359403112989;
        public override long TypeId => TYPEID;

        sealed class Log__TaskId : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BTaskId)Belong)._TaskId = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.RedoQueue.BTaskId: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("TaskId").Append('=').Append(TaskId).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = TaskId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
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
                TaskId = _o_.ReadLong(_t_);
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
            if (TaskId < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _TaskId = vlog.LongValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            TaskId = 0;
        }
    }
}
