// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

// MoveMmo
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BMove extends Zeze.Transaction.Bean implements BMoveReadOnly {
    public static final long TYPEID = 5823156345754273331L;

    public static final int eStateMask = 0xf;
    public static final int eStateStand = 0; // 站立，静止状态
    public static final int eStateSlide = 1; // 滑动（斜坡）这是一种失控状态，滑动方向由斜坡决定
    public static final int eStateFly = 2; // 空中允许转向
    public static final int eStateFlyLine = 3; // 空中不允许转向
    public static final int eStateSwim = 4; // 水面（游泳）
    public static final int eStateSwimUnderwater = 5; // 水中（游泳）
    public static final int eStateStandUnderwater = 6; // 水中（游泳）
    public static final int eControlMoveMask = 0x3;
    public static final int eControlMoveNone = 0;
    public static final int eControlMoveForward = 1;
    public static final int eControlMoveBack = 2;
    public static final int eControlTurnMask = 0xc;
    public static final int eControlTurnNone = 0;
    public static final int eControlTurnLeft = 4;
    public static final int eControlTurnRight = 8;

    private Zeze.Serialize.Vector3 _Position; // 命令时刻的客户端真实位置。
    private Zeze.Serialize.Vector3 _Direct; // 命令时刻的客户端真实朝向。
    private int _State; // 状态
    private int _Control; // 控制命令
    private long _Timestamp; // 命令时刻的时戳。

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
    public int getState() {
        if (!isManaged())
            return _State;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _State;
        var log = (Log__State)txn.getLog(objectId() + 3);
        return log != null ? log.value : _State;
    }

    public void setState(int value) {
        if (!isManaged()) {
            _State = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__State(this, 3, value));
    }

    @Override
    public int getControl() {
        if (!isManaged())
            return _Control;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Control;
        var log = (Log__Control)txn.getLog(objectId() + 4);
        return log != null ? log.value : _Control;
    }

    public void setControl(int value) {
        if (!isManaged()) {
            _Control = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Control(this, 4, value));
    }

    @Override
    public long getTimestamp() {
        if (!isManaged())
            return _Timestamp;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Timestamp;
        var log = (Log__Timestamp)txn.getLog(objectId() + 5);
        return log != null ? log.value : _Timestamp;
    }

    public void setTimestamp(long value) {
        if (!isManaged()) {
            _Timestamp = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Timestamp(this, 5, value));
    }

    @SuppressWarnings("deprecation")
    public BMove() {
        _Position = Zeze.Serialize.Vector3.ZERO;
        _Direct = Zeze.Serialize.Vector3.ZERO;
    }

    @SuppressWarnings("deprecation")
    public BMove(Zeze.Serialize.Vector3 _Position_, Zeze.Serialize.Vector3 _Direct_, int _State_, int _Control_, long _Timestamp_) {
        if (_Position_ == null)
            _Position_ = Zeze.Serialize.Vector3.ZERO;
        _Position = _Position_;
        if (_Direct_ == null)
            _Direct_ = Zeze.Serialize.Vector3.ZERO;
        _Direct = _Direct_;
        _State = _State_;
        _Control = _Control_;
        _Timestamp = _Timestamp_;
    }

    @Override
    public void reset() {
        setPosition(Zeze.Serialize.Vector3.ZERO);
        setDirect(Zeze.Serialize.Vector3.ZERO);
        setState(0);
        setControl(0);
        setTimestamp(0);
        _unknown_ = null;
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
        setState(other._State);
        setControl(other._Control);
        setTimestamp(other._Timestamp);
        _unknown_ = null;
    }

    public void assign(BMove other) {
        setPosition(other.getPosition());
        setDirect(other.getDirect());
        setState(other.getState());
        setControl(other.getControl());
        setTimestamp(other.getTimestamp());
        _unknown_ = other._unknown_;
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

    private static final class Log__State extends Zeze.Transaction.Logs.LogInt {
        public Log__State(BMove bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BMove)getBelong())._State = value; }
    }

    private static final class Log__Control extends Zeze.Transaction.Logs.LogInt {
        public Log__Control(BMove bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BMove)getBelong())._Control = value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("State=").append(getState()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Control=").append(getControl()).append(',').append(System.lineSeparator());
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
            int _x_ = getState();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getControl();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getTimestamp();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
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
            setPosition(_o_.ReadVector3(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setDirect(_o_.ReadVector3(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setState(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setControl(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setTimestamp(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getState() < 0)
            return true;
        if (getControl() < 0)
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
                case 3: _State = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 4: _Control = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 5: _Timestamp = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
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
        setState(rs.getInt(_parents_name_ + "State"));
        setControl(rs.getInt(_parents_name_ + "Control"));
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
        st.appendInt(_parents_name_ + "State", getState());
        st.appendInt(_parents_name_ + "Control", getControl());
        st.appendLong(_parents_name_ + "Timestamp", getTimestamp());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Position", "vector3", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Direct", "vector3", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "State", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Control", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Timestamp", "long", "", ""));
        return vars;
    }

// MoveMmo
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5823156345754273331L;

    public static final int eStateMask = 0xf;
    public static final int eStateStand = 0; // 站立，静止状态
    public static final int eStateSlide = 1; // 滑动（斜坡）这是一种失控状态，滑动方向由斜坡决定
    public static final int eStateFly = 2; // 空中允许转向
    public static final int eStateFlyLine = 3; // 空中不允许转向
    public static final int eStateSwim = 4; // 水面（游泳）
    public static final int eStateSwimUnderwater = 5; // 水中（游泳）
    public static final int eStateStandUnderwater = 6; // 水中（游泳）
    public static final int eControlMoveMask = 0x3;
    public static final int eControlMoveNone = 0;
    public static final int eControlMoveForward = 1;
    public static final int eControlMoveBack = 2;
    public static final int eControlTurnMask = 0xc;
    public static final int eControlTurnNone = 0;
    public static final int eControlTurnLeft = 4;
    public static final int eControlTurnRight = 8;

    private Zeze.Serialize.Vector3 _Position; // 命令时刻的客户端真实位置。
    private Zeze.Serialize.Vector3 _Direct; // 命令时刻的客户端真实朝向。
    private int _State; // 状态
    private int _Control; // 控制命令
    private long _Timestamp; // 命令时刻的时戳。

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

    public int getState() {
        return _State;
    }

    public void setState(int value) {
        _State = value;
    }

    public int getControl() {
        return _Control;
    }

    public void setControl(int value) {
        _Control = value;
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
    public Data(Zeze.Serialize.Vector3 _Position_, Zeze.Serialize.Vector3 _Direct_, int _State_, int _Control_, long _Timestamp_) {
        if (_Position_ == null)
            _Position_ = Zeze.Serialize.Vector3.ZERO;
        _Position = _Position_;
        if (_Direct_ == null)
            _Direct_ = Zeze.Serialize.Vector3.ZERO;
        _Direct = _Direct_;
        _State = _State_;
        _Control = _Control_;
        _Timestamp = _Timestamp_;
    }

    @Override
    public void reset() {
        _Position = Zeze.Serialize.Vector3.ZERO;
        _Direct = Zeze.Serialize.Vector3.ZERO;
        _State = 0;
        _Control = 0;
        _Timestamp = 0;
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
        _State = other.getState();
        _Control = other.getControl();
        _Timestamp = other.getTimestamp();
    }

    public void assign(BMove.Data other) {
        _Position = other._Position;
        _Direct = other._Direct;
        _State = other._State;
        _Control = other._Control;
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
        sb.append(Zeze.Util.Str.indent(level)).append("State=").append(_State).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Control=").append(_Control).append(',').append(System.lineSeparator());
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
            int _x_ = _State;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _Control;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = _Timestamp;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
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
            _State = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _Control = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
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
