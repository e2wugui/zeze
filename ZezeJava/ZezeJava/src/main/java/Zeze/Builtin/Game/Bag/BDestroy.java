// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BDestroy extends Zeze.Transaction.Bean implements BDestroyReadOnly {
    public static final long TYPEID = -3139270057603893776L;

    private String _BagName;
    private int _Position;

    private static final java.lang.invoke.VarHandle vh_BagName;
    private static final java.lang.invoke.VarHandle vh_Position;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_BagName = _l_.findVarHandle(BDestroy.class, "_BagName", String.class);
            vh_Position = _l_.findVarHandle(BDestroy.class, "_Position", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getBagName() {
        if (!isManaged())
            return _BagName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _BagName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _BagName;
    }

    public void setBagName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _BagName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_BagName, _v_));
    }

    @Override
    public int getPosition() {
        if (!isManaged())
            return _Position;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Position;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Position;
    }

    public void setPosition(int _v_) {
        if (!isManaged()) {
            _Position = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_Position, _v_));
    }

    @SuppressWarnings("deprecation")
    public BDestroy() {
        _BagName = "";
    }

    @SuppressWarnings("deprecation")
    public BDestroy(String _BagName_, int _Position_) {
        if (_BagName_ == null)
            _BagName_ = "";
        _BagName = _BagName_;
        _Position = _Position_;
    }

    @Override
    public void reset() {
        setBagName("");
        setPosition(0);
        _unknown_ = null;
    }

    public void assign(BDestroy _o_) {
        setBagName(_o_.getBagName());
        setPosition(_o_.getPosition());
        _unknown_ = _o_._unknown_;
    }

    public BDestroy copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDestroy copy() {
        var _c_ = new BDestroy();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BDestroy _a_, BDestroy _b_) {
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
        _s_.append("Zeze.Builtin.Game.Bag.BDestroy: {\n");
        _s_.append(_i1_).append("BagName=").append(getBagName()).append(",\n");
        _s_.append(_i1_).append("Position=").append(getPosition()).append('\n');
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
            String _x_ = getBagName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getPosition();
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
            setBagName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPosition(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BDestroy))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BDestroy)_o_;
        if (!getBagName().equals(_b_.getBagName()))
            return false;
        if (getPosition() != _b_.getPosition())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getPosition() < 0)
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
                case 1: _BagName = _v_.stringValue(); break;
                case 2: _Position = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setBagName(_r_.getString(_pn_ + "BagName"));
        if (getBagName() == null)
            setBagName("");
        setPosition(_r_.getInt(_pn_ + "Position"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "BagName", getBagName());
        _s_.appendInt(_pn_ + "Position", getPosition());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "BagName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Position", "int", "", ""));
        return _v_;
    }
}
