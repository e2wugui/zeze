// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BBag extends Zeze.Transaction.Bean {
    private int _Capacity;
    private final Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.Game.Bag.BItem> _Items; // key is bag position

    public int getCapacity() {
        if (!isManaged())
            return _Capacity;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Capacity;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Capacity)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _Capacity;
    }

    public void setCapacity(int value) {
        if (!isManaged()) {
            _Capacity = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Capacity(this, value));
    }

    public Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.Game.Bag.BItem> getItems() {
        return _Items;
    }

    public BBag() {
         this(0);
    }

    public BBag(int _varId_) {
        super(_varId_);
        _Items = new Zeze.Transaction.Collections.PMap2<>(getObjectId() + 2, (_v) -> new Log__Items(this, _v));
    }

    public void Assign(BBag other) {
        setCapacity(other.getCapacity());
        getItems().clear();
        for (var e : other.getItems().entrySet())
            getItems().put(e.getKey(), e.getValue().Copy());
    }

    public BBag CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BBag Copy() {
        var copy = new BBag();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BBag a, BBag b) {
        BBag save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -5051317137860806350L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__Capacity extends Zeze.Transaction.Log1<BBag, Integer> {
        public Log__Capacity(BBag self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._Capacity = this.getValue(); }
    }

    private static final class Log__Items extends Zeze.Transaction.Collections.PMap.LogV<Integer, Zeze.Builtin.Game.Bag.BItem> {
        public Log__Items(BBag host, org.pcollections.PMap<Integer, Zeze.Builtin.Game.Bag.BItem> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 2; }
        public BBag getBeanTyped() { return (BBag)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._Items); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Bag.BBag: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Capacity").append('=').append(getCapacity()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Items").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getItems().entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().BuildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(System.lineSeparator());
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
            int _x_ = getCapacity();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getItems();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().Encode(_o_);
                }
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setCapacity(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = getItems();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Game.Bag.BItem(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Items.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        if (getCapacity() < 0)
            return true;
        for (var _v_ : getItems().values()) {
            if (_v_.NegativeCheck())
                return true;
        }
        return false;
    }
}
