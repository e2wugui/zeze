// auto-generated
package Game.Login;

import Zeze.Serialize.*;

public final class BOnline extends Zeze.Transaction.Bean implements BOnlineReadOnly {
    public static final int StateOffline = 0;
    public static final int StateOnline = 2;
    public static final int StateNetBroken = 3; // 客户端连接断开时，一定时间内可以重连。超时会删除 Online-Record。

    private String _LinkName;
    private long _LinkSid;
    private int _State;
    private Zeze.Transaction.Collections.PSet1<String > _ReliableNotifyMark;
    private Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> _ReliableNotifyQueue; // full encoded protocol list
    private long _ReliableNotifyConfirmCount;
    private long _ReliableNotifyTotalCount;
    private int _ProviderId; // Config.AutoKeyLocalId
    private long _ProviderSessionId; // 登录所在Linkd与当前Provider的连接在Linkd方的SessionId

    public String getLinkName(){
        if (false == this.isManaged())
            return _LinkName;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _LinkName;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__LinkName)txn.GetLog(this.getObjectId() + 1);
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

    public long getLinkSid(){
        if (false == this.isManaged())
            return _LinkSid;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _LinkSid;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__LinkSid)txn.GetLog(this.getObjectId() + 2);
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

    public int getState(){
        if (false == this.isManaged())
            return _State;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _State;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__State)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _State;
    }

    public void setState(int value){
        if (false == this.isManaged()) {
            _State = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__State(this, value));
    }

    public Zeze.Transaction.Collections.PSet1<String > getReliableNotifyMark() {
        return _ReliableNotifyMark;
    }

    public Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> getReliableNotifyQueue() {
        return _ReliableNotifyQueue;
    }


    public long getReliableNotifyConfirmCount(){
        if (false == this.isManaged())
            return _ReliableNotifyConfirmCount;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ReliableNotifyConfirmCount;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ReliableNotifyConfirmCount)txn.GetLog(this.getObjectId() + 6);
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

    public long getReliableNotifyTotalCount(){
        if (false == this.isManaged())
            return _ReliableNotifyTotalCount;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ReliableNotifyTotalCount;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ReliableNotifyTotalCount)txn.GetLog(this.getObjectId() + 7);
        return log != null ? log.getValue() : _ReliableNotifyTotalCount;
    }

    public void setReliableNotifyTotalCount(long value){
        if (false == this.isManaged()) {
            _ReliableNotifyTotalCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ReliableNotifyTotalCount(this, value));
    }

    public int getProviderId(){
        if (false == this.isManaged())
            return _ProviderId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ProviderId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ProviderId)txn.GetLog(this.getObjectId() + 8);
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
        var log = (Log__ProviderSessionId)txn.GetLog(this.getObjectId() + 9);
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


    public BOnline() {
         this(0);
    }

    public BOnline(int _varId_) {
        super(_varId_);
        _LinkName = "";
        _State = StateOffline;
        _ReliableNotifyMark = new Zeze.Transaction.Collections.PSet1<String >(getObjectId() + 4, (_v) -> new Log__ReliableNotifyMark(this, _v));
        _ReliableNotifyQueue = new Zeze.Transaction.Collections.PList1<Zeze.Net.Binary>(getObjectId() + 5, (_v) -> new Log__ReliableNotifyQueue(this, _v));
    }

    public void Assign(BOnline other) {
        setLinkName(other.getLinkName());
        setLinkSid(other.getLinkSid());
        setState(other.getState());
        getReliableNotifyMark().clear();
        for (var e : other.getReliableNotifyMark()) {
            getReliableNotifyMark().add(e);
        }
        getReliableNotifyQueue().clear();
        for (var e : other.getReliableNotifyQueue()) {
            getReliableNotifyQueue().add(e);
        }
        setReliableNotifyConfirmCount(other.getReliableNotifyConfirmCount());
        setReliableNotifyTotalCount(other.getReliableNotifyTotalCount());
        setProviderId(other.getProviderId());
        setProviderSessionId(other.getProviderSessionId());
    }

    public BOnline CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BOnline Copy() {
        var copy = new BOnline();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BOnline a, BOnline b) {
        BOnline save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = 620718797232710243L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__LinkName extends Zeze.Transaction.Log1<BOnline, String> {
        public Log__LinkName(BOnline self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._LinkName = this.getValue(); }
    }

    private final static class Log__LinkSid extends Zeze.Transaction.Log1<BOnline, Long> {
        public Log__LinkSid(BOnline self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._LinkSid = this.getValue(); }
    }

    private final static class Log__State extends Zeze.Transaction.Log1<BOnline, Integer> {
        public Log__State(BOnline self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._State = this.getValue(); }
    }

    private final class Log__ReliableNotifyMark extends Zeze.Transaction.Collections.PSet.LogV<String> {
        public Log__ReliableNotifyMark(BOnline host, org.pcollections.PSet<String> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 4; }
        public BOnline getBeanTyped() { return (BOnline)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._ReliableNotifyMark); }
    }

    private final class Log__ReliableNotifyQueue extends Zeze.Transaction.Collections.PList.LogV<Zeze.Net.Binary> {
        public Log__ReliableNotifyQueue(BOnline host, org.pcollections.PVector<Zeze.Net.Binary> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 5; }
        public BOnline getBeanTyped() { return (BOnline)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._ReliableNotifyQueue); }
    }

    private final static class Log__ReliableNotifyConfirmCount extends Zeze.Transaction.Log1<BOnline, Long> {
        public Log__ReliableNotifyConfirmCount(BOnline self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 6; }
        @Override
        public void Commit() { this.getBeanTyped()._ReliableNotifyConfirmCount = this.getValue(); }
    }

    private final static class Log__ReliableNotifyTotalCount extends Zeze.Transaction.Log1<BOnline, Long> {
        public Log__ReliableNotifyTotalCount(BOnline self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 7; }
        @Override
        public void Commit() { this.getBeanTyped()._ReliableNotifyTotalCount = this.getValue(); }
    }

    private final static class Log__ProviderId extends Zeze.Transaction.Log1<BOnline, Integer> {
        public Log__ProviderId(BOnline self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 8; }
        @Override
        public void Commit() { this.getBeanTyped()._ProviderId = this.getValue(); }
    }

    private final static class Log__ProviderSessionId extends Zeze.Transaction.Log1<BOnline, Long> {
        public Log__ProviderSessionId(BOnline self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 9; }
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
        sb.append(" ".repeat(level * 4)).append("Game.Login.BOnline: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("LinkName").append("=").append(getLinkName()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("LinkSid").append("=").append(getLinkSid()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("State").append("=").append(getState()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ReliableNotifyMark").append("=[").append(System.lineSeparator());
        level++;
        for (var _item_ : getReliableNotifyMark()) {
            sb.append(" ".repeat(level * 4)).append("Item").append("=").append(_item_).append(",").append(System.lineSeparator());
        }
        level--;
        sb.append(" ".repeat(level * 4)).append("],").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ReliableNotifyQueue").append("=[").append(System.lineSeparator());
        level++;
        for (var _item_ : getReliableNotifyQueue()) {
            sb.append(" ".repeat(level * 4)).append("Item").append("=").append(_item_).append(",").append(System.lineSeparator());
        }
        level--;
        sb.append(" ".repeat(level * 4)).append("],").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ReliableNotifyConfirmCount").append("=").append(getReliableNotifyConfirmCount()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ReliableNotifyTotalCount").append("=").append(getReliableNotifyTotalCount()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ProviderId").append("=").append(getProviderId()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ProviderSessionId").append("=").append(getProviderSessionId()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(9); // Variables.Count
        _os_.WriteInt(ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteString(getLinkName());
        _os_.WriteInt(ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getLinkSid());
        _os_.WriteInt(ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getState());
        _os_.WriteInt(ByteBuffer.SET | 4 << ByteBuffer.TAG_SHIFT);
        {
            var _state_ = _os_.BeginWriteSegment();
            _os_.WriteInt(ByteBuffer.STRING);
            _os_.WriteInt(getReliableNotifyMark().size());
            for (var _v_ : getReliableNotifyMark()) {
                _os_.WriteString(_v_);
            }
            _os_.EndWriteSegment(_state_); 
        }
        _os_.WriteInt(ByteBuffer.LIST | 5 << ByteBuffer.TAG_SHIFT);
        {
            var _state_ = _os_.BeginWriteSegment();
            _os_.WriteInt(ByteBuffer.BYTES);
            _os_.WriteInt(getReliableNotifyQueue().size());
            for (var _v_ : getReliableNotifyQueue()) {
                _os_.WriteBinary(_v_);
            }
            _os_.EndWriteSegment(_state_); 
        }
        _os_.WriteInt(ByteBuffer.LONG | 6 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getReliableNotifyConfirmCount());
        _os_.WriteInt(ByteBuffer.LONG | 7 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getReliableNotifyTotalCount());
        _os_.WriteInt(ByteBuffer.INT | 8 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getProviderId());
        _os_.WriteInt(ByteBuffer.LONG | 9 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getProviderSessionId());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT): 
                    setLinkName(_os_.ReadString());
                    break;
                case (ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT): 
                    setLinkSid(_os_.ReadLong());
                    break;
                case (ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT): 
                    setState(_os_.ReadInt());
                    break;
                case (ByteBuffer.SET | 4 << ByteBuffer.TAG_SHIFT):
                    {
                        var _state_ = _os_.BeginReadSegment();
                        _os_.ReadInt(); // skip collection.value typetag
                        getReliableNotifyMark().clear();
                        for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
                            String _v_;
                            _v_ = _os_.ReadString();
                            getReliableNotifyMark().add(_v_);
                        }
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case (ByteBuffer.LIST | 5 << ByteBuffer.TAG_SHIFT):
                    {
                        var _state_ = _os_.BeginReadSegment();
                        _os_.ReadInt(); // skip collection.value typetag
                        getReliableNotifyQueue().clear();
                        for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
                            Zeze.Net.Binary _v_;
                            _v_ = _os_.ReadBinary();
                            getReliableNotifyQueue().add(_v_);
                        }
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case (ByteBuffer.LONG | 6 << ByteBuffer.TAG_SHIFT): 
                    setReliableNotifyConfirmCount(_os_.ReadLong());
                    break;
                case (ByteBuffer.LONG | 7 << ByteBuffer.TAG_SHIFT): 
                    setReliableNotifyTotalCount(_os_.ReadLong());
                    break;
                case (ByteBuffer.INT | 8 << ByteBuffer.TAG_SHIFT): 
                    setProviderId(_os_.ReadInt());
                    break;
                case (ByteBuffer.LONG | 9 << ByteBuffer.TAG_SHIFT): 
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
        _ReliableNotifyMark.InitRootInfo(root, this);
        _ReliableNotifyQueue.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        if (getLinkSid() < 0) return true;
        if (getState() < 0) return true;
        if (getReliableNotifyConfirmCount() < 0) return true;
        if (getReliableNotifyTotalCount() < 0) return true;
        if (getProviderId() < 0) return true;
        if (getProviderSessionId() < 0) return true;
        return false;
    }

}
