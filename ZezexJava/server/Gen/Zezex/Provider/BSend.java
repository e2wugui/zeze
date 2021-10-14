// auto-generated
package Zezex.Provider;

import Zeze.Serialize.*;

public final class BSend extends Zeze.Transaction.Bean implements BSendReadOnly {
    private Zeze.Transaction.Collections.PSet1<Long > _linkSids;
    private int _protocolType;
    private Zeze.Net.Binary _protocolWholeData; // 完整的协议打包，包括了 type, size
    private long _ConfirmSerialId; // 不为0的时候，linkd发送SendConfirm回逻辑服务器

    public Zeze.Transaction.Collections.PSet1<Long > getLinkSids() {
        return _linkSids;
    }

    public int getProtocolType(){
        if (false == this.isManaged())
            return _protocolType;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _protocolType;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__protocolType)txn.GetLog(this.getObjectId() + 2);
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

    public Zeze.Net.Binary getProtocolWholeData(){
        if (false == this.isManaged())
            return _protocolWholeData;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _protocolWholeData;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__protocolWholeData)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _protocolWholeData;
    }

    public void setProtocolWholeData(Zeze.Net.Binary value){
            if (null == value)
                throw new IllegalArgumentException();
        if (false == this.isManaged()) {
            _protocolWholeData = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__protocolWholeData(this, value));
    }

    public long getConfirmSerialId(){
        if (false == this.isManaged())
            return _ConfirmSerialId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ConfirmSerialId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ConfirmSerialId)txn.GetLog(this.getObjectId() + 4);
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


    public BSend() {
         this(0);
    }

    public BSend(int _varId_) {
        super(_varId_);
        _linkSids = new Zeze.Transaction.Collections.PSet1<Long >(getObjectId() + 1, (_v) -> new Log__linkSids(this, _v));
        _protocolWholeData = Zeze.Net.Binary.Empty;
    }

    public void Assign(BSend other) {
        getLinkSids().clear();
        for (var e : other.getLinkSids()) {
            getLinkSids().add(e);
        }
        setProtocolType(other.getProtocolType());
        setProtocolWholeData(other.getProtocolWholeData());
        setConfirmSerialId(other.getConfirmSerialId());
    }

    public BSend CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BSend Copy() {
        var copy = new BSend();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BSend a, BSend b) {
        BSend save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = 9160848190830466174L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final class Log__linkSids extends Zeze.Transaction.Collections.PSet.LogV<Long> {
        public Log__linkSids(BSend host, org.pcollections.PSet<Long> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 1; }
        public BSend getBeanTyped() { return (BSend)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._linkSids); }
    }

    private final static class Log__protocolType extends Zeze.Transaction.Log1<BSend, Integer> {
        public Log__protocolType(BSend self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._protocolType = this.getValue(); }
    }

    private final static class Log__protocolWholeData extends Zeze.Transaction.Log1<BSend, Zeze.Net.Binary> {
        public Log__protocolWholeData(BSend self, Zeze.Net.Binary value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._protocolWholeData = this.getValue(); }
    }

    private final static class Log__ConfirmSerialId extends Zeze.Transaction.Log1<BSend, Long> {
        public Log__ConfirmSerialId(BSend self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 4; }
        @Override
        public void Commit() { this.getBeanTyped()._ConfirmSerialId = this.getValue(); }
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
        sb.append(" ".repeat(level * 4)).append("Zezex.Provider.BSend: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("linkSids").append("=[").append(System.lineSeparator());
        level++;
        for (var _item_ : getLinkSids()) {
            sb.append(" ".repeat(level * 4)).append("Item").append("=").append(_item_).append(",").append(System.lineSeparator());
        }
        level--;
        sb.append(" ".repeat(level * 4)).append("],").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("protocolType").append("=").append(getProtocolType()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("protocolWholeData").append("=").append(getProtocolWholeData()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ConfirmSerialId").append("=").append(getConfirmSerialId()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(4); // Variables.Count
        _os_.WriteInt(ByteBuffer.SET | 1 << ByteBuffer.TAG_SHIFT);
        {
            var _state_ = _os_.BeginWriteSegment();
            _os_.WriteInt(ByteBuffer.LONG);
            _os_.WriteInt(getLinkSids().size());
            for (var _v_ : getLinkSids()) {
                _os_.WriteLong(_v_);
            }
            _os_.EndWriteSegment(_state_); 
        }
        _os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getProtocolType());
        _os_.WriteInt(ByteBuffer.BYTES | 3 << ByteBuffer.TAG_SHIFT);
        _os_.WriteBinary(getProtocolWholeData());
        _os_.WriteInt(ByteBuffer.LONG | 4 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getConfirmSerialId());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.SET | 1 << ByteBuffer.TAG_SHIFT):
                    {
                        var _state_ = _os_.BeginReadSegment();
                        _os_.ReadInt(); // skip collection.value typetag
                        getLinkSids().clear();
                        for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
                            long _v_;
                            _v_ = _os_.ReadLong();
                            getLinkSids().add(_v_);
                        }
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case (ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT): 
                    setProtocolType(_os_.ReadInt());
                    break;
                case (ByteBuffer.BYTES | 3 << ByteBuffer.TAG_SHIFT): 
                    setProtocolWholeData(_os_.ReadBinary());
                    break;
                case (ByteBuffer.LONG | 4 << ByteBuffer.TAG_SHIFT): 
                    setConfirmSerialId(_os_.ReadLong());
                    break;
                default:
                    ByteBuffer.SkipUnknownField(_tagid_, _os_);
                    break;
            }
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _linkSids.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        for (var _v_ : getLinkSids())
        {
            if (_v_ < 0) return true;
        }
        if (getProtocolType() < 0) return true;
        if (getConfirmSerialId() < 0) return true;
        return false;
    }

}
