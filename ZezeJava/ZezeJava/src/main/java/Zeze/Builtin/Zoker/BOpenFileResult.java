// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BOpenFileResult extends Zeze.Transaction.Bean implements BOpenFileResultReadOnly {
    public static final long TYPEID = -1853465675021018096L;

    private long _Offset;

    @Override
    public long getOffset() {
        if (!isManaged())
            return _Offset;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Offset;
        var log = (Log__Offset)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Offset;
    }

    public void setOffset(long _v_) {
        if (!isManaged()) {
            _Offset = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Offset(this, 1, _v_));
    }

    @SuppressWarnings("deprecation")
    public BOpenFileResult() {
    }

    @SuppressWarnings("deprecation")
    public BOpenFileResult(long _Offset_) {
        _Offset = _Offset_;
    }

    @Override
    public void reset() {
        setOffset(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Zoker.BOpenFileResult.Data toData() {
        var _d_ = new Zeze.Builtin.Zoker.BOpenFileResult.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Zoker.BOpenFileResult.Data)_o_);
    }

    public void assign(BOpenFileResult.Data _o_) {
        setOffset(_o_._Offset);
        _unknown_ = null;
    }

    public void assign(BOpenFileResult _o_) {
        setOffset(_o_.getOffset());
        _unknown_ = _o_._unknown_;
    }

    public BOpenFileResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOpenFileResult copy() {
        var _c_ = new BOpenFileResult();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BOpenFileResult _a_, BOpenFileResult _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Offset extends Zeze.Transaction.Logs.LogLong {
        public Log__Offset(BOpenFileResult _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BOpenFileResult)getBelong())._Offset = value; }
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
        _s_.append("Zeze.Builtin.Zoker.BOpenFileResult: {\n");
        _s_.append(_i1_).append("Offset=").append(getOffset()).append('\n');
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
            long _x_ = getOffset();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            setOffset(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BOpenFileResult))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BOpenFileResult)_o_;
        if (getOffset() != _b_.getOffset())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getOffset() < 0)
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
                case 1: _Offset = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setOffset(_r_.getLong(_pn_ + "Offset"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "Offset", getOffset());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Offset", "long", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -1853465675021018096L;

    private long _Offset;

    public long getOffset() {
        return _Offset;
    }

    public void setOffset(long _v_) {
        _Offset = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(long _Offset_) {
        _Offset = _Offset_;
    }

    @Override
    public void reset() {
        _Offset = 0;
    }

    @Override
    public Zeze.Builtin.Zoker.BOpenFileResult toBean() {
        var _b_ = new Zeze.Builtin.Zoker.BOpenFileResult();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BOpenFileResult)_o_);
    }

    public void assign(BOpenFileResult _o_) {
        _Offset = _o_.getOffset();
    }

    public void assign(BOpenFileResult.Data _o_) {
        _Offset = _o_._Offset;
    }

    @Override
    public BOpenFileResult.Data copy() {
        var _c_ = new BOpenFileResult.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BOpenFileResult.Data _a_, BOpenFileResult.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BOpenFileResult.Data clone() {
        return (BOpenFileResult.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Zoker.BOpenFileResult: {\n");
        _s_.append(_i1_).append("Offset=").append(_Offset).append('\n');
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
            long _x_ = _Offset;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Offset = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
