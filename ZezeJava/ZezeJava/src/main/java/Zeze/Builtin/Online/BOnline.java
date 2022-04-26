// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BOnline extends Zeze.Transaction.Bean {
    public static final int StateOffline = 0;
    public static final int StateOnline = 2;
    public static final int StateNetBroken = 3; // 客户端连接断开时，一定时间内可以重连。超时会删除 Online-Record。

    private String _LinkName;
    private long _LinkSid;
    private int _State;
    private final Zeze.Transaction.Collections.PSet1<String> _ReliableNotifyMark;
    private final Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> _ReliableNotifyQueue; // full encoded protocol list
    private long _ReliableNotifyConfirmCount;
    private long _ReliableNotifyTotalCount;
    private int _ProviderId; // Config.AutoKeyLocalId
    private long _ProviderSessionId; // 登录所在Linkd与当前Provider的连接在Linkd方的SessionId
    private long _LoginVersion;

    public String getLinkName() {
        if (!isManaged())
            return _LinkName;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _LinkName;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__LinkName)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _LinkName;
    }

    public void setLinkName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _LinkName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__LinkName(this, value));
    }

    public long getLinkSid() {
        if (!isManaged())
            return _LinkSid;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _LinkSid;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__LinkSid)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _LinkSid;
    }

    public void setLinkSid(long value) {
        if (!isManaged()) {
            _LinkSid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__LinkSid(this, value));
    }

    public int getState() {
        if (!isManaged())
            return _State;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _State;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__State)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _State;
    }

    public void setState(int value) {
        if (!isManaged()) {
            _State = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__State(this, value));
    }

    public Zeze.Transaction.Collections.PSet1<String> getReliableNotifyMark() {
        return _ReliableNotifyMark;
    }

    public Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> getReliableNotifyQueue() {
        return _ReliableNotifyQueue;
    }

    public long getReliableNotifyConfirmCount() {
        if (!isManaged())
            return _ReliableNotifyConfirmCount;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ReliableNotifyConfirmCount;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ReliableNotifyConfirmCount)txn.GetLog(this.getObjectId() + 6);
        return log != null ? log.getValue() : _ReliableNotifyConfirmCount;
    }

    public void setReliableNotifyConfirmCount(long value) {
        if (!isManaged()) {
            _ReliableNotifyConfirmCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ReliableNotifyConfirmCount(this, value));
    }

    public long getReliableNotifyTotalCount() {
        if (!isManaged())
            return _ReliableNotifyTotalCount;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ReliableNotifyTotalCount;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ReliableNotifyTotalCount)txn.GetLog(this.getObjectId() + 7);
        return log != null ? log.getValue() : _ReliableNotifyTotalCount;
    }

    public void setReliableNotifyTotalCount(long value) {
        if (!isManaged()) {
            _ReliableNotifyTotalCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ReliableNotifyTotalCount(this, value));
    }

    public int getProviderId() {
        if (!isManaged())
            return _ProviderId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ProviderId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ProviderId)txn.GetLog(this.getObjectId() + 8);
        return log != null ? log.getValue() : _ProviderId;
    }

    public void setProviderId(int value) {
        if (!isManaged()) {
            _ProviderId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ProviderId(this, value));
    }

    public long getProviderSessionId() {
        if (!isManaged())
            return _ProviderSessionId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ProviderSessionId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ProviderSessionId)txn.GetLog(this.getObjectId() + 9);
        return log != null ? log.getValue() : _ProviderSessionId;
    }

    public void setProviderSessionId(long value) {
        if (!isManaged()) {
            _ProviderSessionId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ProviderSessionId(this, value));
    }

    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _LoginVersion;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__LoginVersion)txn.GetLog(this.getObjectId() + 10);
        return log != null ? log.getValue() : _LoginVersion;
    }

    public void setLoginVersion(long value) {
        if (!isManaged()) {
            _LoginVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__LoginVersion(this, value));
    }

    public BOnline() {
         this(0);
    }

    public BOnline(int _varId_) {
        super(_varId_);
        _LinkName = "";
        _State = StateOffline;
        _ReliableNotifyMark = new Zeze.Transaction.Collections.PSet1<>(getObjectId() + 4, (_v) -> new Log__ReliableNotifyMark(this, _v));
        _ReliableNotifyQueue = new Zeze.Transaction.Collections.PList1<>(getObjectId() + 5, (_v) -> new Log__ReliableNotifyQueue(this, _v));
    }

    public void Assign(BOnline other) {
        setLinkName(other.getLinkName());
        setLinkSid(other.getLinkSid());
        setState(other.getState());
        getReliableNotifyMark().clear();
        for (var e : other.getReliableNotifyMark())
            getReliableNotifyMark().add(e);
        getReliableNotifyQueue().clear();
        for (var e : other.getReliableNotifyQueue())
            getReliableNotifyQueue().add(e);
        setReliableNotifyConfirmCount(other.getReliableNotifyConfirmCount());
        setReliableNotifyTotalCount(other.getReliableNotifyTotalCount());
        setProviderId(other.getProviderId());
        setProviderSessionId(other.getProviderSessionId());
        setLoginVersion(other.getLoginVersion());
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

    public static final long TYPEID = -7786403987996508020L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__LinkName extends Zeze.Transaction.Log1<BOnline, String> {
        public Log__LinkName(BOnline self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._LinkName = this.getValue(); }
    }

    private static final class Log__LinkSid extends Zeze.Transaction.Log1<BOnline, Long> {
        public Log__LinkSid(BOnline self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._LinkSid = this.getValue(); }
    }

    private static final class Log__State extends Zeze.Transaction.Log1<BOnline, Integer> {
        public Log__State(BOnline self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._State = this.getValue(); }
    }

    private static final class Log__ReliableNotifyMark extends Zeze.Transaction.Collections.PSet.LogV<String> {
        public Log__ReliableNotifyMark(BOnline host, org.pcollections.PSet<String> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 4; }
        public BOnline getBeanTyped() { return (BOnline)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._ReliableNotifyMark); }
    }

    private static final class Log__ReliableNotifyQueue extends Zeze.Transaction.Collections.PList.LogV<Zeze.Net.Binary> {
        public Log__ReliableNotifyQueue(BOnline host, org.pcollections.PVector<Zeze.Net.Binary> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 5; }
        public BOnline getBeanTyped() { return (BOnline)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._ReliableNotifyQueue); }
    }

    private static final class Log__ReliableNotifyConfirmCount extends Zeze.Transaction.Log1<BOnline, Long> {
        public Log__ReliableNotifyConfirmCount(BOnline self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 6; }
        @Override
        public void Commit() { this.getBeanTyped()._ReliableNotifyConfirmCount = this.getValue(); }
    }

    private static final class Log__ReliableNotifyTotalCount extends Zeze.Transaction.Log1<BOnline, Long> {
        public Log__ReliableNotifyTotalCount(BOnline self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 7; }
        @Override
        public void Commit() { this.getBeanTyped()._ReliableNotifyTotalCount = this.getValue(); }
    }

    private static final class Log__ProviderId extends Zeze.Transaction.Log1<BOnline, Integer> {
        public Log__ProviderId(BOnline self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 8; }
        @Override
        public void Commit() { this.getBeanTyped()._ProviderId = this.getValue(); }
    }

    private static final class Log__ProviderSessionId extends Zeze.Transaction.Log1<BOnline, Long> {
        public Log__ProviderSessionId(BOnline self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 9; }
        @Override
        public void Commit() { this.getBeanTyped()._ProviderSessionId = this.getValue(); }
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Log1<BOnline, Long> {
        public Log__LoginVersion(BOnline self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 10; }
        @Override
        public void Commit() { this.getBeanTyped()._LoginVersion = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Online.BOnline: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LinkName").append('=').append(getLinkName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LinkSid").append('=').append(getLinkSid()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("State").append('=').append(getState()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyMark").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getReliableNotifyMark()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(_item_).append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyQueue").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getReliableNotifyQueue()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(_item_).append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyConfirmCount").append('=').append(getReliableNotifyConfirmCount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyTotalCount").append('=').append(getReliableNotifyTotalCount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProviderId").append('=').append(getProviderId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProviderSessionId").append('=').append(getProviderSessionId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LoginVersion").append('=').append(getLoginVersion()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @Override
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            String _x_ = getLinkName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getLinkSid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getState();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getReliableNotifyMark();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_)
                    _o_.WriteString(_v_);
            }
        }
        {
            var _x_ = getReliableNotifyQueue();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_)
                    _o_.WriteBinary(_v_);
            }
        }
        {
            long _x_ = getReliableNotifyConfirmCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getReliableNotifyTotalCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getProviderId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getProviderSessionId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 10, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setLinkName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLinkSid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setState(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = getReliableNotifyMark();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            var _x_ = getReliableNotifyQueue();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBinary(_t_));
            } else
                _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setReliableNotifyConfirmCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setReliableNotifyTotalCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            setProviderId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            setProviderSessionId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 10) {
            setLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _ReliableNotifyMark.InitRootInfo(root, this);
        _ReliableNotifyQueue.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        if (getLinkSid() < 0)
            return true;
        if (getState() < 0)
            return true;
        if (getReliableNotifyConfirmCount() < 0)
            return true;
        if (getReliableNotifyTotalCount() < 0)
            return true;
        if (getProviderId() < 0)
            return true;
        if (getProviderSessionId() < 0)
            return true;
        if (getLoginVersion() < 0)
            return true;
        return false;
    }
}
