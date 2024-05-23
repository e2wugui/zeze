// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Provider
{
    public interface BSendReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BSend Copy();

        public System.Collections.Generic.IReadOnlyList<long>LinkSids { get; }
        public long ProtocolType { get; }
        public Zeze.Net.Binary ProtocolWholeData { get; }
    }

    public sealed class BSend : Zeze.Transaction.Bean, BSendReadOnly
    {
        readonly Zeze.Transaction.Collections.CollList1<long> _linkSids;
        long _protocolType;
        Zeze.Net.Binary _protocolWholeData; // 完整的协议打包，包括了 type, size

        public Zeze.Transaction.Collections.CollList1<long> LinkSids => _linkSids;
        System.Collections.Generic.IReadOnlyList<long> Zeze.Builtin.Provider.BSendReadOnly.LinkSids => _linkSids;

        public long ProtocolType
        {
            get
            {
                if (!IsManaged)
                    return _protocolType;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _protocolType;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__protocolType)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _protocolType;
            }
            set
            {
                if (!IsManaged)
                {
                    _protocolType = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__protocolType() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public Zeze.Net.Binary ProtocolWholeData
        {
            get
            {
                if (!IsManaged)
                    return _protocolWholeData;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _protocolWholeData;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__protocolWholeData)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _protocolWholeData;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _protocolWholeData = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__protocolWholeData() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public BSend()
        {
            _linkSids = new Zeze.Transaction.Collections.CollList1<long>() { VariableId = 1 };
            _protocolWholeData = Zeze.Net.Binary.Empty;
        }

        public BSend(long _protocolType_, Zeze.Net.Binary _protocolWholeData_)
        {
            _linkSids = new Zeze.Transaction.Collections.CollList1<long>() { VariableId = 1 };
            _protocolType = _protocolType_;
            _protocolWholeData = _protocolWholeData_;
        }

        public void Assign(BSend other)
        {
            LinkSids.Clear();
            foreach (var e in other.LinkSids)
                LinkSids.Add(e);
            ProtocolType = other.ProtocolType;
            ProtocolWholeData = other.ProtocolWholeData;
        }

        public BSend CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BSend Copy()
        {
            var copy = new BSend();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BSend a, BSend b)
        {
            BSend save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 545774009128015305;
        public override long TypeId => TYPEID;


        sealed class Log__protocolType : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BSend)Belong)._protocolType = this.Value; }
        }

        sealed class Log__protocolWholeData : Zeze.Transaction.Log<Zeze.Net.Binary>
        {
            public override void Commit() { ((BSend)Belong)._protocolWholeData = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Provider.BSend: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkSids").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in LinkSids)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Item).Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ProtocolType").Append('=').Append(ProtocolType).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ProtocolWholeData").Append('=').Append(ProtocolWholeData).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                var _x_ = LinkSids;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                    foreach (var _v_ in _x_)
                    {
                        _o_.WriteLong(_v_);
                        _n_--;
                    }
                    if (_n_ != 0)
                        throw new System.Exception(_n_.ToString());
                }
            }
            {
                long _x_ = ProtocolType;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                var _x_ = ProtocolWholeData;
                if (_x_.Count != 0)
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
                var _x_ = LinkSids;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    {
                        _x_.Add(_o_.ReadLong(_t_));
                    }
                }
                else
                    _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                ProtocolType = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                ProtocolWholeData = _o_.ReadBinary(_t_);
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
            _linkSids.InitRootInfo(root, this);
        }

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _linkSids.InitRootInfoWithRedo(root, this);
        }

        public override bool NegativeCheck()
        {
            foreach (var _v_ in LinkSids)
            {
                if (_v_ < 0) return true;
            }
            if (ProtocolType < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _linkSids.FollowerApply(vlog); break;
                    case 2: _protocolType = vlog.LongValue(); break;
                    case 3: _protocolWholeData = vlog.BinaryValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            LinkSids.Clear();
            ProtocolType = 0;
            ProtocolWholeData = Zeze.Net.Binary.Empty;
        }
    }
}
