// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.HotDistribute
{
    public interface BLastVersionBeanInfoReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BLastVersionBeanInfo Copy();

        public string Name { get; }
        public System.Collections.Generic.IReadOnlyList<Zeze.Builtin.HotDistribute.BVariableReadOnly>Variables { get; }
    }

    public sealed class BLastVersionBeanInfo : Zeze.Transaction.Bean, BLastVersionBeanInfoReadOnly
    {
        string _Name;
        readonly Zeze.Transaction.Collections.CollList2<Zeze.Builtin.HotDistribute.BVariable> _Variables;

        public string Name
        {
            get
            {
                if (!IsManaged)
                    return _Name;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Name;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Name)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _Name;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _Name = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Name() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public Zeze.Transaction.Collections.CollList2<Zeze.Builtin.HotDistribute.BVariable> Variables => _Variables;
        System.Collections.Generic.IReadOnlyList<Zeze.Builtin.HotDistribute.BVariableReadOnly> Zeze.Builtin.HotDistribute.BLastVersionBeanInfoReadOnly.Variables => _Variables;

        public BLastVersionBeanInfo()
        {
            _Name = "";
            _Variables = new Zeze.Transaction.Collections.CollList2<Zeze.Builtin.HotDistribute.BVariable>() { VariableId = 2 };
        }

        public BLastVersionBeanInfo(string _Name_)
        {
            _Name = _Name_;
            _Variables = new Zeze.Transaction.Collections.CollList2<Zeze.Builtin.HotDistribute.BVariable>() { VariableId = 2 };
        }

        public void Assign(BLastVersionBeanInfo other)
        {
            Name = other.Name;
            Variables.Clear();
            foreach (var e in other.Variables)
                Variables.Add(e.Copy());
        }

        public BLastVersionBeanInfo CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BLastVersionBeanInfo Copy()
        {
            var copy = new BLastVersionBeanInfo();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BLastVersionBeanInfo a, BLastVersionBeanInfo b)
        {
            BLastVersionBeanInfo save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -6575391224958548024;
        public override long TypeId => TYPEID;

        sealed class Log__Name : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BLastVersionBeanInfo)Belong)._Name = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.HotDistribute.BLastVersionBeanInfo: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Name").Append('=').Append(Name).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Variables").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in Variables)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Environment.NewLine);
                Item.BuildString(sb, level + 4);
                sb.Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = Name;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = Variables;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.BEAN);
                    foreach (var _v_ in _x_)
                    {
                        _v_.Encode(_o_);
                        _n_--;
                    }
                    if (_n_ != 0)
                        throw new System.Exception(_n_.ToString());
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
                Name = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                var _x_ = Variables;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    {
                        _x_.Add(_o_.ReadBean(new Zeze.Builtin.HotDistribute.BVariable(), _t_));
                    }
                }
                else
                    _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
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
            _Variables.InitRootInfo(root, this);
        }

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _Variables.InitRootInfoWithRedo(root, this);
        }

        public override bool NegativeCheck()
        {
            foreach (var _v_ in Variables)
            {
                if (_v_.NegativeCheck()) return true;
            }
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _Name = ((Zeze.Transaction.Log<string>)vlog).Value; break;
                    case 2: _Variables.FollowerApply(vlog); break;
                }
            }
        }

        public override void ClearParameters()
        {
            Name = "";
            Variables.Clear();
        }
    }
}
