// auto-generated
package Game.Bag;

import Zeze.Serialize.*;

public final class BBag extends Zeze.Transaction.Bean implements BBagReadOnly {
    private long _Money;
    private int _Capacity;
    private Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem> _Items; // key is bag position

    public long getMoney(){
        if (false == this.isManaged())
            return _Money;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _Money;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Money)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _Money;
    }

    public void setMoney(long value){
        if (false == this.isManaged()) {
            _Money = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Money(this, value));
    }

    public int getCapacity(){
        if (false == this.isManaged())
            return _Capacity;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _Capacity;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Capacity)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _Capacity;
    }

    public void setCapacity(int value){
        if (false == this.isManaged()) {
            _Capacity = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Capacity(this, value));
    }

    public Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem> getItems() {
        return _Items;
    }


    public BBag() {
         this(0);
    }

    public BBag(int _varId_) {
        super(_varId_);
        _Items = new Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem>(getObjectId() + 3, (_v) -> new Log__Items(this, _v));
    }

    public void Assign(BBag other) {
        setMoney(other.getMoney());
        setCapacity(other.getCapacity());
        getItems().clear();
        for (var e : other.getItems().entrySet()) {
            getItems().put(e.getKey(), e.getValue().Copy());
        }
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

    public final static long TYPEID = -7082293047368631199L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__Money extends Zeze.Transaction.Log1<BBag, Long> {
        public Log__Money(BBag self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._Money = this.getValue(); }
    }

    private final static class Log__Capacity extends Zeze.Transaction.Log1<BBag, Integer> {
        public Log__Capacity(BBag self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._Capacity = this.getValue(); }
    }

    private final class Log__Items extends Zeze.Transaction.Collections.PMap.LogV<Integer, Game.Bag.BItem> {
        public Log__Items(BBag host, org.pcollections.PMap<Integer, Game.Bag.BItem> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 3; }
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
        sb.append(" ".repeat(level * 4)).append("Game.Bag.BBag: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("Money").append("=").append(getMoney()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("Capacity").append("=").append(getCapacity()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("Items").append("=[").append(System.lineSeparator());
        level++;
        for (var _kv_ : getItems().entrySet()) {
            sb.append("(").append(System.lineSeparator());
            sb.append(" ".repeat(level * 4)).append("Key").append("=").append(_kv_.getKey()).append(",").append(System.lineSeparator());
            sb.append(" ".repeat(level * 4)).append("Value").append("=").append(System.lineSeparator());
            _kv_.getValue().BuildString(sb, level + 1);
            sb.append(",").append(System.lineSeparator());
            sb.append(")").append(System.lineSeparator());
        }
        level--;
        sb.append(" ".repeat(level * 4)).append("]").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(3); // Variables.Count
        _os_.WriteInt(ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getMoney());
        _os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getCapacity());
        _os_.WriteInt(ByteBuffer.MAP | 3 << ByteBuffer.TAG_SHIFT);
        {
            var _state_ = _os_.BeginWriteSegment();
            _os_.WriteInt(ByteBuffer.INT);
            _os_.WriteInt(ByteBuffer.BEAN);
            _os_.WriteInt(getItems().size());
            for  (var _e_ : getItems().entrySet())
            {
                _os_.WriteInt(_e_.getKey());
                _e_.getValue().Encode(_os_);
            }
            _os_.EndWriteSegment(_state_); 
        }
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT): 
                    setMoney(_os_.ReadLong());
                    break;
                case (ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT): 
                    setCapacity(_os_.ReadInt());
                    break;
                case (ByteBuffer.MAP | 3 << ByteBuffer.TAG_SHIFT):
                    {
                        var _state_ = _os_.BeginReadSegment();
                        _os_.ReadInt(); // skip key typetag
                        _os_.ReadInt(); // skip value typetag
                        getItems().clear();
                        for (int size = _os_.ReadInt(); size > 0; --size) {
                            int _k_;
                            _k_ = _os_.ReadInt();
                            Game.Bag.BItem _v_ = new Game.Bag.BItem();
                            _v_.Decode(_os_);
                            getItems().put(_k_, _v_);
                        }
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                default:
                    ByteBuffer.SkipUnknownField(_tagid_, _os_);
                    break;
            }
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Items.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        if (getMoney() < 0) return true;
        if (getCapacity() < 0) return true;
        for (var _v_ : getItems().values())
        {
            if (_v_.NegativeCheck()) return true;
        }
        return false;
    }

}
