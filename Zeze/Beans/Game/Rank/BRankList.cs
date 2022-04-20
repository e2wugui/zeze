// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Beans.Game.Rank
{
    public interface BRankListReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public System.Collections.Generic.IReadOnlyList<Zeze.Beans.Game.Rank.BRankValueReadOnly>RankList { get; }
    }

    public sealed class BRankList : Zeze.Transaction.Bean, BRankListReadOnly
    {
        readonly Zeze.Transaction.Collections.PList2<Zeze.Beans.Game.Rank.BRankValue> _RankList;

        public Zeze.Transaction.Collections.PList2<Zeze.Beans.Game.Rank.BRankValue> RankList => _RankList;
        System.Collections.Generic.IReadOnlyList<Zeze.Beans.Game.Rank.BRankValueReadOnly> Zeze.Beans.Game.Rank.BRankListReadOnly.RankList => _RankList;

        public BRankList() : this(0)
        {
        }

        public BRankList(int _varId_) : base(_varId_)
        {
            _RankList = new Zeze.Transaction.Collections.PList2<Zeze.Beans.Game.Rank.BRankValue>(ObjectId + 1, _v => new Log__RankList(this, _v));
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

        public BRankList Copy()
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

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = -81169960734319308;
        public override long TypeId => TYPEID;

        sealed class Log__RankList : Zeze.Transaction.Collections.PList2<Zeze.Beans.Game.Rank.BRankValue>.LogV
        {
            public Log__RankList(BRankList host, System.Collections.Immutable.ImmutableList<Zeze.Beans.Game.Rank.BRankValue> value) : base(host, value) {}
            public override long LogKey => Bean.ObjectId + 1;
            public BRankList BeanTyped => (BRankList)Bean;
            public override void Commit() { Commit(BeanTyped._RankList); }
        }

        public override string ToString()
        {
            System.Text.StringBuilder sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Beans.Game.Rank.BRankList: {").Append(Environment.NewLine);
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
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.BEAN);
                    foreach (var _v_ in _x_)
                        _v_.Encode(_o_);
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
                        _x_.Add(_o_.ReadBean(new Zeze.Beans.Game.Rank.BRankValue(), _t_));
                }
                else
                    _o_.SkipUnknownField(_t_);
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

        public override bool NegativeCheck()
        {
            foreach (var _v_ in RankList)
            {
                if (_v_.NegativeCheck()) return true;
            }
            return false;
        }
    }
}
