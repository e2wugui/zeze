// auto-generated
package Zezex.Provider;

import Zeze.Serialize.*;

public final class BDispatch extends Zeze.Transaction.Bean implements BDispatchReadOnly {
    private long _linkSid;
    private String _account;
    private int _protocolType;
    private Zeze.Net.Binary _protocolData; // 协议打包，不包括 type, size
    private Zeze.Transaction.Collections.PList1<Long> _states; // SetUserState
    private Zeze.Net.Binary _statex; // SetUserState

    public long getLinkSid(){
        if (false == this.isManaged())
            return _linkSid;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _linkSid;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__linkSid)txn.GetLog(this.getObjectId() + 1);
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

    public String getAccount(){
        if (false == this.isManaged())
            return _account;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _account;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__account)txn.GetLog(this.getObjectId() + 2);
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

    public int getProtocolType(){
        if (false == this.isManaged())
            return _protocolType;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _protocolType;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__protocolType)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _protocolType;
    }

    public void setProtocolType(int value){
        if (false == this.isManaged()) {
            _protocolType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__protocolType(this, value));
    }

    public Zeze.Net.Binary getProtocolData(){
        if (false == this.isManaged())
            return _protocolData;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _protocolData;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__protocolData)txn.GetLog(this.getObjectId() + 4);
        return log != null ? log.getValue() : _protocolData;
    }

    public void setProtocolData(Zeze.Net.Binary value){
            if (null == value)
                throw new IllegalArgumentException();
        if (false == this.isManaged()) {
            _protocolData = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__protocolData(this, value));
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


    public BDispatch() {
         this(0);
    }

    public BDispatch(int _varId_) {
        super(_varId_);
        _account = "";
        _protocolData = Zeze.Net.Binary.Empty;
        _states = new Zeze.Transaction.Collections.PList1<Long>(getObjectId() + 5, (_v) -> new Log__states(this, _v));
        _statex = Zeze.Net.Binary.Empty;
    }

    public void Assign(BDispatch other) {
        setLinkSid(other.getLinkSid());
        setAccount(other.getAccount());
        setProtocolType(other.getProtocolType());
        setProtocolData(other.getProtocolData());
        getStates().clear();
        for (var e : other.getStates()) {
            getStates().add(e);
        }
        setStatex(other.getStatex());
    }

    public BDispatch CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BDispatch Copy() {
        var copy = new BDispatch();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BDispatch a, BDispatch b) {
        BDispatch save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = 5741746203543905036L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__linkSid extends Zeze.Transaction.Log1<BDispatch, Long> {
        public Log__linkSid(BDispatch self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._linkSid = this.getValue(); }
    }

    private final static class Log__account extends Zeze.Transaction.Log1<BDispatch, String> {
        public Log__account(BDispatch self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._account = this.getValue(); }
    }

    private final static class Log__protocolType extends Zeze.Transaction.Log1<BDispatch, Integer> {
        public Log__protocolType(BDispatch self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._protocolType = this.getValue(); }
    }

    private final static class Log__protocolData extends Zeze.Transaction.Log1<BDispatch, Zeze.Net.Binary> {
        public Log__protocolData(BDispatch self, Zeze.Net.Binary value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 4; }
        @Override
        public void Commit() { this.getBeanTyped()._protocolData = this.getValue(); }
    }

    private final class Log__states extends Zeze.Transaction.Collections.PList.LogV<Long> {
        public Log__states(BDispatch host, org.pcollections.PVector<Long> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 5; }
        public BDispatch getBeanTyped() { return (BDispatch)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._states); }
    }

    private final static class Log__statex extends Zeze.Transaction.Log1<BDispatch, Zeze.Net.Binary> {
        public Log__statex(BDispatch self, Zeze.Net.Binary value) { super(self, value); }
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
        sb.append(" ".repeat(level * 4)).append("Zezex.Provider.BDispatch: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("linkSid").append("=").append(getLinkSid()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("account").append("=").append(getAccount()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("protocolType").append("=").append(getProtocolType()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("protocolData").append("=").append(getProtocolData()).append(",").append(System.lineSeparator());
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
        _os_.WriteInt(6); // Variables.Count
        _os_.WriteInt(ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getLinkSid());
        _os_.WriteInt(ByteBuffer.STRING | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteString(getAccount());
        _os_.WriteInt(ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getProtocolType());
        _os_.WriteInt(ByteBuffer.BYTES | 4 << ByteBuffer.TAG_SHIFT);
        _os_.WriteBinary(getProtocolData());
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
                case (ByteBuffer.LONG | 1 << ByteBuffer.TAG_SHIFT): 
                    setLinkSid(_os_.ReadLong());
                    break;
                case (ByteBuffer.STRING | 2 << ByteBuffer.TAG_SHIFT): 
                    setAccount(_os_.ReadString());
                    break;
                case (ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT): 
                    setProtocolType(_os_.ReadInt());
                    break;
                case (ByteBuffer.BYTES | 4 << ByteBuffer.TAG_SHIFT): 
                    setProtocolData(_os_.ReadBinary());
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
        if (getProtocolType() < 0) return true;
        for (var _v_ : getStates())
        {
            if (_v_ < 0) return true;
        }
        return false;
    }

}
