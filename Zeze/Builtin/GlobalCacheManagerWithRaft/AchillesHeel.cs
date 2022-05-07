// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.GlobalCacheManagerWithRaft
{
    public interface AchillesHeelReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public int ServerId { get; }
        public string SecureKey { get; }
        public int GlobalCacheManagerHashIndex { get; }
    }

    public sealed class AchillesHeel : Zeze.Transaction.Bean, AchillesHeelReadOnly
    {
        int _ServerId;
        string _SecureKey;
        int _GlobalCacheManagerHashIndex;

        public int ServerId
        {
            get
            {
                if (!IsManaged)
                    return _ServerId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ServerId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ServerId)txn.GetLog(ObjectId + 1);
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
                txn.PutLog(new Log__ServerId() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public string SecureKey
        {
            get
            {
                if (!IsManaged)
                    return _SecureKey;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _SecureKey;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__SecureKey)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _SecureKey;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException();
                if (!IsManaged)
                {
                    _SecureKey = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__SecureKey() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public int GlobalCacheManagerHashIndex
        {
            get
            {
                if (!IsManaged)
                    return _GlobalCacheManagerHashIndex;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _GlobalCacheManagerHashIndex;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__GlobalCacheManagerHashIndex)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _GlobalCacheManagerHashIndex;
            }
            set
            {
                if (!IsManaged)
                {
                    _GlobalCacheManagerHashIndex = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__GlobalCacheManagerHashIndex() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public AchillesHeel() : this(0)
        {
        }

        public AchillesHeel(int _varId_) : base(_varId_)
        {
            _SecureKey = "";
        }

        public void Assign(AchillesHeel other)
        {
            ServerId = other.ServerId;
            SecureKey = other.SecureKey;
            GlobalCacheManagerHashIndex = other.GlobalCacheManagerHashIndex;
        }

        public AchillesHeel CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public AchillesHeel Copy()
        {
            var copy = new AchillesHeel();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(AchillesHeel a, AchillesHeel b)
        {
            AchillesHeel save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = -2721594206374974168;
        public override long TypeId => TYPEID;

        sealed class Log__ServerId : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((AchillesHeel)Belong)._ServerId = this.Value; }
        }

        sealed class Log__SecureKey : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((AchillesHeel)Belong)._SecureKey = this.Value; }
        }

        sealed class Log__GlobalCacheManagerHashIndex : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((AchillesHeel)Belong)._GlobalCacheManagerHashIndex = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.GlobalCacheManagerWithRaft.AchillesHeel: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ServerId").Append('=').Append(ServerId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("SecureKey").Append('=').Append(SecureKey).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("GlobalCacheManagerHashIndex").Append('=').Append(GlobalCacheManagerHashIndex).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = ServerId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                string _x_ = SecureKey;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                int _x_ = GlobalCacheManagerHashIndex;
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
                ServerId = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                SecureKey = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                GlobalCacheManagerHashIndex = _o_.ReadInt(_t_);
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
            if (ServerId < 0) return true;
            if (GlobalCacheManagerHashIndex < 0) return true;
            return false;
        }
        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _ServerId = ((Zeze.Transaction.Log<int>)vlog).Value; break;
                    case 2: _SecureKey = ((Zeze.Transaction.Log<string>)vlog).Value; break;
                    case 3: _GlobalCacheManagerHashIndex = ((Zeze.Transaction.Log<int>)vlog).Value; break;
                }
            }
        }

    }
}
