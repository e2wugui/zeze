// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Provider
{
    public interface BSendResultReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BSendResult Copy();
        public void BuildString(System.Text.StringBuilder sb, int level);
        public long ObjectId { get; }
        public int VariableId { get; }
        public Zeze.Transaction.TableKey TableKey { get; }
        public bool IsManaged { get; }
        public int CapacityHintOfByteBuffer { get; }

        public System.Collections.Generic.IReadOnlyList<long>ErrorLinkSids { get; }
    }

    public sealed class BSendResult : Zeze.Transaction.Bean, BSendResultReadOnly
    {
        readonly Zeze.Transaction.Collections.CollList1<long> _ErrorLinkSids;

        public Zeze.Transaction.Collections.CollList1<long> ErrorLinkSids => _ErrorLinkSids;
        System.Collections.Generic.IReadOnlyList<long> Zeze.Builtin.Provider.BSendResultReadOnly.ErrorLinkSids => _ErrorLinkSids;

        public BSendResult()
        {
            _ErrorLinkSids = new Zeze.Transaction.Collections.CollList1<long>() { VariableId = 1 };
        }

        public void Assign(BSendResult other)
        {
            ErrorLinkSids.Clear();
            foreach (var e in other.ErrorLinkSids)
                ErrorLinkSids.Add(e);
        }

        public BSendResult CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BSendResult Copy()
        {
            var copy = new BSendResult();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BSendResult a, BSendResult b)
        {
            BSendResult save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -7186434891670297524;
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Provider.BSendResult: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ErrorLinkSids").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in ErrorLinkSids)
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
                var _x_ = ErrorLinkSids;
                int _n_ = _x_?.Count ?? 0;
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
            _o_.WriteByte(0);
        }

        public override void Decode(ByteBuffer _o_)
        {
            int _t_ = _o_.ReadByte();
            int _i_ = _o_.ReadTagSize(_t_);
            if (_i_ == 1)
            {
                var _x_ = ErrorLinkSids;
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
            _ErrorLinkSids.InitRootInfo(root, this);
        }

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _ErrorLinkSids.InitRootInfoWithRedo(root, this);
        }

        public override bool NegativeCheck()
        {
            foreach (var _v_ in ErrorLinkSids)
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
                    case 1: _ErrorLinkSids.FollowerApply(vlog); break;
                }
            }
        }

        public override void ClearParameters()
        {
            ErrorLinkSids.Clear();
        }
    }
}
