// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Beans.ProviderDirect
{
    public interface BTransmitReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public string ActionName { get; }
        public System.Collections.Generic.IReadOnlyDictionary<long,Zeze.Beans.ProviderDirect.BTransmitContextReadOnly> Roles { get; }
        public long Sender { get; }
        public string ServiceNamePrefix { get; }
        public string ParameterBeanName { get; }
        public Zeze.Net.Binary ParameterBeanValue { get; }
    }

    public sealed class BTransmit : Zeze.Transaction.Bean, BTransmitReadOnly
    {
        string _ActionName;
        readonly Zeze.Transaction.Collections.PMap2<long, Zeze.Beans.ProviderDirect.BTransmitContext> _Roles; // 查询目标角色。
        Zeze.Transaction.Collections.PMapReadOnly<long,Zeze.Beans.ProviderDirect.BTransmitContextReadOnly,Zeze.Beans.ProviderDirect.BTransmitContext> _RolesReadOnly;
        long _Sender; // 结果发送给Sender。
        string _ServiceNamePrefix;
        string _ParameterBeanName; // fullname
        Zeze.Net.Binary _ParameterBeanValue; // encoded bean

        public string ActionName
        {
            get
            {
                if (!IsManaged)
                    return _ActionName;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ActionName;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ActionName)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _ActionName;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _ActionName = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ActionName(this, value));
            }
        }

        public Zeze.Transaction.Collections.PMap2<long, Zeze.Beans.ProviderDirect.BTransmitContext> Roles => _Roles;
        System.Collections.Generic.IReadOnlyDictionary<long,Zeze.Beans.ProviderDirect.BTransmitContextReadOnly> Zeze.Beans.ProviderDirect.BTransmitReadOnly.Roles => _RolesReadOnly;

        public long Sender
        {
            get
            {
                if (!IsManaged)
                    return _Sender;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Sender;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Sender)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _Sender;
            }
            set
            {
                if (!IsManaged)
                {
                    _Sender = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Sender(this, value));
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
                var log = (Log__ServiceNamePrefix)txn.GetLog(ObjectId + 4);
                return log != null ? log.Value : _ServiceNamePrefix;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _ServiceNamePrefix = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ServiceNamePrefix(this, value));
            }
        }

        public string ParameterBeanName
        {
            get
            {
                if (!IsManaged)
                    return _ParameterBeanName;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ParameterBeanName;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ParameterBeanName)txn.GetLog(ObjectId + 5);
                return log != null ? log.Value : _ParameterBeanName;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _ParameterBeanName = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ParameterBeanName(this, value));
            }
        }

        public Zeze.Net.Binary ParameterBeanValue
        {
            get
            {
                if (!IsManaged)
                    return _ParameterBeanValue;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ParameterBeanValue;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ParameterBeanValue)txn.GetLog(ObjectId + 6);
                return log != null ? log.Value : _ParameterBeanValue;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _ParameterBeanValue = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ParameterBeanValue(this, value));
            }
        }

        public BTransmit() : this(0)
        {
        }

        public BTransmit(int _varId_) : base(_varId_)
        {
            _ActionName = "";
            _Roles = new Zeze.Transaction.Collections.PMap2<long, Zeze.Beans.ProviderDirect.BTransmitContext>(ObjectId + 2, _v => new Log__Roles(this, _v));
            _RolesReadOnly = new Zeze.Transaction.Collections.PMapReadOnly<long,Zeze.Beans.ProviderDirect.BTransmitContextReadOnly,Zeze.Beans.ProviderDirect.BTransmitContext>(_Roles);
            _ServiceNamePrefix = "";
            _ParameterBeanName = "";
            _ParameterBeanValue = Zeze.Net.Binary.Empty;
        }

        public void Assign(BTransmit other)
        {
            ActionName = other.ActionName;
            Roles.Clear();
            foreach (var e in other.Roles)
                Roles.Add(e.Key, e.Value.Copy());
            Sender = other.Sender;
            ServiceNamePrefix = other.ServiceNamePrefix;
            ParameterBeanName = other.ParameterBeanName;
            ParameterBeanValue = other.ParameterBeanValue;
        }

        public BTransmit CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BTransmit Copy()
        {
            var copy = new BTransmit();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BTransmit a, BTransmit b)
        {
            BTransmit save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = -5782855291832116376;
        public override long TypeId => TYPEID;

        sealed class Log__ActionName : Zeze.Transaction.Log<BTransmit, string>
        {
            public Log__ActionName(BTransmit self, string value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 1;
            public override void Commit() { this.BeanTyped._ActionName = this.Value; }
        }

        sealed class Log__Roles : Zeze.Transaction.Collections.PMap2<long, Zeze.Beans.ProviderDirect.BTransmitContext>.LogV
        {
            public Log__Roles(BTransmit host, System.Collections.Immutable.ImmutableDictionary<long, Zeze.Beans.ProviderDirect.BTransmitContext> value) : base(host, value) {}
            public override long LogKey => Bean.ObjectId + 2;
            public BTransmit BeanTyped => (BTransmit)Bean;
            public override void Commit() { Commit(BeanTyped._Roles); }
        }

        sealed class Log__Sender : Zeze.Transaction.Log<BTransmit, long>
        {
            public Log__Sender(BTransmit self, long value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 3;
            public override void Commit() { this.BeanTyped._Sender = this.Value; }
        }

        sealed class Log__ServiceNamePrefix : Zeze.Transaction.Log<BTransmit, string>
        {
            public Log__ServiceNamePrefix(BTransmit self, string value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 4;
            public override void Commit() { this.BeanTyped._ServiceNamePrefix = this.Value; }
        }

        sealed class Log__ParameterBeanName : Zeze.Transaction.Log<BTransmit, string>
        {
            public Log__ParameterBeanName(BTransmit self, string value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 5;
            public override void Commit() { this.BeanTyped._ParameterBeanName = this.Value; }
        }

        sealed class Log__ParameterBeanValue : Zeze.Transaction.Log<BTransmit, Zeze.Net.Binary>
        {
            public Log__ParameterBeanValue(BTransmit self, Zeze.Net.Binary value) : base(self, value) {}
            public override long LogKey => this.Bean.ObjectId + 6;
            public override void Commit() { this.BeanTyped._ParameterBeanValue = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Beans.ProviderDirect.BTransmit: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ActionName").Append('=').Append(ActionName).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Roles").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var _kv_ in Roles)
            {
                sb.Append('(').Append(Environment.NewLine);
                var Key = _kv_.Key;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Key").Append('=').Append(Key).Append(',').Append(Environment.NewLine);
                var Value = _kv_.Value;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Value").Append('=').Append(Environment.NewLine);
                Value.BuildString(sb, level + 4);
                sb.Append(',').Append(Environment.NewLine);
                sb.Append(')').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Sender").Append('=').Append(Sender).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ServiceNamePrefix").Append('=').Append(ServiceNamePrefix).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ParameterBeanName").Append('=').Append(ParameterBeanName).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ParameterBeanValue").Append('=').Append(ParameterBeanValue).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = ActionName;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = Roles;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                    _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                    foreach (var _e_ in _x_)
                    {
                        _o_.WriteLong(_e_.Key);
                        _e_.Value.Encode(_o_);
                    }
                }
            }
            {
                long _x_ = Sender;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                string _x_ = ServiceNamePrefix;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                string _x_ = ParameterBeanName;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = ParameterBeanValue;
                if (_x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
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
                ActionName = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                var _x_ = Roles;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP)
                {
                    int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                    for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--)
                    {
                        var _k_ = _o_.ReadLong(_s_);
                        var _v_ = _o_.ReadBean(new Zeze.Beans.ProviderDirect.BTransmitContext(), _t_);
                        _x_.Add(_k_, _v_);
                    }
                }
                else
                    _o_.SkipUnknownField(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                Sender = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                ServiceNamePrefix = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                ParameterBeanName = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 6)
            {
                ParameterBeanValue = _o_.ReadBinary(_t_);
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
            _Roles.InitRootInfo(root, this);
        }

        public override bool NegativeCheck()
        {
            foreach (var _v_ in Roles.Values)
            {
                if (_v_.NegativeCheck()) return true;
            }
            if (Sender < 0) return true;
            return false;
        }
    }
}
