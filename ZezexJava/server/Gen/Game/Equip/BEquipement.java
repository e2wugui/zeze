// auto-generated
package Game.Equip;

import Zeze.Serialize.*;

public final class BEquipement extends Zeze.Transaction.Bean implements BEquipementReadOnly {
    private int _BagPos;

    public int getBagPos(){
        if (false == this.isManaged())
            return _BagPos;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _BagPos;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__BagPos)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _BagPos;
    }

    public void setBagPos(int value){
        if (false == this.isManaged()) {
            _BagPos = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__BagPos(this, value));
    }


    public BEquipement() {
         this(0);
    }

    public BEquipement(int _varId_) {
        super(_varId_);
    }

    public void Assign(BEquipement other) {
        setBagPos(other.getBagPos());
    }

    public BEquipement CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BEquipement Copy() {
        var copy = new BEquipement();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BEquipement a, BEquipement b) {
        BEquipement save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = 6617691447491079020L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__BagPos extends Zeze.Transaction.Log1<BEquipement, Integer> {
        public Log__BagPos(BEquipement self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._BagPos = this.getValue(); }
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
        sb.append(" ".repeat(level * 4)).append("Game.Equip.BEquipement: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("BagPos").append("=").append(getBagPos()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(1); // Variables.Count
        _os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getBagPos());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT): 
                    setBagPos(_os_.ReadInt());
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
        if (getBagPos() < 0) return true;
        return false;
    }

}
