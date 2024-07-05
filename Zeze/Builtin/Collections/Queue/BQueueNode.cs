// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// 一个节点可以存多个KeyValue对，
// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Collections.Queue
{
    public interface BQueueNodeReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BQueueNode Copy();

        public long NextNodeId { get; }
        public System.Collections.Generic.IReadOnlyList<Zeze.Builtin.Collections.Queue.BQueueNodeValueReadOnly>Values { get; }
    }

    public sealed class BQueueNode : Zeze.Transaction.Bean, BQueueNodeReadOnly
    {
        long _NextNodeId; // 后一个节点ID. 0表示已到达结尾。
        readonly Zeze.Transaction.Collections.CollList2<Zeze.Builtin.Collections.Queue.BQueueNodeValue> _Values;

        public long NextNodeId
        {
            get
            {
                if (!IsManaged)
                    return _NextNodeId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _NextNodeId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__NextNodeId)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _NextNodeId;
            }
            set
            {
                if (!IsManaged)
                {
                    _NextNodeId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__NextNodeId() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public Zeze.Transaction.Collections.CollList2<Zeze.Builtin.Collections.Queue.BQueueNodeValue> Values => _Values;
        System.Collections.Generic.IReadOnlyList<Zeze.Builtin.Collections.Queue.BQueueNodeValueReadOnly> Zeze.Builtin.Collections.Queue.BQueueNodeReadOnly.Values => _Values;

        public BQueueNode()
        {
            _Values = new Zeze.Transaction.Collections.CollList2<Zeze.Builtin.Collections.Queue.BQueueNodeValue>() { VariableId = 2 };
        }

        public BQueueNode(long _NextNodeId_)
        {
            _NextNodeId = _NextNodeId_;
            _Values = new Zeze.Transaction.Collections.CollList2<Zeze.Builtin.Collections.Queue.BQueueNodeValue>() { VariableId = 2 };
        }

        public void Assign(BQueueNode other)
        {
            NextNodeId = other.NextNodeId;
            Values.Clear();
            foreach (var e in other.Values)
                Values.Add(e.Copy());
        }

        public BQueueNode CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BQueueNode Copy()
        {
            var copy = new BQueueNode();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BQueueNode a, BQueueNode b)
        {
            BQueueNode save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 400956918018571167;
        public override long TypeId => TYPEID;

        sealed class Log__NextNodeId : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BQueueNode)Belong)._NextNodeId = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Collections.Queue.BQueueNode: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("NextNodeId").Append('=').Append(NextNodeId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Values").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in Values)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Environment.NewLine);
                Item.BuildString(sb, level + 4);
                sb.Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = NextNodeId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                var _x_ = Values;
                int _n_ = _x_?.Count ?? 0;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.BEAN);
                    foreach (var _v_ in _x_)
                    {
                        _v_.Encode(_o_);
                        _n_--;
                    }
                    if (_n_ != 0)
                        throw new System.Exception(_n_.ToString());
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
                NextNodeId = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                var _x_ = Values;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    {
                        _x_.Add(_o_.ReadBean(new Zeze.Builtin.Collections.Queue.BQueueNodeValue(), _t_));
                    }
                }
                else
                    _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
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
            _Values.InitRootInfo(root, this);
        }

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _Values.InitRootInfoWithRedo(root, this);
        }

        public override bool NegativeCheck()
        {
            if (NextNodeId < 0) return true;
            foreach (var _v_ in Values)
            {
                if (_v_.NegativeCheck()) return true;
            }
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _NextNodeId = vlog.LongValue(); break;
                    case 2: _Values.FollowerApply(vlog); break;
                }
            }
        }

        public override void ClearParameters()
        {
            NextNodeId = 0;
            Values.Clear();
        }
    }
}
