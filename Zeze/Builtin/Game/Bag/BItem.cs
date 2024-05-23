// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Game.Bag
{
    public interface BItemReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BItem Copy();

        public int Id { get; }
        public int Number { get; }
        public Zeze.Transaction.DynamicBeanReadOnly Item { get; }

    }

    public sealed class BItem : Zeze.Transaction.Bean, BItemReadOnly
    {
        int _Id;
        int _Number;
        readonly Zeze.Transaction.DynamicBean _Item;
        public static Zeze.Transaction.DynamicBean NewDynamicBeanItem()
        {
            return new Zeze.Transaction.DynamicBean(3, Zeze.Game.Bag.GetSpecialTypeIdFromBean, Zeze.Game.Bag.CreateBeanFromSpecialTypeId);
        }

        public static long GetSpecialTypeIdFromBean_3(Zeze.Transaction.Bean bean)
        {
            return Zeze.Game.Bag.GetSpecialTypeIdFromBean(bean);
        }

        public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_3(long typeId)
        {
            return Zeze.Game.Bag.CreateBeanFromSpecialTypeId(typeId);
        }


        public int _zeze_map_key_int_ { get; set; }

        public int Id
        {
            get
            {
                if (!IsManaged)
                    return _Id;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Id;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Id)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _Id;
            }
            set
            {
                if (!IsManaged)
                {
                    _Id = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Id() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public int Number
        {
            get
            {
                if (!IsManaged)
                    return _Number;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Number;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Number)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _Number;
            }
            set
            {
                if (!IsManaged)
                {
                    _Number = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Number() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public Zeze.Transaction.DynamicBean Item => _Item;
        Zeze.Transaction.DynamicBeanReadOnly Zeze.Builtin.Game.Bag.BItemReadOnly.Item => Item;

        public BItem()
        {
            _Item = new Zeze.Transaction.DynamicBean(3, Zeze.Game.Bag.GetSpecialTypeIdFromBean, Zeze.Game.Bag.CreateBeanFromSpecialTypeId);
        }

        public BItem(int _Id_, int _Number_)
        {
            _Id = _Id_;
            _Number = _Number_;
            _Item = new Zeze.Transaction.DynamicBean(3, Zeze.Game.Bag.GetSpecialTypeIdFromBean, Zeze.Game.Bag.CreateBeanFromSpecialTypeId);
        }

        public void Assign(BItem other)
        {
            Id = other.Id;
            Number = other.Number;
            Item.Assign(other.Item);
        }

        public BItem CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BItem Copy()
        {
            var copy = new BItem();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BItem a, BItem b)
        {
            BItem save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 8937000213993683283;
        public override long TypeId => TYPEID;

        sealed class Log__Id : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BItem)Belong)._Id = this.Value; }
        }

        sealed class Log__Number : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BItem)Belong)._Number = this.Value; }
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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Game.Bag.BItem: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Id").Append('=').Append(Id).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Number").Append('=').Append(Number).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Environment.NewLine);
            Item.Bean.BuildString(sb, level + 4);
            sb.Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = Id;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = Number;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                var _x_ = Item;
                if (!_x_.IsEmpty())
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.DYNAMIC);
                    _x_.Encode(_o_);
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
                Id = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                Number = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                _o_.ReadDynamic(Item, _t_);
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
            _Item.InitRootInfo(root, this);
        }

        protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)
        {
            _Item.InitRootInfoWithRedo(root, this);
        }

        public override bool NegativeCheck()
        {
            if (Id < 0) return true;
            if (Number < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _Id = vlog.IntValue(); break;
                    case 2: _Number = vlog.IntValue(); break;
                    case 3: _Item.FollowerApply(vlog); break;
                }
            }
        }

        public override void ClearParameters()
        {
            Id = 0;
            Number = 0;
            Item.ClearParameters();
        }
    }
}
