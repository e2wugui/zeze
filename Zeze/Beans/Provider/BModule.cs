// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Beans.Provider
{
    public interface BModuleReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public int ChoiceType { get; }
        public int ConfigType { get; }
        public int SubscribeType { get; }
    }

    public sealed class BModule : Zeze.Transaction.Bean, BModuleReadOnly
    {
        public const int ChoiceTypeDefault = 0; // choice by load
        public const int ChoiceTypeHashAccount = 1;
        public const int ChoiceTypeHashRoleId = 2;
        public const int ChoiceTypeFeedFullOneByOne = 3;
        public const int ConfigTypeDefault = 0;
        public const int ConfigTypeSpecial = 1;
        public const int ConfigTypeDynamic = 2;

        int _ChoiceType;
        int _ConfigType;
        int _SubscribeType;

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
                txn.PutLog(new Log__ChoiceType(this, value));
            }
        }

        public int ConfigType
        {
            get
            {
                if (!IsManaged)
                    return _ConfigType;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ConfigType;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ConfigType)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _ConfigType;
            }
            set
            {
                if (!IsManaged)
                {
                    _ConfigType = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ConfigType(this, value));
            }
        }

        public int SubscribeType
        {
            get
            {
                if (!IsManaged)
                    return _SubscribeType;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _SubscribeType;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__SubscribeType)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _SubscribeType;
            }
            set
            {
                if (!IsManaged)
                {
                    _SubscribeType = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__SubscribeType(this, value));
            }
        }

        public BModule() : this(0)
        {
        }

        public BModule(int _varId_) : base(_varId_)
        {
        }

        public void Assign(BModule other)
        {
            ChoiceType = other.ChoiceType;
            ConfigType = other.ConfigType;
            SubscribeType = other.SubscribeType;
        }

        public BModule CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BModule Copy()
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

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = 4661660648607110389;
        public override long TypeId => TYPEID;

        sealed class Log__ChoiceType : Zeze.Transaction.Log<BModule, int>
        {
            public Log__ChoiceType(BModule self, int value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 1;
            public override void Commit() { this.BeanTyped._ChoiceType = this.Value; }
        }

        sealed class Log__ConfigType : Zeze.Transaction.Log<BModule, int>
        {
            public Log__ConfigType(BModule self, int value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 2;
            public override void Commit() { this.BeanTyped._ConfigType = this.Value; }
        }

        sealed class Log__SubscribeType : Zeze.Transaction.Log<BModule, int>
        {
            public Log__SubscribeType(BModule self, int value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 3;
            public override void Commit() { this.BeanTyped._SubscribeType = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Beans.Provider.BModule: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ChoiceType").Append('=').Append(ChoiceType).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ConfigType").Append('=').Append(ConfigType).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("SubscribeType").Append('=').Append(SubscribeType).Append(Environment.NewLine);
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
                int _x_ = ConfigType;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = SubscribeType;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
                ChoiceType = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                ConfigType = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                SubscribeType = _o_.ReadInt(_t_);
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
            if (ChoiceType < 0) return true;
            if (ConfigType < 0) return true;
            if (SubscribeType < 0) return true;
            return false;
        }
    }
}
