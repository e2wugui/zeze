// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BItem extends Zeze.Transaction.Bean {
    private int _Id;
    private int _Number;
    private final Zeze.Transaction.DynamicBean _Item;

    public int getId() {
        if (!isManaged())
            return _Id;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Id;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Id)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _Id;
    }

    public void setId(int value) {
        if (!isManaged()) {
            _Id = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Id(this, value));
    }

    public int getNumber() {
        if (!isManaged())
            return _Number;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Number;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Number)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _Number;
    }

    public void setNumber(int value) {
        if (!isManaged()) {
            _Number = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Number(this, value));
    }

    public Zeze.Transaction.DynamicBean getItem() {
        return _Item;
    }

    public BItem() {
         this(0);
    }

    public BItem(int _varId_) {
        super(_varId_);
        _Item = new Zeze.Transaction.DynamicBean(3, Zeze.Game.Bag::GetSpecialTypeIdFromBean, Zeze.Game.Bag::CreateBeanFromSpecialTypeId);
    }

    public void Assign(BItem other) {
        setId(other.getId());
        setNumber(other.getNumber());
        getItem().Assign(other.getItem());
    }

    public BItem CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BItem Copy() {
        var copy = new BItem();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BItem a, BItem b) {
        BItem save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 8937000213993683283L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__Id extends Zeze.Transaction.Log1<BItem, Integer> {
        public Log__Id(BItem self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._Id = this.getValue(); }
    }

    private static final class Log__Number extends Zeze.Transaction.Log1<BItem, Integer> {
        public Log__Number(BItem self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._Number = this.getValue(); }
    }

    public static long GetSpecialTypeIdFromBean_Item(Zeze.Transaction.Bean bean) {
        var _typeId_ = bean.getTypeId();
        if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        throw new RuntimeException("Unknown Bean! dynamic@Zeze.Builtin.Game.Bag.BItem:Item");
    }

    public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_Item(long typeId) {
        return null;
    }


    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Bag.BItem: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Id").append('=').append(getId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Number").append('=').append(getNumber()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(System.lineSeparator());
        getItem().getBean().BuildString(sb, level + 4);
        sb.append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @Override
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            int _x_ = getId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getNumber();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getItem();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.DYNAMIC);
                _x_.Encode(_o_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setNumber(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadDynamic(getItem(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Item.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        if (getId() < 0)
            return true;
        if (getNumber() < 0)
            return true;
        return false;
    }
}
