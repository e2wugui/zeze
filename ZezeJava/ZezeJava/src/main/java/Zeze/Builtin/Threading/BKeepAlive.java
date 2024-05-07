// auto-generated @formatter:off
package Zeze.Builtin.Threading;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BKeepAlive extends Zeze.Transaction.Bean implements BKeepAliveReadOnly {
    public static final long TYPEID = -6747942781414109078L;

    private int _ServerId;
    private long _AppSerialId;

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
    public long getAppSerialId() {
        if (!isManaged())
            return _AppSerialId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _AppSerialId;
        var log = (Log__AppSerialId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _AppSerialId;
    }

    public void setAppSerialId(long value) {
        if (!isManaged()) {
            _AppSerialId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__AppSerialId(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BKeepAlive() {
    }

    @SuppressWarnings("deprecation")
    public BKeepAlive(int _ServerId_, long _AppSerialId_) {
        _ServerId = _ServerId_;
        _AppSerialId = _AppSerialId_;
    }

    @Override
    public void reset() {
        setServerId(0);
        setAppSerialId(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Threading.BKeepAlive.Data toData() {
        var data = new Zeze.Builtin.Threading.BKeepAlive.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Threading.BKeepAlive.Data)other);
    }

    public void assign(BKeepAlive.Data other) {
        setServerId(other._ServerId);
        setAppSerialId(other._AppSerialId);
        _unknown_ = null;
    }

    public void assign(BKeepAlive other) {
        setServerId(other.getServerId());
        setAppSerialId(other.getAppSerialId());
        _unknown_ = other._unknown_;
    }

    public BKeepAlive copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BKeepAlive copy() {
        var copy = new BKeepAlive();
        copy.assign(this);
        return copy;
    }

    public static void swap(BKeepAlive a, BKeepAlive b) {
        BKeepAlive save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BKeepAlive bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BKeepAlive)getBelong())._ServerId = value; }
    }

    private static final class Log__AppSerialId extends Zeze.Transaction.Logs.LogLong {
        public Log__AppSerialId(BKeepAlive bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BKeepAlive)getBelong())._AppSerialId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Threading.BKeepAlive: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId=").append(getServerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("AppSerialId=").append(getAppSerialId()).append(System.lineSeparator());
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
            long _x_ = getAppSerialId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setAppSerialId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BKeepAlive))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BKeepAlive)_o_;
        if (getServerId() != _b_.getServerId())
            return false;
        if (getAppSerialId() != _b_.getAppSerialId())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getServerId() < 0)
            return true;
        if (getAppSerialId() < 0)
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
                case 1: _ServerId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _AppSerialId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setServerId(rs.getInt(_parents_name_ + "ServerId"));
        setAppSerialId(rs.getLong(_parents_name_ + "AppSerialId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "ServerId", getServerId());
        st.appendLong(_parents_name_ + "AppSerialId", getAppSerialId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServerId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "AppSerialId", "long", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -6747942781414109078L;

    private int _ServerId;
    private long _AppSerialId;

    public int getServerId() {
        return _ServerId;
    }

    public void setServerId(int value) {
        _ServerId = value;
    }

    public long getAppSerialId() {
        return _AppSerialId;
    }

    public void setAppSerialId(long value) {
        _AppSerialId = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(int _ServerId_, long _AppSerialId_) {
        _ServerId = _ServerId_;
        _AppSerialId = _AppSerialId_;
    }

    @Override
    public void reset() {
        _ServerId = 0;
        _AppSerialId = 0;
    }

    @Override
    public Zeze.Builtin.Threading.BKeepAlive toBean() {
        var bean = new Zeze.Builtin.Threading.BKeepAlive();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BKeepAlive)other);
    }

    public void assign(BKeepAlive other) {
        _ServerId = other.getServerId();
        _AppSerialId = other.getAppSerialId();
    }

    public void assign(BKeepAlive.Data other) {
        _ServerId = other._ServerId;
        _AppSerialId = other._AppSerialId;
    }

    @Override
    public BKeepAlive.Data copy() {
        var copy = new BKeepAlive.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BKeepAlive.Data a, BKeepAlive.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BKeepAlive.Data clone() {
        return (BKeepAlive.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Threading.BKeepAlive: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId=").append(_ServerId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("AppSerialId=").append(_AppSerialId).append(System.lineSeparator());
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
            int _x_ = _ServerId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = _AppSerialId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _ServerId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _AppSerialId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
