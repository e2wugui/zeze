// auto-generated @formatter:off
package Zeze.Builtin.LinkdBase;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// linkd to client
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BReportError extends Zeze.Transaction.Bean implements BReportErrorReadOnly {
    public static final long TYPEID = -947669033141460287L;

    public static final int FromLink = 0; // code字段见下面Code开头的枚举
    public static final int FromProvider = 1; // code字段见BKick里定义的Error开头的枚举
    public static final int FromDynamicModule = 2; // code字段是moduleId
    public static final int CodeMuteKick = 0; // 只断客户端连接，不发送消息给客户端，用于重连时确保旧的连接快速断开
    public static final int CodeNotAuthed = 1;
    public static final int CodeNoProvider = 2;
    public static final int CodeProviderBusy = 3;
    public static final int CodeProviderBroken = 4; // link跟provider断开,跟此provider静态绑定的客户端需要收到此协议执行重新登录流程

    private int _from; // FromLink, FromProvider, or FromDynamicModule
    private int _code;
    private String _desc;

    @Override
    public int getFrom() {
        if (!isManaged())
            return _from;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _from;
        var log = (Log__from)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _from;
    }

    public void setFrom(int _v_) {
        if (!isManaged()) {
            _from = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__from(this, 1, _v_));
    }

    @Override
    public int getCode() {
        if (!isManaged())
            return _code;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _code;
        var log = (Log__code)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _code;
    }

    public void setCode(int _v_) {
        if (!isManaged()) {
            _code = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__code(this, 2, _v_));
    }

    @Override
    public String getDesc() {
        if (!isManaged())
            return _desc;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _desc;
        var log = (Log__desc)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _desc;
    }

    public void setDesc(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _desc = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__desc(this, 3, _v_));
    }

    @SuppressWarnings("deprecation")
    public BReportError() {
        _desc = "";
    }

    @SuppressWarnings("deprecation")
    public BReportError(int _from_, int _code_, String _desc_) {
        _from = _from_;
        _code = _code_;
        if (_desc_ == null)
            _desc_ = "";
        _desc = _desc_;
    }

    @Override
    public void reset() {
        setFrom(0);
        setCode(0);
        setDesc("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LinkdBase.BReportError.Data toData() {
        var _d_ = new Zeze.Builtin.LinkdBase.BReportError.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.LinkdBase.BReportError.Data)_o_);
    }

    public void assign(BReportError.Data _o_) {
        setFrom(_o_._from);
        setCode(_o_._code);
        setDesc(_o_._desc);
        _unknown_ = null;
    }

    public void assign(BReportError _o_) {
        setFrom(_o_.getFrom());
        setCode(_o_.getCode());
        setDesc(_o_.getDesc());
        _unknown_ = _o_._unknown_;
    }

    public BReportError copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BReportError copy() {
        var _c_ = new BReportError();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BReportError _a_, BReportError _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__from extends Zeze.Transaction.Logs.LogInt {
        public Log__from(BReportError _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BReportError)getBelong())._from = value; }
    }

    private static final class Log__code extends Zeze.Transaction.Logs.LogInt {
        public Log__code(BReportError _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BReportError)getBelong())._code = value; }
    }

    private static final class Log__desc extends Zeze.Transaction.Logs.LogString {
        public Log__desc(BReportError _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BReportError)getBelong())._desc = value; }
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
        _s_.append("Zeze.Builtin.LinkdBase.BReportError: {\n");
        _s_.append(_i1_).append("from=").append(getFrom()).append(",\n");
        _s_.append(_i1_).append("code=").append(getCode()).append(",\n");
        _s_.append(_i1_).append("desc=").append(getDesc()).append('\n');
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
            int _x_ = getFrom();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getCode();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getDesc();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            setFrom(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setCode(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setDesc(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BReportError))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BReportError)_o_;
        if (getFrom() != _b_.getFrom())
            return false;
        if (getCode() != _b_.getCode())
            return false;
        if (!getDesc().equals(_b_.getDesc()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getFrom() < 0)
            return true;
        if (getCode() < 0)
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
                case 1: _from = _v_.intValue(); break;
                case 2: _code = _v_.intValue(); break;
                case 3: _desc = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setFrom(_r_.getInt(_pn_ + "from"));
        setCode(_r_.getInt(_pn_ + "code"));
        setDesc(_r_.getString(_pn_ + "desc"));
        if (getDesc() == null)
            setDesc("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "from", getFrom());
        _s_.appendInt(_pn_ + "code", getCode());
        _s_.appendString(_pn_ + "desc", getDesc());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "from", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "code", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "desc", "string", "", ""));
        return _v_;
    }

// linkd to client
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -947669033141460287L;

    public static final int FromLink = 0; // code字段见下面Code开头的枚举
    public static final int FromProvider = 1; // code字段见BKick里定义的Error开头的枚举
    public static final int FromDynamicModule = 2; // code字段是moduleId
    public static final int CodeMuteKick = 0; // 只断客户端连接，不发送消息给客户端，用于重连时确保旧的连接快速断开
    public static final int CodeNotAuthed = 1;
    public static final int CodeNoProvider = 2;
    public static final int CodeProviderBusy = 3;
    public static final int CodeProviderBroken = 4; // link跟provider断开,跟此provider静态绑定的客户端需要收到此协议执行重新登录流程

    private int _from; // FromLink, FromProvider, or FromDynamicModule
    private int _code;
    private String _desc;

    public int getFrom() {
        return _from;
    }

    public void setFrom(int _v_) {
        _from = _v_;
    }

    public int getCode() {
        return _code;
    }

    public void setCode(int _v_) {
        _code = _v_;
    }

    public String getDesc() {
        return _desc;
    }

    public void setDesc(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _desc = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _desc = "";
    }

    @SuppressWarnings("deprecation")
    public Data(int _from_, int _code_, String _desc_) {
        _from = _from_;
        _code = _code_;
        if (_desc_ == null)
            _desc_ = "";
        _desc = _desc_;
    }

    @Override
    public void reset() {
        _from = 0;
        _code = 0;
        _desc = "";
    }

    @Override
    public Zeze.Builtin.LinkdBase.BReportError toBean() {
        var _b_ = new Zeze.Builtin.LinkdBase.BReportError();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BReportError)_o_);
    }

    public void assign(BReportError _o_) {
        _from = _o_.getFrom();
        _code = _o_.getCode();
        _desc = _o_.getDesc();
    }

    public void assign(BReportError.Data _o_) {
        _from = _o_._from;
        _code = _o_._code;
        _desc = _o_._desc;
    }

    @Override
    public BReportError.Data copy() {
        var _c_ = new BReportError.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BReportError.Data _a_, BReportError.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BReportError.Data clone() {
        return (BReportError.Data)super.clone();
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
        _s_.append("Zeze.Builtin.LinkdBase.BReportError: {\n");
        _s_.append(_i1_).append("from=").append(_from).append(",\n");
        _s_.append(_i1_).append("code=").append(_code).append(",\n");
        _s_.append(_i1_).append("desc=").append(_desc).append('\n');
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
            int _x_ = _from;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _code;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = _desc;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            _from = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _code = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _desc = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
