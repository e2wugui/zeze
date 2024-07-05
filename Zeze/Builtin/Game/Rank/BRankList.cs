// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Game.Rank
{
    public interface BRankListReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BRankList Copy();

        public System.Collections.Generic.IReadOnlyList<Zeze.Builtin.Game.Rank.BRankValueReadOnly>RankList { get; }
    }

    public sealed class BRankList : Zeze.Transaction.Bean, BRankListReadOnly
    {
        readonly Zeze.Transaction.Collections.CollList2<Zeze.Builtin.Game.Rank.BRankValue> _RankList;

        public Zeze.Transaction.Collections.CollList2<Zeze.Builtin.Game.Rank.BRankValue> RankList => _RankList;
        System.Collections.Generic.IReadOnlyList<Zeze.Builtin.Game.Rank.BRankValueReadOnly> Zeze.Builtin.Game.Rank.BRankListReadOnly.RankList => _RankList;

        public BRankList()
        {
            _RankList = new Zeze.Transaction.Collections.CollList2<Zeze.Builtin.Game.Rank.BRankValue>() { VariableId = 1 };
        }

        public void Assign(BRankList other)
        {
            RankList.Clear();
            foreach (var e in other.RankList)
                RankList.Add(e.Copy());
        }

        public BRankList CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BRankList Copy()
        {
            var copy = new BRankList();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BRankList a, BRankList b)
        {
            BRankList save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = -1625874326687776700;
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Game.Rank.BRankList: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("RankList").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in RankList)
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
                var _x_ = RankList;
                int _n_ = _x_?.Count ?? 0;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
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
                var _x_ = RankList;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    {
                        _x_.Add(_o_.ReadBean(new Zeze.Builtin.Game.Rank.BRankValue(), _t_));
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
            _RankList.InitRootInfo(root, this);
        }

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _RankList.InitRootInfoWithRedo(root, this);
        }

        public override bool NegativeCheck()
        {
            foreach (var _v_ in RankList)
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
                    case 1: _RankList.FollowerApply(vlog); break;
                }
            }
        }

        public override void ClearParameters()
        {
            RankList.Clear();
        }
    }
}
