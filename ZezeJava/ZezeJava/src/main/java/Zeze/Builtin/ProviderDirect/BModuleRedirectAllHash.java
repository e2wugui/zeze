// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BModuleRedirectAllHash extends Zeze.Transaction.Bean {
    private long _ReturnCode;
    private Zeze.Net.Binary _Params;

    private Object __zeze_map_key__;

    @Override
    public Object getMapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void setMapKey(Object value) {
        __zeze_map_key__ = value;
    }

    public long getReturnCode() {
        if (!isManaged())
            return _ReturnCode;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ReturnCode;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ReturnCode)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.Value : _ReturnCode;
    }

    public void setReturnCode(long value) {
        if (!isManaged()) {
            _ReturnCode = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ReturnCode(this, 1, value));
    }

    public Zeze.Net.Binary getParams() {
        if (!isManaged())
            return _Params;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Params;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Params)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.Value : _Params;
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
        txn.PutLog(new Log__Params(this, 2, value));
    }

    public BModuleRedirectAllHash() {
         this(0);
    }

    public BModuleRedirectAllHash(int _varId_) {
        super(_varId_);
        _Params = Zeze.Net.Binary.Empty;
    }

    public void Assign(BModuleRedirectAllHash other) {
        setReturnCode(other.getReturnCode());
        setParams(other.getParams());
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

    public static final long TYPEID = 5611412794338295457L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__ReturnCode extends Zeze.Transaction.Logs.LogLong {
        public Log__ReturnCode(BModuleRedirectAllHash bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BModuleRedirectAllHash)getBelong())._ReturnCode = Value; }
    }

    private static final class Log__Params extends Zeze.Transaction.Logs.LogBinary {
        public Log__Params(BModuleRedirectAllHash bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BModuleRedirectAllHash)getBelong())._Params = Value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ReturnCode").append('=').append(getReturnCode()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Params").append('=').append(getParams()).append(System.lineSeparator());
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
    public void Decode(ByteBuffer _o_) {
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
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    public boolean NegativeCheck() {
        if (getReturnCode() < 0)
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void FollowerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _ReturnCode = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 2: _Params = ((Zeze.Transaction.Logs.LogBinary)vlog).Value; break;
            }
        }
    }
}
