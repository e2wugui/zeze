// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BAchillesHeelConfig extends Zeze.Transaction.Bean implements BAchillesHeelConfigReadOnly {
    public static final long TYPEID = 6351123425648255834L;

    private int _MaxNetPing;
    private int _ServerProcessTime;
    private int _ServerReleaseTimeout;

    @Override
    public int getMaxNetPing() {
        if (!isManaged())
            return _MaxNetPing;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _MaxNetPing;
        var log = (Log__MaxNetPing)txn.getLog(objectId() + 1);
        return log != null ? log.value : _MaxNetPing;
    }

    public void setMaxNetPing(int value) {
        if (!isManaged()) {
            _MaxNetPing = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__MaxNetPing(this, 1, value));
    }

    @Override
    public int getServerProcessTime() {
        if (!isManaged())
            return _ServerProcessTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServerProcessTime;
        var log = (Log__ServerProcessTime)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ServerProcessTime;
    }

    public void setServerProcessTime(int value) {
        if (!isManaged()) {
            _ServerProcessTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServerProcessTime(this, 2, value));
    }

    @Override
    public int getServerReleaseTimeout() {
        if (!isManaged())
            return _ServerReleaseTimeout;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServerReleaseTimeout;
        var log = (Log__ServerReleaseTimeout)txn.getLog(objectId() + 3);
        return log != null ? log.value : _ServerReleaseTimeout;
    }

    public void setServerReleaseTimeout(int value) {
        if (!isManaged()) {
            _ServerReleaseTimeout = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServerReleaseTimeout(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BAchillesHeelConfig() {
    }

    @SuppressWarnings("deprecation")
    public BAchillesHeelConfig(int _MaxNetPing_, int _ServerProcessTime_, int _ServerReleaseTimeout_) {
        _MaxNetPing = _MaxNetPing_;
        _ServerProcessTime = _ServerProcessTime_;
        _ServerReleaseTimeout = _ServerReleaseTimeout_;
    }

    @Override
    public void reset() {
        setMaxNetPing(0);
        setServerProcessTime(0);
        setServerReleaseTimeout(0);
        _unknown_ = null;
    }

    public void assign(BAchillesHeelConfig other) {
        setMaxNetPing(other.getMaxNetPing());
        setServerProcessTime(other.getServerProcessTime());
        setServerReleaseTimeout(other.getServerReleaseTimeout());
        _unknown_ = other._unknown_;
    }

    public BAchillesHeelConfig copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAchillesHeelConfig copy() {
        var copy = new BAchillesHeelConfig();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAchillesHeelConfig a, BAchillesHeelConfig b) {
        BAchillesHeelConfig save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__MaxNetPing extends Zeze.Transaction.Logs.LogInt {
        public Log__MaxNetPing(BAchillesHeelConfig bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAchillesHeelConfig)getBelong())._MaxNetPing = value; }
    }

    private static final class Log__ServerProcessTime extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerProcessTime(BAchillesHeelConfig bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAchillesHeelConfig)getBelong())._ServerProcessTime = value; }
    }

    private static final class Log__ServerReleaseTimeout extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerReleaseTimeout(BAchillesHeelConfig bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAchillesHeelConfig)getBelong())._ServerReleaseTimeout = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.GlobalCacheManagerWithRaft.BAchillesHeelConfig: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MaxNetPing=").append(getMaxNetPing()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServerProcessTime=").append(getServerProcessTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServerReleaseTimeout=").append(getServerReleaseTimeout()).append(System.lineSeparator());
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
            int _x_ = getMaxNetPing();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getServerProcessTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getServerReleaseTimeout();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            setMaxNetPing(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setServerProcessTime(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setServerReleaseTimeout(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getMaxNetPing() < 0)
            return true;
        if (getServerProcessTime() < 0)
            return true;
        if (getServerReleaseTimeout() < 0)
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
                case 1: _MaxNetPing = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _ServerProcessTime = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _ServerReleaseTimeout = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setMaxNetPing(rs.getInt(_parents_name_ + "MaxNetPing"));
        setServerProcessTime(rs.getInt(_parents_name_ + "ServerProcessTime"));
        setServerReleaseTimeout(rs.getInt(_parents_name_ + "ServerReleaseTimeout"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "MaxNetPing", getMaxNetPing());
        st.appendInt(_parents_name_ + "ServerProcessTime", getServerProcessTime());
        st.appendInt(_parents_name_ + "ServerReleaseTimeout", getServerReleaseTimeout());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "MaxNetPing", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ServerProcessTime", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ServerReleaseTimeout", "int", "", ""));
        return vars;
    }
}
