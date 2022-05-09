// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.Collections.Queue
{
    public interface BQueueNodeValueReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public long Timestamp { get; }
        public Zeze.Transaction.DynamicBeanReadOnly Value { get; }

    }

    public sealed class BQueueNodeValue : Zeze.Transaction.Bean, BQueueNodeValueReadOnly
    {
        long _Timestamp;
        readonly Zeze.Transaction.DynamicBean _Value;

        public long Timestamp
        {
            get
            {
                if (!IsManaged)
                    return _Timestamp;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Timestamp;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Timestamp)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _Timestamp;
            }
            set
            {
                if (!IsManaged)
                {
                    _Timestamp = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Timestamp() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public Zeze.Transaction.DynamicBean Value => _Value;
        Zeze.Transaction.DynamicBeanReadOnly Zeze.Builtin.Collections.Queue.BQueueNodeValueReadOnly.Value => Value;

        public BQueueNodeValue() : this(0)
        {
        }

        public BQueueNodeValue(int _varId_) : base(_varId_)
        {
            _Value = new Zeze.Transaction.DynamicBean(2, Zeze.Collections.Queue.GetSpecialTypeIdFromBean, Zeze.Collections.Queue.CreateBeanFromSpecialTypeId);
        }

        public void Assign(BQueueNodeValue other)
        {
            Timestamp = other.Timestamp;
            Value.Assign(other.Value);
        }

        public BQueueNodeValue CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BQueueNodeValue Copy()
        {
            var copy = new BQueueNodeValue();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BQueueNodeValue a, BQueueNodeValue b)
        {
            BQueueNodeValue save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = 486912310764000976;
        public override long TypeId => TYPEID;

        sealed class Log__Timestamp : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BQueueNodeValue)Belong)._Timestamp = this.Value; }
        }

        public static long GetSpecialTypeIdFromBean_Value(Zeze.Transaction.Bean bean)
        {
            switch (bean.TypeId)
            {
                case Zeze.Transaction.EmptyBean.TYPEID: return Zeze.Transaction.EmptyBean.TYPEID;
            }
            throw new System.Exception("Unknown Bean! dynamic@Zeze.Builtin.Collections.Queue.BQueueNodeValue:Value");
        }

        public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_Value(long typeId)
        {
            return null;
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Collections.Queue.BQueueNodeValue: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Timestamp").Append('=').Append(Timestamp).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Value").Append('=').Append(Environment.NewLine);
            Value.Bean.BuildString(sb, level + 4);
            sb.Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = Timestamp;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                var _x_ = Value;
                if (!_x_.IsEmpty())
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.DYNAMIC);
                    _x_.Encode(_o_);
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
                Timestamp = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                _o_.ReadDynamic(Value, _t_);
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
            _Value.InitRootInfo(root, this);
        }

        public override bool NegativeCheck()
        {
            if (Timestamp < 0) return true;
            return false;
        }
        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _Timestamp = ((Zeze.Transaction.Log<long>)vlog).Value; break;
                    case 2: _Value.FollowerApply(vlog); break;
                }
            }
        }

    }
}
