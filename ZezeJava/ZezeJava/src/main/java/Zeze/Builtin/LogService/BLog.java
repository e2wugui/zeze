// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BLog extends Zeze.Transaction.Bean implements BLogReadOnly {
    public static final long TYPEID = 3900400357954919579L;

    private long _Time;
    private String _Log;

    @Override
    public long getTime() {
        if (!isManaged())
            return _Time;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Time;
        var log = (Log__Time)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Time;
    }

    public void setTime(long value) {
        if (!isManaged()) {
            _Time = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Time(this, 1, value));
    }

    @Override
    public String getLog() {
        if (!isManaged())
            return _Log;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Log;
        var log = (Log__Log)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Log;
    }

    public void setLog(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Log = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Log(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BLog() {
        _Log = "";
    }

    @SuppressWarnings("deprecation")
    public BLog(long _Time_, String _Log_) {
        _Time = _Time_;
        if (_Log_ == null)
            _Log_ = "";
        _Log = _Log_;
    }

    @Override
    public void reset() {
        setTime(0);
        setLog("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LogService.BLog.Data toData() {
        var data = new Zeze.Builtin.LogService.BLog.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.LogService.BLog.Data)other);
    }

    public void assign(BLog.Data other) {
        setTime(other._Time);
        setLog(other._Log);
        _unknown_ = null;
    }

    public void assign(BLog other) {
        setTime(other.getTime());
        setLog(other.getLog());
        _unknown_ = other._unknown_;
    }

    public BLog copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLog copy() {
        var copy = new BLog();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLog a, BLog b) {
        BLog save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Time extends Zeze.Transaction.Logs.LogLong {
        public Log__Time(BLog bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLog)getBelong())._Time = value; }
    }

    private static final class Log__Log extends Zeze.Transaction.Logs.LogString {
        public Log__Log(BLog bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLog)getBelong())._Log = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BLog: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Time=").append(getTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Log=").append(getLog()).append(System.lineSeparator());
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
            long _x_ = getTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getLog();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
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
            setTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLog(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getTime() < 0)
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
                case 1: _Time = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _Log = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTime(rs.getLong(_parents_name_ + "Time"));
        setLog(rs.getString(_parents_name_ + "Log"));
        if (getLog() == null)
            setLog("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "Time", getTime());
        st.appendString(_parents_name_ + "Log", getLog());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Time", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Log", "string", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 3900400357954919579L;

    private long _Time;
    private String _Log;

    public long getTime() {
        return _Time;
    }

    public void setTime(long value) {
        _Time = value;
    }

    public String getLog() {
        return _Log;
    }

    public void setLog(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Log = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Log = "";
    }

    @SuppressWarnings("deprecation")
    public Data(long _Time_, String _Log_) {
        _Time = _Time_;
        if (_Log_ == null)
            _Log_ = "";
        _Log = _Log_;
    }

    @Override
    public void reset() {
        _Time = 0;
        _Log = "";
    }

    @Override
    public Zeze.Builtin.LogService.BLog toBean() {
        var bean = new Zeze.Builtin.LogService.BLog();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BLog)other);
    }

    public void assign(BLog other) {
        _Time = other.getTime();
        _Log = other.getLog();
    }

    public void assign(BLog.Data other) {
        _Time = other._Time;
        _Log = other._Log;
    }

    @Override
    public BLog.Data copy() {
        var copy = new BLog.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLog.Data a, BLog.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLog.Data clone() {
        return (BLog.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BLog: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Time=").append(_Time).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Log=").append(_Log).append(System.lineSeparator());
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
            long _x_ = _Time;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = _Log;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
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
            _Time = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Log = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
