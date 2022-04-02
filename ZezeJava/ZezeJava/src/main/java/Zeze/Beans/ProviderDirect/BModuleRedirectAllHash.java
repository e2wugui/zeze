// auto-generated @formatter:off
package Zeze.Beans.ProviderDirect;

import Zeze.Serialize.ByteBuffer;

public final class BModuleRedirectAllHash extends Zeze.Transaction.Bean {
    private long _ReturnCode; // 实现函数的返回。
    private Zeze.Net.Binary _Params; // 目前不支持out|ref，这个先保留。
    private final Zeze.Transaction.Collections.PList2<Zeze.Beans.ProviderDirect.BActionParam> _Actions; // 按回调顺序。！不是定义顺序！

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

    public Zeze.Transaction.Collections.PList2<Zeze.Beans.ProviderDirect.BActionParam> getActions() {
        return _Actions;
    }

    public BModuleRedirectAllHash() {
         this(0);
    }

    public BModuleRedirectAllHash(int _varId_) {
        super(_varId_);
        _Params = Zeze.Net.Binary.Empty;
        _Actions = new Zeze.Transaction.Collections.PList2<>(getObjectId() + 6, (_v) -> new Log__Actions(this, _v));
    }

    public void Assign(BModuleRedirectAllHash other) {
        setReturnCode(other.getReturnCode());
        setParams(other.getParams());
        getActions().clear();
        for (var e : other.getActions())
            getActions().add(e.Copy());
    }

    public BModuleRedirectAllHash CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BModuleRedirectAllHash Copy() {
        var copy = new BModuleRedirectAllHash();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BModuleRedirectAllHash a, BModuleRedirectAllHash b) {
        BModuleRedirectAllHash save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 154626821827676561L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__ReturnCode extends Zeze.Transaction.Log1<BModuleRedirectAllHash, Long> {
        public Log__ReturnCode(BModuleRedirectAllHash self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 4; }
        @Override
        public void Commit() { this.getBeanTyped()._ReturnCode = this.getValue(); }
    }

    private static final class Log__Params extends Zeze.Transaction.Log1<BModuleRedirectAllHash, Zeze.Net.Binary> {
        public Log__Params(BModuleRedirectAllHash self, Zeze.Net.Binary value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 5; }
        @Override
        public void Commit() { this.getBeanTyped()._Params = this.getValue(); }
    }

    private static final class Log__Actions extends Zeze.Transaction.Collections.PList.LogV<Zeze.Beans.ProviderDirect.BActionParam> {
        public Log__Actions(BModuleRedirectAllHash host, org.pcollections.PVector<Zeze.Beans.ProviderDirect.BActionParam> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 6; }
        public BModuleRedirectAllHash getBeanTyped() { return (BModuleRedirectAllHash)getBean(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Beans.ProviderDirect.BModuleRedirectAllHash: {").append(System.lineSeparator());
        level += 4;
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
                    _x_.add(_o_.ReadBean(new Zeze.Beans.ProviderDirect.BActionParam(), _t_));
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
        if (getReturnCode() < 0)
            return true;
        return false;
    }
}
