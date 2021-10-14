// auto-generated
package Game.Login;

import Zeze.Serialize.*;

public final class BLogin extends Zeze.Transaction.Bean implements BLoginReadOnly {
    private long _RoleId;

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


    public BLogin() {
         this(0);
    }

    public BLogin(int _varId_) {
        super(_varId_);
    }

    public void Assign(BLogin other) {
        setRoleId(other.getRoleId());
    }

    public BLogin CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BLogin Copy() {
        var copy = new BLogin();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BLogin a, BLogin b) {
        BLogin save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = 8607117430330547217L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__RoleId extends Zeze.Transaction.Log1<BLogin, Long> {
        public Log__RoleId(BLogin self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._RoleId = this.getValue(); }
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
        sb.append(" ".repeat(level * 4)).append("Game.Login.BLogin: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("RoleId").append("=").append(getRoleId()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(1); // Variables.Count
        _os_.WriteInt(ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getRoleId());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT): 
                    setRoleId(_os_.ReadLong());
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
        return false;
    }

}
