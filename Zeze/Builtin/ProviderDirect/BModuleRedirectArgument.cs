// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.ProviderDirect
{
    public interface BModuleRedirectArgumentReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BModuleRedirectArgument Copy();

        public int ModuleId { get; }
        public int HashCode { get; }
        public int RedirectType { get; }
        public string MethodFullName { get; }
        public Zeze.Net.Binary Params { get; }
        public string ServiceNamePrefix { get; }
    }

    public sealed class BModuleRedirectArgument : Zeze.Transaction.Bean, BModuleRedirectArgumentReadOnly
    {
        int _ModuleId;
        int _HashCode; // server 计算。see BBind.ChoiceType。
        int _RedirectType; // 如果是ToServer，ServerId存在HashCode中。
        string _MethodFullName; // format="ModuleFullName:MethodName"
        Zeze.Net.Binary _Params;
        string _ServiceNamePrefix;

        public int ModuleId
        {
            get
            {
                if (!IsManaged)
                    return _ModuleId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ModuleId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ModuleId)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _ModuleId;
            }
            set
            {
                if (!IsManaged)
                {
                    _ModuleId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ModuleId() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public int HashCode
        {
            get
            {
                if (!IsManaged)
                    return _HashCode;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _HashCode;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__HashCode)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _HashCode;
            }
            set
            {
                if (!IsManaged)
                {
                    _HashCode = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__HashCode() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public int RedirectType
        {
            get
            {
                if (!IsManaged)
                    return _RedirectType;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _RedirectType;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__RedirectType)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _RedirectType;
            }
            set
            {
                if (!IsManaged)
                {
                    _RedirectType = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__RedirectType() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public string MethodFullName
        {
            get
            {
                if (!IsManaged)
                    return _MethodFullName;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _MethodFullName;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__MethodFullName)txn.GetLog(ObjectId + 4);
                return log != null ? log.Value : _MethodFullName;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _MethodFullName = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__MethodFullName() { Belong = this, VariableId = 4, Value = value });
            }
        }

        public Zeze.Net.Binary Params
        {
            get
            {
                if (!IsManaged)
                    return _Params;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Params;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Params)txn.GetLog(ObjectId + 5);
                return log != null ? log.Value : _Params;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _Params = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Params() { Belong = this, VariableId = 5, Value = value });
            }
        }

        public string ServiceNamePrefix
        {
            get
            {
                if (!IsManaged)
                    return _ServiceNamePrefix;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ServiceNamePrefix;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ServiceNamePrefix)txn.GetLog(ObjectId + 6);
                return log != null ? log.Value : _ServiceNamePrefix;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _ServiceNamePrefix = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ServiceNamePrefix() { Belong = this, VariableId = 6, Value = value });
            }
        }

        public BModuleRedirectArgument()
        {
            _MethodFullName = "";
            _Params = Zeze.Net.Binary.Empty;
            _ServiceNamePrefix = "";
        }

        public BModuleRedirectArgument(int _ModuleId_, int _HashCode_, int _RedirectType_, string _MethodFullName_, Zeze.Net.Binary _Params_, string _ServiceNamePrefix_)
        {
            _ModuleId = _ModuleId_;
            _HashCode = _HashCode_;
            _RedirectType = _RedirectType_;
            _MethodFullName = _MethodFullName_;
            _Params = _Params_;
            _ServiceNamePrefix = _ServiceNamePrefix_;
        }

        public void Assign(BModuleRedirectArgument other)
        {
            ModuleId = other.ModuleId;
            HashCode = other.HashCode;
            RedirectType = other.RedirectType;
            MethodFullName = other.MethodFullName;
            Params = other.Params;
            ServiceNamePrefix = other.ServiceNamePrefix;
        }

        public BModuleRedirectArgument CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BModuleRedirectArgument Copy()
        {
            var copy = new BModuleRedirectArgument();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BModuleRedirectArgument a, BModuleRedirectArgument b)
        {
            BModuleRedirectArgument save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -5561456902586805165;
        public override long TypeId => TYPEID;

        sealed class Log__ModuleId : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BModuleRedirectArgument)Belong)._ModuleId = this.Value; }
        }

        sealed class Log__HashCode : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BModuleRedirectArgument)Belong)._HashCode = this.Value; }
        }

        sealed class Log__RedirectType : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BModuleRedirectArgument)Belong)._RedirectType = this.Value; }
        }

        sealed class Log__MethodFullName : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BModuleRedirectArgument)Belong)._MethodFullName = this.Value; }
        }

        sealed class Log__Params : Zeze.Transaction.Log<Zeze.Net.Binary>
        {
            public override void Commit() { ((BModuleRedirectArgument)Belong)._Params = this.Value; }
        }

        sealed class Log__ServiceNamePrefix : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BModuleRedirectArgument)Belong)._ServiceNamePrefix = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.ProviderDirect.BModuleRedirectArgument: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ModuleId").Append('=').Append(ModuleId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("HashCode").Append('=').Append(HashCode).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("RedirectType").Append('=').Append(RedirectType).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("MethodFullName").Append('=').Append(MethodFullName).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Params").Append('=').Append(Params).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ServiceNamePrefix").Append('=').Append(ServiceNamePrefix).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = ModuleId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = HashCode;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = RedirectType;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                string _x_ = MethodFullName;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = Params;
                if (_x_ != null && _x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                string _x_ = ServiceNamePrefix;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
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
                ModuleId = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                HashCode = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                RedirectType = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                MethodFullName = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                Params = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 6)
            {
                ServiceNamePrefix = _o_.ReadString(_t_);
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
            if (ModuleId < 0) return true;
            if (HashCode < 0) return true;
            if (RedirectType < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _ModuleId = vlog.IntValue(); break;
                    case 2: _HashCode = vlog.IntValue(); break;
                    case 3: _RedirectType = vlog.IntValue(); break;
                    case 4: _MethodFullName = vlog.StringValue(); break;
                    case 5: _Params = vlog.BinaryValue(); break;
                    case 6: _ServiceNamePrefix = vlog.StringValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            ModuleId = 0;
            HashCode = 0;
            RedirectType = 0;
            MethodFullName = "";
            Params = Zeze.Net.Binary.Empty;
            ServiceNamePrefix = "";
        }
    }
}
