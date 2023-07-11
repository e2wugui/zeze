// auto-generated @formatter:off
package Zeze.Builtin.World.Static;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BSwitchWorldResult extends Zeze.Transaction.Bean implements BSwitchWorldResultReadOnly {
    public static final long TYPEID = 5224332116004611997L;

    private long _MapInstanceId;
    private int _ServerId; // 目前没有逻辑作用，用来调试。

    @Override
    public long getMapInstanceId() {
        if (!isManaged())
            return _MapInstanceId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _MapInstanceId;
        var log = (Log__MapInstanceId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _MapInstanceId;
    }

    public void setMapInstanceId(long value) {
        if (!isManaged()) {
            _MapInstanceId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__MapInstanceId(this, 1, value));
    }

    @Override
    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServerId;
        var log = (Log__ServerId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int value) {
        if (!isManaged()) {
            _ServerId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServerId(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BSwitchWorldResult() {
    }

    @SuppressWarnings("deprecation")
    public BSwitchWorldResult(long _MapInstanceId_, int _ServerId_) {
        _MapInstanceId = _MapInstanceId_;
        _ServerId = _ServerId_;
    }

    @Override
    public void reset() {
        setMapInstanceId(0);
        setServerId(0);
    }

    @Override
    public Zeze.Builtin.World.Static.BSwitchWorldResult.Data toData() {
        var data = new Zeze.Builtin.World.Static.BSwitchWorldResult.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.Static.BSwitchWorldResult.Data)other);
    }

    public void assign(BSwitchWorldResult.Data other) {
        setMapInstanceId(other._MapInstanceId);
        setServerId(other._ServerId);
    }

    public void assign(BSwitchWorldResult other) {
        setMapInstanceId(other.getMapInstanceId());
        setServerId(other.getServerId());
    }

    public BSwitchWorldResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSwitchWorldResult copy() {
        var copy = new BSwitchWorldResult();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSwitchWorldResult a, BSwitchWorldResult b) {
        BSwitchWorldResult save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__MapInstanceId extends Zeze.Transaction.Logs.LogLong {
        public Log__MapInstanceId(BSwitchWorldResult bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSwitchWorldResult)getBelong())._MapInstanceId = value; }
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BSwitchWorldResult bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSwitchWorldResult)getBelong())._ServerId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.Static.BSwitchWorldResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MapInstanceId=").append(getMapInstanceId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId=").append(getServerId()).append(System.lineSeparator());
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
            long _x_ = getMapInstanceId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setMapInstanceId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getMapInstanceId() < 0)
            return true;
        if (getServerId() < 0)
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
                case 1: _MapInstanceId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _ServerId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setMapInstanceId(rs.getLong(_parents_name_ + "MapInstanceId"));
        setServerId(rs.getInt(_parents_name_ + "ServerId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "MapInstanceId", getMapInstanceId());
        st.appendInt(_parents_name_ + "ServerId", getServerId());
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5224332116004611997L;

    private long _MapInstanceId;
    private int _ServerId; // 目前没有逻辑作用，用来调试。

    public long getMapInstanceId() {
        return _MapInstanceId;
    }

    public void setMapInstanceId(long value) {
        _MapInstanceId = value;
    }

    public int getServerId() {
        return _ServerId;
    }

    public void setServerId(int value) {
        _ServerId = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(long _MapInstanceId_, int _ServerId_) {
        _MapInstanceId = _MapInstanceId_;
        _ServerId = _ServerId_;
    }

    @Override
    public void reset() {
        _MapInstanceId = 0;
        _ServerId = 0;
    }

    @Override
    public Zeze.Builtin.World.Static.BSwitchWorldResult toBean() {
        var bean = new Zeze.Builtin.World.Static.BSwitchWorldResult();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BSwitchWorldResult)other);
    }

    public void assign(BSwitchWorldResult other) {
        _MapInstanceId = other.getMapInstanceId();
        _ServerId = other.getServerId();
    }

    public void assign(BSwitchWorldResult.Data other) {
        _MapInstanceId = other._MapInstanceId;
        _ServerId = other._ServerId;
    }

    @Override
    public BSwitchWorldResult.Data copy() {
        var copy = new BSwitchWorldResult.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSwitchWorldResult.Data a, BSwitchWorldResult.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSwitchWorldResult.Data clone() {
        return (BSwitchWorldResult.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.Static.BSwitchWorldResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MapInstanceId=").append(_MapInstanceId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId=").append(_ServerId).append(System.lineSeparator());
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
            long _x_ = _MapInstanceId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = _ServerId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _MapInstanceId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ServerId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
