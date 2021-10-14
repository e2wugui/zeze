// auto-generated
package Zezex.Provider;

import Zeze.Serialize.*;

public final class BTransmitContext extends Zeze.Transaction.Bean implements BTransmitContextReadOnly {
    private long _LinkSid;
    private int _ProviderId;
    private long _ProviderSessionId;

    public long getLinkSid(){
        if (false == this.isManaged())
            return _LinkSid;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _LinkSid;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__LinkSid)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _LinkSid;
    }

    public void setLinkSid(long value){
        if (false == this.isManaged()) {
            _LinkSid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__LinkSid(this, value));
    }

    public int getProviderId(){
        if (false == this.isManaged())
            return _ProviderId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ProviderId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ProviderId)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _ProviderId;
    }

    public void setProviderId(int value){
        if (false == this.isManaged()) {
            _ProviderId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ProviderId(this, value));
    }

    public long getProviderSessionId(){
        if (false == this.isManaged())
            return _ProviderSessionId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ProviderSessionId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ProviderSessionId)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _ProviderSessionId;
    }

    public void setProviderSessionId(long value){
        if (false == this.isManaged()) {
            _ProviderSessionId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ProviderSessionId(this, value));
    }


    public BTransmitContext() {
         this(0);
    }

    public BTransmitContext(int _varId_) {
        super(_varId_);
    }

    public void Assign(BTransmitContext other) {
        setLinkSid(other.getLinkSid());
        setProviderId(other.getProviderId());
        setProviderSessionId(other.getProviderSessionId());
    }

    public BTransmitContext CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BTransmitContext Copy() {
        var copy = new BTransmitContext();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BTransmitContext a, BTransmitContext b) {
        BTransmitContext save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = -3920505070130795749L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__LinkSid extends Zeze.Transaction.Log1<BTransmitContext, Long> {
        public Log__LinkSid(BTransmitContext self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._LinkSid = this.getValue(); }
    }

    private final static class Log__ProviderId extends Zeze.Transaction.Log1<BTransmitContext, Integer> {
        public Log__ProviderId(BTransmitContext self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._ProviderId = this.getValue(); }
    }

    private final static class Log__ProviderSessionId extends Zeze.Transaction.Log1<BTransmitContext, Long> {
        public Log__ProviderSessionId(BTransmitContext self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._ProviderSessionId = this.getValue(); }
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
        sb.append(" ".repeat(level * 4)).append("Zezex.Provider.BTransmitContext: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("LinkSid").append("=").append(getLinkSid()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ProviderId").append("=").append(getProviderId()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ProviderSessionId").append("=").append(getProviderSessionId()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(3); // Variables.Count
        _os_.WriteInt(ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getLinkSid());
        _os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getProviderId());
        _os_.WriteInt(ByteBuffer.LONG | 3 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getProviderSessionId());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT): 
                    setLinkSid(_os_.ReadLong());
                    break;
                case (ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT): 
                    setProviderId(_os_.ReadInt());
                    break;
                case (ByteBuffer.LONG | 3 << ByteBuffer.TAG_SHIFT): 
                    setProviderSessionId(_os_.ReadLong());
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
        if (getLinkSid() < 0) return true;
        if (getProviderId() < 0) return true;
        if (getProviderSessionId() < 0) return true;
        return false;
    }

}
