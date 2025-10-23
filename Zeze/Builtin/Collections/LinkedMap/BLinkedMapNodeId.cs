// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Collections.LinkedMap
{
    public interface BLinkedMapNodeIdReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BLinkedMapNodeId Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public long NodeId { get; }
    }

    public sealed class BLinkedMapNodeId : Zeze.Transaction.Bean, BLinkedMapNodeIdReadOnly
    {
        long _NodeId; // KeyValue对所属的节点ID. 每个节点有多个KeyValue对共享

        public long NodeId
        {
            get
            {
                if (!IsManaged)
                    return _NodeId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _NodeId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__NodeId)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _NodeId;
            }
            set
            {
                if (!IsManaged)
                {
                    _NodeId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__NodeId() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public BLinkedMapNodeId()
        {
        }

        public BLinkedMapNodeId(long _NodeId_)
        {
            _NodeId = _NodeId_;
        }

        public void Assign(BLinkedMapNodeId other)
        {
            NodeId = other.NodeId;
        }

        public BLinkedMapNodeId CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BLinkedMapNodeId Copy()
        {
            var copy = new BLinkedMapNodeId();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BLinkedMapNodeId a, BLinkedMapNodeId b)
        {
            BLinkedMapNodeId save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -6424218657633143196;
        public override long TypeId => TYPEID;

        sealed class Log__NodeId : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BLinkedMapNodeId)Belong)._NodeId = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("NodeId").Append('=').Append(NodeId).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = NodeId;
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
                NodeId = _o_.ReadLong(_t_);
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
            if (NodeId < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _NodeId = vlog.LongValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            NodeId = 0;
        }
    }
}
