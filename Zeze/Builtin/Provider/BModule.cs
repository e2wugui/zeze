// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// gs to link
// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Provider
{
    public interface BModuleReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BModule Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public int ChoiceType { get; }
        public bool Dynamic { get; }
    }

    public sealed class BModule : Zeze.Transaction.Bean, BModuleReadOnly
    {
        public const int ChoiceTypeDefault = 0; // choice by load
        public const int ChoiceTypeHashAccount = 1;
        public const int ChoiceTypeHashRoleId = 2;
        public const int ChoiceTypeFeedFullOneByOne = 3;

        int _ChoiceType;
        bool _Dynamic;

        public int _zeze_map_key_int_ { get; set; }

        public int ChoiceType
        {
            get
            {
                if (!IsManaged)
                    return _ChoiceType;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ChoiceType;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ChoiceType)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _ChoiceType;
            }
            set
            {
                if (!IsManaged)
                {
                    _ChoiceType = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ChoiceType() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public bool Dynamic
        {
            get
            {
                if (!IsManaged)
                    return _Dynamic;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Dynamic;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Dynamic)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _Dynamic;
            }
            set
            {
                if (!IsManaged)
                {
                    _Dynamic = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Dynamic() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public BModule()
        {
        }

        public BModule(int _ChoiceType_, bool _Dynamic_)
        {
            _ChoiceType = _ChoiceType_;
            _Dynamic = _Dynamic_;
        }

        public void Assign(BModule other)
        {
            ChoiceType = other.ChoiceType;
            Dynamic = other.Dynamic;
        }

        public BModule CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BModule Copy()
        {
            var copy = new BModule();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BModule a, BModule b)
        {
            BModule save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 5883923521926593765;
        public override long TypeId => TYPEID;

        sealed class Log__ChoiceType : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BModule)Belong)._ChoiceType = this.Value; }
        }

        sealed class Log__Dynamic : Zeze.Transaction.Log<bool>
        {
            public override void Commit() { ((BModule)Belong)._Dynamic = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Provider.BModule: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ChoiceType").Append('=').Append(ChoiceType).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Dynamic").Append('=').Append(Dynamic).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = ChoiceType;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                bool _x_ = Dynamic;
                if (_x_)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteByte(1);
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
                ChoiceType = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                Dynamic = _o_.ReadBool(_t_);
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
            if (ChoiceType < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _ChoiceType = vlog.IntValue(); break;
                    case 2: _Dynamic = vlog.BoolValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            ChoiceType = 0;
            Dynamic = false;
        }
    }
}
