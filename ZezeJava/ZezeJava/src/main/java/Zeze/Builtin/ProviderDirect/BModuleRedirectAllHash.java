// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BModuleRedirectAllHash extends Zeze.Transaction.Bean {
    public static final long TYPEID = 5611412794338295457L;

    private long _ReturnCode;
    private Zeze.Net.Binary _Params;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

    public long getReturnCode() {
        if (!isManaged())
            return _ReturnCode;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ReturnCode;
        var log = (Log__ReturnCode)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ReturnCode;
    }

    public void setReturnCode(long value) {
        if (!isManaged()) {
            _ReturnCode = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ReturnCode(this, 1, value));
    }

    public Zeze.Net.Binary getParams() {
        if (!isManaged())
            return _Params;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Params;
        var log = (Log__Params)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Params;
    }

    public void setParams(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Params = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Params(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectAllHash() {
        _Params = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectAllHash(long _ReturnCode_, Zeze.Net.Binary _Params_) {
        _ReturnCode = _ReturnCode_;
        if (_Params_ == null)
            throw new IllegalArgumentException();
        _Params = _Params_;
    }

    public void assign(BModuleRedirectAllHash other) {
        setReturnCode(other.getReturnCode());
        setParams(other.getParams());
    }

    @Deprecated
    public void Assign(BModuleRedirectAllHash other) {
        assign(other);
    }

    public BModuleRedirectAllHash copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BModuleRedirectAllHash copy() {
        var copy = new BModuleRedirectAllHash();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BModuleRedirectAllHash Copy() {
        return copy();
    }

    public static void swap(BModuleRedirectAllHash a, BModuleRedirectAllHash b) {
        BModuleRedirectAllHash save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ReturnCode extends Zeze.Transaction.Logs.LogLong {
        public Log__ReturnCode(BModuleRedirectAllHash bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllHash)getBelong())._ReturnCode = value; }
    }

    private static final class Log__Params extends Zeze.Transaction.Logs.LogBinary {
        public Log__Params(BModuleRedirectAllHash bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModuleRedirectAllHash)getBelong())._Params = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ReturnCode").append('=').append(getReturnCode()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Params").append('=').append(getParams()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            long _x_ = getReturnCode();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getParams();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setReturnCode(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setParams(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void resetChildrenRootInfo() {
    }

    @Override
    public boolean negativeCheck() {
        if (getReturnCode() < 0)
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _ReturnCode = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _Params = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }
}
