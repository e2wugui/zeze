// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.LoginQueue
{
    public interface BQueuePositionReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BQueuePosition Copy();

        public int QueuePosition { get; }
    }

    public sealed class BQueuePosition : Zeze.Transaction.Bean, BQueuePositionReadOnly
    {
        int _QueuePosition;

        public int QueuePosition
        {
            get
            {
                if (!IsManaged)
                    return _QueuePosition;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _QueuePosition;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__QueuePosition)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _QueuePosition;
            }
            set
            {
                if (!IsManaged)
                {
                    _QueuePosition = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__QueuePosition() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public BQueuePosition()
        {
        }

        public BQueuePosition(int _QueuePosition_)
        {
            _QueuePosition = _QueuePosition_;
        }

        public void Assign(BQueuePosition other)
        {
            QueuePosition = other.QueuePosition;
        }

        public BQueuePosition CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BQueuePosition Copy()
        {
            var copy = new BQueuePosition();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BQueuePosition a, BQueuePosition b)
        {
            BQueuePosition save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 7306227661961276364;
        public override long TypeId => TYPEID;

        sealed class Log__QueuePosition : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BQueuePosition)Belong)._QueuePosition = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.LoginQueue.BQueuePosition: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("QueuePosition").Append('=').Append(QueuePosition).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = QueuePosition;
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
                QueuePosition = _o_.ReadInt(_t_);
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
            if (QueuePosition < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _QueuePosition = vlog.IntValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            QueuePosition = 0;
        }
    }
}
