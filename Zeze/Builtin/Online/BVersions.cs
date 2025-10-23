// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Online
{
    public interface BVersionsReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BVersions Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public System.Collections.Generic.IReadOnlyDictionary<string,Zeze.Builtin.Online.BVersionReadOnly> Logins { get; }
        public long LastLoginVersion { get; }
    }

    public sealed class BVersions : Zeze.Transaction.Bean, BVersionsReadOnly
    {
        readonly Zeze.Transaction.Collections.CollMap2<string, Zeze.Builtin.Online.BVersion> _Logins; // key is ClientId
        readonly Zeze.Transaction.Collections.CollMapReadOnly<string,Zeze.Builtin.Online.BVersionReadOnly,Zeze.Builtin.Online.BVersion> _LoginsReadOnly;
        long _LastLoginVersion; // 用来生成 account 登录版本号。每次递增。

        public Zeze.Transaction.Collections.CollMap2<string, Zeze.Builtin.Online.BVersion> Logins => _Logins;
        System.Collections.Generic.IReadOnlyDictionary<string,Zeze.Builtin.Online.BVersionReadOnly> Zeze.Builtin.Online.BVersionsReadOnly.Logins => _LoginsReadOnly;

        public long LastLoginVersion
        {
            get
            {
                if (!IsManaged)
                    return _LastLoginVersion;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _LastLoginVersion;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__LastLoginVersion)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _LastLoginVersion;
            }
            set
            {
                if (!IsManaged)
                {
                    _LastLoginVersion = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__LastLoginVersion() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public BVersions()
        {
            _Logins = new Zeze.Transaction.Collections.CollMap2<string, Zeze.Builtin.Online.BVersion>() { VariableId = 1 };
            _LoginsReadOnly = new Zeze.Transaction.Collections.CollMapReadOnly<string,Zeze.Builtin.Online.BVersionReadOnly,Zeze.Builtin.Online.BVersion>(_Logins);
        }

        public BVersions(long _LastLoginVersion_)
        {
            _Logins = new Zeze.Transaction.Collections.CollMap2<string, Zeze.Builtin.Online.BVersion>() { VariableId = 1 };
            _LoginsReadOnly = new Zeze.Transaction.Collections.CollMapReadOnly<string,Zeze.Builtin.Online.BVersionReadOnly,Zeze.Builtin.Online.BVersion>(_Logins);
            _LastLoginVersion = _LastLoginVersion_;
        }

        public void Assign(BVersions other)
        {
            Logins.Clear();
            foreach (var e in other.Logins)
                Logins.Add(e.Key, e.Value.Copy());
            LastLoginVersion = other.LastLoginVersion;
        }

        public BVersions CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BVersions Copy()
        {
            var copy = new BVersions();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BVersions a, BVersions b)
        {
            BVersions save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 3480529937760660740;
        public override long TypeId => TYPEID;


        sealed class Log__LastLoginVersion : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BVersions)Belong)._LastLoginVersion = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Online.BVersions: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Logins").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var _kv_ in Logins)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append('(').Append(Environment.NewLine);
                var Key = _kv_.Key;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Key").Append('=').Append(Key).Append(',').Append(Environment.NewLine);
                var Value = _kv_.Value;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Value").Append('=').Append(Environment.NewLine);
                Value.BuildString(sb, level + 4);
                sb.Append(',').Append(Environment.NewLine);
                sb.Append(Zeze.Util.Str.Indent(level)).Append(')').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LastLoginVersion").Append('=').Append(LastLoginVersion).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                var _x_ = Logins;
                int _n_ = _x_?.Count ?? 0;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                    _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BEAN);
                    foreach (var _e_ in _x_)
                    {
                        _o_.WriteString(_e_.Key);
                        _e_.Value.Encode(_o_);
                        _n_--;
                    }
                    if (_n_ != 0)
                        throw new System.Exception(_n_.ToString());
                }
            }
            {
                long _x_ = LastLoginVersion;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
                var _x_ = Logins;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP)
                {
                    int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                    for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--)
                    {
                        var _k_ = _o_.ReadString(_s_);
                        var _v_ = _o_.ReadBean(new Zeze.Builtin.Online.BVersion(), _t_);
                        _x_.Add(_k_, _v_);
                    }
                }
                else
                    _o_.SkipUnknownFieldOrThrow(_t_, "Map");
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                LastLoginVersion = _o_.ReadLong(_t_);
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
            _Logins.InitRootInfo(root, this);
        }

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _Logins.InitRootInfoWithRedo(root, this);
        }

        public override bool NegativeCheck()
        {
            foreach (var _v_ in Logins.Values)
            {
                if (_v_.NegativeCheck()) return true;
            }
            if (LastLoginVersion < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _Logins.FollowerApply(vlog); break;
                    case 2: _LastLoginVersion = vlog.LongValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            Logins.Clear();
            LastLoginVersion = 0;
        }
    }
}
