// auto-generated @formatter:off
package demo.Game;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BSession extends Zeze.Transaction.Bean implements BSessionReadOnly {
    public static final long TYPEID = -2175683550594248654L;

    private long _LastActive;
    private int _RoomId;

    private static final java.lang.invoke.VarHandle vh_LastActive;
    private static final java.lang.invoke.VarHandle vh_RoomId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_LastActive = _l_.findVarHandle(BSession.class, "_LastActive", long.class);
            vh_RoomId = _l_.findVarHandle(BSession.class, "_RoomId", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getLastActive() {
        if (!isManaged())
            return _LastActive;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LastActive;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _LastActive;
    }

    public void setLastActive(long _v_) {
        if (!isManaged()) {
            _LastActive = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_LastActive, _v_));
    }

    @Override
    public int getRoomId() {
        if (!isManaged())
            return _RoomId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RoomId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _RoomId;
    }

    public void setRoomId(int _v_) {
        if (!isManaged()) {
            _RoomId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_RoomId, _v_));
    }

    @SuppressWarnings("deprecation")
    public BSession() {
    }

    @SuppressWarnings("deprecation")
    public BSession(long _LastActive_, int _RoomId_) {
        _LastActive = _LastActive_;
        _RoomId = _RoomId_;
    }

    @Override
    public void reset() {
        setLastActive(0);
        setRoomId(0);
        _unknown_ = null;
    }

    public void assign(BSession _o_) {
        setLastActive(_o_.getLastActive());
        setRoomId(_o_.getRoomId());
        _unknown_ = _o_._unknown_;
    }

    public BSession copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSession copy() {
        var _c_ = new BSession();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSession _a_, BSession _b_) {
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
        _s_.append("demo.Game.BSession: {\n");
        _s_.append(_i1_).append("LastActive=").append(getLastActive()).append(",\n");
        _s_.append(_i1_).append("RoomId=").append(getRoomId()).append('\n');
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
            long _x_ = getLastActive();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getRoomId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setLastActive(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setRoomId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BSession))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSession)_o_;
        if (getLastActive() != _b_.getLastActive())
            return false;
        if (getRoomId() != _b_.getRoomId())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getLastActive() < 0)
            return true;
        if (getRoomId() < 0)
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
                case 1: _LastActive = _v_.longValue(); break;
                case 2: _RoomId = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setLastActive(_r_.getLong(_pn_ + "LastActive"));
        setRoomId(_r_.getInt(_pn_ + "RoomId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "LastActive", getLastActive());
        _s_.appendInt(_pn_ + "RoomId", getRoomId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "LastActive", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "RoomId", "int", "", ""));
        return _v_;
    }
}
