// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BModuleRedirectAllHash extends Zeze.Transaction.Bean implements BModuleRedirectAllHashReadOnly {
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

    @Override
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

    @Override
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
            _Params_ = Zeze.Net.Binary.Empty;
        _Params = _Params_;
    }

    @Override
    public void reset() {
        setReturnCode(0);
        setParams(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data toData() {
        var data = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data)other);
    }

    public void assign(BModuleRedirectAllHash.Data other) {
        setReturnCode(other._ReturnCode);
        setParams(other._Params);
        _unknown_ = null;
    }

    public void assign(BModuleRedirectAllHash other) {
        setReturnCode(other.getReturnCode());
        setParams(other.getParams());
        _unknown_ = other._unknown_;
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
        sb.append(Zeze.Util.Str.indent(level)).append("ReturnCode=").append(getReturnCode()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Params=").append(getParams()).append(System.lineSeparator());
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

    private byte[] _unknown_;

    public byte[] unknown() {
        return _unknown_;
    }

    public void clearUnknown() {
        _unknown_ = null;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        var _ua_ = _unknown_;
        var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
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
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
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
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
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

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setReturnCode(rs.getLong(_parents_name_ + "ReturnCode"));
        setParams(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Params")));
        if (getParams() == null)
            setParams(Zeze.Net.Binary.Empty);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "ReturnCode", getReturnCode());
        st.appendBinary(_parents_name_ + "Params", getParams());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ReturnCode", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Params", "binary", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BModuleRedirectAllHash
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5611412794338295457L;

    private long _ReturnCode;
    private Zeze.Net.Binary _Params;

    public long getReturnCode() {
        return _ReturnCode;
    }

    public void setReturnCode(long value) {
        _ReturnCode = value;
    }

    public Zeze.Net.Binary getParams() {
        return _Params;
    }

    public void setParams(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Params = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Params = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(long _ReturnCode_, Zeze.Net.Binary _Params_) {
        _ReturnCode = _ReturnCode_;
        if (_Params_ == null)
            _Params_ = Zeze.Net.Binary.Empty;
        _Params = _Params_;
    }

    @Override
    public void reset() {
        _ReturnCode = 0;
        _Params = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash toBean() {
        var bean = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BModuleRedirectAllHash)other);
    }

    public void assign(BModuleRedirectAllHash other) {
        _ReturnCode = other.getReturnCode();
        _Params = other.getParams();
    }

    public void assign(BModuleRedirectAllHash.Data other) {
        _ReturnCode = other._ReturnCode;
        _Params = other._Params;
    }

    @Override
    public BModuleRedirectAllHash.Data copy() {
        var copy = new BModuleRedirectAllHash.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BModuleRedirectAllHash.Data a, BModuleRedirectAllHash.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BModuleRedirectAllHash.Data clone() {
        return (BModuleRedirectAllHash.Data)super.clone();
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
        sb.append(Zeze.Util.Str.indent(level)).append("ReturnCode=").append(_ReturnCode).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Params=").append(_Params).append(System.lineSeparator());
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
            long _x_ = _ReturnCode;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Params;
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
            _ReturnCode = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Params = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
