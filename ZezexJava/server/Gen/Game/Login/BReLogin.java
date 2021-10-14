// auto-generated
package Game.Login;

import Zeze.Serialize.*;

public final class BReLogin extends Zeze.Transaction.Bean implements BReLoginReadOnly {
    private long _RoleId;
    private long _ReliableNotifyConfirmCount;

    public long getRoleId(){
        if (false == this.isManaged())
            return _RoleId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _RoleId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__RoleId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _RoleId;
    }

    public void setRoleId(long value){
        if (false == this.isManaged()) {
            _RoleId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__RoleId(this, value));
    }

    public long getReliableNotifyConfirmCount(){
        if (false == this.isManaged())
            return _ReliableNotifyConfirmCount;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ReliableNotifyConfirmCount;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ReliableNotifyConfirmCount)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _ReliableNotifyConfirmCount;
    }

    public void setReliableNotifyConfirmCount(long value){
        if (false == this.isManaged()) {
            _ReliableNotifyConfirmCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ReliableNotifyConfirmCount(this, value));
    }


    public BReLogin() {
         this(0);
    }

    public BReLogin(int _varId_) {
        super(_varId_);
    }

    public void Assign(BReLogin other) {
        setRoleId(other.getRoleId());
        setReliableNotifyConfirmCount(other.getReliableNotifyConfirmCount());
    }

    public BReLogin CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BReLogin Copy() {
        var copy = new BReLogin();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BReLogin a, BReLogin b) {
        BReLogin save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = -7137643269568049052L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__RoleId extends Zeze.Transaction.Log1<BReLogin, Long> {
        public Log__RoleId(BReLogin self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._RoleId = this.getValue(); }
    }

    private final static class Log__ReliableNotifyConfirmCount extends Zeze.Transaction.Log1<BReLogin, Long> {
        public Log__ReliableNotifyConfirmCount(BReLogin self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._ReliableNotifyConfirmCount = this.getValue(); }
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
        sb.append(" ".repeat(level * 4)).append("Game.Login.BReLogin: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("RoleId").append("=").append(getRoleId()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ReliableNotifyConfirmCount").append("=").append(getReliableNotifyConfirmCount()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(2); // Variables.Count
        _os_.WriteInt(ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getRoleId());
        _os_.WriteInt(ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getReliableNotifyConfirmCount());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT): 
                    setRoleId(_os_.ReadLong());
                    break;
                case (ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT): 
                    setReliableNotifyConfirmCount(_os_.ReadLong());
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
        if (getRoleId() < 0) return true;
        if (getReliableNotifyConfirmCount() < 0) return true;
        return false;
    }

}
