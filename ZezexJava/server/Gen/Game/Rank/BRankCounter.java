// auto-generated
package Game.Rank;

import Zeze.Serialize.*;

public final class BRankCounter extends Zeze.Transaction.Bean implements BRankCounterReadOnly {
    private long _Value;

    public long getValue(){
        if (false == this.isManaged())
            return _Value;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _Value;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Value)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _Value;
    }

    public void setValue(long value){
        if (false == this.isManaged()) {
            _Value = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Value(this, value));
    }


    public BRankCounter() {
         this(0);
    }

    public BRankCounter(int _varId_) {
        super(_varId_);
    }

    public void Assign(BRankCounter other) {
        setValue(other.getValue());
    }

    public BRankCounter CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BRankCounter Copy() {
        var copy = new BRankCounter();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BRankCounter a, BRankCounter b) {
        BRankCounter save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = -4202658848701450829L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__Value extends Zeze.Transaction.Log1<BRankCounter, Long> {
        public Log__Value(BRankCounter self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._Value = this.getValue(); }
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
        sb.append(" ".repeat(level * 4)).append("Game.Rank.BRankCounter: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("Value").append("=").append(getValue()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(1); // Variables.Count
        _os_.WriteInt(ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getValue());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT): 
                    setValue(_os_.ReadLong());
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
        if (getValue() < 0) return true;
        return false;
    }

}
