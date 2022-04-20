// auto-generated @formatter:off
package Zeze.Beans.Game.Bag;

import Zeze.Serialize.ByteBuffer;

public final class BChanged extends Zeze.Transaction.Bean {
    public static final int TagIncrementChange = 0; // 增量修改。
    public static final int TagRecordRemoved = 1; // 整个记录删除了。
    public static final int TagRecordReplace = 2; // 整个记录发生了变更，需要先清除本地数据，再替换进去。

    private String _BagName;
    private int _Tag; // 处理方式
    private final Zeze.Transaction.Collections.PMap2<Integer, Zeze.Beans.Game.Bag.BItem> _Replaced; // key is position
    private final Zeze.Transaction.Collections.PSet1<Integer> _Removed; // key is position

    public String getBagName() {
        if (!isManaged())
            return _BagName;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _BagName;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__BagName)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _BagName;
    }

    public void setBagName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _BagName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__BagName(this, value));
    }

    public int getTag() {
        if (!isManaged())
            return _Tag;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Tag;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Tag)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _Tag;
    }

    public void setTag(int value) {
        if (!isManaged()) {
            _Tag = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Tag(this, value));
    }

    public Zeze.Transaction.Collections.PMap2<Integer, Zeze.Beans.Game.Bag.BItem> getReplaced() {
        return _Replaced;
    }

    public Zeze.Transaction.Collections.PSet1<Integer> getRemoved() {
        return _Removed;
    }

    public BChanged() {
         this(0);
    }

    public BChanged(int _varId_) {
        super(_varId_);
        _BagName = "";
        _Replaced = new Zeze.Transaction.Collections.PMap2<>(getObjectId() + 3, (_v) -> new Log__Replaced(this, _v));
        _Removed = new Zeze.Transaction.Collections.PSet1<>(getObjectId() + 4, (_v) -> new Log__Removed(this, _v));
    }

    public void Assign(BChanged other) {
        setBagName(other.getBagName());
        setTag(other.getTag());
        getReplaced().clear();
        for (var e : other.getReplaced().entrySet())
            getReplaced().put(e.getKey(), e.getValue().Copy());
        getRemoved().clear();
        for (var e : other.getRemoved())
            getRemoved().add(e);
    }

    public BChanged CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BChanged Copy() {
        var copy = new BChanged();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BChanged a, BChanged b) {
        BChanged save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -256031737420695466L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__BagName extends Zeze.Transaction.Log1<BChanged, String> {
        public Log__BagName(BChanged self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._BagName = this.getValue(); }
    }

    private static final class Log__Tag extends Zeze.Transaction.Log1<BChanged, Integer> {
        public Log__Tag(BChanged self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._Tag = this.getValue(); }
    }

    private static final class Log__Replaced extends Zeze.Transaction.Collections.PMap.LogV<Integer, Zeze.Beans.Game.Bag.BItem> {
        public Log__Replaced(BChanged host, org.pcollections.PMap<Integer, Zeze.Beans.Game.Bag.BItem> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 3; }
        public BChanged getBeanTyped() { return (BChanged)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._Replaced); }
    }

    private static final class Log__Removed extends Zeze.Transaction.Collections.PSet.LogV<Integer> {
        public Log__Removed(BChanged host, org.pcollections.PSet<Integer> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 4; }
        public BChanged getBeanTyped() { return (BChanged)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._Removed); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Beans.Game.Bag.BChanged: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("BagName").append('=').append(getBagName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Tag").append('=').append(getTag()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Replaced").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getReplaced().entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().BuildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Removed").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getRemoved()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(_item_).append(',').append(System.lineSeparator());
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

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            String _x_ = getBagName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getTag();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getReplaced();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().Encode(_o_);
                }
            }
        }
        {
            var _x_ = getRemoved();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_)
                    _o_.WriteLong(_v_);
            }
        }
        _o_.WriteByte(0);
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setBagName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTag(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = getReplaced();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Beans.Game.Bag.BItem(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = getRemoved();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
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
        _Replaced.InitRootInfo(root, this);
        _Removed.InitRootInfo(root, this);
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean NegativeCheck() {
        if (getTag() < 0)
            return true;
        for (var _v_ : getReplaced().values()) {
            if (_v_.NegativeCheck())
                return true;
        }
        for (var _v_ : getRemoved()) {
            if (_v_ < 0)
                return true;
        }
        return false;
    }
}
