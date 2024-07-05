// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Provider
{
    public interface BSubscribeReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BSubscribe Copy();

        public System.Collections.Generic.IReadOnlyDictionary<int,Zeze.Builtin.Provider.BModuleReadOnly> Modules { get; }
    }

    public sealed class BSubscribe : Zeze.Transaction.Bean, BSubscribeReadOnly
    {
        readonly Zeze.Transaction.Collections.CollMap2<int, Zeze.Builtin.Provider.BModule> _modules; // moduleId -> BModule
        readonly Zeze.Transaction.Collections.CollMapReadOnly<int,Zeze.Builtin.Provider.BModuleReadOnly,Zeze.Builtin.Provider.BModule> _modulesReadOnly;

        public Zeze.Transaction.Collections.CollMap2<int, Zeze.Builtin.Provider.BModule> Modules => _modules;
        System.Collections.Generic.IReadOnlyDictionary<int,Zeze.Builtin.Provider.BModuleReadOnly> Zeze.Builtin.Provider.BSubscribeReadOnly.Modules => _modulesReadOnly;

        public BSubscribe()
        {
            _modules = new Zeze.Transaction.Collections.CollMap2<int, Zeze.Builtin.Provider.BModule>() { VariableId = 1 };
            _modulesReadOnly = new Zeze.Transaction.Collections.CollMapReadOnly<int,Zeze.Builtin.Provider.BModuleReadOnly,Zeze.Builtin.Provider.BModule>(_modules);
        }

        public void Assign(BSubscribe other)
        {
            Modules.Clear();
            foreach (var e in other.Modules)
                Modules.Add(e.Key, e.Value.Copy());
        }

        public BSubscribe CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BSubscribe Copy()
        {
            var copy = new BSubscribe();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BSubscribe a, BSubscribe b)
        {
            BSubscribe save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 1112180088628051173;
        public override long TypeId => TYPEID;


        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Provider.BSubscribe: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Modules").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var _kv_ in Modules)
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                var _x_ = Modules;
                int _n_ = _x_?.Count ?? 0;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                    _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                    foreach (var _e_ in _x_)
                    {
                        _o_.WriteLong(_e_.Key);
                        _e_.Value.Encode(_o_);
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
                var _x_ = Modules;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP)
                {
                    int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                    for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--)
                    {
                        var _k_ = _o_.ReadInt(_s_);
                        var _v_ = _o_.ReadBean(new Zeze.Builtin.Provider.BModule(), _t_);
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
            _modules.InitRootInfo(root, this);
        }

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _modules.InitRootInfoWithRedo(root, this);
        }

        public override bool NegativeCheck()
        {
            foreach (var _v_ in Modules.Values)
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
                    case 1: _modules.FollowerApply(vlog); break;
                }
            }
        }

        public override void ClearParameters()
        {
            Modules.Clear();
        }
    }
}
