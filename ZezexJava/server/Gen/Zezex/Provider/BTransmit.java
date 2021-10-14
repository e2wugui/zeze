// auto-generated
package Zezex.Provider;

import Zeze.Serialize.*;

public final class BTransmit extends Zeze.Transaction.Bean implements BTransmitReadOnly {
    private String _ActionName;
    private Zeze.Transaction.Collections.PMap2<Long, Zezex.Provider.BTransmitContext> _Roles; // 查询目标角色。
    private long _Sender; // 结果发送给Sender。
    private String _ServiceNamePrefix;

    public String getActionName(){
        if (false == this.isManaged())
            return _ActionName;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ActionName;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ActionName)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _ActionName;
    }

    public void setActionName(String value){
            if (null == value)
                throw new IllegalArgumentException();
        if (false == this.isManaged()) {
            _ActionName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ActionName(this, value));
    }

    public Zeze.Transaction.Collections.PMap2<Long, Zezex.Provider.BTransmitContext> getRoles() {
        return _Roles;
    }

    public long getSender(){
        if (false == this.isManaged())
            return _Sender;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _Sender;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Sender)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _Sender;
    }

    public void setSender(long value){
        if (false == this.isManaged()) {
            _Sender = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Sender(this, value));
    }

    public String getServiceNamePrefix(){
        if (false == this.isManaged())
            return _ServiceNamePrefix;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ServiceNamePrefix;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ServiceNamePrefix)txn.GetLog(this.getObjectId() + 4);
        return log != null ? log.getValue() : _ServiceNamePrefix;
    }

    public void setServiceNamePrefix(String value){
            if (null == value)
                throw new IllegalArgumentException();
        if (false == this.isManaged()) {
            _ServiceNamePrefix = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ServiceNamePrefix(this, value));
    }


    public BTransmit() {
         this(0);
    }

    public BTransmit(int _varId_) {
        super(_varId_);
        _ActionName = "";
        _Roles = new Zeze.Transaction.Collections.PMap2<Long, Zezex.Provider.BTransmitContext>(getObjectId() + 2, (_v) -> new Log__Roles(this, _v));
        _ServiceNamePrefix = "";
    }

    public void Assign(BTransmit other) {
        setActionName(other.getActionName());
        getRoles().clear();
        for (var e : other.getRoles().entrySet()) {
            getRoles().put(e.getKey(), e.getValue().Copy());
        }
        setSender(other.getSender());
        setServiceNamePrefix(other.getServiceNamePrefix());
    }

    public BTransmit CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BTransmit Copy() {
        var copy = new BTransmit();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BTransmit a, BTransmit b) {
        BTransmit save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = 1899659324986950870L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__ActionName extends Zeze.Transaction.Log1<BTransmit, String> {
        public Log__ActionName(BTransmit self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._ActionName = this.getValue(); }
    }

    private final class Log__Roles extends Zeze.Transaction.Collections.PMap.LogV<Long, Zezex.Provider.BTransmitContext> {
        public Log__Roles(BTransmit host, org.pcollections.PMap<Long, Zezex.Provider.BTransmitContext> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 2; }
        public BTransmit getBeanTyped() { return (BTransmit)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._Roles); }
    }

    private final static class Log__Sender extends Zeze.Transaction.Log1<BTransmit, Long> {
        public Log__Sender(BTransmit self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._Sender = this.getValue(); }
    }

    private final static class Log__ServiceNamePrefix extends Zeze.Transaction.Log1<BTransmit, String> {
        public Log__ServiceNamePrefix(BTransmit self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 4; }
        @Override
        public void Commit() { this.getBeanTyped()._ServiceNamePrefix = this.getValue(); }
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
        sb.append(" ".repeat(level * 4)).append("Zezex.Provider.BTransmit: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("ActionName").append("=").append(getActionName()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("Roles").append("=[").append(System.lineSeparator());
        level++;
        for (var _kv_ : getRoles().entrySet()) {
            sb.append("(").append(System.lineSeparator());
            sb.append(" ".repeat(level * 4)).append("Key").append("=").append(_kv_.getKey()).append(",").append(System.lineSeparator());
            sb.append(" ".repeat(level * 4)).append("Value").append("=").append(System.lineSeparator());
            _kv_.getValue().BuildString(sb, level + 1);
            sb.append(",").append(System.lineSeparator());
            sb.append(")").append(System.lineSeparator());
        }
        level--;
        sb.append(" ".repeat(level * 4)).append("],").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("Sender").append("=").append(getSender()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ServiceNamePrefix").append("=").append(getServiceNamePrefix()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(4); // Variables.Count
        _os_.WriteInt(ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteString(getActionName());
        _os_.WriteInt(ByteBuffer.MAP | 2 << ByteBuffer.TAG_SHIFT);
        {
            var _state_ = _os_.BeginWriteSegment();
            _os_.WriteInt(ByteBuffer.LONG);
            _os_.WriteInt(ByteBuffer.BEAN);
            _os_.WriteInt(getRoles().size());
            for  (var _e_ : getRoles().entrySet())
            {
                _os_.WriteLong(_e_.getKey());
                _e_.getValue().Encode(_os_);
            }
            _os_.EndWriteSegment(_state_); 
        }
        _os_.WriteInt(ByteBuffer.LONG | 3 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getSender());
        _os_.WriteInt(ByteBuffer.STRING | 4 << ByteBuffer.TAG_SHIFT);
        _os_.WriteString(getServiceNamePrefix());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.STRING | 1 << ByteBuffer.TAG_SHIFT): 
                    setActionName(_os_.ReadString());
                    break;
                case (ByteBuffer.MAP | 2 << ByteBuffer.TAG_SHIFT):
                    {
                        var _state_ = _os_.BeginReadSegment();
                        _os_.ReadInt(); // skip key typetag
                        _os_.ReadInt(); // skip value typetag
                        getRoles().clear();
                        for (int size = _os_.ReadInt(); size > 0; --size) {
                            long _k_;
                            _k_ = _os_.ReadLong();
                            Zezex.Provider.BTransmitContext _v_ = new Zezex.Provider.BTransmitContext();
                            _v_.Decode(_os_);
                            getRoles().put(_k_, _v_);
                        }
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                case (ByteBuffer.LONG | 3 << ByteBuffer.TAG_SHIFT): 
                    setSender(_os_.ReadLong());
                    break;
                case (ByteBuffer.STRING | 4 << ByteBuffer.TAG_SHIFT): 
                    setServiceNamePrefix(_os_.ReadString());
                    break;
                default:
                    ByteBuffer.SkipUnknownField(_tagid_, _os_);
                    break;
            }
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Roles.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        for (var _v_ : getRoles().values())
        {
            if (_v_.NegativeCheck()) return true;
        }
        if (getSender() < 0) return true;
        return false;
    }

}
