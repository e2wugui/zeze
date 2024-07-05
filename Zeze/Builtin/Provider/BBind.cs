// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Provider
{
    public interface BBindReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BBind Copy();

        public System.Collections.Generic.IReadOnlyDictionary<int,Zeze.Builtin.Provider.BModuleReadOnly> Modules { get; }
        public System.Collections.Generic.IReadOnlySet<long> LinkSids { get; }
    }

    public sealed class BBind : Zeze.Transaction.Bean, BBindReadOnly
    {
        public const int ResultSuccess = 0;
        public const int ResultFailed = 1;

        readonly Zeze.Transaction.Collections.CollMap2<int, Zeze.Builtin.Provider.BModule> _modules; // moduleId -> BModule
        readonly Zeze.Transaction.Collections.CollMapReadOnly<int,Zeze.Builtin.Provider.BModuleReadOnly,Zeze.Builtin.Provider.BModule> _modulesReadOnly;
        readonly Zeze.Transaction.Collections.CollSet1<long> _linkSids;

        public Zeze.Transaction.Collections.CollMap2<int, Zeze.Builtin.Provider.BModule> Modules => _modules;
        System.Collections.Generic.IReadOnlyDictionary<int,Zeze.Builtin.Provider.BModuleReadOnly> Zeze.Builtin.Provider.BBindReadOnly.Modules => _modulesReadOnly;

        public Zeze.Transaction.Collections.CollSet1<long> LinkSids => _linkSids;
        System.Collections.Generic.IReadOnlySet<long> Zeze.Builtin.Provider.BBindReadOnly.LinkSids => _linkSids;

        public BBind()
        {
            _modules = new Zeze.Transaction.Collections.CollMap2<int, Zeze.Builtin.Provider.BModule>() { VariableId = 1 };
            _modulesReadOnly = new Zeze.Transaction.Collections.CollMapReadOnly<int,Zeze.Builtin.Provider.BModuleReadOnly,Zeze.Builtin.Provider.BModule>(_modules);
            _linkSids = new Zeze.Transaction.Collections.CollSet1<long>() { VariableId = 2 };
        }

        public void Assign(BBind other)
        {
            Modules.Clear();
            foreach (var e in other.Modules)
                Modules.Add(e.Key, e.Value.Copy());
            LinkSids.Clear();
            foreach (var e in other.LinkSids)
                LinkSids.Add(e);
        }

        public BBind CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BBind Copy()
        {
            var copy = new BBind();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BBind a, BBind b)
        {
            BBind save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 318036402741860020;
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Provider.BBind: {").Append(Environment.NewLine);
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkSids").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in LinkSids)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Item).Append(',').Append(Environment.NewLine);
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
            {
                var _x_ = LinkSids;
                int _n_ = _x_?.Count ?? 0;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
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
            if (_i_ == 2)
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
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }

        protected override void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root)
        {
            _modules.InitRootInfo(root, this);
            _linkSids.InitRootInfo(root, this);
        }

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _modules.InitRootInfoWithRedo(root, this);
            _linkSids.InitRootInfoWithRedo(root, this);
        }

        public override bool NegativeCheck()
        {
            foreach (var _v_ in Modules.Values)
            {
                if (_v_.NegativeCheck()) return true;
            }
            foreach (var _v_ in LinkSids)
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
                    case 1: _modules.FollowerApply(vlog); break;
                    case 2: _linkSids.FollowerApply(vlog); break;
                }
            }
        }

        public override void ClearParameters()
        {
            Modules.Clear();
            LinkSids.Clear();
        }
    }
}
