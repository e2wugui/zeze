// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
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
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    private static final java.lang.invoke.VarHandle vh_ReturnCode;
    private static final java.lang.invoke.VarHandle vh_Params;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ReturnCode = _l_.findVarHandle(BModuleRedirectAllHash.class, "_ReturnCode", long.class);
            vh_Params = _l_.findVarHandle(BModuleRedirectAllHash.class, "_Params", Zeze.Net.Binary.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getReturnCode() {
        if (!isManaged())
            return _ReturnCode;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ReturnCode;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ReturnCode;
    }

    public void setReturnCode(long _v_) {
        if (!isManaged()) {
            _ReturnCode = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_ReturnCode, _v_));
    }

    @Override
    public Zeze.Net.Binary getParams() {
        if (!isManaged())
            return _Params;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Params;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Params;
    }

    public void setParams(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Params = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 2, vh_Params, _v_));
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
        var _d_ = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash.Data)_o_);
    }

    public void assign(BModuleRedirectAllHash.Data _o_) {
        setReturnCode(_o_._ReturnCode);
        setParams(_o_._Params);
        _unknown_ = null;
    }

    public void assign(BModuleRedirectAllHash _o_) {
        setReturnCode(_o_.getReturnCode());
        setParams(_o_.getParams());
        _unknown_ = _o_._unknown_;
    }

    public BModuleRedirectAllHash copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BModuleRedirectAllHash copy() {
        var _c_ = new BModuleRedirectAllHash();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BModuleRedirectAllHash _a_, BModuleRedirectAllHash _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash: {\n");
        _s_.append(_i1_).append("ReturnCode=").append(getReturnCode()).append(",\n");
        _s_.append(_i1_).append("Params=").append(getParams()).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
    public void decode(IByteBuffer _o_) {
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
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BModuleRedirectAllHash))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BModuleRedirectAllHash)_o_;
        if (getReturnCode() != _b_.getReturnCode())
            return false;
        if (!getParams().equals(_b_.getParams()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getReturnCode() < 0)
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _ReturnCode = _v_.longValue(); break;
                case 2: _Params = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setReturnCode(_r_.getLong(_pn_ + "ReturnCode"));
        setParams(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Params")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "ReturnCode", getReturnCode());
        _s_.appendBinary(_pn_ + "Params", getParams());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ReturnCode", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Params", "binary", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5611412794338295457L;

    private long _ReturnCode;
    private Zeze.Net.Binary _Params;

    public long getReturnCode() {
        return _ReturnCode;
    }

    public void setReturnCode(long _v_) {
        _ReturnCode = _v_;
    }

    public Zeze.Net.Binary getParams() {
        return _Params;
    }

    public void setParams(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Params = _v_;
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
        var _b_ = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BModuleRedirectAllHash)_o_);
    }

    public void assign(BModuleRedirectAllHash _o_) {
        _ReturnCode = _o_.getReturnCode();
        _Params = _o_.getParams();
    }

    public void assign(BModuleRedirectAllHash.Data _o_) {
        _ReturnCode = _o_._ReturnCode;
        _Params = _o_._Params;
    }

    @Override
    public BModuleRedirectAllHash.Data copy() {
        var _c_ = new BModuleRedirectAllHash.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BModuleRedirectAllHash.Data _a_, BModuleRedirectAllHash.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
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
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash: {\n");
        _s_.append(_i1_).append("ReturnCode=").append(_ReturnCode).append(",\n");
        _s_.append(_i1_).append("Params=").append(_Params).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
    public void decode(IByteBuffer _o_) {
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

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BModuleRedirectAllHash.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BModuleRedirectAllHash.Data)_o_;
        if (_ReturnCode != _b_._ReturnCode)
            return false;
        if (!_Params.equals(_b_._Params))
            return false;
        return true;
    }
}
}
