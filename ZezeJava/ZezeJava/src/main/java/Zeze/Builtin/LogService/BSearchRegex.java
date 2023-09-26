// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BSearchRegex extends Zeze.Transaction.Bean implements BSearchRegexReadOnly {
    public static final long TYPEID = -7977842463281973232L;

    private long _BeginTime;
    private long _EndTime;
    private String _Pattern;

    @Override
    public long getBeginTime() {
        if (!isManaged())
            return _BeginTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _BeginTime;
        var log = (Log__BeginTime)txn.getLog(objectId() + 1);
        return log != null ? log.value : _BeginTime;
    }

    public void setBeginTime(long value) {
        if (!isManaged()) {
            _BeginTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__BeginTime(this, 1, value));
    }

    @Override
    public long getEndTime() {
        if (!isManaged())
            return _EndTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _EndTime;
        var log = (Log__EndTime)txn.getLog(objectId() + 2);
        return log != null ? log.value : _EndTime;
    }

    public void setEndTime(long value) {
        if (!isManaged()) {
            _EndTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__EndTime(this, 2, value));
    }

    @Override
    public String getPattern() {
        if (!isManaged())
            return _Pattern;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Pattern;
        var log = (Log__Pattern)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Pattern;
    }

    public void setPattern(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Pattern = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Pattern(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BSearchRegex() {
        _Pattern = "";
    }

    @SuppressWarnings("deprecation")
    public BSearchRegex(long _BeginTime_, long _EndTime_, String _Pattern_) {
        _BeginTime = _BeginTime_;
        _EndTime = _EndTime_;
        if (_Pattern_ == null)
            _Pattern_ = "";
        _Pattern = _Pattern_;
    }

    @Override
    public void reset() {
        setBeginTime(0);
        setEndTime(0);
        setPattern("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LogService.BSearchRegex.Data toData() {
        var data = new Zeze.Builtin.LogService.BSearchRegex.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.LogService.BSearchRegex.Data)other);
    }

    public void assign(BSearchRegex.Data other) {
        setBeginTime(other._BeginTime);
        setEndTime(other._EndTime);
        setPattern(other._Pattern);
        _unknown_ = null;
    }

    public void assign(BSearchRegex other) {
        setBeginTime(other.getBeginTime());
        setEndTime(other.getEndTime());
        setPattern(other.getPattern());
        _unknown_ = other._unknown_;
    }

    public BSearchRegex copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSearchRegex copy() {
        var copy = new BSearchRegex();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSearchRegex a, BSearchRegex b) {
        BSearchRegex save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__BeginTime extends Zeze.Transaction.Logs.LogLong {
        public Log__BeginTime(BSearchRegex bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSearchRegex)getBelong())._BeginTime = value; }
    }

    private static final class Log__EndTime extends Zeze.Transaction.Logs.LogLong {
        public Log__EndTime(BSearchRegex bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSearchRegex)getBelong())._EndTime = value; }
    }

    private static final class Log__Pattern extends Zeze.Transaction.Logs.LogString {
        public Log__Pattern(BSearchRegex bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSearchRegex)getBelong())._Pattern = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BSearchRegex: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("BeginTime=").append(getBeginTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EndTime=").append(getEndTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Pattern=").append(getPattern()).append(System.lineSeparator());
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
            long _x_ = getBeginTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getEndTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getPattern();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            setBeginTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setEndTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setPattern(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getBeginTime() < 0)
            return true;
        if (getEndTime() < 0)
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
                case 1: _BeginTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _EndTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _Pattern = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setBeginTime(rs.getLong(_parents_name_ + "BeginTime"));
        setEndTime(rs.getLong(_parents_name_ + "EndTime"));
        setPattern(rs.getString(_parents_name_ + "Pattern"));
        if (getPattern() == null)
            setPattern("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "BeginTime", getBeginTime());
        st.appendLong(_parents_name_ + "EndTime", getEndTime());
        st.appendString(_parents_name_ + "Pattern", getPattern());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "BeginTime", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "EndTime", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Pattern", "string", "", ""));
        return vars;
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -7977842463281973232L;

    private long _BeginTime;
    private long _EndTime;
    private String _Pattern;

    public long getBeginTime() {
        return _BeginTime;
    }

    public void setBeginTime(long value) {
        _BeginTime = value;
    }

    public long getEndTime() {
        return _EndTime;
    }

    public void setEndTime(long value) {
        _EndTime = value;
    }

    public String getPattern() {
        return _Pattern;
    }

    public void setPattern(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Pattern = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Pattern = "";
    }

    @SuppressWarnings("deprecation")
    public Data(long _BeginTime_, long _EndTime_, String _Pattern_) {
        _BeginTime = _BeginTime_;
        _EndTime = _EndTime_;
        if (_Pattern_ == null)
            _Pattern_ = "";
        _Pattern = _Pattern_;
    }

    @Override
    public void reset() {
        _BeginTime = 0;
        _EndTime = 0;
        _Pattern = "";
    }

    @Override
    public Zeze.Builtin.LogService.BSearchRegex toBean() {
        var bean = new Zeze.Builtin.LogService.BSearchRegex();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BSearchRegex)other);
    }

    public void assign(BSearchRegex other) {
        _BeginTime = other.getBeginTime();
        _EndTime = other.getEndTime();
        _Pattern = other.getPattern();
    }

    public void assign(BSearchRegex.Data other) {
        _BeginTime = other._BeginTime;
        _EndTime = other._EndTime;
        _Pattern = other._Pattern;
    }

    @Override
    public BSearchRegex.Data copy() {
        var copy = new BSearchRegex.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSearchRegex.Data a, BSearchRegex.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSearchRegex.Data clone() {
        return (BSearchRegex.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BSearchRegex: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("BeginTime=").append(_BeginTime).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EndTime=").append(_EndTime).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Pattern=").append(_Pattern).append(System.lineSeparator());
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
            long _x_ = _BeginTime;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = _EndTime;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = _Pattern;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            _BeginTime = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _EndTime = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Pattern = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
