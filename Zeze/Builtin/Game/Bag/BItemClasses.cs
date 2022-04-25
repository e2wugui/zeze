// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.Game.Bag
{
    public interface BItemClassesReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public Zeze.Transaction.Bean CopyBean();

        public System.Collections.Generic.IReadOnlySet<string> ItemClasses { get; }
    }

    public sealed class BItemClasses : Zeze.Transaction.Bean, BItemClassesReadOnly
    {
        readonly Zeze.Transaction.Collections.PSet1<string> _ItemClasses;

        public Zeze.Transaction.Collections.PSet1<string> ItemClasses => _ItemClasses;
        System.Collections.Generic.IReadOnlySet<string> Zeze.Builtin.Game.Bag.BItemClassesReadOnly.ItemClasses => _ItemClasses;

        public BItemClasses() : this(0)
        {
        }

        public BItemClasses(int _varId_) : base(_varId_)
        {
            _ItemClasses = new Zeze.Transaction.Collections.PSet1<string>(ObjectId + 1, _v => new Log__ItemClasses(this, _v));
        }

        public void Assign(BItemClasses other)
        {
            ItemClasses.Clear();
            foreach (var e in other.ItemClasses)
                ItemClasses.Add(e);
        }

        public BItemClasses CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public BItemClasses Copy()
        {
            var copy = new BItemClasses();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BItemClasses a, BItemClasses b)
        {
            BItemClasses save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Transaction.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = 1779211758793833239;
        public override long TypeId => TYPEID;

        sealed class Log__ItemClasses : Zeze.Transaction.Collections.PSet1<string>.LogV
        {
            public Log__ItemClasses(BItemClasses host, System.Collections.Immutable.ImmutableHashSet<string> value) : base(host, value) {}
            public override long LogKey => Bean.ObjectId + 1;
            public BItemClasses BeanTyped => (BItemClasses)Bean;
            public override void Commit() { Commit(BeanTyped._ItemClasses); }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Game.Bag.BItemClasses: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ItemClasses").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in ItemClasses)
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
                var _x_ = ItemClasses;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.BYTES);
                    foreach (var _v_ in _x_)
                    {
                        _o_.WriteString(_v_);
                    }
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
                var _x_ = ItemClasses;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    {
                        _x_.Add(_o_.ReadString(_t_));
                    }
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
            _ItemClasses.InitRootInfo(root, this);
        }

        public override bool NegativeCheck()
        {
            return false;
        }
    }
}
