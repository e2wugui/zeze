// auto-generated @formatter:off
package metagame.builtin.World;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// MoveMmo
@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BMove extends Zeze.Transaction.Bean implements BMoveReadOnly {
    public static final long TYPEID = 4435091272522095506L;

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

    private static final java.lang.invoke.VarHandle vh_Position;
    private static final java.lang.invoke.VarHandle vh_Direct;
    private static final java.lang.invoke.VarHandle vh_State;
    private static final java.lang.invoke.VarHandle vh_Control;
    private static final java.lang.invoke.VarHandle vh_Timestamp;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Position = _l_.findVarHandle(BMove.class, "_Position", Zeze.Serialize.Vector3.class);
            vh_Direct = _l_.findVarHandle(BMove.class, "_Direct", Zeze.Serialize.Vector3.class);
            vh_State = _l_.findVarHandle(BMove.class, "_State", int.class);
            vh_Control = _l_.findVarHandle(BMove.class, "_Control", int.class);
            vh_Timestamp = _l_.findVarHandle(BMove.class, "_Timestamp", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public Zeze.Serialize.Vector3 getPosition() {
        if (!isManaged())
            return _Position;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Position;
        var log = (Zeze.Transaction.Logs.LogVector3)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Position;
    }

    public void setPosition(Zeze.Serialize.Vector3 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Position = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogVector3(this, 1, vh_Position, _v_));
    }

    @Override
    public Zeze.Serialize.Vector3 getDirect() {
        if (!isManaged())
            return _Direct;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Direct;
        var log = (Zeze.Transaction.Logs.LogVector3)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Direct;
    }

    public void setDirect(Zeze.Serialize.Vector3 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Direct = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogVector3(this, 2, vh_Direct, _v_));
    }

    @Override
    public int getState() {
        if (!isManaged())
            return _State;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _State;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _State;
    }

    public void setState(int _v_) {
        if (!isManaged()) {
            _State = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 3, vh_State, _v_));
    }

    @Override
    public int getControl() {
        if (!isManaged())
            return _Control;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Control;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _Control;
    }

    public void setControl(int _v_) {
        if (!isManaged()) {
            _Control = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 4, vh_Control, _v_));
    }

    @Override
    public long getTimestamp() {
        if (!isManaged())
            return _Timestamp;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Timestamp;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _Timestamp;
    }

    public void setTimestamp(long _v_) {
        if (!isManaged()) {
            _Timestamp = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 5, vh_Timestamp, _v_));
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
    public metagame.builtin.World.BMove.Data toData() {
        var _d_ = new metagame.builtin.World.BMove.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((metagame.builtin.World.BMove.Data)_o_);
    }

    public void assign(BMove.Data _o_) {
        setPosition(_o_._Position);
        setDirect(_o_._Direct);
        setState(_o_._State);
        setControl(_o_._Control);
        setTimestamp(_o_._Timestamp);
        _unknown_ = null;
    }

    public void assign(BMove _o_) {
        setPosition(_o_.getPosition());
        setDirect(_o_.getDirect());
        setState(_o_.getState());
        setControl(_o_.getControl());
        setTimestamp(_o_.getTimestamp());
        _unknown_ = _o_._unknown_;
    }

    public BMove copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BMove copy() {
        var _c_ = new BMove();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMove _a_, BMove _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("metagame.builtin.World.BMove: {\n");
        _s_.append(_i1_).append("Position=").append(getPosition()).append(",\n");
        _s_.append(_i1_).append("Direct=").append(getDirect()).append(",\n");
        _s_.append(_i1_).append("State=").append(getState()).append(",\n");
        _s_.append(_i1_).append("Control=").append(getControl()).append(",\n");
        _s_.append(_i1_).append("Timestamp=").append(getTimestamp()).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
    public void decode(IByteBuffer _o_) {
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
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BMove))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMove)_o_;
        if (!getPosition().equals(_b_.getPosition()))
            return false;
        if (!getDirect().equals(_b_.getDirect()))
            return false;
        if (getState() != _b_.getState())
            return false;
        if (getControl() != _b_.getControl())
            return false;
        if (getTimestamp() != _b_.getTimestamp())
            return false;
        return true;
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
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _Position = _v_.vector3Value(); break;
                case 2: _Direct = _v_.vector3Value(); break;
                case 3: _State = _v_.intValue(); break;
                case 4: _Control = _v_.intValue(); break;
                case 5: _Timestamp = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        _p_.add("Position");
        setPosition(Zeze.Serialize.Helper.decodeVector3(_p_, _r_));
        _p_.remove(_p_.size() - 1);
        _p_.add("Direct");
        setDirect(Zeze.Serialize.Helper.decodeVector3(_p_, _r_));
        _p_.remove(_p_.size() - 1);
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setState(_r_.getInt(_pn_ + "State"));
        setControl(_r_.getInt(_pn_ + "Control"));
        setTimestamp(_r_.getLong(_pn_ + "Timestamp"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        _p_.add("Position");
        Zeze.Serialize.Helper.encodeVector3(getPosition(), _p_, _s_);
        _p_.remove(_p_.size() - 1);
        _p_.add("Direct");
        Zeze.Serialize.Helper.encodeVector3(getDirect(), _p_, _s_);
        _p_.remove(_p_.size() - 1);
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "State", getState());
        _s_.appendInt(_pn_ + "Control", getControl());
        _s_.appendLong(_pn_ + "Timestamp", getTimestamp());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Position", "vector3", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Direct", "vector3", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "State", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Control", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Timestamp", "long", "", ""));
        return _v_;
    }

// MoveMmo
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4435091272522095506L;

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

    public void setPosition(Zeze.Serialize.Vector3 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Position = _v_;
    }

    public Zeze.Serialize.Vector3 getDirect() {
        return _Direct;
    }

    public void setDirect(Zeze.Serialize.Vector3 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Direct = _v_;
    }

    public int getState() {
        return _State;
    }

    public void setState(int _v_) {
        _State = _v_;
    }

    public int getControl() {
        return _Control;
    }

    public void setControl(int _v_) {
        _Control = _v_;
    }

    public long getTimestamp() {
        return _Timestamp;
    }

    public void setTimestamp(long _v_) {
        _Timestamp = _v_;
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
    public metagame.builtin.World.BMove toBean() {
        var _b_ = new metagame.builtin.World.BMove();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BMove)_o_);
    }

    public void assign(BMove _o_) {
        _Position = _o_.getPosition();
        _Direct = _o_.getDirect();
        _State = _o_.getState();
        _Control = _o_.getControl();
        _Timestamp = _o_.getTimestamp();
    }

    public void assign(BMove.Data _o_) {
        _Position = _o_._Position;
        _Direct = _o_._Direct;
        _State = _o_._State;
        _Control = _o_._Control;
        _Timestamp = _o_._Timestamp;
    }

    @Override
    public BMove.Data copy() {
        var _c_ = new BMove.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMove.Data _a_, BMove.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
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
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("metagame.builtin.World.BMove: {\n");
        _s_.append(_i1_).append("Position=").append(_Position).append(",\n");
        _s_.append(_i1_).append("Direct=").append(_Direct).append(",\n");
        _s_.append(_i1_).append("State=").append(_State).append(",\n");
        _s_.append(_i1_).append("Control=").append(_Control).append(",\n");
        _s_.append(_i1_).append("Timestamp=").append(_Timestamp).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
    public void decode(IByteBuffer _o_) {
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

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BMove.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMove.Data)_o_;
        if (!_Position.equals(_b_._Position))
            return false;
        if (!_Direct.equals(_b_._Direct))
            return false;
        if (_State != _b_._State)
            return false;
        if (_Control != _b_._Control)
            return false;
        if (_Timestamp != _b_._Timestamp)
            return false;
        return true;
    }
}
}
