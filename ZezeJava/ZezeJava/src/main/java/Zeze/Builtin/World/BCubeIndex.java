// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BCubeIndex extends Zeze.Transaction.Bean implements BCubeIndexReadOnly {
    public static final long TYPEID = 4059678951732915999L;

    private long _X;
    private long _Y;
    private long _Z;

    @Override
    public long getX() {
        if (!isManaged())
            return _X;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _X;
        var log = (Log__X)txn.getLog(objectId() + 1);
        return log != null ? log.value : _X;
    }

    public void setX(long value) {
        if (!isManaged()) {
            _X = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__X(this, 1, value));
    }

    @Override
    public long getY() {
        if (!isManaged())
            return _Y;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Y;
        var log = (Log__Y)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Y;
    }

    public void setY(long value) {
        if (!isManaged()) {
            _Y = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Y(this, 2, value));
    }

    @Override
    public long getZ() {
        if (!isManaged())
            return _Z;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Z;
        var log = (Log__Z)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Z;
    }

    public void setZ(long value) {
        if (!isManaged()) {
            _Z = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Z(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BCubeIndex() {
    }

    @SuppressWarnings("deprecation")
    public BCubeIndex(long _X_, long _Y_, long _Z_) {
        _X = _X_;
        _Y = _Y_;
        _Z = _Z_;
    }

    @Override
    public void reset() {
        setX(0);
        setY(0);
        setZ(0);
    }

    @Override
    public Zeze.Builtin.World.BCubeIndex.Data toData() {
        var data = new Zeze.Builtin.World.BCubeIndex.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BCubeIndex.Data)other);
    }

    public void assign(BCubeIndex.Data other) {
        setX(other._X);
        setY(other._Y);
        setZ(other._Z);
    }

    public void assign(BCubeIndex other) {
        setX(other.getX());
        setY(other.getY());
        setZ(other.getZ());
    }

    public BCubeIndex copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCubeIndex copy() {
        var copy = new BCubeIndex();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCubeIndex a, BCubeIndex b) {
        BCubeIndex save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__X extends Zeze.Transaction.Logs.LogLong {
        public Log__X(BCubeIndex bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCubeIndex)getBelong())._X = value; }
    }

    private static final class Log__Y extends Zeze.Transaction.Logs.LogLong {
        public Log__Y(BCubeIndex bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCubeIndex)getBelong())._Y = value; }
    }

    private static final class Log__Z extends Zeze.Transaction.Logs.LogLong {
        public Log__Z(BCubeIndex bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCubeIndex)getBelong())._Z = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BCubeIndex: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("X=").append(getX()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Y=").append(getY()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Z=").append(getZ()).append(System.lineSeparator());
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
            long _x_ = getX();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getY();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getZ();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            setX(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setY(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setZ(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getX() < 0)
            return true;
        if (getY() < 0)
            return true;
        if (getZ() < 0)
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
                case 1: _X = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _Y = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _Z = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setX(rs.getLong(_parents_name_ + "X"));
        setY(rs.getLong(_parents_name_ + "Y"));
        setZ(rs.getLong(_parents_name_ + "Z"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "X", getX());
        st.appendLong(_parents_name_ + "Y", getY());
        st.appendLong(_parents_name_ + "Z", getZ());
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4059678951732915999L;

    private long _X;
    private long _Y;
    private long _Z;

    public long getX() {
        return _X;
    }

    public void setX(long value) {
        _X = value;
    }

    public long getY() {
        return _Y;
    }

    public void setY(long value) {
        _Y = value;
    }

    public long getZ() {
        return _Z;
    }

    public void setZ(long value) {
        _Z = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(long _X_, long _Y_, long _Z_) {
        _X = _X_;
        _Y = _Y_;
        _Z = _Z_;
    }

    @Override
    public void reset() {
        _X = 0;
        _Y = 0;
        _Z = 0;
    }

    @Override
    public Zeze.Builtin.World.BCubeIndex toBean() {
        var bean = new Zeze.Builtin.World.BCubeIndex();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BCubeIndex)other);
    }

    public void assign(BCubeIndex other) {
        _X = other.getX();
        _Y = other.getY();
        _Z = other.getZ();
    }

    public void assign(BCubeIndex.Data other) {
        _X = other._X;
        _Y = other._Y;
        _Z = other._Z;
    }

    @Override
    public BCubeIndex.Data copy() {
        var copy = new BCubeIndex.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCubeIndex.Data a, BCubeIndex.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BCubeIndex.Data clone() {
        return (BCubeIndex.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BCubeIndex: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("X=").append(_X).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Y=").append(_Y).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Z=").append(_Z).append(System.lineSeparator());
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
            long _x_ = _X;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = _Y;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = _Z;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            _X = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Y = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Z = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
