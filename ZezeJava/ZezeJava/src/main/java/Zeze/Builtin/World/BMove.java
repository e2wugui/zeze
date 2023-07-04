// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BMove extends Zeze.Transaction.Bean implements BMoveReadOnly {
    public static final long TYPEID = 5823156345754273331L;

    private Zeze.Serialize.Vector3 _Position; // 移动命令时客户端的真实位置。
    private Zeze.Serialize.Vector3 _Direct; // 移动命令时客户端真实的朝向。
    private int _Command; // 0 直线，1 后退，2 左转，3 右转，4 停止
    private long _Timestamp; // 命令发起时刻的时戳。

    @Override
    public Zeze.Serialize.Vector3 getPosition() {
        if (!isManaged())
            return _Position;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Position;
        var log = (Log__Position)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Position;
    }

    public void setPosition(Zeze.Serialize.Vector3 value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Position = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Position(this, 1, value));
    }

    @Override
    public Zeze.Serialize.Vector3 getDirect() {
        if (!isManaged())
            return _Direct;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Direct;
        var log = (Log__Direct)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Direct;
    }

    public void setDirect(Zeze.Serialize.Vector3 value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Direct = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Direct(this, 2, value));
    }

    @Override
    public int getCommand() {
        if (!isManaged())
            return _Command;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Command;
        var log = (Log__Command)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Command;
    }

    public void setCommand(int value) {
        if (!isManaged()) {
            _Command = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Command(this, 3, value));
    }

    @Override
    public long getTimestamp() {
        if (!isManaged())
            return _Timestamp;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Timestamp;
        var log = (Log__Timestamp)txn.getLog(objectId() + 4);
        return log != null ? log.value : _Timestamp;
    }

    public void setTimestamp(long value) {
        if (!isManaged()) {
            _Timestamp = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Timestamp(this, 4, value));
    }

    @SuppressWarnings("deprecation")
    public BMove() {
        _Position = Zeze.Serialize.Vector3.ZERO;
        _Direct = Zeze.Serialize.Vector3.ZERO;
    }

    @SuppressWarnings("deprecation")
    public BMove(Zeze.Serialize.Vector3 _Position_, Zeze.Serialize.Vector3 _Direct_, int _Command_, long _Timestamp_) {
        if (_Position_ == null)
            _Position_ = Zeze.Serialize.Vector3.ZERO;
        _Position = _Position_;
        if (_Direct_ == null)
            _Direct_ = Zeze.Serialize.Vector3.ZERO;
        _Direct = _Direct_;
        _Command = _Command_;
        _Timestamp = _Timestamp_;
    }

    @Override
    public Zeze.Builtin.World.BMove.Data toData() {
        var data = new Zeze.Builtin.World.BMove.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BMove.Data)other);
    }

    public void assign(BMove.Data other) {
        setPosition(other._Position);
        setDirect(other._Direct);
        setCommand(other._Command);
        setTimestamp(other._Timestamp);
    }

    public void assign(BMove other) {
        setPosition(other.getPosition());
        setDirect(other.getDirect());
        setCommand(other.getCommand());
        setTimestamp(other.getTimestamp());
    }

    public BMove copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BMove copy() {
        var copy = new BMove();
        copy.assign(this);
        return copy;
    }

    public static void swap(BMove a, BMove b) {
        BMove save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Position extends Zeze.Transaction.Logs.LogVector3 {
        public Log__Position(BMove bean, int varId, Zeze.Serialize.Vector3 value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BMove)getBelong())._Position = value; }
    }

    private static final class Log__Direct extends Zeze.Transaction.Logs.LogVector3 {
        public Log__Direct(BMove bean, int varId, Zeze.Serialize.Vector3 value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BMove)getBelong())._Direct = value; }
    }

    private static final class Log__Command extends Zeze.Transaction.Logs.LogInt {
        public Log__Command(BMove bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BMove)getBelong())._Command = value; }
    }

    private static final class Log__Timestamp extends Zeze.Transaction.Logs.LogLong {
        public Log__Timestamp(BMove bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BMove)getBelong())._Timestamp = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BMove: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Position=").append(getPosition()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Direct=").append(getDirect()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Command=").append(getCommand()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Timestamp=").append(getTimestamp()).append(System.lineSeparator());
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
            var _x_ = getPosition();
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            var _x_ = getDirect();
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            int _x_ = getCommand();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getTimestamp();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
            setPosition(_o_.ReadVector3(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setDirect(_o_.ReadVector3(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setCommand(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setTimestamp(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getCommand() < 0)
            return true;
        if (getTimestamp() < 0)
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
                case 1: _Position = ((Zeze.Transaction.Logs.LogVector3)vlog).value; break;
                case 2: _Direct = ((Zeze.Transaction.Logs.LogVector3)vlog).value; break;
                case 3: _Command = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 4: _Timestamp = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        parents.add("Position");
        setPosition(Zeze.Serialize.Helper.decodeVector3(parents, rs));
        parents.remove(parents.size() - 1);
        parents.add("Direct");
        setDirect(Zeze.Serialize.Helper.decodeVector3(parents, rs));
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setCommand(rs.getInt(_parents_name_ + "Command"));
        setTimestamp(rs.getLong(_parents_name_ + "Timestamp"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        parents.add("Position");
        Zeze.Serialize.Helper.encodeVector3(getPosition(), parents, st);
        parents.remove(parents.size() - 1);
        parents.add("Direct");
        Zeze.Serialize.Helper.encodeVector3(getDirect(), parents, st);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "Command", getCommand());
        st.appendLong(_parents_name_ + "Timestamp", getTimestamp());
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5823156345754273331L;

    private Zeze.Serialize.Vector3 _Position; // 移动命令时客户端的真实位置。
    private Zeze.Serialize.Vector3 _Direct; // 移动命令时客户端真实的朝向。
    private int _Command; // 0 直线，1 后退，2 左转，3 右转，4 停止
    private long _Timestamp; // 命令发起时刻的时戳。

    public Zeze.Serialize.Vector3 getPosition() {
        return _Position;
    }

    public void setPosition(Zeze.Serialize.Vector3 value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Position = value;
    }

    public Zeze.Serialize.Vector3 getDirect() {
        return _Direct;
    }

    public void setDirect(Zeze.Serialize.Vector3 value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Direct = value;
    }

    public int getCommand() {
        return _Command;
    }

    public void setCommand(int value) {
        _Command = value;
    }

    public long getTimestamp() {
        return _Timestamp;
    }

    public void setTimestamp(long value) {
        _Timestamp = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Position = Zeze.Serialize.Vector3.ZERO;
        _Direct = Zeze.Serialize.Vector3.ZERO;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Serialize.Vector3 _Position_, Zeze.Serialize.Vector3 _Direct_, int _Command_, long _Timestamp_) {
        if (_Position_ == null)
            _Position_ = Zeze.Serialize.Vector3.ZERO;
        _Position = _Position_;
        if (_Direct_ == null)
            _Direct_ = Zeze.Serialize.Vector3.ZERO;
        _Direct = _Direct_;
        _Command = _Command_;
        _Timestamp = _Timestamp_;
    }

    @Override
    public Zeze.Builtin.World.BMove toBean() {
        var bean = new Zeze.Builtin.World.BMove();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BMove)other);
    }

    public void assign(BMove other) {
        _Position = other.getPosition();
        _Direct = other.getDirect();
        _Command = other.getCommand();
        _Timestamp = other.getTimestamp();
    }

    public void assign(BMove.Data other) {
        _Position = other._Position;
        _Direct = other._Direct;
        _Command = other._Command;
        _Timestamp = other._Timestamp;
    }

    @Override
    public BMove.Data copy() {
        var copy = new BMove.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BMove.Data a, BMove.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BMove.Data clone() {
        return (BMove.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BMove: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Position=").append(_Position).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Direct=").append(_Direct).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Command=").append(_Command).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Timestamp=").append(_Timestamp).append(System.lineSeparator());
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
            var _x_ = _Position;
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            var _x_ = _Direct;
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            int _x_ = _Command;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = _Timestamp;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
            _Position = _o_.ReadVector3(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Direct = _o_.ReadVector3(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Command = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _Timestamp = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
