// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Collections.DepartmentTree
{
    public interface BDepartmentRootReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BDepartmentRoot Copy();

        public string Root { get; }
        public System.Collections.Generic.IReadOnlyDictionary<string,Zeze.Transaction.DynamicBeanReadOnly> Managers { get; }
        public long NextDepartmentId { get; }
        public System.Collections.Generic.IReadOnlyDictionary<string,long> Childs { get; }
    }

    public sealed class BDepartmentRoot : Zeze.Transaction.Bean, BDepartmentRootReadOnly
    {
        string _Root; // 群主
        readonly Zeze.Transaction.Collections.CollMap2<string, Zeze.Transaction.DynamicBean> _Managers;
        readonly Zeze.Transaction.Collections.CollMapReadOnly<string,Zeze.Transaction.DynamicBeanReadOnly,Zeze.Transaction.DynamicBean> _ManagersReadOnly;
        public static Zeze.Transaction.DynamicBean NewDynamicBeanManagers()
        {
            return new Zeze.Transaction.DynamicBean(2, Zeze.Collections.DepartmentTree.GetSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree.CreateBeanFromSpecialTypeId);
        }

        public static long GetSpecialTypeIdFromBean_2(Zeze.Transaction.Bean bean)
        {
            return Zeze.Collections.DepartmentTree.GetSpecialTypeIdFromBean(bean);
        }

        public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_2(long typeId)
        {
            return Zeze.Collections.DepartmentTree.CreateBeanFromSpecialTypeId(typeId);
        }

        long _NextDepartmentId; // 部门Id种子
        readonly Zeze.Transaction.Collections.CollMap1<string, long> _Childs; // name 2 id。采用整体保存，因为需要排序和重名判断。需要加数量上限。
        readonly Zeze.Transaction.Collections.CollMapReadOnly<string,long,long> _ChildsReadOnly;

        public string Root
        {
            get
            {
                if (!IsManaged)
                    return _Root;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Root;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Root)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _Root;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _Root = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Root() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public Zeze.Transaction.Collections.CollMap2<string, Zeze.Transaction.DynamicBean> Managers => _Managers;
        System.Collections.Generic.IReadOnlyDictionary<string,Zeze.Transaction.DynamicBeanReadOnly> Zeze.Builtin.Collections.DepartmentTree.BDepartmentRootReadOnly.Managers => _ManagersReadOnly;

        public long NextDepartmentId
        {
            get
            {
                if (!IsManaged)
                    return _NextDepartmentId;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _NextDepartmentId;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__NextDepartmentId)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _NextDepartmentId;
            }
            set
            {
                if (!IsManaged)
                {
                    _NextDepartmentId = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__NextDepartmentId() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public Zeze.Transaction.Collections.CollMap1<string, long> Childs => _Childs;
        System.Collections.Generic.IReadOnlyDictionary<string,long> Zeze.Builtin.Collections.DepartmentTree.BDepartmentRootReadOnly.Childs => _ChildsReadOnly;

        public BDepartmentRoot()
        {
            _Root = "";
            _Managers = new Zeze.Transaction.Collections.CollMap2<string, Zeze.Transaction.DynamicBean>() { VariableId = 2 };
            _ManagersReadOnly = new Zeze.Transaction.Collections.CollMapReadOnly<string,Zeze.Transaction.DynamicBeanReadOnly,Zeze.Transaction.DynamicBean>(_Managers);
            _Childs = new Zeze.Transaction.Collections.CollMap1<string, long>() { VariableId = 4 };
            _ChildsReadOnly = new Zeze.Transaction.Collections.CollMapReadOnly<string,long,long>(_Childs);
        }

        public BDepartmentRoot(string _Root_, long _NextDepartmentId_)
        {
            _Root = _Root_;
            _Managers = new Zeze.Transaction.Collections.CollMap2<string, Zeze.Transaction.DynamicBean>() { VariableId = 2 };
            _ManagersReadOnly = new Zeze.Transaction.Collections.CollMapReadOnly<string,Zeze.Transaction.DynamicBeanReadOnly,Zeze.Transaction.DynamicBean>(_Managers);
            _NextDepartmentId = _NextDepartmentId_;
            _Childs = new Zeze.Transaction.Collections.CollMap1<string, long>() { VariableId = 4 };
            _ChildsReadOnly = new Zeze.Transaction.Collections.CollMapReadOnly<string,long,long>(_Childs);
        }

        public void Assign(BDepartmentRoot other)
        {
            Root = other.Root;
            Managers.Clear();
            foreach (var e in other.Managers)
                Managers.Add(e.Key, e.Value.Copy());
            NextDepartmentId = other.NextDepartmentId;
            Childs.Clear();
            foreach (var e in other.Childs)
                Childs.Add(e.Key, e.Value);
        }

        public BDepartmentRoot CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BDepartmentRoot Copy()
        {
            var copy = new BDepartmentRoot();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BDepartmentRoot a, BDepartmentRoot b)
        {
            BDepartmentRoot save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 50884757418508709;
        public override long TypeId => TYPEID;

        sealed class Log__Root : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BDepartmentRoot)Belong)._Root = this.Value; }
        }


        sealed class Log__NextDepartmentId : Zeze.Transaction.Log<long>
        {
            public override void Commit() { ((BDepartmentRoot)Belong)._NextDepartmentId = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Collections.DepartmentTree.BDepartmentRoot: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Root").Append('=').Append(Root).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Managers").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var _kv_ in Managers)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append('(').Append(Environment.NewLine);
                var Key = _kv_.Key;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Key").Append('=').Append(Key).Append(',').Append(Environment.NewLine);
                var Value = _kv_.Value;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Value").Append('=').Append(Environment.NewLine);
                Value.Bean.BuildString(sb, level + 4);
                sb.Append(',').Append(Environment.NewLine);
                sb.Append(Zeze.Util.Str.Indent(level)).Append(')').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("NextDepartmentId").Append('=').Append(NextDepartmentId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Childs").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var _kv_ in Childs)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append('(').Append(Environment.NewLine);
                var Key = _kv_.Key;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Key").Append('=').Append(Key).Append(',').Append(Environment.NewLine);
                var Value = _kv_.Value;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Value").Append('=').Append(Value).Append(',').Append(Environment.NewLine);
                sb.Append(Zeze.Util.Str.Indent(level)).Append(')').Append(Environment.NewLine);
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
                string _x_ = Root;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = Managers;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                    _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.DYNAMIC);
                    foreach (var _e_ in _x_)
                    {
                        _o_.WriteString(_e_.Key);
                        _x_.Encode(_o_);
                        _n_--;
                    }
                    if (_n_ != 0)
                        throw new System.Exception(_n_.ToString());
                }
            }
            {
                long _x_ = NextDepartmentId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                var _x_ = Childs;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.MAP);
                    _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.INTEGER);
                    foreach (var _e_ in _x_)
                    {
                        _o_.WriteString(_e_.Key);
                        _o_.WriteLong(_e_.Value);
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
                Root = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                var _x_ = Managers;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP)
                {
                    int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                    for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--)
                    {
                        var _k_ = _o_.ReadString(_s_);
                        var _v_ = new Zeze.Transaction.DynamicBean(0, Zeze.Collections.DepartmentTree.GetSpecialTypeIdFromBean, Zeze.Collections.DepartmentTree.CreateBeanFromSpecialTypeId);
                        _v_.Decode(_o_);
                        _x_.Add(_k_, _v_);
                    }
                }
                else
                    _o_.SkipUnknownFieldOrThrow(_t_, "Map");
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                NextDepartmentId = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                var _x_ = Childs;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP)
                {
                    int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                    for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--)
                    {
                        var _k_ = _o_.ReadString(_s_);
                        var _v_ = _o_.ReadLong(_t_);
                        _x_.Add(_k_, _v_);
                    }
                }
                else
                    _o_.SkipUnknownFieldOrThrow(_t_, "Map");
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
            _Managers.InitRootInfo(root, this);
            _Childs.InitRootInfo(root, this);
        }

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _Managers.InitRootInfoWithRedo(root, this);
            _Childs.InitRootInfoWithRedo(root, this);
        }

        public override bool NegativeCheck()
        {
            if (NextDepartmentId < 0) return true;
            foreach (var _v_ in Childs.Values)
            {
                if (_v_ < 0) return true;
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
                    case 1: _Root = vlog.StringValue(); break;
                    case 2: _Managers.FollowerApply(vlog); break;
                    case 3: _NextDepartmentId = vlog.LongValue(); break;
                    case 4: _Childs.FollowerApply(vlog); break;
                }
            }
        }

        public override void ClearParameters()
        {
            Root = "";
            Managers.Clear();
            NextDepartmentId = 0;
            Childs.Clear();
        }
    }
}
