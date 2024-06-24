// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 一个timer的信息
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BIndex extends Zeze.Transaction.Bean implements BIndexReadOnly {
    public static final long TYPEID = 8921847554177605341L;

    private int _ServerId; // 所属的serverId
    private long _NodeId; // 所属的节点ID
    private long _SerialId; // timer系列号，用来区分是否新注册的。
    private long _Version; // 当前timer所属server的版本, 如果当前server的版本更新,可以修改其所属

    @Override
    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServerId;
        var log = (Log__ServerId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int value) {
        if (!isManaged()) {
            _ServerId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServerId(this, 1, value));
    }

    @Override
    public long getNodeId() {
        if (!isManaged())
            return _NodeId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NodeId;
        var log = (Log__NodeId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _NodeId;
    }

    public void setNodeId(long value) {
        if (!isManaged()) {
            _NodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__NodeId(this, 2, value));
    }

    @Override
    public long getSerialId() {
        if (!isManaged())
            return _SerialId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SerialId;
        var log = (Log__SerialId)txn.getLog(objectId() + 3);
        return log != null ? log.value : _SerialId;
    }

    public void setSerialId(long value) {
        if (!isManaged()) {
            _SerialId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__SerialId(this, 3, value));
    }

    @Override
    public long getVersion() {
        if (!isManaged())
            return _Version;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Version;
        var log = (Log__Version)txn.getLog(objectId() + 4);
        return log != null ? log.value : _Version;
    }

    public void setVersion(long value) {
        if (!isManaged()) {
            _Version = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Version(this, 4, value));
    }

    @SuppressWarnings("deprecation")
    public BIndex() {
    }

    @SuppressWarnings("deprecation")
    public BIndex(int _ServerId_, long _NodeId_, long _SerialId_, long _Version_) {
        _ServerId = _ServerId_;
        _NodeId = _NodeId_;
        _SerialId = _SerialId_;
        _Version = _Version_;
    }

    @Override
    public void reset() {
        setServerId(0);
        setNodeId(0);
        setSerialId(0);
        setVersion(0);
        _unknown_ = null;
    }

    public void assign(BIndex other) {
        setServerId(other.getServerId());
        setNodeId(other.getNodeId());
        setSerialId(other.getSerialId());
        setVersion(other.getVersion());
        _unknown_ = other._unknown_;
    }

    public BIndex copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BIndex copy() {
        var copy = new BIndex();
        copy.assign(this);
        return copy;
    }

    public static void swap(BIndex a, BIndex b) {
        BIndex save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BIndex bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BIndex)getBelong())._ServerId = value; }
    }

    private static final class Log__NodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__NodeId(BIndex bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BIndex)getBelong())._NodeId = value; }
    }

    private static final class Log__SerialId extends Zeze.Transaction.Logs.LogLong {
        public Log__SerialId(BIndex bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BIndex)getBelong())._SerialId = value; }
    }

    private static final class Log__Version extends Zeze.Transaction.Logs.LogLong {
        public Log__Version(BIndex bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BIndex)getBelong())._Version = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BIndex: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId=").append(getServerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NodeId=").append(getNodeId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SerialId=").append(getSerialId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Version=").append(getVersion()).append(System.lineSeparator());
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
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getSerialId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setSerialId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BIndex))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BIndex)_o_;
        if (getServerId() != _b_.getServerId())
            return false;
        if (getNodeId() != _b_.getNodeId())
            return false;
        if (getSerialId() != _b_.getSerialId())
            return false;
        if (getVersion() != _b_.getVersion())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getServerId() < 0)
            return true;
        if (getNodeId() < 0)
            return true;
        if (getSerialId() < 0)
            return true;
        if (getVersion() < 0)
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
                case 1: _ServerId = vlog.intValue(); break;
                case 2: _NodeId = vlog.longValue(); break;
                case 3: _SerialId = vlog.longValue(); break;
                case 4: _Version = vlog.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setServerId(rs.getInt(_parents_name_ + "ServerId"));
        setNodeId(rs.getLong(_parents_name_ + "NodeId"));
        setSerialId(rs.getLong(_parents_name_ + "SerialId"));
        setVersion(rs.getLong(_parents_name_ + "Version"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "ServerId", getServerId());
        st.appendLong(_parents_name_ + "NodeId", getNodeId());
        st.appendLong(_parents_name_ + "SerialId", getSerialId());
        st.appendLong(_parents_name_ + "Version", getVersion());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServerId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "NodeId", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "SerialId", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Version", "long", "", ""));
        return vars;
    }
}
