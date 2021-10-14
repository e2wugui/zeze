// auto-generated
package Zezex.Provider;

import Zeze.Serialize.*;

public final class BLinkBroken extends Zeze.Transaction.Bean implements BLinkBrokenReadOnly {
    public static final int REASON_PEERCLOSE = 0;

    private String _account;
    private long _linkSid;
    private int _reason;
    private Zeze.Transaction.Collections.PList1<Long> _states; // SetUserState
    private Zeze.Net.Binary _statex; // SetUserState

    public String getAccount(){
        if (false == this.isManaged())
            return _account;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _account;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__account)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _account;
    }

    public void setAccount(String value){
            if (null == value)
                throw new IllegalArgumentException();
        if (false == this.isManaged()) {
            _account = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__account(this, value));
    }

    public long getLinkSid(){
        if (false == this.isManaged())
            return _linkSid;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _linkSid;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__linkSid)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _linkSid;
    }

    public void setLinkSid(long value){
        if (false == this.isManaged()) {
            _linkSid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__linkSid(this, value));
    }

    public int getReason(){
        if (false == this.isManaged())
            return _reason;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _reason;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__reason)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _reason;
    }

    public void setReason(int value){
        if (false == this.isManaged()) {
            _reason = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__reason(this, value));
    }

    public Zeze.Transaction.Collections.PList1<Long> getStates() {
        return _states;
    }


    public Zeze.Net.Binary getStatex(){
        if (false == this.isManaged())
            return _statex;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _statex;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__statex)txn.GetLog(this.getObjectId() + 6);
        return log != null ? log.getValue() : _statex;
    }

    public void setStatex(Zeze.Net.Binary value){
            if (null == value)
                throw new IllegalArgumentException();
        if (false == this.isManaged()) {
            _statex = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__statex(this, value));
    }


    public BLinkBroken() {
         this(0);
    }

    public BLinkBroken(int _varId_) {
        super(_varId_);
        _account = "";
        _states = new Zeze.Transaction.Collections.PList1<Long>(getObjectId() + 5, (_v) -> new Log__states(this, _v));
        _statex = Zeze.Net.Binary.Empty;
    }

    public void Assign(BLinkBroken other) {
        setAccount(other.getAccount());
        setLinkSid(other.getLinkSid());
        setReason(other.getReason());
        getStates().clear();
        for (var e : other.getStates()) {
            getStates().add(e);
        }
        setStatex(other.getStatex());
    }

    public BLinkBroken CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BLinkBroken Copy() {
        var copy = new BLinkBroken();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BLinkBroken a, BLinkBroken b) {
        BLinkBroken save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = 5003488678339236183L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__account extends Zeze.Transaction.Log1<BLinkBroken, String> {
        public Log__account(BLinkBroken self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._account = this.getValue(); }
    }

    private final static class Log__linkSid extends Zeze.Transaction.Log1<BLinkBroken, Long> {
        public Log__linkSid(BLinkBroken self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._linkSid = this.getValue(); }
    }

    private final static class Log__reason extends Zeze.Transaction.Log1<BLinkBroken, Integer> {
        public Log__reason(BLinkBroken self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._reason = this.getValue(); }
    }

    private final class Log__states extends Zeze.Transaction.Collections.PList.LogV<Long> {
        public Log__states(BLinkBroken host, org.pcollections.PVector<Long> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 5; }
        public BLinkBroken getBeanTyped() { return (BLinkBroken)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._states); }
    }

    private final static class Log__statex extends Zeze.Transaction.Log1<BLinkBroken, Zeze.Net.Binary> {
        public Log__statex(BLinkBroken self, Zeze.Net.Binary value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 6; }
        @Override
        public void Commit() { this.getBeanTyped()._statex = this.getValue(); }
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
        sb.append(" ".repeat(level * 4)).append("Zezex.Provider.BLinkBroken: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("account").append("=").append(getAccount()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("linkSid").append("=").append(getLinkSid()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("reason").append("=").append(getReason()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("states").append("=[").append(System.lineSeparator());
        level++;
        for (var _item_ : getStates()) {
            sb.append(" ".repeat(level * 4)).append("Item").append("=").append(_item_).append(",").append(System.lineSeparator());
        }
        level--;
        sb.append(" ".repeat(level * 4)).append("],").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("statex").append("=").append(getStatex()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(5); // Variables.Count
        _os_.WriteInt(ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteString(getAccount());
        _os_.WriteInt(ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getLinkSid());
        _os_.WriteInt(ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getReason());
        _os_.WriteInt(ByteBuffer.LIST | 5 << ByteBuffer.TAG_SHIFT);
        {
            var _state_ = _os_.BeginWriteSegment();
            _os_.WriteInt(ByteBuffer.LONG);
            _os_.WriteInt(getStates().size());
            for (var _v_ : getStates()) {
                _os_.WriteLong(_v_);
            }
            _os_.EndWriteSegment(_state_); 
        }
        _os_.WriteInt(ByteBuffer.BYTES | 6 << ByteBuffer.TAG_SHIFT);
        _os_.WriteBinary(getStatex());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT): 
                    setAccount(_os_.ReadString());
                    break;
                case (ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT): 
                    setLinkSid(_os_.ReadLong());
                    break;
                case (ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT): 
                    setReason(_os_.ReadInt());
                    break;
                case (ByteBuffer.LIST | 5 << ByteBuffer.TAG_SHIFT):
                    {
                        var _state_ = _os_.BeginReadSegment();
                        _os_.ReadInt(); // skip collection.value typetag
                        getStates().clear();
                        for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
                            long _v_;
                            _v_ = _os_.ReadLong();
                            getStates().add(_v_);
                        }
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case (ByteBuffer.BYTES | 6 << ByteBuffer.TAG_SHIFT): 
                    setStatex(_os_.ReadBinary());
                    break;
                default:
                    ByteBuffer.SkipUnknownField(_tagid_, _os_);
                    break;
            }
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _states.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        if (getLinkSid() < 0) return true;
        if (getReason() < 0) return true;
        for (var _v_ : getStates())
        {
            if (_v_ < 0) return true;
        }
        return false;
    }

}
