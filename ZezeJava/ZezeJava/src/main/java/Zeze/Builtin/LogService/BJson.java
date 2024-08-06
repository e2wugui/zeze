// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BJson extends Zeze.Transaction.Bean implements BJsonReadOnly {
    public static final long TYPEID = 2743837942654367657L;

    private String _Json;

    @Override
    public String getJson() {
        if (!isManaged())
            return _Json;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Json;
        var log = (Log__Json)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Json;
    }

    public void setJson(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Json = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Json(this, 1, _v_));
    }

    @SuppressWarnings("deprecation")
    public BJson() {
        _Json = "";
    }

    @SuppressWarnings("deprecation")
    public BJson(String _Json_) {
        if (_Json_ == null)
            _Json_ = "";
        _Json = _Json_;
    }

    @Override
    public void reset() {
        setJson("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LogService.BJson.Data toData() {
        var _d_ = new Zeze.Builtin.LogService.BJson.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.LogService.BJson.Data)_o_);
    }

    public void assign(BJson.Data _o_) {
        setJson(_o_._Json);
        _unknown_ = null;
    }

    public void assign(BJson _o_) {
        setJson(_o_.getJson());
        _unknown_ = _o_._unknown_;
    }

    public BJson copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BJson copy() {
        var _c_ = new BJson();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BJson _a_, BJson _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Json extends Zeze.Transaction.Logs.LogString {
        public Log__Json(BJson _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BJson)getBelong())._Json = value; }
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
        _s_.append("Zeze.Builtin.LogService.BJson: {\n");
        _s_.append(_i1_).append("Json=").append(getJson()).append('\n');
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
            String _x_ = getJson();
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
            setJson(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BJson))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BJson)_o_;
        if (!getJson().equals(_b_.getJson()))
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
                case 1: _Json = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setJson(_r_.getString(_pn_ + "Json"));
        if (getJson() == null)
            setJson("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Json", getJson());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Json", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 2743837942654367657L;

    private String _Json;

    public String getJson() {
        return _Json;
    }

    public void setJson(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Json = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Json = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _Json_) {
        if (_Json_ == null)
            _Json_ = "";
        _Json = _Json_;
    }

    @Override
    public void reset() {
        _Json = "";
    }

    @Override
    public Zeze.Builtin.LogService.BJson toBean() {
        var _b_ = new Zeze.Builtin.LogService.BJson();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BJson)_o_);
    }

    public void assign(BJson _o_) {
        _Json = _o_.getJson();
    }

    public void assign(BJson.Data _o_) {
        _Json = _o_._Json;
    }

    @Override
    public BJson.Data copy() {
        var _c_ = new BJson.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BJson.Data _a_, BJson.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BJson.Data clone() {
        return (BJson.Data)super.clone();
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
        _s_.append("Zeze.Builtin.LogService.BJson: {\n");
        _s_.append(_i1_).append("Json=").append(_Json).append('\n');
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
            String _x_ = _Json;
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
            _Json = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
