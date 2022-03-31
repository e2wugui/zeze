// auto-generated @formatter:off
package Zeze.Beans.Provider;

import Zeze.Serialize.ByteBuffer;

public final class BModuleRedirectResult extends Zeze.Transaction.Bean {
    private int _ModuleId;
    private int _ServerId; // 目标server的id。
    private long _ReturnCode; // 实现函数的返回。
    private Zeze.Net.Binary _Params;
    private final Zeze.Transaction.Collections.PList2<Zeze.Beans.Provider.BActionParam> _Actions; // 按回调顺序。！不是定义顺序！

    public int getModuleId() {
        if (!isManaged())
            return _ModuleId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ModuleId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ModuleId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _ModuleId;
    }

    public void setModuleId(int value) {
        if (!isManaged()) {
            _ModuleId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ModuleId(this, value));
    }

    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ServerId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ServerId)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _ServerId;
    }

    public void setServerId(int value) {
        if (!isManaged()) {
            _ServerId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ServerId(this, value));
    }

    public long getReturnCode() {
        if (!isManaged())
            return _ReturnCode;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ReturnCode;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ReturnCode)txn.GetLog(this.getObjectId() + 4);
        return log != null ? log.getValue() : _ReturnCode;
    }

    public void setReturnCode(long value) {
        if (!isManaged()) {
            _ReturnCode = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ReturnCode(this, value));
    }

    public Zeze.Net.Binary getParams() {
        if (!isManaged())
            return _Params;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Params;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Params)txn.GetLog(this.getObjectId() + 5);
        return log != null ? log.getValue() : _Params;
    }

    public void setParams(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Params = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Params(this, value));
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Beans.Provider.BActionParam> getActions() {
        return _Actions;
    }

    public BModuleRedirectResult() {
         this(0);
    }

    public BModuleRedirectResult(int _varId_) {
        super(_varId_);
        _Params = Zeze.Net.Binary.Empty;
        _Actions = new Zeze.Transaction.Collections.PList2<>(getObjectId() + 6, (_v) -> new Log__Actions(this, _v));
    }

    public void Assign(BModuleRedirectResult other) {
        setModuleId(other.getModuleId());
        setServerId(other.getServerId());
        setReturnCode(other.getReturnCode());
        setParams(other.getParams());
        getActions().clear();
        for (var e : other.getActions())
            getActions().add(e.Copy());
    }

    public BModuleRedirectResult CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BModuleRedirectResult Copy() {
        var copy = new BModuleRedirectResult();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BModuleRedirectResult a, BModuleRedirectResult b) {
        BModuleRedirectResult save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -4567094030324022132L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__ModuleId extends Zeze.Transaction.Log1<BModuleRedirectResult, Integer> {
        public Log__ModuleId(BModuleRedirectResult self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._ModuleId = this.getValue(); }
    }

    private static final class Log__ServerId extends Zeze.Transaction.Log1<BModuleRedirectResult, Integer> {
        public Log__ServerId(BModuleRedirectResult self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._ServerId = this.getValue(); }
    }

    private static final class Log__ReturnCode extends Zeze.Transaction.Log1<BModuleRedirectResult, Long> {
        public Log__ReturnCode(BModuleRedirectResult self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 4; }
        @Override
        public void Commit() { this.getBeanTyped()._ReturnCode = this.getValue(); }
    }

    private static final class Log__Params extends Zeze.Transaction.Log1<BModuleRedirectResult, Zeze.Net.Binary> {
        public Log__Params(BModuleRedirectResult self, Zeze.Net.Binary value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 5; }
        @Override
        public void Commit() { this.getBeanTyped()._Params = this.getValue(); }
    }

    private static final class Log__Actions extends Zeze.Transaction.Collections.PList.LogV<Zeze.Beans.Provider.BActionParam> {
        public Log__Actions(BModuleRedirectResult host, org.pcollections.PVector<Zeze.Beans.Provider.BActionParam> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 6; }
        public BModuleRedirectResult getBeanTyped() { return (BModuleRedirectResult)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._Actions); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Beans.Provider.BModuleRedirectResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ModuleId").append('=').append(getModuleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId").append('=').append(getServerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReturnCode").append('=').append(getReturnCode()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Params").append('=').append(getParams()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Actions").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getActions()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(System.lineSeparator());
            _item_.BuildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(System.lineSeparator());
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

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            int _x_ = getModuleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getReturnCode();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getParams();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = getActions();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_)
                    _v_.Encode(_o_);
            }
        }
        _o_.WriteByte(0);
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setModuleId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0 && _i_ < 4) {
            _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setReturnCode(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setParams(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            var _x_ = getActions();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Beans.Provider.BActionParam(), _t_));
            } else
                _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Actions.InitRootInfo(root, this);
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean NegativeCheck() {
        if (getModuleId() < 0)
            return true;
        if (getServerId() < 0)
            return true;
        if (getReturnCode() < 0)
            return true;
        return false;
    }
}
