// auto-generated
package Game.Equip;

import Zeze.Serialize.*;

public final class BUnequipement extends Zeze.Transaction.Bean implements BUnequipementReadOnly {
    private int _EquipPos;

    public int getEquipPos(){
        if (false == this.isManaged())
            return _EquipPos;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _EquipPos;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__EquipPos)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _EquipPos;
    }

    public void setEquipPos(int value){
        if (false == this.isManaged()) {
            _EquipPos = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__EquipPos(this, value));
    }


    public BUnequipement() {
         this(0);
    }

    public BUnequipement(int _varId_) {
        super(_varId_);
    }

    public void Assign(BUnequipement other) {
        setEquipPos(other.getEquipPos());
    }

    public BUnequipement CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BUnequipement Copy() {
        var copy = new BUnequipement();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BUnequipement a, BUnequipement b) {
        BUnequipement save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = -1486699047078318477L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__EquipPos extends Zeze.Transaction.Log1<BUnequipement, Integer> {
        public Log__EquipPos(BUnequipement self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._EquipPos = this.getValue(); }
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
        sb.append(" ".repeat(level * 4)).append("Game.Equip.BUnequipement: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("EquipPos").append("=").append(getEquipPos()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(1); // Variables.Count
        _os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getEquipPos());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT): 
                    setEquipPos(_os_.ReadInt());
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
        if (getEquipPos() < 0) return true;
        return false;
    }

}
