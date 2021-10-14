// auto-generated
package Game.Bag;

import Zeze.Serialize.*;

public final class BMove extends Zeze.Transaction.Bean implements BMoveReadOnly {
    private int _PositionFrom;
    private int _PositionTo;
    private int _number; // -1 表示全部

    public int getPositionFrom(){
        if (false == this.isManaged())
            return _PositionFrom;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _PositionFrom;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__PositionFrom)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _PositionFrom;
    }

    public void setPositionFrom(int value){
        if (false == this.isManaged()) {
            _PositionFrom = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__PositionFrom(this, value));
    }

    public int getPositionTo(){
        if (false == this.isManaged())
            return _PositionTo;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _PositionTo;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__PositionTo)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _PositionTo;
    }

    public void setPositionTo(int value){
        if (false == this.isManaged()) {
            _PositionTo = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__PositionTo(this, value));
    }

    public int getNumber(){
        if (false == this.isManaged())
            return _number;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _number;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__number)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _number;
    }

    public void setNumber(int value){
        if (false == this.isManaged()) {
            _number = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__number(this, value));
    }


    public BMove() {
         this(0);
    }

    public BMove(int _varId_) {
        super(_varId_);
    }

    public void Assign(BMove other) {
        setPositionFrom(other.getPositionFrom());
        setPositionTo(other.getPositionTo());
        setNumber(other.getNumber());
    }

    public BMove CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BMove Copy() {
        var copy = new BMove();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BMove a, BMove b) {
        BMove save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = -3340594790196747034L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__PositionFrom extends Zeze.Transaction.Log1<BMove, Integer> {
        public Log__PositionFrom(BMove self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._PositionFrom = this.getValue(); }
    }

    private final static class Log__PositionTo extends Zeze.Transaction.Log1<BMove, Integer> {
        public Log__PositionTo(BMove self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._PositionTo = this.getValue(); }
    }

    private final static class Log__number extends Zeze.Transaction.Log1<BMove, Integer> {
        public Log__number(BMove self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._number = this.getValue(); }
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
        sb.append(" ".repeat(level * 4)).append("Game.Bag.BMove: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("PositionFrom").append("=").append(getPositionFrom()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("PositionTo").append("=").append(getPositionTo()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("number").append("=").append(getNumber()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(3); // Variables.Count
        _os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getPositionFrom());
        _os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getPositionTo());
        _os_.WriteInt(ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getNumber());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT): 
                    setPositionFrom(_os_.ReadInt());
                    break;
                case (ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT): 
                    setPositionTo(_os_.ReadInt());
                    break;
                case (ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT): 
                    setNumber(_os_.ReadInt());
                    break;
                default:
                    ByteBuffer.SkipUnknownField(_tagid_, _os_);
                    break;
            }
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    public boolean NegativeCheck() {
        if (getPositionFrom() < 0) return true;
        if (getPositionTo() < 0) return true;
        if (getNumber() < 0) return true;
        return false;
    }

}
