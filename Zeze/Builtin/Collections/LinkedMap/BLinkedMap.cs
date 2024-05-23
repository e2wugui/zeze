// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Collections.LinkedMap
{
    public interface BLinkedMapReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BLinkedMap Copy();

        public long HeadNodeId { get; }
        public long TailNodeId { get; }
        public long Count { get; }
        public long LastNodeId { get; }
    }

    public sealed class BLinkedMap : Zeze.Transaction.Bean, BLinkedMapReadOnly
    {
        long _HeadNodeId;
        long _TailNodeId;
        long _Count;
        long _LastNodeId; // 最近分配过的NodeId, 用于下次分配

        public long HeadNodeId
        {
            get
            {
                if (!IsManaged)
                    return _HeadNodeId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _HeadNodeId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__HeadNodeId)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _HeadNodeId;
            }
            set
            {
                if (!IsManaged)
                {
                    _HeadNodeId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__HeadNodeId() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public long TailNodeId
        {
            get
            {
                if (!IsManaged)
                    return _TailNodeId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _TailNodeId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__TailNodeId)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _TailNodeId;
            }
            set
            {
                if (!IsManaged)
                {
                    _TailNodeId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__TailNodeId() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public long Count
        {
            get
            {
                if (!IsManaged)
                    return _Count;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Count;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Count)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _Count;
            }
            set
            {
                if (!IsManaged)
                {
                    _Count = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Count() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public long LastNodeId
        {
            get
            {
                if (!IsManaged)
                    return _LastNodeId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _LastNodeId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__LastNodeId)txn.GetLog(ObjectId + 4);
                return log != null ? log.Value : _LastNodeId;
            }
            set
            {
                if (!IsManaged)
                {
                    _LastNodeId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__LastNodeId() { Belong = this, VariableId = 4, Value = value });
            }
        }

        public BLinkedMap()
        {
        }

        public BLinkedMap(long _HeadNodeId_, long _TailNodeId_, long _Count_, long _LastNodeId_)
        {
            _HeadNodeId = _HeadNodeId_;
            _TailNodeId = _TailNodeId_;
            _Count = _Count_;
            _LastNodeId = _LastNodeId_;
        }

        public void Assign(BLinkedMap other)
        {
            HeadNodeId = other.HeadNodeId;
            TailNodeId = other.TailNodeId;
            Count = other.Count;
            LastNodeId = other.LastNodeId;
        }

        public BLinkedMap CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BLinkedMap Copy()
        {
            var copy = new BLinkedMap();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BLinkedMap a, BLinkedMap b)
        {
            BLinkedMap save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -8443895985300072767;
        public override long TypeId => TYPEID;

        sealed class Log__HeadNodeId : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BLinkedMap)Belong)._HeadNodeId = this.Value; }
        }

        sealed class Log__TailNodeId : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BLinkedMap)Belong)._TailNodeId = this.Value; }
        }

        sealed class Log__Count : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BLinkedMap)Belong)._Count = this.Value; }
        }

        sealed class Log__LastNodeId : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BLinkedMap)Belong)._LastNodeId = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Collections.LinkedMap.BLinkedMap: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("HeadNodeId").Append('=').Append(HeadNodeId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("TailNodeId").Append('=').Append(TailNodeId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Count").Append('=').Append(Count).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LastNodeId").Append('=').Append(LastNodeId).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = HeadNodeId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                long _x_ = TailNodeId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                long _x_ = Count;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                long _x_ = LastNodeId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
                HeadNodeId = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                TailNodeId = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                Count = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                LastNodeId = _o_.ReadLong(_t_);
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
            if (HeadNodeId < 0) return true;
            if (TailNodeId < 0) return true;
            if (Count < 0) return true;
            if (LastNodeId < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _HeadNodeId = vlog.LongValue(); break;
                    case 2: _TailNodeId = vlog.LongValue(); break;
                    case 3: _Count = vlog.LongValue(); break;
                    case 4: _LastNodeId = vlog.LongValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            HeadNodeId = 0;
            TailNodeId = 0;
            Count = 0;
            LastNodeId = 0;
        }
    }
}
