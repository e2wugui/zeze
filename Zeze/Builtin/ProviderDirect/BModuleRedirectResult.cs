// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.ProviderDirect
{
    public interface BModuleRedirectResultReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BModuleRedirectResult Copy();

        public int ModuleId { get; }
        public int ServerId { get; }
        public Zeze.Net.Binary Params { get; }
    }

    public sealed class BModuleRedirectResult : Zeze.Transaction.Bean, BModuleRedirectResultReadOnly
    {
        int _ModuleId;
        int _ServerId; // 目标server的id。
        Zeze.Net.Binary _Params;

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

        public int ServerId
        {
            get
            {
                if (!IsManaged)
                    return _ServerId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ServerId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ServerId)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _ServerId;
            }
            set
            {
                if (!IsManaged)
                {
                    _ServerId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ServerId() { Belong = this, VariableId = 2, Value = value });
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
                var log = (Log__Params)txn.GetLog(ObjectId + 3);
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
                txn.PutLog(new Log__Params() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public BModuleRedirectResult()
        {
            _Params = Zeze.Net.Binary.Empty;
        }

        public BModuleRedirectResult(int _ModuleId_, int _ServerId_, Zeze.Net.Binary _Params_)
        {
            _ModuleId = _ModuleId_;
            _ServerId = _ServerId_;
            _Params = _Params_;
        }

        public void Assign(BModuleRedirectResult other)
        {
            ModuleId = other.ModuleId;
            ServerId = other.ServerId;
            Params = other.Params;
        }

        public BModuleRedirectResult CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BModuleRedirectResult Copy()
        {
            var copy = new BModuleRedirectResult();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BModuleRedirectResult a, BModuleRedirectResult b)
        {
            BModuleRedirectResult save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 6325051164605397555;
        public override long TypeId => TYPEID;

        sealed class Log__ModuleId : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BModuleRedirectResult)Belong)._ModuleId = this.Value; }
        }

        sealed class Log__ServerId : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BModuleRedirectResult)Belong)._ServerId = this.Value; }
        }

        sealed class Log__Params : Zeze.Transaction.Log<Zeze.Net.Binary>
        {
            public override void Commit() { ((BModuleRedirectResult)Belong)._Params = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.ProviderDirect.BModuleRedirectResult: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ModuleId").Append('=').Append(ModuleId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ServerId").Append('=').Append(ServerId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Params").Append('=').Append(Params).Append(Environment.NewLine);
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
                int _x_ = ServerId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                var _x_ = Params;
                if (_x_ != null && _x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
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
                ServerId = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                Params = _o_.ReadBinary(_t_);
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
            if (ServerId < 0) return true;
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
                    case 2: _ServerId = vlog.IntValue(); break;
                    case 3: _Params = vlog.BinaryValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            ModuleId = 0;
            ServerId = 0;
            Params = Zeze.Net.Binary.Empty;
        }
    }
}
