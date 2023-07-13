// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BEnterConfirm extends Zeze.Transaction.Bean implements BEnterConfirmReadOnly {
    public static final long TYPEID = 7682260201605989500L;

    private long _MapInstanceId;
    private long _EntityId; // 自己的实体Id，来自BEnterWorld。

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
    public long getEntityId() {
        if (!isManaged())
            return _EntityId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _EntityId;
        var log = (Log__EntityId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _EntityId;
    }

    public void setEntityId(long value) {
        if (!isManaged()) {
            _EntityId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__EntityId(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BEnterConfirm() {
    }

    @SuppressWarnings("deprecation")
    public BEnterConfirm(long _MapInstanceId_, long _EntityId_) {
        _MapInstanceId = _MapInstanceId_;
        _EntityId = _EntityId_;
    }

    @Override
    public void reset() {
        setMapInstanceId(0);
        setEntityId(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.World.BEnterConfirm.Data toData() {
        var data = new Zeze.Builtin.World.BEnterConfirm.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BEnterConfirm.Data)other);
    }

    public void assign(BEnterConfirm.Data other) {
        setMapInstanceId(other._MapInstanceId);
        setEntityId(other._EntityId);
        _unknown_ = null;
    }

    public void assign(BEnterConfirm other) {
        setMapInstanceId(other.getMapInstanceId());
        setEntityId(other.getEntityId());
        _unknown_ = other._unknown_;
    }

    public BEnterConfirm copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BEnterConfirm copy() {
        var copy = new BEnterConfirm();
        copy.assign(this);
        return copy;
    }

    public static void swap(BEnterConfirm a, BEnterConfirm b) {
        BEnterConfirm save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__MapInstanceId extends Zeze.Transaction.Logs.LogLong {
        public Log__MapInstanceId(BEnterConfirm bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BEnterConfirm)getBelong())._MapInstanceId = value; }
    }

    private static final class Log__EntityId extends Zeze.Transaction.Logs.LogLong {
        public Log__EntityId(BEnterConfirm bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BEnterConfirm)getBelong())._EntityId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BEnterConfirm: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MapInstanceId=").append(getMapInstanceId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EntityId=").append(getEntityId()).append(System.lineSeparator());
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
            long _x_ = getMapInstanceId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getEntityId();
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
            setMapInstanceId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setEntityId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getMapInstanceId() < 0)
            return true;
        if (getEntityId() < 0)
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
                case 2: _EntityId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setMapInstanceId(rs.getLong(_parents_name_ + "MapInstanceId"));
        setEntityId(rs.getLong(_parents_name_ + "EntityId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "MapInstanceId", getMapInstanceId());
        st.appendLong(_parents_name_ + "EntityId", getEntityId());
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7682260201605989500L;

    private long _MapInstanceId;
    private long _EntityId; // 自己的实体Id，来自BEnterWorld。

    public long getMapInstanceId() {
        return _MapInstanceId;
    }

    public void setMapInstanceId(long value) {
        _MapInstanceId = value;
    }

    public long getEntityId() {
        return _EntityId;
    }

    public void setEntityId(long value) {
        _EntityId = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(long _MapInstanceId_, long _EntityId_) {
        _MapInstanceId = _MapInstanceId_;
        _EntityId = _EntityId_;
    }

    @Override
    public void reset() {
        _MapInstanceId = 0;
        _EntityId = 0;
    }

    @Override
    public Zeze.Builtin.World.BEnterConfirm toBean() {
        var bean = new Zeze.Builtin.World.BEnterConfirm();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BEnterConfirm)other);
    }

    public void assign(BEnterConfirm other) {
        _MapInstanceId = other.getMapInstanceId();
        _EntityId = other.getEntityId();
    }

    public void assign(BEnterConfirm.Data other) {
        _MapInstanceId = other._MapInstanceId;
        _EntityId = other._EntityId;
    }

    @Override
    public BEnterConfirm.Data copy() {
        var copy = new BEnterConfirm.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BEnterConfirm.Data a, BEnterConfirm.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BEnterConfirm.Data clone() {
        return (BEnterConfirm.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BEnterConfirm: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MapInstanceId=").append(_MapInstanceId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EntityId=").append(_EntityId).append(System.lineSeparator());
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
            long _x_ = _EntityId;
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
            _MapInstanceId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _EntityId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
