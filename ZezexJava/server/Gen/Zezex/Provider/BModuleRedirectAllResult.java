// auto-generated
package Zezex.Provider;

import Zeze.Serialize.*;

public final class BModuleRedirectAllResult extends Zeze.Transaction.Bean implements BModuleRedirectAllResultReadOnly {
    private int _ModuleId;
    private int _ServerId; // 目标server的id。
    private long _SourceProvider; // 从BModuleRedirectAllRequest里面得到。
    private String _MethodFullName; // format="ModuleFullName:MethodName"
    private long _SessionId; // 发起请求者初始化，返回结果时带回。
    private Zeze.Transaction.Collections.PMap2<Integer, Zezex.Provider.BModuleRedirectAllHash> _Hashs; // 发送给具体进程时需要处理的分组hash-index（目前由linkd填写）

    public int getModuleId(){
        if (false == this.isManaged())
            return _ModuleId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ModuleId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ModuleId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _ModuleId;
    }

    public void setModuleId(int value){
        if (false == this.isManaged()) {
            _ModuleId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ModuleId(this, value));
    }

    public int getServerId(){
        if (false == this.isManaged())
            return _ServerId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _ServerId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ServerId)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _ServerId;
    }

    public void setServerId(int value){
        if (false == this.isManaged()) {
            _ServerId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ServerId(this, value));
    }

    public long getSourceProvider(){
        if (false == this.isManaged())
            return _SourceProvider;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _SourceProvider;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__SourceProvider)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _SourceProvider;
    }

    public void setSourceProvider(long value){
        if (false == this.isManaged()) {
            _SourceProvider = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__SourceProvider(this, value));
    }

    public String getMethodFullName(){
        if (false == this.isManaged())
            return _MethodFullName;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _MethodFullName;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__MethodFullName)txn.GetLog(this.getObjectId() + 4);
        return log != null ? log.getValue() : _MethodFullName;
    }

    public void setMethodFullName(String value){
            if (null == value)
                throw new IllegalArgumentException();
        if (false == this.isManaged()) {
            _MethodFullName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__MethodFullName(this, value));
    }

    public long getSessionId(){
        if (false == this.isManaged())
            return _SessionId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _SessionId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__SessionId)txn.GetLog(this.getObjectId() + 5);
        return log != null ? log.getValue() : _SessionId;
    }

    public void setSessionId(long value){
        if (false == this.isManaged()) {
            _SessionId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__SessionId(this, value));
    }

    public Zeze.Transaction.Collections.PMap2<Integer, Zezex.Provider.BModuleRedirectAllHash> getHashs() {
        return _Hashs;
    }


    public BModuleRedirectAllResult() {
         this(0);
    }

    public BModuleRedirectAllResult(int _varId_) {
        super(_varId_);
        _MethodFullName = "";
        _Hashs = new Zeze.Transaction.Collections.PMap2<Integer, Zezex.Provider.BModuleRedirectAllHash>(getObjectId() + 6, (_v) -> new Log__Hashs(this, _v));
    }

    public void Assign(BModuleRedirectAllResult other) {
        setModuleId(other.getModuleId());
        setServerId(other.getServerId());
        setSourceProvider(other.getSourceProvider());
        setMethodFullName(other.getMethodFullName());
        setSessionId(other.getSessionId());
        getHashs().clear();
        for (var e : other.getHashs().entrySet()) {
            getHashs().put(e.getKey(), e.getValue().Copy());
        }
    }

    public BModuleRedirectAllResult CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BModuleRedirectAllResult Copy() {
        var copy = new BModuleRedirectAllResult();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BModuleRedirectAllResult a, BModuleRedirectAllResult b) {
        BModuleRedirectAllResult save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = 1951985867510056420L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__ModuleId extends Zeze.Transaction.Log1<BModuleRedirectAllResult, Integer> {
        public Log__ModuleId(BModuleRedirectAllResult self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._ModuleId = this.getValue(); }
    }

    private final static class Log__ServerId extends Zeze.Transaction.Log1<BModuleRedirectAllResult, Integer> {
        public Log__ServerId(BModuleRedirectAllResult self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._ServerId = this.getValue(); }
    }

    private final static class Log__SourceProvider extends Zeze.Transaction.Log1<BModuleRedirectAllResult, Long> {
        public Log__SourceProvider(BModuleRedirectAllResult self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._SourceProvider = this.getValue(); }
    }

    private final static class Log__MethodFullName extends Zeze.Transaction.Log1<BModuleRedirectAllResult, String> {
        public Log__MethodFullName(BModuleRedirectAllResult self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 4; }
        @Override
        public void Commit() { this.getBeanTyped()._MethodFullName = this.getValue(); }
    }

    private final static class Log__SessionId extends Zeze.Transaction.Log1<BModuleRedirectAllResult, Long> {
        public Log__SessionId(BModuleRedirectAllResult self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 5; }
        @Override
        public void Commit() { this.getBeanTyped()._SessionId = this.getValue(); }
    }

    private final class Log__Hashs extends Zeze.Transaction.Collections.PMap.LogV<Integer, Zezex.Provider.BModuleRedirectAllHash> {
        public Log__Hashs(BModuleRedirectAllResult host, org.pcollections.PMap<Integer, Zezex.Provider.BModuleRedirectAllHash> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 6; }
        public BModuleRedirectAllResult getBeanTyped() { return (BModuleRedirectAllResult)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._Hashs); }
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
        sb.append(" ".repeat(level * 4)).append("Zezex.Provider.BModuleRedirectAllResult: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("ModuleId").append("=").append(getModuleId()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ServerId").append("=").append(getServerId()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("SourceProvider").append("=").append(getSourceProvider()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("MethodFullName").append("=").append(getMethodFullName()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("SessionId").append("=").append(getSessionId()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("Hashs").append("=[").append(System.lineSeparator());
        level++;
        for (var _kv_ : getHashs().entrySet()) {
            sb.append("(").append(System.lineSeparator());
            sb.append(" ".repeat(level * 4)).append("Key").append("=").append(_kv_.getKey()).append(",").append(System.lineSeparator());
            sb.append(" ".repeat(level * 4)).append("Value").append("=").append(System.lineSeparator());
            _kv_.getValue().BuildString(sb, level + 1);
            sb.append(",").append(System.lineSeparator());
            sb.append(")").append(System.lineSeparator());
        }
        level--;
        sb.append(" ".repeat(level * 4)).append("]").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(6); // Variables.Count
        _os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getModuleId());
        _os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getServerId());
        _os_.WriteInt(ByteBuffer.LONG | 3 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getSourceProvider());
        _os_.WriteInt(ByteBuffer.STRING | 4 << ByteBuffer.TAG_SHIFT);
        _os_.WriteString(getMethodFullName());
        _os_.WriteInt(ByteBuffer.LONG | 5 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getSessionId());
        _os_.WriteInt(ByteBuffer.MAP | 6 << ByteBuffer.TAG_SHIFT);
        {
            var _state_ = _os_.BeginWriteSegment();
            _os_.WriteInt(ByteBuffer.INT);
            _os_.WriteInt(ByteBuffer.BEAN);
            _os_.WriteInt(getHashs().size());
            for  (var _e_ : getHashs().entrySet())
            {
                _os_.WriteInt(_e_.getKey());
                _e_.getValue().Encode(_os_);
            }
            _os_.EndWriteSegment(_state_); 
        }
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT): 
                    setModuleId(_os_.ReadInt());
                    break;
                case (ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT): 
                    setServerId(_os_.ReadInt());
                    break;
                case (ByteBuffer.LONG | 3 << ByteBuffer.TAG_SHIFT): 
                    setSourceProvider(_os_.ReadLong());
                    break;
                case (ByteBuffer.STRING | 4 << ByteBuffer.TAG_SHIFT): 
                    setMethodFullName(_os_.ReadString());
                    break;
                case (ByteBuffer.LONG | 5 << ByteBuffer.TAG_SHIFT): 
                    setSessionId(_os_.ReadLong());
                    break;
                case (ByteBuffer.MAP | 6 << ByteBuffer.TAG_SHIFT):
                    {
                        var _state_ = _os_.BeginReadSegment();
                        _os_.ReadInt(); // skip key typetag
                        _os_.ReadInt(); // skip value typetag
                        getHashs().clear();
                        for (int size = _os_.ReadInt(); size > 0; --size) {
                            int _k_;
                            _k_ = _os_.ReadInt();
                            Zezex.Provider.BModuleRedirectAllHash _v_ = new Zezex.Provider.BModuleRedirectAllHash();
                            _v_.Decode(_os_);
                            getHashs().put(_k_, _v_);
                        }
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                default:
                    ByteBuffer.SkipUnknownField(_tagid_, _os_);
                    break;
            }
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Hashs.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        if (getModuleId() < 0) return true;
        if (getServerId() < 0) return true;
        if (getSourceProvider() < 0) return true;
        if (getSessionId() < 0) return true;
        for (var _v_ : getHashs().values())
        {
            if (_v_.NegativeCheck()) return true;
        }
        return false;
    }

}
