// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Collections.LinkedMap
{
    public interface BLinkedMapNodeValueReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BLinkedMapNodeValue Copy();

        public string Id { get; }
        public Zeze.Transaction.DynamicBeanReadOnly Value { get; }

    }

    public sealed class BLinkedMapNodeValue : Zeze.Transaction.Bean, BLinkedMapNodeValueReadOnly
    {
        string _Id; // LinkedMap的Key转成字符串类型
        readonly Zeze.Transaction.DynamicBean _Value;
        public static Zeze.Transaction.DynamicBean NewDynamicBeanValue()
        {
            return new Zeze.Transaction.DynamicBean(2, Zeze.Collections.LinkedMap.GetSpecialTypeIdFromBean, Zeze.Collections.LinkedMap.CreateBeanFromSpecialTypeId);
        }

        public static long GetSpecialTypeIdFromBean_2(Zeze.Transaction.Bean bean)
        {
            return Zeze.Collections.LinkedMap.GetSpecialTypeIdFromBean(bean);
        }

        public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_2(long typeId)
        {
            return Zeze.Collections.LinkedMap.CreateBeanFromSpecialTypeId(typeId);
        }


        public string Id
        {
            get
            {
                if (!IsManaged)
                    return _Id;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Id;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Id)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _Id;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _Id = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Id() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public Zeze.Transaction.DynamicBean Value => _Value;
        Zeze.Transaction.DynamicBeanReadOnly Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeValueReadOnly.Value => Value;

        public BLinkedMapNodeValue()
        {
            _Id = "";
            _Value = new Zeze.Transaction.DynamicBean(2, Zeze.Collections.LinkedMap.GetSpecialTypeIdFromBean, Zeze.Collections.LinkedMap.CreateBeanFromSpecialTypeId);
        }

        public BLinkedMapNodeValue(string _Id_)
        {
            _Id = _Id_;
            _Value = new Zeze.Transaction.DynamicBean(2, Zeze.Collections.LinkedMap.GetSpecialTypeIdFromBean, Zeze.Collections.LinkedMap.CreateBeanFromSpecialTypeId);
        }

        public void Assign(BLinkedMapNodeValue other)
        {
            Id = other.Id;
            Value.Assign(other.Value);
        }

        public BLinkedMapNodeValue CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BLinkedMapNodeValue Copy()
        {
            var copy = new BLinkedMapNodeValue();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BLinkedMapNodeValue a, BLinkedMapNodeValue b)
        {
            BLinkedMapNodeValue save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -6110801358414370128;
        public override long TypeId => TYPEID;

        sealed class Log__Id : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BLinkedMapNodeValue)Belong)._Id = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeValue: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Id").Append('=').Append(Id).Append(',').Append(Environment.NewLine);
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
                string _x_ = Id;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
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
                Id = _o_.ReadString(_t_);
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

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _Value.InitRootInfoWithRedo(root, this);
        }

        public override bool NegativeCheck()
        {
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _Id = vlog.StringValue(); break;
                    case 2: _Value.FollowerApply(vlog); break;
                }
            }
        }

        public override void ClearParameters()
        {
            Id = "";
            Value.ClearParameters();
        }
    }
}
