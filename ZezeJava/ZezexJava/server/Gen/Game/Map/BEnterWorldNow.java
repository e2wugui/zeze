// auto-generated @formatter:off
package Game.Map;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BEnterWorldNow extends Zeze.Transaction.Bean implements BEnterWorldNowReadOnly {
    public static final long TYPEID = 2861757223843224449L;

    private int _MapInstanceId;
    private float _x;
    private float _y;
    private float _z;
    private int _ResouceId;

    private static final java.lang.invoke.VarHandle vh_MapInstanceId;
    private static final java.lang.invoke.VarHandle vh_x;
    private static final java.lang.invoke.VarHandle vh_y;
    private static final java.lang.invoke.VarHandle vh_z;
    private static final java.lang.invoke.VarHandle vh_ResouceId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_MapInstanceId = _l_.findVarHandle(BEnterWorldNow.class, "_MapInstanceId", int.class);
            vh_x = _l_.findVarHandle(BEnterWorldNow.class, "_x", float.class);
            vh_y = _l_.findVarHandle(BEnterWorldNow.class, "_y", float.class);
            vh_z = _l_.findVarHandle(BEnterWorldNow.class, "_z", float.class);
            vh_ResouceId = _l_.findVarHandle(BEnterWorldNow.class, "_ResouceId", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getMapInstanceId() {
        if (!isManaged())
            return _MapInstanceId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _MapInstanceId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _MapInstanceId;
    }

    public void setMapInstanceId(int _v_) {
        if (!isManaged()) {
            _MapInstanceId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_MapInstanceId, _v_));
    }

    @Override
    public float getX() {
        if (!isManaged())
            return _x;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _x;
        var log = (Zeze.Transaction.Logs.LogFloat)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _x;
    }

    public void setX(float _v_) {
        if (!isManaged()) {
            _x = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogFloat(this, 2, vh_x, _v_));
    }

    @Override
    public float getY() {
        if (!isManaged())
            return _y;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _y;
        var log = (Zeze.Transaction.Logs.LogFloat)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _y;
    }

    public void setY(float _v_) {
        if (!isManaged()) {
            _y = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogFloat(this, 3, vh_y, _v_));
    }

    @Override
    public float getZ() {
        if (!isManaged())
            return _z;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _z;
        var log = (Zeze.Transaction.Logs.LogFloat)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _z;
    }

    public void setZ(float _v_) {
        if (!isManaged()) {
            _z = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogFloat(this, 4, vh_z, _v_));
    }

    @Override
    public int getResouceId() {
        if (!isManaged())
            return _ResouceId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ResouceId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _ResouceId;
    }

    public void setResouceId(int _v_) {
        if (!isManaged()) {
            _ResouceId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 5, vh_ResouceId, _v_));
    }

    @SuppressWarnings("deprecation")
    public BEnterWorldNow() {
    }

    @SuppressWarnings("deprecation")
    public BEnterWorldNow(int _MapInstanceId_, float _x_, float _y_, float _z_, int _ResouceId_) {
        _MapInstanceId = _MapInstanceId_;
        _x = _x_;
        _y = _y_;
        _z = _z_;
        _ResouceId = _ResouceId_;
    }

    @Override
    public void reset() {
        setMapInstanceId(0);
        setX(0);
        setY(0);
        setZ(0);
        setResouceId(0);
        _unknown_ = null;
    }

    public void assign(BEnterWorldNow _o_) {
        setMapInstanceId(_o_.getMapInstanceId());
        setX(_o_.getX());
        setY(_o_.getY());
        setZ(_o_.getZ());
        setResouceId(_o_.getResouceId());
        _unknown_ = _o_._unknown_;
    }

    public BEnterWorldNow copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BEnterWorldNow copy() {
        var _c_ = new BEnterWorldNow();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BEnterWorldNow _a_, BEnterWorldNow _b_) {
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
        _s_.append("Game.Map.BEnterWorldNow: {\n");
        _s_.append(_i1_).append("MapInstanceId=").append(getMapInstanceId()).append(",\n");
        _s_.append(_i1_).append("x=").append(getX()).append(",\n");
        _s_.append(_i1_).append("y=").append(getY()).append(",\n");
        _s_.append(_i1_).append("z=").append(getZ()).append(",\n");
        _s_.append(_i1_).append("ResouceId=").append(getResouceId()).append('\n');
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

    private byte @Nullable [] _unknown_;

    public byte @Nullable [] unknown() {
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
            int _x_ = getMapInstanceId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            float _x_ = getX();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.FLOAT);
                _o_.WriteFloat(_x_);
            }
        }
        {
            float _x_ = getY();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.FLOAT);
                _o_.WriteFloat(_x_);
            }
        }
        {
            float _x_ = getZ();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.FLOAT);
                _o_.WriteFloat(_x_);
            }
        }
        {
            int _x_ = getResouceId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
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
            setMapInstanceId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setX(_o_.ReadFloat(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setY(_o_.ReadFloat(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setZ(_o_.ReadFloat(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setResouceId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BEnterWorldNow))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BEnterWorldNow)_o_;
        if (getMapInstanceId() != _b_.getMapInstanceId())
            return false;
        if (getX() != _b_.getX())
            return false;
        if (getY() != _b_.getY())
            return false;
        if (getZ() != _b_.getZ())
            return false;
        if (getResouceId() != _b_.getResouceId())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getMapInstanceId() < 0)
            return true;
        if (getResouceId() < 0)
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
                case 1: _MapInstanceId = _v_.intValue(); break;
                case 2: _x = _v_.floatValue(); break;
                case 3: _y = _v_.floatValue(); break;
                case 4: _z = _v_.floatValue(); break;
                case 5: _ResouceId = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setMapInstanceId(_r_.getInt(_pn_ + "MapInstanceId"));
        setX(_r_.getFloat(_pn_ + "x"));
        setY(_r_.getFloat(_pn_ + "y"));
        setZ(_r_.getFloat(_pn_ + "z"));
        setResouceId(_r_.getInt(_pn_ + "ResouceId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "MapInstanceId", getMapInstanceId());
        _s_.appendFloat(_pn_ + "x", getX());
        _s_.appendFloat(_pn_ + "y", getY());
        _s_.appendFloat(_pn_ + "z", getZ());
        _s_.appendInt(_pn_ + "ResouceId", getResouceId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "MapInstanceId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "x", "float", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "y", "float", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "z", "float", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "ResouceId", "int", "", ""));
        return _v_;
    }
}
