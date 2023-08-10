// auto-generated @formatter:off
package Zeze.Builtin.Token;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BGetTokenArg extends Zeze.Transaction.Bean implements BGetTokenArgReadOnly {
    public static final long TYPEID = 3072398246496713168L;

    private String _token; // 请求token
    private long _maxCount; // 此值＞0且请求token次数(包括当前请求)≥此值时,服务器会清除此token及绑定的所有状态(当前请求仍然能正常获得)

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

    @Override
    public long getMaxCount() {
        if (!isManaged())
            return _maxCount;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _maxCount;
        var log = (Log__maxCount)txn.getLog(objectId() + 2);
        return log != null ? log.value : _maxCount;
    }

    public void setMaxCount(long value) {
        if (!isManaged()) {
            _maxCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__maxCount(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BGetTokenArg() {
        _token = "";
    }

    @SuppressWarnings("deprecation")
    public BGetTokenArg(String _token_, long _maxCount_) {
        if (_token_ == null)
            _token_ = "";
        _token = _token_;
        _maxCount = _maxCount_;
    }

    @Override
    public void reset() {
        setToken("");
        setMaxCount(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Token.BGetTokenArg.Data toData() {
        var data = new Zeze.Builtin.Token.BGetTokenArg.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Token.BGetTokenArg.Data)other);
    }

    public void assign(BGetTokenArg.Data other) {
        setToken(other._token);
        setMaxCount(other._maxCount);
        _unknown_ = null;
    }

    public void assign(BGetTokenArg other) {
        setToken(other.getToken());
        setMaxCount(other.getMaxCount());
        _unknown_ = other._unknown_;
    }

    public BGetTokenArg copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BGetTokenArg copy() {
        var copy = new BGetTokenArg();
        copy.assign(this);
        return copy;
    }

    public static void swap(BGetTokenArg a, BGetTokenArg b) {
        BGetTokenArg save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__token extends Zeze.Transaction.Logs.LogString {
        public Log__token(BGetTokenArg bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BGetTokenArg)getBelong())._token = value; }
    }

    private static final class Log__maxCount extends Zeze.Transaction.Logs.LogLong {
        public Log__maxCount(BGetTokenArg bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BGetTokenArg)getBelong())._maxCount = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Token.BGetTokenArg: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("token=").append(getToken()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("maxCount=").append(getMaxCount()).append(System.lineSeparator());
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
        {
            long _x_ = getMaxCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
        if (_i_ == 2) {
            setMaxCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getMaxCount() < 0)
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
                case 1: _token = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _maxCount = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setToken(rs.getString(_parents_name_ + "token"));
        if (getToken() == null)
            setToken("");
        setMaxCount(rs.getLong(_parents_name_ + "maxCount"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "token", getToken());
        st.appendLong(_parents_name_ + "maxCount", getMaxCount());
    }

    @Override
    public java.util.List<Zeze.Transaction.Bean.Variable> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Transaction.Bean.Variable(1, "token", "string", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(2, "maxCount", "long", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BGetTokenArg
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 3072398246496713168L;

    private String _token; // 请求token
    private long _maxCount; // 此值＞0且请求token次数(包括当前请求)≥此值时,服务器会清除此token及绑定的所有状态(当前请求仍然能正常获得)

    public String getToken() {
        return _token;
    }

    public void setToken(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _token = value;
    }

    public long getMaxCount() {
        return _maxCount;
    }

    public void setMaxCount(long value) {
        _maxCount = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _token = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _token_, long _maxCount_) {
        if (_token_ == null)
            _token_ = "";
        _token = _token_;
        _maxCount = _maxCount_;
    }

    @Override
    public void reset() {
        _token = "";
        _maxCount = 0;
    }

    @Override
    public Zeze.Builtin.Token.BGetTokenArg toBean() {
        var bean = new Zeze.Builtin.Token.BGetTokenArg();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BGetTokenArg)other);
    }

    public void assign(BGetTokenArg other) {
        _token = other.getToken();
        _maxCount = other.getMaxCount();
    }

    public void assign(BGetTokenArg.Data other) {
        _token = other._token;
        _maxCount = other._maxCount;
    }

    @Override
    public BGetTokenArg.Data copy() {
        var copy = new BGetTokenArg.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BGetTokenArg.Data a, BGetTokenArg.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BGetTokenArg.Data clone() {
        return (BGetTokenArg.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Token.BGetTokenArg: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("token=").append(_token).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("maxCount=").append(_maxCount).append(System.lineSeparator());
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
        {
            long _x_ = _maxCount;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
        if (_i_ == 2) {
            _maxCount = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
