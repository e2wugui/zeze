// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.GlobalCacheManagerWithRaft
{
    public interface ReduceParamReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public ReduceParam Copy();

        public Zeze.Net.Binary GlobalKey { get; }
        public int State { get; }
        public Zeze.Util.Id128 ReduceTid { get; }
    }

    public sealed class ReduceParam : Zeze.Transaction.Bean, ReduceParamReadOnly
    {
        Zeze.Net.Binary _GlobalKey;
        int _State;
        Zeze.Util.Id128 _ReduceTid;

        public Zeze.Net.Binary GlobalKey
        {
            get
            {
                if (!IsManaged)
                    return _GlobalKey;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _GlobalKey;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__GlobalKey)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _GlobalKey;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _GlobalKey = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__GlobalKey() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public int State
        {
            get
            {
                if (!IsManaged)
                    return _State;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _State;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__State)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _State;
            }
            set
            {
                if (!IsManaged)
                {
                    _State = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__State() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public Zeze.Util.Id128 ReduceTid
        {
            get
            {
                if (!IsManaged)
                    return _ReduceTid;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _ReduceTid;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__ReduceTid)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _ReduceTid;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _ReduceTid = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__ReduceTid() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public ReduceParam()
        {
            _GlobalKey = Zeze.Net.Binary.Empty;
            _ReduceTid = new Zeze.Util.Id128();
        }

        public ReduceParam(Zeze.Net.Binary _GlobalKey_, int _State_, Zeze.Util.Id128 _ReduceTid_)
        {
            _GlobalKey = _GlobalKey_;
            _State = _State_;
            _ReduceTid = _ReduceTid_;
        }

        public void Assign(ReduceParam other)
        {
            GlobalKey = other.GlobalKey;
            State = other.State;
            ReduceTid = other.ReduceTid;
        }

        public ReduceParam CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override ReduceParam Copy()
        {
            var copy = new ReduceParam();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(ReduceParam a, ReduceParam b)
        {
            ReduceParam save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -4489915946741208436;
        public override long TypeId => TYPEID;

        sealed class Log__GlobalKey : Zeze.Transaction.Log<Zeze.Net.Binary>
        {
            public override void Commit() { ((ReduceParam)Belong)._GlobalKey = this.Value; }
        }

        sealed class Log__State : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((ReduceParam)Belong)._State = this.Value; }
        }

        sealed class Log__ReduceTid : Zeze.Transaction.Log<Zeze.Util.Id128>
        {
            public override void Commit() { ((ReduceParam)Belong)._ReduceTid = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.GlobalCacheManagerWithRaft.ReduceParam: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("GlobalKey").Append('=').Append(GlobalKey).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("State").Append('=').Append(State).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ReduceTid").Append('=').Append(Environment.NewLine);
            ReduceTid.BuildString(sb, level + 4);
            sb.Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                var _x_ = GlobalKey;
                if (_x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                int _x_ = State;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _a_ = _o_.WriteIndex;
                int _j_ = _o_.WriteTag(_i_, 3, ByteBuffer.BEAN);
                int _b_ = _o_.WriteIndex;
                ReduceTid.Encode(_o_);
                if (_b_ + 1 == _o_.WriteIndex)
                    _o_.WriteIndex = _a_;
                else
                    _i_ = _j_;
            }
            _o_.WriteByte(0);
        }

        public override void Decode(ByteBuffer _o_)
        {
            int _t_ = _o_.ReadByte();
            int _i_ = _o_.ReadTagSize(_t_);
            if (_i_ == 1)
            {
                GlobalKey = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                State = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                _o_.ReadBean(ReduceTid, _t_);
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
            if (State < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _GlobalKey = ((Zeze.Transaction.Log<Zeze.Net.Binary>)vlog).Value; break;
                    case 2: _State = ((Zeze.Transaction.Log<int>)vlog).Value; break;
                    case 3: _ReduceTid = ((Zeze.Transaction.Log<Zeze.Util.Id128>)vlog).Value; break;
                }
            }
        }

        public override void ClearParameters()
        {
            GlobalKey = Zeze.Net.Binary.Empty;
            State = 0;
            ReduceTid = new Zeze.Util.Id128();
        }
    }
}
