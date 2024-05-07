// auto-generated @formatter:off
package Zeze.Builtin.Token;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTokenStatus extends Zeze.Transaction.Bean implements BTokenStatusReadOnly {
    public static final long TYPEID = 3897751576670303080L;

    private long _newCount; // 已分配的token数量
    private long _curCount; // 当前有效的token数量
    private int _connectCount; // 当前的网络连接数量
    private String _perfLog; // 最近生成的性能日志

    @Override
    public long getNewCount() {
        if (!isManaged())
            return _newCount;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _newCount;
        var log = (Log__newCount)txn.getLog(objectId() + 1);
        return log != null ? log.value : _newCount;
    }

    public void setNewCount(long value) {
        if (!isManaged()) {
            _newCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__newCount(this, 1, value));
    }

    @Override
    public long getCurCount() {
        if (!isManaged())
            return _curCount;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _curCount;
        var log = (Log__curCount)txn.getLog(objectId() + 2);
        return log != null ? log.value : _curCount;
    }

    public void setCurCount(long value) {
        if (!isManaged()) {
            _curCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__curCount(this, 2, value));
    }

    @Override
    public int getConnectCount() {
        if (!isManaged())
            return _connectCount;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _connectCount;
        var log = (Log__connectCount)txn.getLog(objectId() + 3);
        return log != null ? log.value : _connectCount;
    }

    public void setConnectCount(int value) {
        if (!isManaged()) {
            _connectCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__connectCount(this, 3, value));
    }

    @Override
    public String getPerfLog() {
        if (!isManaged())
            return _perfLog;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _perfLog;
        var log = (Log__perfLog)txn.getLog(objectId() + 4);
        return log != null ? log.value : _perfLog;
    }

    public void setPerfLog(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _perfLog = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__perfLog(this, 4, value));
    }

    @SuppressWarnings("deprecation")
    public BTokenStatus() {
        _perfLog = "";
    }

    @SuppressWarnings("deprecation")
    public BTokenStatus(long _newCount_, long _curCount_, int _connectCount_, String _perfLog_) {
        _newCount = _newCount_;
        _curCount = _curCount_;
        _connectCount = _connectCount_;
        if (_perfLog_ == null)
            _perfLog_ = "";
        _perfLog = _perfLog_;
    }

    @Override
    public void reset() {
        setNewCount(0);
        setCurCount(0);
        setConnectCount(0);
        setPerfLog("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Token.BTokenStatus.Data toData() {
        var data = new Zeze.Builtin.Token.BTokenStatus.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Token.BTokenStatus.Data)other);
    }

    public void assign(BTokenStatus.Data other) {
        setNewCount(other._newCount);
        setCurCount(other._curCount);
        setConnectCount(other._connectCount);
        setPerfLog(other._perfLog);
        _unknown_ = null;
    }

    public void assign(BTokenStatus other) {
        setNewCount(other.getNewCount());
        setCurCount(other.getCurCount());
        setConnectCount(other.getConnectCount());
        setPerfLog(other.getPerfLog());
        _unknown_ = other._unknown_;
    }

    public BTokenStatus copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTokenStatus copy() {
        var copy = new BTokenStatus();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTokenStatus a, BTokenStatus b) {
        BTokenStatus save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__newCount extends Zeze.Transaction.Logs.LogLong {
        public Log__newCount(BTokenStatus bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTokenStatus)getBelong())._newCount = value; }
    }

    private static final class Log__curCount extends Zeze.Transaction.Logs.LogLong {
        public Log__curCount(BTokenStatus bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTokenStatus)getBelong())._curCount = value; }
    }

    private static final class Log__connectCount extends Zeze.Transaction.Logs.LogInt {
        public Log__connectCount(BTokenStatus bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTokenStatus)getBelong())._connectCount = value; }
    }

    private static final class Log__perfLog extends Zeze.Transaction.Logs.LogString {
        public Log__perfLog(BTokenStatus bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTokenStatus)getBelong())._perfLog = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Token.BTokenStatus: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("newCount=").append(getNewCount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("curCount=").append(getCurCount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("connectCount=").append(getConnectCount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("perfLog=").append(getPerfLog()).append(System.lineSeparator());
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
            long _x_ = getNewCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getCurCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getConnectCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getPerfLog();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
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
            setNewCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setCurCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setConnectCount(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setPerfLog(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTokenStatus))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTokenStatus)_o_;
        if (getNewCount() != _b_.getNewCount())
            return false;
        if (getCurCount() != _b_.getCurCount())
            return false;
        if (getConnectCount() != _b_.getConnectCount())
            return false;
        if (!getPerfLog().equals(_b_.getPerfLog()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getNewCount() < 0)
            return true;
        if (getCurCount() < 0)
            return true;
        if (getConnectCount() < 0)
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
                case 1: _newCount = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _curCount = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _connectCount = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 4: _perfLog = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setNewCount(rs.getLong(_parents_name_ + "newCount"));
        setCurCount(rs.getLong(_parents_name_ + "curCount"));
        setConnectCount(rs.getInt(_parents_name_ + "connectCount"));
        setPerfLog(rs.getString(_parents_name_ + "perfLog"));
        if (getPerfLog() == null)
            setPerfLog("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "newCount", getNewCount());
        st.appendLong(_parents_name_ + "curCount", getCurCount());
        st.appendInt(_parents_name_ + "connectCount", getConnectCount());
        st.appendString(_parents_name_ + "perfLog", getPerfLog());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "newCount", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "curCount", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "connectCount", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "perfLog", "string", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 3897751576670303080L;

    private long _newCount; // 已分配的token数量
    private long _curCount; // 当前有效的token数量
    private int _connectCount; // 当前的网络连接数量
    private String _perfLog; // 最近生成的性能日志

    public long getNewCount() {
        return _newCount;
    }

    public void setNewCount(long value) {
        _newCount = value;
    }

    public long getCurCount() {
        return _curCount;
    }

    public void setCurCount(long value) {
        _curCount = value;
    }

    public int getConnectCount() {
        return _connectCount;
    }

    public void setConnectCount(int value) {
        _connectCount = value;
    }

    public String getPerfLog() {
        return _perfLog;
    }

    public void setPerfLog(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _perfLog = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _perfLog = "";
    }

    @SuppressWarnings("deprecation")
    public Data(long _newCount_, long _curCount_, int _connectCount_, String _perfLog_) {
        _newCount = _newCount_;
        _curCount = _curCount_;
        _connectCount = _connectCount_;
        if (_perfLog_ == null)
            _perfLog_ = "";
        _perfLog = _perfLog_;
    }

    @Override
    public void reset() {
        _newCount = 0;
        _curCount = 0;
        _connectCount = 0;
        _perfLog = "";
    }

    @Override
    public Zeze.Builtin.Token.BTokenStatus toBean() {
        var bean = new Zeze.Builtin.Token.BTokenStatus();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BTokenStatus)other);
    }

    public void assign(BTokenStatus other) {
        _newCount = other.getNewCount();
        _curCount = other.getCurCount();
        _connectCount = other.getConnectCount();
        _perfLog = other.getPerfLog();
    }

    public void assign(BTokenStatus.Data other) {
        _newCount = other._newCount;
        _curCount = other._curCount;
        _connectCount = other._connectCount;
        _perfLog = other._perfLog;
    }

    @Override
    public BTokenStatus.Data copy() {
        var copy = new BTokenStatus.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTokenStatus.Data a, BTokenStatus.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BTokenStatus.Data clone() {
        return (BTokenStatus.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Token.BTokenStatus: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("newCount=").append(_newCount).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("curCount=").append(_curCount).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("connectCount=").append(_connectCount).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("perfLog=").append(_perfLog).append(System.lineSeparator());
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
            long _x_ = _newCount;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = _curCount;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = _connectCount;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = _perfLog;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
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
            _newCount = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _curCount = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _connectCount = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _perfLog = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
