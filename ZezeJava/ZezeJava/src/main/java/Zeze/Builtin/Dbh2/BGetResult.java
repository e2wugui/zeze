// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BGetResult extends Zeze.Transaction.Bean implements BGetResultReadOnly {
    public static final long TYPEID = -3248537090181056461L;

    private boolean _Null;
    private Zeze.Net.Binary _Value;

    @Override
    public boolean isNull() {
        if (!isManaged())
            return _Null;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Null;
        var log = (Log__Null)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Null;
    }

    public void setNull(boolean _v_) {
        if (!isManaged()) {
            _Null = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Null(this, 1, _v_));
    }

    @Override
    public Zeze.Net.Binary getValue() {
        if (!isManaged())
            return _Value;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Value;
        var log = (Log__Value)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Value;
    }

    public void setValue(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Value = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Value(this, 2, _v_));
    }

    @SuppressWarnings("deprecation")
    public BGetResult() {
        _Value = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BGetResult(boolean _Null_, Zeze.Net.Binary _Value_) {
        _Null = _Null_;
        if (_Value_ == null)
            _Value_ = Zeze.Net.Binary.Empty;
        _Value = _Value_;
    }

    @Override
    public void reset() {
        setNull(false);
        setValue(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.BGetResult.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.BGetResult.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.BGetResult.Data)_o_);
    }

    public void assign(BGetResult.Data _o_) {
        setNull(_o_._Null);
        setValue(_o_._Value);
        _unknown_ = null;
    }

    public void assign(BGetResult _o_) {
        setNull(_o_.isNull());
        setValue(_o_.getValue());
        _unknown_ = _o_._unknown_;
    }

    public BGetResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BGetResult copy() {
        var _c_ = new BGetResult();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BGetResult _a_, BGetResult _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Null extends Zeze.Transaction.Logs.LogBool {
        public Log__Null(BGetResult _b_, int _i_, boolean _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BGetResult)getBelong())._Null = value; }
    }

    private static final class Log__Value extends Zeze.Transaction.Logs.LogBinary {
        public Log__Value(BGetResult _b_, int _i_, Zeze.Net.Binary _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BGetResult)getBelong())._Value = value; }
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
        _s_.append("Zeze.Builtin.Dbh2.BGetResult: {\n");
        _s_.append(_i1_).append("Null=").append(isNull()).append(",\n");
        _s_.append(_i1_).append("Value=").append(getValue()).append('\n');
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
            boolean _x_ = isNull();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            var _x_ = getValue();
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
            setNull(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setValue(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BGetResult))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BGetResult)_o_;
        if (isNull() != _b_.isNull())
            return false;
        if (!getValue().equals(_b_.getValue()))
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
                case 1: _Null = _v_.booleanValue(); break;
                case 2: _Value = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setNull(_r_.getBoolean(_pn_ + "Null"));
        setValue(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Value")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendBoolean(_pn_ + "Null", isNull());
        _s_.appendBinary(_pn_ + "Value", getValue());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Null", "bool", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Value", "binary", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -3248537090181056461L;

    private boolean _Null;
    private Zeze.Net.Binary _Value;

    public boolean isNull() {
        return _Null;
    }

    public void setNull(boolean _v_) {
        _Null = _v_;
    }

    public Zeze.Net.Binary getValue() {
        return _Value;
    }

    public void setValue(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Value = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Value = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(boolean _Null_, Zeze.Net.Binary _Value_) {
        _Null = _Null_;
        if (_Value_ == null)
            _Value_ = Zeze.Net.Binary.Empty;
        _Value = _Value_;
    }

    @Override
    public void reset() {
        _Null = false;
        _Value = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.Dbh2.BGetResult toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.BGetResult();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BGetResult)_o_);
    }

    public void assign(BGetResult _o_) {
        _Null = _o_.isNull();
        _Value = _o_.getValue();
    }

    public void assign(BGetResult.Data _o_) {
        _Null = _o_._Null;
        _Value = _o_._Value;
    }

    @Override
    public BGetResult.Data copy() {
        var _c_ = new BGetResult.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BGetResult.Data _a_, BGetResult.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BGetResult.Data clone() {
        return (BGetResult.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Dbh2.BGetResult: {\n");
        _s_.append(_i1_).append("Null=").append(_Null).append(",\n");
        _s_.append(_i1_).append("Value=").append(_Value).append('\n');
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
            boolean _x_ = _Null;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            var _x_ = _Value;
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
            _Null = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Value = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
