// auto-generated @formatter:off
package Game.Map;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BEnterWorldDone extends Zeze.Transaction.Bean implements BEnterWorldDoneReadOnly {
    public static final long TYPEID = 3901261098602822919L;

    private int _MapInstanceId;

    private static final java.lang.invoke.VarHandle vh_MapInstanceId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_MapInstanceId = _l_.findVarHandle(BEnterWorldDone.class, "_MapInstanceId", int.class);
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

    @SuppressWarnings("deprecation")
    public BEnterWorldDone() {
    }

    @SuppressWarnings("deprecation")
    public BEnterWorldDone(int _MapInstanceId_) {
        _MapInstanceId = _MapInstanceId_;
    }

    @Override
    public void reset() {
        setMapInstanceId(0);
        _unknown_ = null;
    }

    public void assign(BEnterWorldDone _o_) {
        setMapInstanceId(_o_.getMapInstanceId());
        _unknown_ = _o_._unknown_;
    }

    public BEnterWorldDone copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BEnterWorldDone copy() {
        var _c_ = new BEnterWorldDone();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BEnterWorldDone _a_, BEnterWorldDone _b_) {
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
        _s_.append("Game.Map.BEnterWorldDone: {\n");
        _s_.append(_i1_).append("MapInstanceId=").append(getMapInstanceId()).append('\n');
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
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BEnterWorldDone))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BEnterWorldDone)_o_;
        if (getMapInstanceId() != _b_.getMapInstanceId())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getMapInstanceId() < 0)
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
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setMapInstanceId(_r_.getInt(_pn_ + "MapInstanceId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "MapInstanceId", getMapInstanceId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "MapInstanceId", "int", "", ""));
        return _v_;
    }
}
