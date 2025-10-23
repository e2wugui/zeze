// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Game.Online
{
    public interface BNotifyReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BNotify Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public Zeze.Net.Binary FullEncodedProtocol { get; }
    }

    public sealed class BNotify : Zeze.Transaction.Bean, BNotifyReadOnly
    {
        Zeze.Net.Binary _FullEncodedProtocol;

        public Zeze.Net.Binary FullEncodedProtocol
        {
            get
            {
                if (!IsManaged)
                    return _FullEncodedProtocol;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _FullEncodedProtocol;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__FullEncodedProtocol)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _FullEncodedProtocol;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _FullEncodedProtocol = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__FullEncodedProtocol() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public BNotify()
        {
            _FullEncodedProtocol = Zeze.Net.Binary.Empty;
        }

        public BNotify(Zeze.Net.Binary _FullEncodedProtocol_)
        {
            _FullEncodedProtocol = _FullEncodedProtocol_;
        }

        public void Assign(BNotify other)
        {
            FullEncodedProtocol = other.FullEncodedProtocol;
        }

        public BNotify CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BNotify Copy()
        {
            var copy = new BNotify();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BNotify a, BNotify b)
        {
            BNotify save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 663625160021568926;
        public override long TypeId => TYPEID;

        sealed class Log__FullEncodedProtocol : Zeze.Transaction.Log<Zeze.Net.Binary>
        {
            public override void Commit() { ((BNotify)Belong)._FullEncodedProtocol = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Game.Online.BNotify: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("FullEncodedProtocol").Append('=').Append(FullEncodedProtocol).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                var _x_ = FullEncodedProtocol;
                if (_x_ != null && _x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
                FullEncodedProtocol = _o_.ReadBinary(_t_);
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
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _FullEncodedProtocol = vlog.BinaryValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            FullEncodedProtocol = Zeze.Net.Binary.Empty;
        }
    }
}
