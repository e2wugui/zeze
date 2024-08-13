// auto-generated @formatter:off
package Zeze.Builtin.Onz;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BFuncProcedureResult extends Zeze.Transaction.Bean implements BFuncProcedureResultReadOnly {
    public static final long TYPEID = -8838408566661515511L;

    private Zeze.Net.Binary _FuncResult;

    private static final java.lang.invoke.VarHandle vh_FuncResult;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_FuncResult = _l_.findVarHandle(BFuncProcedureResult.class, "_FuncResult", Zeze.Net.Binary.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public Zeze.Net.Binary getFuncResult() {
        if (!isManaged())
            return _FuncResult;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _FuncResult;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _FuncResult;
    }

    public void setFuncResult(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _FuncResult = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 1, vh_FuncResult, _v_));
    }

    @SuppressWarnings("deprecation")
    public BFuncProcedureResult() {
        _FuncResult = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BFuncProcedureResult(Zeze.Net.Binary _FuncResult_) {
        if (_FuncResult_ == null)
            _FuncResult_ = Zeze.Net.Binary.Empty;
        _FuncResult = _FuncResult_;
    }

    @Override
    public void reset() {
        setFuncResult(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Onz.BFuncProcedureResult.Data toData() {
        var _d_ = new Zeze.Builtin.Onz.BFuncProcedureResult.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Onz.BFuncProcedureResult.Data)_o_);
    }

    public void assign(BFuncProcedureResult.Data _o_) {
        setFuncResult(_o_._FuncResult);
        _unknown_ = null;
    }

    public void assign(BFuncProcedureResult _o_) {
        setFuncResult(_o_.getFuncResult());
        _unknown_ = _o_._unknown_;
    }

    public BFuncProcedureResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BFuncProcedureResult copy() {
        var _c_ = new BFuncProcedureResult();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BFuncProcedureResult _a_, BFuncProcedureResult _b_) {
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
        _s_.append("Zeze.Builtin.Onz.BFuncProcedureResult: {\n");
        _s_.append(_i1_).append("FuncResult=").append(getFuncResult()).append('\n');
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
            var _x_ = getFuncResult();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
            setFuncResult(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BFuncProcedureResult))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BFuncProcedureResult)_o_;
        if (!getFuncResult().equals(_b_.getFuncResult()))
            return false;
        return true;
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
                case 1: _FuncResult = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setFuncResult(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "FuncResult")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendBinary(_pn_ + "FuncResult", getFuncResult());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "FuncResult", "binary", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -8838408566661515511L;

    private Zeze.Net.Binary _FuncResult;

    public Zeze.Net.Binary getFuncResult() {
        return _FuncResult;
    }

    public void setFuncResult(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _FuncResult = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _FuncResult = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _FuncResult_) {
        if (_FuncResult_ == null)
            _FuncResult_ = Zeze.Net.Binary.Empty;
        _FuncResult = _FuncResult_;
    }

    @Override
    public void reset() {
        _FuncResult = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.Onz.BFuncProcedureResult toBean() {
        var _b_ = new Zeze.Builtin.Onz.BFuncProcedureResult();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BFuncProcedureResult)_o_);
    }

    public void assign(BFuncProcedureResult _o_) {
        _FuncResult = _o_.getFuncResult();
    }

    public void assign(BFuncProcedureResult.Data _o_) {
        _FuncResult = _o_._FuncResult;
    }

    @Override
    public BFuncProcedureResult.Data copy() {
        var _c_ = new BFuncProcedureResult.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BFuncProcedureResult.Data _a_, BFuncProcedureResult.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BFuncProcedureResult.Data clone() {
        return (BFuncProcedureResult.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Onz.BFuncProcedureResult: {\n");
        _s_.append(_i1_).append("FuncResult=").append(_FuncResult).append('\n');
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
            var _x_ = _FuncResult;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
            _FuncResult = _o_.ReadBinary(_t_);
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
        if (!(_o_ instanceof BFuncProcedureResult.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BFuncProcedureResult.Data)_o_;
        if (!_FuncResult.equals(_b_._FuncResult))
            return false;
        return true;
    }
}
}
