// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BRegister extends Zeze.Transaction.Bean implements BRegisterReadOnly {
    public static final long TYPEID = -6705392585502947760L;

    private String _ZokerName;

    @Override
    public String getZokerName() {
        if (!isManaged())
            return _ZokerName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ZokerName;
        var log = (Log__ZokerName)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ZokerName;
    }

    public void setZokerName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ZokerName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ZokerName(this, 1, _v_));
    }

    @SuppressWarnings("deprecation")
    public BRegister() {
        _ZokerName = "";
    }

    @SuppressWarnings("deprecation")
    public BRegister(String _ZokerName_) {
        if (_ZokerName_ == null)
            _ZokerName_ = "";
        _ZokerName = _ZokerName_;
    }

    @Override
    public void reset() {
        setZokerName("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Zoker.BRegister.Data toData() {
        var _d_ = new Zeze.Builtin.Zoker.BRegister.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Zoker.BRegister.Data)_o_);
    }

    public void assign(BRegister.Data _o_) {
        setZokerName(_o_._ZokerName);
        _unknown_ = null;
    }

    public void assign(BRegister _o_) {
        setZokerName(_o_.getZokerName());
        _unknown_ = _o_._unknown_;
    }

    public BRegister copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRegister copy() {
        var _c_ = new BRegister();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BRegister _a_, BRegister _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ZokerName extends Zeze.Transaction.Logs.LogString {
        public Log__ZokerName(BRegister _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BRegister)getBelong())._ZokerName = value; }
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
        _s_.append("Zeze.Builtin.Zoker.BRegister: {\n");
        _s_.append(_i1_).append("ZokerName=").append(getZokerName()).append('\n');
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
            String _x_ = getZokerName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
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
            setZokerName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BRegister))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BRegister)_o_;
        if (!getZokerName().equals(_b_.getZokerName()))
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
                case 1: _ZokerName = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setZokerName(_r_.getString(_pn_ + "ZokerName"));
        if (getZokerName() == null)
            setZokerName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "ZokerName", getZokerName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ZokerName", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -6705392585502947760L;

    private String _ZokerName;

    public String getZokerName() {
        return _ZokerName;
    }

    public void setZokerName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ZokerName = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ZokerName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _ZokerName_) {
        if (_ZokerName_ == null)
            _ZokerName_ = "";
        _ZokerName = _ZokerName_;
    }

    @Override
    public void reset() {
        _ZokerName = "";
    }

    @Override
    public Zeze.Builtin.Zoker.BRegister toBean() {
        var _b_ = new Zeze.Builtin.Zoker.BRegister();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BRegister)_o_);
    }

    public void assign(BRegister _o_) {
        _ZokerName = _o_.getZokerName();
    }

    public void assign(BRegister.Data _o_) {
        _ZokerName = _o_._ZokerName;
    }

    @Override
    public BRegister.Data copy() {
        var _c_ = new BRegister.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BRegister.Data _a_, BRegister.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BRegister.Data clone() {
        return (BRegister.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Zoker.BRegister: {\n");
        _s_.append(_i1_).append("ZokerName=").append(_ZokerName).append('\n');
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
            String _x_ = _ZokerName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _ZokerName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
