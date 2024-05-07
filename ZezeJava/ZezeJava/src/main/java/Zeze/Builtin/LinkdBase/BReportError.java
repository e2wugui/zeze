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
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _from;
        var log = (Log__from)txn.getLog(objectId() + 1);
        return log != null ? log.value : _from;
    }

    public void setFrom(int value) {
        if (!isManaged()) {
            _from = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__from(this, 1, value));
    }

    @Override
    public int getCode() {
        if (!isManaged())
            return _code;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _code;
        var log = (Log__code)txn.getLog(objectId() + 2);
        return log != null ? log.value : _code;
    }

    public void setCode(int value) {
        if (!isManaged()) {
            _code = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__code(this, 2, value));
    }

    @Override
    public String getDesc() {
        if (!isManaged())
            return _desc;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _desc;
        var log = (Log__desc)txn.getLog(objectId() + 3);
        return log != null ? log.value : _desc;
    }

    public void setDesc(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _desc = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__desc(this, 3, value));
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
        var data = new Zeze.Builtin.LinkdBase.BReportError.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.LinkdBase.BReportError.Data)other);
    }

    public void assign(BReportError.Data other) {
        setFrom(other._from);
        setCode(other._code);
        setDesc(other._desc);
        _unknown_ = null;
    }

    public void assign(BReportError other) {
        setFrom(other.getFrom());
        setCode(other.getCode());
        setDesc(other.getDesc());
        _unknown_ = other._unknown_;
    }

    public BReportError copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BReportError copy() {
        var copy = new BReportError();
        copy.assign(this);
        return copy;
    }

    public static void swap(BReportError a, BReportError b) {
        BReportError save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__from extends Zeze.Transaction.Logs.LogInt {
        public Log__from(BReportError bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReportError)getBelong())._from = value; }
    }

    private static final class Log__code extends Zeze.Transaction.Logs.LogInt {
        public Log__code(BReportError bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReportError)getBelong())._code = value; }
    }

    private static final class Log__desc extends Zeze.Transaction.Logs.LogString {
        public Log__desc(BReportError bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReportError)getBelong())._desc = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LinkdBase.BReportError: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("from=").append(getFrom()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("code=").append(getCode()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("desc=").append(getDesc()).append(System.lineSeparator());
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
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _from = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _code = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _desc = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setFrom(rs.getInt(_parents_name_ + "from"));
        setCode(rs.getInt(_parents_name_ + "code"));
        setDesc(rs.getString(_parents_name_ + "desc"));
        if (getDesc() == null)
            setDesc("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "from", getFrom());
        st.appendInt(_parents_name_ + "code", getCode());
        st.appendString(_parents_name_ + "desc", getDesc());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "from", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "code", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "desc", "string", "", ""));
        return vars;
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

    public void setFrom(int value) {
        _from = value;
    }

    public int getCode() {
        return _code;
    }

    public void setCode(int value) {
        _code = value;
    }

    public String getDesc() {
        return _desc;
    }

    public void setDesc(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _desc = value;
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
        var bean = new Zeze.Builtin.LinkdBase.BReportError();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BReportError)other);
    }

    public void assign(BReportError other) {
        _from = other.getFrom();
        _code = other.getCode();
        _desc = other.getDesc();
    }

    public void assign(BReportError.Data other) {
        _from = other._from;
        _code = other._code;
        _desc = other._desc;
    }

    @Override
    public BReportError.Data copy() {
        var copy = new BReportError.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BReportError.Data a, BReportError.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
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
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LinkdBase.BReportError: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("from=").append(_from).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("code=").append(_code).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("desc=").append(_desc).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

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
