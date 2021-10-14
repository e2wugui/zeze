// auto-generated
package Zezex.Provider;

import Zeze.Serialize.*;

public final class BSendConfirm extends Zeze.Transaction.Bean implements BSendConfirmReadOnly {
    private long _ConfirmSerialId; // SendConfirm 参数，即Send.Argument.ConfirmSerialId
    private String _LinkName;

    public long getConfirmSerialId(){
        if (false == this.isManaged())
            return _ConfirmSerialId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ConfirmSerialId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ConfirmSerialId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _ConfirmSerialId;
    }

    public void setConfirmSerialId(long value){
        if (false == this.isManaged()) {
            _ConfirmSerialId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ConfirmSerialId(this, value));
    }

    public String getLinkName(){
        if (false == this.isManaged())
            return _LinkName;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _LinkName;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__LinkName)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _LinkName;
    }

    public void setLinkName(String value){
            if (null == value)
                throw new IllegalArgumentException();
        if (false == this.isManaged()) {
            _LinkName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__LinkName(this, value));
    }


    public BSendConfirm() {
         this(0);
    }

    public BSendConfirm(int _varId_) {
        super(_varId_);
        _LinkName = "";
    }

    public void Assign(BSendConfirm other) {
        setConfirmSerialId(other.getConfirmSerialId());
        setLinkName(other.getLinkName());
    }

    public BSendConfirm CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BSendConfirm Copy() {
        var copy = new BSendConfirm();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BSendConfirm a, BSendConfirm b) {
        BSendConfirm save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = -8948494963149231406L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__ConfirmSerialId extends Zeze.Transaction.Log1<BSendConfirm, Long> {
        public Log__ConfirmSerialId(BSendConfirm self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._ConfirmSerialId = this.getValue(); }
    }

    private final static class Log__LinkName extends Zeze.Transaction.Log1<BSendConfirm, String> {
        public Log__LinkName(BSendConfirm self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._LinkName = this.getValue(); }
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
        sb.append(" ".repeat(level * 4)).append("Zezex.Provider.BSendConfirm: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("ConfirmSerialId").append("=").append(getConfirmSerialId()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("LinkName").append("=").append(getLinkName()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(2); // Variables.Count
        _os_.WriteInt(ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getConfirmSerialId());
        _os_.WriteInt(ByteBuffer.STRING | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteString(getLinkName());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT): 
                    setConfirmSerialId(_os_.ReadLong());
                    break;
                case (ByteBuffer.STRING | 2 << ByteBuffer.TAG_SHIFT): 
                    setLinkName(_os_.ReadString());
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
        if (getConfirmSerialId() < 0) return true;
        return false;
    }

}
