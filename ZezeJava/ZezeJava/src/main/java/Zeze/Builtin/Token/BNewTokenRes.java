// auto-generated @formatter:off
package Zeze.Builtin.Token;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BNewTokenRes extends Zeze.Transaction.Bean implements BNewTokenResReadOnly {
    public static final long TYPEID = 4376622442286005260L;

    private String _token; // 新分配的token. RPC回复成功时有效,否则返回空串. 目前的设计是24个半角字符(大小写字母和数字组合)

    @Override
    public String getToken() {
        if (!isManaged())
            return _token;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _token;
        var log = (Log__token)txn.getLog(objectId() + 1);
        return log != null ? log.value : _token;
    }

    public void setToken(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _token = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__token(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BNewTokenRes() {
        _token = "";
    }

    @SuppressWarnings("deprecation")
    public BNewTokenRes(String _token_) {
        if (_token_ == null)
            _token_ = "";
        _token = _token_;
    }

    @Override
    public void reset() {
        setToken("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Token.BNewTokenRes.Data toData() {
        var data = new Zeze.Builtin.Token.BNewTokenRes.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Token.BNewTokenRes.Data)other);
    }

    public void assign(BNewTokenRes.Data other) {
        setToken(other._token);
        _unknown_ = null;
    }

    public void assign(BNewTokenRes other) {
        setToken(other.getToken());
        _unknown_ = other._unknown_;
    }

    public BNewTokenRes copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BNewTokenRes copy() {
        var copy = new BNewTokenRes();
        copy.assign(this);
        return copy;
    }

    public static void swap(BNewTokenRes a, BNewTokenRes b) {
        BNewTokenRes save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__token extends Zeze.Transaction.Logs.LogString {
        public Log__token(BNewTokenRes bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BNewTokenRes)getBelong())._token = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Token.BNewTokenRes: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("token=").append(getToken()).append(System.lineSeparator());
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
            String _x_ = getToken();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
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
            setToken(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
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
                case 1: _token = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setToken(rs.getString(_parents_name_ + "token"));
        if (getToken() == null)
            setToken("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "token", getToken());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "token", "string", "", ""));
        return vars;
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4376622442286005260L;

    private String _token; // 新分配的token. RPC回复成功时有效,否则返回空串. 目前的设计是24个半角字符(大小写字母和数字组合)

    public String getToken() {
        return _token;
    }

    public void setToken(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _token = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _token = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _token_) {
        if (_token_ == null)
            _token_ = "";
        _token = _token_;
    }

    @Override
    public void reset() {
        _token = "";
    }

    @Override
    public Zeze.Builtin.Token.BNewTokenRes toBean() {
        var bean = new Zeze.Builtin.Token.BNewTokenRes();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BNewTokenRes)other);
    }

    public void assign(BNewTokenRes other) {
        _token = other.getToken();
    }

    public void assign(BNewTokenRes.Data other) {
        _token = other._token;
    }

    @Override
    public BNewTokenRes.Data copy() {
        var copy = new BNewTokenRes.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BNewTokenRes.Data a, BNewTokenRes.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BNewTokenRes.Data clone() {
        return (BNewTokenRes.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Token.BNewTokenRes: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("token=").append(_token).append(System.lineSeparator());
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
            String _x_ = _token;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _token = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
