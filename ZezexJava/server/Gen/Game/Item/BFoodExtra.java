// auto-generated
package Game.Item;

import Zeze.Serialize.*;

public final class BFoodExtra extends Zeze.Transaction.Bean implements BFoodExtraReadOnly {
    private int _Ammount;

    public int getAmmount(){
        if (false == this.isManaged())
            return _Ammount;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _Ammount;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Ammount)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _Ammount;
    }

    public void setAmmount(int value){
        if (false == this.isManaged()) {
            _Ammount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Ammount(this, value));
    }


    public BFoodExtra() {
         this(0);
    }

    public BFoodExtra(int _varId_) {
        super(_varId_);
    }

    public void Assign(BFoodExtra other) {
        setAmmount(other.getAmmount());
    }

    public BFoodExtra CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BFoodExtra Copy() {
        var copy = new BFoodExtra();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BFoodExtra a, BFoodExtra b) {
        BFoodExtra save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = -5635260117858385112L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__Ammount extends Zeze.Transaction.Log1<BFoodExtra, Integer> {
        public Log__Ammount(BFoodExtra self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._Ammount = this.getValue(); }
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
        sb.append(" ".repeat(level * 4)).append("Game.Item.BFoodExtra: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("Ammount").append("=").append(getAmmount()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(1); // Variables.Count
        _os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getAmmount());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT): 
                    setAmmount(_os_.ReadInt());
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
        if (getAmmount() < 0) return true;
        return false;
    }

}
